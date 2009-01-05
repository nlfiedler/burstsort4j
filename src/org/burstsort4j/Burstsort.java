/*
 * Copyright (C) 2008  Nathan Fiedler
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id$
 */
package org.burstsort4j;

/**
 * Implementation of the original Burstsort, (arrays) version.
 *
 * @author Nathan Fiedler
 */
public class Burstsort {
    /** Size of the alphabet that is supported. */
    private static final short ALPHABET = 256;
    /** Null terminator character. */
    private static final char NULLTERM = '\0';
    /** Size of buckets. */
    private static final short THRESHOLD = 8192;
    /** Used to store reference to next bucket in last cell of bucket. */
    private static final short THRESHOLDMINUSONE = THRESHOLD - 1;
    /** Constants for growing the buckets. */
    private static final short[] bucket_inc = new short[] {
        (short) 0,
        (short) 16,
        (short) 128,
        (short) 1024,
        (short) 8192,
        (short) 16384,
        (short) 32768
    };

    /**
     * Retrieve the character in String s at offset d. If d is greater
     * than or equal to the length of the string, return zero. This
     * simulates fixed-length strings that are zero-padded.
     *
     * @param  s  string.
     * @param  d  offset.
     * @return  character in s at d, or zero.
     */
    private static final char charAt(CharSequence s, int d) {
        return d < s.length() ? s.charAt(d) : NULLTERM;
    }

    /**
     * Inserts a set of strings into the burst trie structure, in
     * preparation for in-order traversal (hence sorting).
     *
     * @param  root     root of the structure.
     * @param  strings  strings to be inserted.
     */
    private static void insert(BurstTrie root, String[] strings) {
        for (int i = 0; i < strings.length; i++) {
            // Start at root each time
            BurstTrie curr = root;
            // Locate trie node in which to insert string
            int p = 0;
            char c = charAt(strings[i], p);
            while (curr.counts[c] < 0) {
                curr = (BurstTrie) curr.ptrs[c];
                p++;
                c = charAt(strings[i], p);
            }
            curr.insert(c, strings[i]);
            if (curr.counts[c] > 0) {
                // is bucket size above the THRESHOLD?
                while (curr.counts[c] >= THRESHOLD && c != NULLTERM) {
                    // advance depth of character
                    p++;
                    // allocate memory for new trie node
                    BurstTrie newt = new BurstTrie();
                    // burst...
                    char cc = NULLTERM;
                    String[] ptrs = (String[]) curr.ptrs[c];
                    for (int j = 0; j < curr.counts[c]; j++) {
                        // access the next depth character
                        cc = charAt(ptrs[j], p);
                        newt.insert(cc, ptrs[j]);
                    }
                    // old pointer points to the new trie node
                    curr.ptrs[c] = newt;
                    // flag to indicate pointer to trie node and not bucket
                    curr.counts[c] = -1;
                    // used to burst recursive, so point curr to new
                    curr = newt;
                    // point to character used in previous string
                    c = cc;
                }
            }
        }
    }

    /**
     * Sorts the given set of strings using the original burstsort
     * algorithm.
     *
     * @param  strings  array of strings to be sorted.
     */
    public static void sort(String[] strings) {
        if (strings != null && strings.length > 1) {
            BurstTrie root = new BurstTrie();
            insert(root, strings);
            traverse(root, strings, 0, 0);
        }
    }

