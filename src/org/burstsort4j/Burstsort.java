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
            // are buckets already created?
            if (curr.counts[c] < 1) {
                // create bucket
                if (c == NULLTERM) {
                    // allocate memory for the bucket
                    curr.nulltailptr = new Object[THRESHOLD];
                    curr.ptrs[c] = curr.nulltailptr;
                    // point to the first cell of the bucket
                    curr.nulltailidx = 0;
                    // insert the string
                    curr.nulltailptr[curr.nulltailidx] = strings[i];
                    // point to next cell
                    curr.nulltailidx++;
                    // increment count of items
                    curr.counts[c]++;
                } else {
                    curr.ptrs[c] = new String[bucket_inc[1]];
                    ((String[]) curr.ptrs[c])[curr.counts[c]++] = strings[i];
                    curr.levels[c]++;
                }
            } else {
                // bucket already created, insert string in bucket
                if (c == NULLTERM) {
                    // insert the string
                    curr.nulltailptr[curr.nulltailidx] = strings[i];
                    // point to next cell
                    curr.nulltailidx++;
                    // increment count of items
                    curr.counts[c]++;
                    // check if the bucket is reaching the threshold
                    if (curr.counts[c] % THRESHOLDMINUSONE == 0) {
                        // Grow the null bucket by daisy chaining a new array.
                        Object[] tmp = new Object[THRESHOLD];
                        curr.nulltailptr[curr.nulltailidx] = tmp;
                        curr.nulltailptr = tmp;
                        // point to the first cell in the new array
                        curr.nulltailidx = 0;
                    }
                } else {
                    // insert string in bucket and increment the item counter
                    String[] arr = (String[]) curr.ptrs[c];
                    arr[curr.counts[c]++] = strings[i];
                    // Staggered Approach: if the size of the bucket is above
                    // level x, then realloc and increase the level count
                    // check for null string buckets as they are not to be
                    // incremented check if the number of items in the bucket
                    // is above a threshold.
                    if (curr.counts[c] < THRESHOLD && curr.counts[c] > (bucket_inc[curr.levels[c]] - 1)) {
                        String[] temp = (String[]) curr.ptrs[c];
                        curr.ptrs[c] = new String[bucket_inc[++curr.levels[c]]];
                        System.arraycopy(temp, 0, curr.ptrs[c], 0, temp.length);
                    }
                }

                // is bucket size above the THRESHOLD?
                while (curr.counts[c] >= THRESHOLD && c != NULLTERM) {
                    // advance depth of character
                    p++;

                    // allocate memory for new trie node
                    BurstTrie newt = new BurstTrie();
                    // burst...
                    int currcounts = curr.counts[c];
                    char cc = NULLTERM;
                    for (int j = 0; j < currcounts; j++) {
                        // access the next depth character
                        cc = charAt(((String[]) curr.ptrs[c])[j], p);
                        // Insert string in bucket in new node, create bucket if necessary
                        if (newt.counts[cc] < 1) {
                            // initialize the nullbucketsize, used to keep count
                            // of the number of times the bucket has been reallocated
                            // also make the nulltailptr point to the first element in the bucket
                            if (cc == NULLTERM) {
                                newt.nulltailptr = new Object[THRESHOLD];
                                // point to the first cell of the bucket
                                newt.ptrs[cc] = newt.nulltailptr;
                                newt.nulltailidx = 0;
                                // insert the string
                                newt.nulltailptr[newt.nulltailidx] =
                                        ((String[]) curr.ptrs[c])[j];
                                // point to next cell
                                newt.nulltailidx++;
                                // increment count of items
                                newt.counts[cc]++;
                            } else {
                                newt.ptrs[cc] = new String[bucket_inc[1]];
                                // insert string into bucket
                                // increment the item counter for the bucket
                                // increment the level counter for the bucket
                                ((String[]) newt.ptrs[cc])[newt.counts[cc]++] =
                                        ((String[]) curr.ptrs[c])[j];
                                newt.levels[cc]++;
                            }
                        } else {
                            // insert the string in the buckets
                            if (cc == NULLTERM) {
                                // insert the string
                                newt.nulltailptr[newt.nulltailidx] =
                                        ((String[]) curr.ptrs[c])[j];
                                // point to next cell
                                newt.nulltailidx++;
                                // increment count of items
                                newt.counts[cc]++;
                                // check if the bucket is reaching the threshold
                                if (newt.counts[cc] % THRESHOLDMINUSONE == 0) {
                                    Object[] tmp = new Object[THRESHOLD];
                                    newt.nulltailptr[newt.nulltailidx] = tmp;
                                    // point to the first cell in the new array
                                    newt.nulltailptr = tmp;
                                    newt.nulltailidx = 0;
                                }
                            } else {
                                // insert string in bucket and increment the item counter
                                ((String[]) newt.ptrs[cc])[newt.counts[cc]++] =
                                        ((String[]) curr.ptrs[c])[j];
                                // Staggered Approach: if the size of the bucket is above
                                // level x, then realloc and increase the level count
                                // check for null string buckets as they are not to be
                                // incremented check if the number of items in the bucket
                                // is above a threshold.
                                if (newt.counts[cc] < THRESHOLD &&
                                        newt.counts[cc] > (bucket_inc[newt.levels[cc]] - 1)) {
                                    String[] temp = (String[]) newt.ptrs[cc];
                                    newt.ptrs[cc] = new String[bucket_inc[++newt.levels[cc]]];
                                    System.arraycopy(temp, 0, newt.ptrs[cc], 0, temp.length);
                                }
                            }
                        }
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
        public Object[] nulltailptr;
        /** last element in null bucket */
        public int nulltailidx;
        /** level counter of bucket size */
        public int[] levels = new int[ALPHABET];
        /** count of items in bucket */
        public int[] counts = new int[ALPHABET];
        /** pointers to buckets or trie node */
        public Object[] ptrs = new Object[ALPHABET];
    }
}
