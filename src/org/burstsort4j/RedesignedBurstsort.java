/*
 * Copyright 2009-2011 Nathan Fiedler. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
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
 * Array-based implementation of the redesigned Burstsort, referred to as
 * P+BR-Burstsort. There are both single-threaded and multi-threaded
 * implementations, designed to take advantage of multiple CPU cores,
 * if available.
 *
 * <p>The redesigned burstsort is more memory efficient than the original
 * burstsort, but may run slower as a result of the additional memory
 * allocations and array copies that are performed.</p>
 *
 * @author Nathan Fiedler
 */
public class RedesignedBurstsort {
    /** Null terminator character. */
    private static final char NULLTERM = '\0';
    /** Maximum number of elements in any given bucket (except null bucket). */
    private static final short THRESHOLD = 8192;
    /** Size of the alphabet that is supported. */
    private static final short ALPHABET = 256;
    /** Initial size for new sub-buckets. Combined with the growth factor
     * it is crucial that the sub-bucket threshold is contained in the
     * resulting sequence (e.g. 2, 4, 8, 16, 32, 64, 128, 256). */
    private static final short SUBBUCKET_START_SIZE = 16;
    /** The sub-bucket growth factor. The growth factor must strike a
     * balance between spending time allocating and copying arrays and
     * wasting space. */
    private static final short SUBBUCKET_GROWTH_FACTOR = 2;
    /** Size of the sub-buckets referenced within the bucket index. */
    private static final short SUBBUCKET_THRESHOLD = 256;

