/*
 * Copyright (C) 2008-2009  Nathan Fiedler
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
 * A node in the burst trie structure based on the original Burstsort
 * algorithm, consisting of a null tail pointer bucket and zero or more
 * buckets for the other entries. Entries may point either to a bucket
 * or another trie node.
 *
 * @author  Nathan Fiedler
 */
public class BurstTrieNode {
    /** Size of the alphabet that is supported. */
    public static final short ALPHABET = 256;
    /** Constants for growing the buckets. */
    private static final short[] BUCKET_LEVELS = new short[]{
        (short) 0,
        (short) 16,
        (short) 128,
        (short) 1024,
        (short) 8192,
        (short) 16384,
        (short) 32768
    };
    /** Reference to the last null bucket in the chain, starting
     * from the reference in ptrs[0]. */
    private Object[] nulltailptr;
    /** last element in null bucket */
    private int nulltailidx;
    /** level counter of bucket size */
    private int[] levels = new int[ALPHABET];
    /** count of items in bucket, or -1 if reference to trie node */
    private int[] counts = new int[ALPHABET];
    /** pointers to buckets or trie node */
    private Object[] ptrs = new Object[ALPHABET];

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
    public void add(char c, String s) {
        // are buckets already created?
        if (counts[c] < 1) {
            // create bucket
            if (c == Burstsort.NULLTERM) {
                // allocate memory for the bucket
                nulltailptr = new Object[Burstsort.THRESHOLD];
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
                ptrs[c] = new String[BUCKET_LEVELS[1]];
                ((String[]) ptrs[c])[counts[c]++] = s;
                levels[c]++;
            }
        } else {
            // bucket already created, insert string in bucket
            if (c == Burstsort.NULLTERM) {
                // insert the string
                nulltailptr[nulltailidx] = s;
                // point to next cell
                nulltailidx++;
                // increment count of items
                counts[c]++;
                // check if the bucket is reaching the threshold
                if (counts[c] % Burstsort.THRESHOLDMINUSONE == 0) {
                    // Grow the null bucket by daisy chaining a new array.
                    Object[] tmp = new Object[Burstsort.THRESHOLD];
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
                if (counts[c] < Burstsort.THRESHOLD &&
                        counts[c] > (BUCKET_LEVELS[levels[c]] - 1)) {
                    String[] temp = (String[]) ptrs[c];
                    ptrs[c] = new String[BUCKET_LEVELS[++levels[c]]];
                    System.arraycopy(temp, 0, ptrs[c], 0, temp.length);
                }
            }
        }
    }

    /**
     * Retrieve the trie node or object array for character <em>c</em>.
     *
     * @param  c  character for which to retrieve entry.
     * @return  the trie node entry for the given character.
     */
    public Object get(char c) {
        return ptrs[c];
    }

    /**
     * Set the trie node or object array for character <em>c</em>.
     *
     * @param  c  character for which to store new entry.
     * @param  o  the trie node entry for the given character.
     */
    public void set(char c, Object o) {
        ptrs[c] = o;
        if (o instanceof BurstTrieNode) {
            // flag to indicate pointer to trie node and not bucket
            counts[c] = -1;
        }
    }

    /**
     * Returns the number of strings stored for the given character.
     *
     * @param  c  character for which to get count.
     * @return  number of tail strings; -1 if child is a trie node.
     */
    public int size(char c) {
        return counts[c];
    }
}