    /**
     * Traverse the trie structure, ordering the strings in the array to
     * conform to their lexicographically sorted order as determined by
     * the trie structure.
     *
     * @param  node     node within trie structure.
     * @param  strings  the strings to be ordered.
     * @param  pos      position within array.
     * @param  deep     character offset within strings.
     * @return  new pos value.
     */
    private static int traverse(BurstTrie node, String[] strings, int pos, int deep) {
        for (int i = 0; i < ALPHABET; i++) {
            int count = node.counts[i];
            if (++count == 0) {
                pos = traverse((BurstTrie) node.ptrs[i], strings, pos, deep + 1);
            } else if (--count != 0) {
                int off = pos;
                if (i == 0) {
                    // Visit all of the null buckets.
                    int no_of_buckets = (count / THRESHOLDMINUSONE) + 1;
                    Object[] nullbucket = (Object[]) node.ptrs[i];
                    // traverse all arrays in the bucket
                    for (int k = 1; k <= no_of_buckets; k++) {
                        int no_elements_in_bucket;
                        if (k == no_of_buckets) {
                            no_elements_in_bucket = count % THRESHOLDMINUSONE;
                        } else {
                            no_elements_in_bucket = THRESHOLDMINUSONE;
                        }
                        // traverse all elements in each bucket
                        int j = 0;
                        while (j < no_elements_in_bucket) {
                            strings[off++] = (String) nullbucket[j++];
                        }
                        nullbucket = (Object[]) nullbucket[j];
                    }
                } else {
                    // Visit all of the tail string buckets.
                    for (int j = 0; j < count; j++) {
                        String[] arr = (String[]) node.ptrs[i];
                        strings[off++] = arr[j];
                    }
                    if (count > 1) {
                        if (count < 20) {
                            Insertionsort.sort(strings, pos, off, deep + 1);
                        } else {
                            MultikeyQuicksort.mkqsort(strings, pos, off, deep + 1);
                        }
                    }
                }
                pos = off;
            }
        }
        return pos;
    }

    /**
     * A node in the burst trie structure.
     */
    private static class BurstTrie {
        /** Reference to the last null bucket in the chain, starting
         * from the reference in ptrs[0]. */
        private Object[] nulltailptr;
        /** last element in null bucket */
        private int nulltailidx;
        /** level counter of bucket size */
        private int[] levels = new int[ALPHABET];
        /** count of items in bucket */
        public int[] counts = new int[ALPHABET];
        /** pointers to buckets or trie node */
        public Object[] ptrs = new Object[ALPHABET];

        /**
         * Add the given string into the appropriate bucket, given the
         * character index into the trie. Presumably the character is
         * from the string, but not necessarily so. The character may
         * be the null character, in which case the string is added to
         * the null bucket. Buckets are expanded as needed to accomodate
         * the new string.
         *
         * @param  c  character used to index trie entry.
         * @param  s  the string to be inserted.
         */
        public void insert(char c, String s) {
            // are buckets already created?
            if (counts[c] < 1) {
                // create bucket
                if (c == NULLTERM) {
                    // allocate memory for the bucket
                    nulltailptr = new Object[THRESHOLD];
                    ptrs[c] = nulltailptr;
                    // point to the first cell of the bucket
                    nulltailidx = 0;
                    // insert the string
                    nulltailptr[nulltailidx] = s;
                    // point to next cell
                    nulltailidx++;
                    // increment count of items
                    counts[c]++;
                } else {
                    ptrs[c] = new String[bucket_inc[1]];
                    ((String[]) ptrs[c])[counts[c]++] = s;
                    levels[c]++;
                }
            } else {
                // bucket already created, insert string in bucket
                if (c == NULLTERM) {
                    // insert the string
                    nulltailptr[nulltailidx] = s;
                    // point to next cell
                    nulltailidx++;
                    // increment count of items
                    counts[c]++;
                    // check if the bucket is reaching the threshold
                    if (counts[c] % THRESHOLDMINUSONE == 0) {
                        // Grow the null bucket by daisy chaining a new array.
                        Object[] tmp = new Object[THRESHOLD];
                        nulltailptr[nulltailidx] = tmp;
                        // point to the first cell in the new array
                        nulltailptr = tmp;
                        nulltailidx = 0;
                    }
                } else {
                    // insert string in bucket and increment the item counter
                    ((String[]) ptrs[c])[counts[c]++] = s;
                    // Staggered Approach: if the size of the bucket is above
                    // level x, then realloc and increase the level count
                    // check for null string buckets as they are not to be
                    // incremented check if the number of items in the bucket
                    // is above a threshold.
                    if (counts[c] < THRESHOLD && counts[c] > (bucket_inc[levels[c]] - 1)) {
                        String[] temp = (String[]) ptrs[c];
                        ptrs[c] = new String[bucket_inc[++levels[c]]];
                        System.arraycopy(temp, 0, ptrs[c], 0, temp.length);
                    }
                }
            }
        }
    }
}
