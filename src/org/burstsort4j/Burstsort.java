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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Array-based implementation of the original Burstsort, now referred
 * to as P-burstsort. There are both single-threaded and multi-threaded
 * implementations, designed to take advantage of multiple CPU cores,
 * if available.
 *
 * @author Nathan Fiedler
 */
public class Burstsort {
    /** Null terminator character. */
    private static final char NULLTERM = '\0';
    /** Maximum number of elements in any given bucket; for null bucket set,
     * this is the size of each of the chained buckets). */
    private static final short THRESHOLD = 8192;
    /** Used to store reference to next bucket in last cell of bucket. */
    private static final short THRESHOLDMINUSONE = THRESHOLD - 1;
    /** Size of the alphabet that is supported. */
    private static final short ALPHABET = 256;
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

    /**
     * Creates a new instance of Burstsort.
     */
    private Burstsort() {
    }

    /**
     * Retrieve the character in string s at offset d. If d is greater
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
    private static void insert(Node root, CharSequence[] strings) {
        for (int i = 0; i < strings.length; i++) {
            // Start at root each time
            Node curr = root;
            // Locate trie node in which to insert string
            int p = 0;
            char c = charAt(strings[i], p);
            while (curr.size(c) < 0) {
                curr = (Node) curr.get(c);
                p++;
                c = charAt(strings[i], p);
            }
            curr.add(c, strings[i]);
            // is bucket size above the THRESHOLD?
            while (curr.size(c) >= THRESHOLD && c != NULLTERM) {
                // advance depth of character
                p++;
                // allocate memory for new trie node
                Node newt = new Node();
                // burst...
                char cc = NULLTERM;
                CharSequence[] ptrs = (CharSequence[]) curr.get(c);
                for (short j = 0; j < curr.size(c); j++) {
                    // access the next depth character
                    cc = charAt(ptrs[j], p);
                    newt.add(cc, ptrs[j]);
                }
                // old pointer points to the new trie node
                curr.set(c, newt);
                // used to burst recursive, so point curr to new
                curr = newt;
                // point to character used in previous string
                c = cc;
            }
        }
    }

    /**
     * Sorts the set of strings using the original (P-)burstsort algorithm.
     *
     * @param  strings  array of strings to be sorted.
     */
    public static void sort(CharSequence[] strings) {
        sort(strings, null);
    }

    /**
     * Sorts the given set of strings using the original (P-)burstsort
     * algorithm. If the given output stream is non-null, then metrics
     * regarding the burstsort trie structure will be printed there.
     *
     * @param  strings  array of strings to be sorted.
     * @param  out      if non-null, metrics are printed here.
     */
    public static void sort(CharSequence[] strings, PrintStream out) {
        if (strings != null && strings.length > 1) {
            Node root = new Node();
            insert(root, strings);
            if (out != null) {
                writeMetrics(root, out);
            }
            traverse(root, strings, 0, 0);
        }
    }

    /**
     * Uses all available processors to sort the trie buckets in parallel,
     * thus sorting the overal set of strings in less time. Uses a simple
     * ThreadPoolExecutor with a maximum pool size equal to the number of
     * available processors (usually equivalent to the number of CPU cores).
     *
     * @param  strings  array of strings to be sorted.
     * @throws  InterruptedException  if waiting thread was interrupted.
     */
    public static void sortThreadPool(CharSequence[] strings) throws InterruptedException {
        if (strings != null && strings.length > 1) {
            Node root = new Node();
            insert(root, strings);
            List<Callable<Object>> jobs = new ArrayList<Callable<Object>>();
            traverseParallel(root, strings, 0, 0, jobs);
            ExecutorService executor = Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors());
            // Using ExecutorService.invokeAll() usually adds more time.
            for (Callable<Object> job : jobs) {
                executor.submit(job);
            }
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.DAYS);
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
    private static int traverse(Node node, CharSequence[] strings, int pos, int deep) {
        for (char c = 0; c < ALPHABET; c++) {
            int count = node.size(c);
            if (count < 0) {
                pos = traverse((Node) node.get(c), strings, pos, deep + 1);
            } else if (count > 0) {
                int off = pos;
                if (c == 0) {
                    // Visit all of the null buckets, which are daisy-chained
                    // together with the last reference in each bucket pointing
                    // to the next bucket in the chain.
                    int no_of_buckets = (count / THRESHOLDMINUSONE) + 1;
                    Object[] nullbucket = (Object[]) node.get(c);
                    for (int k = 1; k <= no_of_buckets; k++) {
                        int no_elements_in_bucket;
                        if (k == no_of_buckets) {
                            no_elements_in_bucket = count % THRESHOLDMINUSONE;
                        } else {
                            no_elements_in_bucket = THRESHOLDMINUSONE;
                        }
                        // Copy the string tails to the sorted array.
                        int j = 0;
                        while (j < no_elements_in_bucket) {
                            strings[off++] = (CharSequence) nullbucket[j++];
                        }
                        nullbucket = (Object[]) nullbucket[j];
                    }
                } else {
                    // Sort the tail string bucket.
                    CharSequence[] bucket = (CharSequence[]) node.get(c);
                    if (count > 1) {
                        if (count < 20) {
                            Insertionsort.sort(bucket, 0, count, deep + 1);
                        } else {
                            MultikeyQuicksort.sort(bucket, 0, count, deep + 1);
                        }
                    }
                    // Copy to final destination.
                    System.arraycopy(bucket, 0, strings, off, count);
                }
                pos += count;
            }
        }
        return pos;
    }