    /**
     * Creates a new instance of RedesignedBurstsort.
     */
    private RedesignedBurstsort() {
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
                Object[] bind = (Object[]) curr.get(c);
                for (int j = 0; j < bind.length; /* see below */) {
                    CharSequence[] sub = (CharSequence[]) bind[j];
                    int limit = sub.length;
                    j++;
                    if (j == bind.length) {
                        // Last sub-bucket may not be fully utilized.
                        int last = curr.size(c) % SUBBUCKET_THRESHOLD;
                        if (last > 0) {
                            limit = last;
                        }
                    }
                    for (int k = 0; k < limit; k++) {
                        // access the next depth character
                        cc = charAt(sub[k], p);
                        newt.add(cc, sub[k]);
                    }
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
                // Copy the string references from the bucket to the
                // destination. This is done for all node entries, even
                // the null bucket.
                Object[] bind = (Object[]) node.get(c);
                for (int j = 0; j < bind.length; /* see below */) {
                    CharSequence[] sub = (CharSequence[]) bind[j];
                    int limit = sub.length;
                    j++;
                    if (j == bind.length) {
                        // Last sub-bucket may not be fully utilized.
                        int last = count % SUBBUCKET_THRESHOLD;
                        if (last > 0) {
                            limit = last;
                        }
                    }
                    System.arraycopy(sub, 0, strings, off, limit);
                    off += limit;
                }
                if (c > 0) {
                    // Sort the strings that were just copied to the
                    // destination now that they are all in one array.
                    if (count > 1) {
                        int high = pos + count;
                        MultikeyQuicksort.sort(strings, pos, high, deep + 1);
                    }
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
        final int BIND_LIMIT = THRESHOLD / SUBBUCKET_THRESHOLD;
        for (char c = 0; c < ALPHABET; c++) {
            int count = node.size(c);
            if (count < 0) {
                pos = traverseParallel((Node) node.get(c), strings, pos,
                        deep + 1, jobs);
            } else if (count > 0) {
                Object[] bind = (Object[]) node.get(c);
                // For the null bucket, copy the elements without sorting.
                if (c == 0) {
                    // If this bucket has a lot of entries, then split it up
                    // into multiple jobs for better parallelism.
                    if (bind.length > BIND_LIMIT) {
                        int offset = 0;
                        int remainder = bind.length % BIND_LIMIT;
                        for (int j = bind.length / BIND_LIMIT; j > 0; j--) {
                            int endoff = offset + BIND_LIMIT;
                            if (remainder == 0 && j == 1) {
                                // This is the end of the bucket, copy only
                                // the number of elements stored therein.
                                jobs.add(new CopySortJob(bind, offset, endoff,
                                        count % THRESHOLD, strings, pos, -1));
                                pos += count % THRESHOLD;
                            } else {
                                jobs.add(new CopySortJob(bind, offset, endoff,
                                        THRESHOLD, strings, pos, -1));
                                pos += THRESHOLD;
                            }
                            offset = endoff;
                        }
                        if (remainder > 0) {
                            jobs.add(new CopySortJob(bind, offset, bind.length,
                                    count % THRESHOLD, strings, pos, -1));
                            pos += count % THRESHOLD;
                        }
                    } else {
                        // Plain and simple copy job.
                        jobs.add(new CopySortJob(bind, 0, bind.length, count,
                                strings, pos, -1));
                        pos += count;
                    }
                } else {
                    // This bucket must be both copied and sorted so we
                    // cannot break it into smaller chunks, but it should
                    // already be less than the threshold size anyway.
                    jobs.add(new CopySortJob(bind, 0, bind.length, count,
                            strings, pos, deep + 1));
                    pos += count;
                }
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
        int nodes = 0;
        int buckets = 0;
        int consumedStrings = 0;
        int bucketStrings = 0;
        int bucketSpace = 0;
        int nonEmptyBuckets = 0;
        int smallest = Integer.MAX_VALUE;
        int largest = Integer.MIN_VALUE;
        while (!stack.isEmpty()) {
            node = stack.pop();
            nodes++;
            for (char c = 0; c < ALPHABET; c++) {
                int count = node.size(c);
                if (count < 0) {
                    stack.push((Node) node.get(c));
                } else {
                    buckets++;
                    // Only consider non-empty buckets, as there will
                    // always be empty buckets.
                    if (count > 0) {
                        if (c == 0) {
                            consumedStrings += count;
                        } else {
                            bucketStrings += count;
                        }
                        Object[] bind = (Object[]) node.get(c);
                        bucketSpace += bind.length;
                        for (int j = 0; j < bind.length; j++) {
                            CharSequence[] sub = (CharSequence[]) bind[j];
                            bucketSpace += sub.length;
                        }
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
        out.format("Trie nodes: %d\n", nodes);
        out.format("Total buckets: %d\n", buckets);
        out.format("Bucket strings: %d\n", bucketStrings);
        out.format("Consumed strings: %d\n", consumedStrings);
        out.format("Smallest bucket: %d\n", smallest);
        out.format("Largest bucket: %d\n", largest);
        long sum = consumedStrings + bucketStrings;
        out.format("Average bucket: %d\n", sum / nonEmptyBuckets);
        out.format("Bucket capacity: %d\n", bucketSpace);
        double usage = ((double) sum * 100) / (double) bucketSpace;
        out.format("Usage ratio: %.2f\n", usage);
    }

    /**
     * A node in the burst trie structure based on the redesigned Burstsort
     * algorithm, consisting of a null tail pointer bucket and zero or more
     * buckets for the other entries. Entries may point either to a bucket
     * or another trie node. The buckets consist of a bucket index (BIND)
     * which points to sub-buckets that grown on demand.
     *
     * @author  Nathan Fiedler
     */
    private static class Node {
        /** Reference to the last sub-bucket in the index, if any. */
        private final Object[] lastBucket = new Object[ALPHABET];
        /** Next free slot in the last bucket, if there is a bucket. */
        private final int[] lastIndex = new int[ALPHABET];
        /** count of items in bucket, or -1 if reference to trie node */
        private final int[] counts = new int[ALPHABET];
        /** Pointers to bucket index or a trie node. */
        private final Object[] ptrs = new Object[ALPHABET];

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
                // no, create the new bucket
                CharSequence[] sub = new CharSequence[SUBBUCKET_START_SIZE];
                sub[0] = s;
                lastBucket[c] = sub;
                lastIndex[c] = 1;
                // Make bucket index (BIND) sized-to-fit.
                Object[] bind = new Object[1];
                bind[0] = sub;
                ptrs[c] = bind;
                // Increment count of items for this entry.
                counts[c]++;
            } else {
                // bucket already created, check if growth is required
                CharSequence[] sub = (CharSequence[]) lastBucket[c];
                if (lastIndex[c] < SUBBUCKET_THRESHOLD &&
                        lastIndex[c] == sub.length) {
                    // Grow the sub-bucket to accomodate the new string.
                    CharSequence[] tmp = new CharSequence[
                            sub.length * SUBBUCKET_GROWTH_FACTOR];
                    System.arraycopy(sub, 0, tmp, 0, sub.length);
                    sub = tmp;
                    // Update references to the last sub-bucket.
                    lastBucket[c] = sub;
                    Object[] bind = (Object[]) ptrs[c];
                    bind[bind.length - 1] = sub;
                } else if (lastIndex[c] == SUBBUCKET_THRESHOLD) {
                    // Grow the bucket by adding a new sub-bucket.
                    sub = new CharSequence[SUBBUCKET_START_SIZE];
                    lastBucket[c] = sub;
                    lastIndex[c] = 0;
                    Object[] obind = (Object[]) ptrs[c];
                    // Grow the bucket index by one each time.
                    Object[] bind = new Object[obind.length + 1];
                    System.arraycopy(obind, 0, bind, 0, obind.length);
                    bind[bind.length - 1] = sub;
                    ptrs[c] = bind;
                }
                // insert string in bucket
                sub[lastIndex[c]] = s;
                // point to next cell
                lastIndex[c]++;
                // increment count of items
                counts[c]++;
            }
        }

        /**
         * Retrieve the trie node or bucket index for character <em>c</em>.
         *
         * @param  c  character for which to retrieve entry.
         * @return  the entry for the given character, which may either be
         *          another trie node, or a bucket index (of type Object[]).
         */
        public Object get(char c) {
            return ptrs[c];
        }

        /**
         * Set the trie node or bucket index for character <em>c</em>.
         * Adjusts the "size" property associated with the character if
         * needed.
         *
         * @param  c  character for which to store new entry.
         * @param  o  the entry for the given character.
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

    /**
     * A copy and sort job to be completed after the trie traversal phase.
     * Each job is given a single bucket to be a processed. The job first
     * copies the bucket entries to the output array and then sorts the
     * the string "tails" within the output array.
     *
     * @author  Nathan Fiedler
     */
    private static class CopySortJob implements Callable<Object> {
        /** True if this job has already been completed. */
        private volatile boolean completed;
        /** Bucket index to be copied. */
        private final Object[] bind;
        /** Offset of first entry within bind to process. */
        private final int bstart;
        /** Offset of last entry within bind to process. */
        private final int bend;
        /** The number of elements in the input array. */
        private final int count;
        /** The array to which the sorted strings are written. */
        private final CharSequence[] output;
        /** The position within the strings array at which to store the
         * sorted results. */
        private final int offset;
        /** The depth at which to sort the strings (i.e. the strings often
         * have a common prefix, and depth is the length of that prefix and
         * thus the sort routines can ignore those characters).
         * If this value is less than zero then no sorting is performed. */
        private final int depth;

        /**
         * Constructs an instance of Job which will sort and then copy the
         * input strings to the output array.
         *
         * @param  bind    index for bucket to be copied and sorted.
         * @param  bstart  starting offset into bind.
         * @param  bend    ending offset into bind.
         * @param  count   number of elements in the bucket structure.
         * @param  output  output array; only a subset should be modified.
         * @param  offset  offset within output array to which sorted
         *                 strings will be written.
         * @param  depth   number of charaters in strings to be ignored
         *                 when sorting (i.e. the common prefix), or -1
         *                 if no sorting is to be performed.
         */
        public CopySortJob(Object[] bind, int bstart, int bend, int count,
                CharSequence[] output, int offset, int depth) {
            this.bind = bind;
            this.bstart = bstart;
            this.bend = bend;
            this.count = count;
            this.output = output;
            this.offset = offset;
            this.depth = depth;
        }

        /**
         * Indicates if this job has been completed or not.
         *
         * @return
         */
        public boolean isCompleted() {
            return completed;
        }

        @Override
        public Object call() throws Exception {
            // Copy the string references from the bucket to the
            // destination. This is done for all node entries, even
            // the null bucket.
            int off = offset;
            for (int j = bstart; j < bend; /* see below */) {
                CharSequence[] sub = (CharSequence[]) bind[j];
                int limit = sub.length;
                j++;
                if (j == bend) {
                    // Last sub-bucket may not be fully utilized.
                    int last = count % SUBBUCKET_THRESHOLD;
                    if (last > 0) {
                        limit = last;
                    }
                }
                System.arraycopy(sub, 0, output, off, limit);
                off += limit;
            }
            // If depth is less than zero, then no sorting is needed.
            if (count > 0 && depth >= 0) {
                // Sort the strings that were just copied to the
                // destination now that they are all in one array.
                if (count > 1) {
                    int high = offset + count;
                    MultikeyQuicksort.sort(output, offset, high, depth);
                }
            }
            completed = true;
            return null;
        }
    }
}