    /**
     * Traverse the trie structure, creating jobs for each of the buckets.
     *
     * @param  node     node within trie structure.
     * @param  strings  the strings to be ordered.
     * @param  pos      position within array.
     * @param  deep     character offset within strings.
     * @param  jobs     job list to which new jobs are added.
     * @return  new pos value.
     */
    private static int traverseParallel(Node node, CharSequence[] strings,
            int pos, int deep, List<Callable<Object>> jobs) {
        for (char c = 0; c < ALPHABET; c++) {
            int count = node.size(c);
            if (count < 0) {
                pos = traverseParallel((Node) node.get(c), strings, pos,
                        deep + 1, jobs);
            } else if (count > 0) {
                int off = pos;
                if (c == 0) {
                    // Visit all of the null buckets, which are daisy-chained
                    // together with the last reference in each bucket pointing
                    // to the next bucket in the chain.
                    int no_of_buckets = (count / THRESHOLDMINUSONE) + 1;
                    Object[] nullbucket = (Object[]) node.get(c);
                    for (int k = 1; k <= no_of_buckets; k++) {
                        int no_elements_in_bucket;
                        if (k == no_of_buckets) {
                            no_elements_in_bucket = count % THRESHOLDMINUSONE;
                        } else {
                            no_elements_in_bucket = THRESHOLDMINUSONE;
                        }
                        jobs.add(new CopyJob(nullbucket, no_elements_in_bucket, strings, off));
                        off += no_elements_in_bucket;
                        nullbucket = (Object[]) nullbucket[no_elements_in_bucket];
                    }
                } else {
                    // A regular bucket with string tails that need to
                    // be sorted and copied to the final destination.
                    CharSequence[] bucket = (CharSequence[]) node.get(c);
                    jobs.add(new SortJob(bucket, count, strings, off, deep + 1));
                }
                pos += count;
            }
        }
        return pos;
    }

    /**
     * Collect metrics regarding the burstsort trie structure and write
     * them to the given output stream.
     *
     * @param  node  root node of the trie structure.
     * @param  out   output stream to write to.
     */
    private static void writeMetrics(Node node, PrintStream out) {
        Stack<Node> stack = new Stack<Node>();
        stack.push(node);
        int buckets = 0;
        int nonEmptyBuckets = 0;
        int smallest = Integer.MAX_VALUE;
        int largest = Integer.MIN_VALUE;
        long sum = 0;
        while (!stack.isEmpty()) {
            node = stack.pop();
            for (char c = 0; c < ALPHABET; c++) {
                int count = node.size(c);
                if (count < 0) {
                    stack.push((Node) node.get(c));
                } else {
                    buckets++;
                    // Only consider non-empty buckets, as there will
                    // always be empty buckets.
                    if (count > 0) {
                        sum += count;
                        if (count < smallest) {
                            smallest = count;
                        }
                        nonEmptyBuckets++;
                    }
                    if (count > largest) {
                        largest = count;
                    }
                }
            }
        }
        out.format("Bucket count: %d\n", buckets);
        out.format("Smallest bucket: %d\n", smallest);
        out.format("Largest bucket: %d\n", largest);
        out.format("Average bucket: %d\n", sum / nonEmptyBuckets);
    }

    /**
     * A node in the burst trie structure based on the original Burstsort
     * algorithm, consisting of a null tail pointer bucket and zero or more
     * buckets for the other entries. Entries may point either to a bucket
     * or another trie node.
     *
     * @author  Nathan Fiedler
     */
    private static class Node {
        /** Reference to the last null bucket in the chain, starting
         * from the reference in ptrs[0]. */
        private Object[] nulltailptr;
        /** last element in null bucket */
        private int nulltailidx;
        /** level counter of bucket size */
        private byte[] levels = new byte[ALPHABET];
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
        public void add(char c, CharSequence s) {
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
                    ptrs[c] = new CharSequence[BUCKET_LEVELS[1]];
                    ((CharSequence[]) ptrs[c])[counts[c]++] = s;
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
                    ((CharSequence[]) ptrs[c])[counts[c]++] = s;
                    // Staggered Approach: if the size of the bucket is above
                    // level x, then realloc and increase the level count
                    // check for null string buckets as they are not to be
                    // incremented check if the number of items in the bucket
                    // is above a threshold.
                    if (counts[c] < THRESHOLD &&
                            counts[c] > (BUCKET_LEVELS[levels[c]] - 1)) {
                        CharSequence[] temp = (CharSequence[]) ptrs[c];
                        ptrs[c] = new CharSequence[BUCKET_LEVELS[++levels[c]]];
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
            if (o instanceof Node) {
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
}
