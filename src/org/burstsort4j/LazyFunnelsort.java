/*
 * Copyright (C) 2009  Nathan Fiedler
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * An implementation of the lazy funnelsort algorithm as described by Brodal,
 * Fagerberg, and Vinther, which itself is adapted from the original
 * algorithm by Frigo, Leiserson, Prokop, and Ramachandran.
 *
 * @author Nathan Fiedler
 */
public class LazyFunnelsort {
    /** Brodal, Fagerberg, and Vinther found through experimentation that
     * delegating to quicksort for arrays of size 400 or less improved
     * overall performance of funnelsort. */
    private static final int QUICKSORT_THRESHOLD = 400;

    /**
     * Merges the given list of buffers into a single buffer using an
     * insertion d-way merge as described by Moruz and Brodal in WADS05.
     *
     * @param  inputs  list of buffers to be merged.
     * @param  output  where merged results are stored.
     */
    @SuppressWarnings("unchecked")
    public static void insertionMerge(List<CircularBuffer<Comparable>> inputs,
            CircularBuffer<Comparable> output) {
        // Set up the array (d) of input buffers.
        CircularBuffer<Comparable>[] d = new CircularBuffer[inputs.size()];
        d = inputs.toArray(d);

        // Perform an insertion sort of the buffers using the leading values.
        for (int i = 1; i < d.length; i++) {
            CircularBuffer<Comparable> tmp = d[i];
            int j = i;
            while (j > 0 && tmp.peek().compareTo(d[j - 1].peek()) < 0) {
                d[j] = d[j - 1];
                j--;
            }
            d[j] = tmp;
        }

        // While there are streams to be processed...
        while (d.length > 0) {
            if (d.length == 1) {
                // Copy remainder of last stream to output.
                d[0].drain(output);
                d = new CircularBuffer[0];
            } else if (d.length == 2) {
                // With only two streams, perform a faster merge.
                CircularBuffer<Comparable> a = d[0];
                CircularBuffer<Comparable> b = d[1];
                while (!a.isEmpty() && !b.isEmpty()) {
                    if (a.peek().compareTo(b.peek()) < 0) {
                        output.add(a.remove());
                    } else {
                        output.add(b.remove());
                    }
                }
                if (!a.isEmpty()) {
                    a.drain(output);
                }
                if (!b.isEmpty()) {
                    b.drain(output);
                }
                d = new CircularBuffer[0];
            } else {
                output.add(d[0].remove());
                if (d[0].isEmpty()) {
                    // This stream has been exhausted, remove it from the pool.
                    CircularBuffer[] td = new CircularBuffer[d.length - 1];
                    System.arraycopy(d, 1, td, 0, td.length);
                    d = td;
                } else {
                    // Insert new candidate into correct position in d.
                    CircularBuffer<Comparable> t = d[0];
                    Comparable s = t.peek();
                    int j = 1;
                    while (j < d.length && s.compareTo(d[j].peek()) > 0) {
                        d[j - 1] = d[j];
                        j++;
                    }
                    d[j - 1] = t;
                }
            }
        }
    }

    /**
     * Sorts the set of strings using the "lazy" funnelsort algorithm as
     * described by Brodal, Fagerberg, and Vinther.
     *
     * @param  strings  array of strings to be sorted.
     */
    public static void sort(Comparable[] strings) {
        if (strings == null || strings.length < 2) {
            return;
        }
        sort(strings, 0, strings.length);
    }

    /**
     * Sorts the elements within the array starting at the offset and
     * ending at offset plus the count.
     *
     * @param  strings  array containing elements to be sorted.
     * @param  offset   first position within array to be sorted.
     * @param  count    number of elements from offset to be sorted.
     */
    @SuppressWarnings("unchecked")
    private static void sort(Comparable[] strings, int offset, int count) {
        if (count > QUICKSORT_THRESHOLD) {
            // Divide input into n^(1/3) arrays of size n^(2/3).
            int num_blocks = Math.round((float) Math.cbrt((double) count));
            int block_size = count / num_blocks;
            int mark = offset;
            for (int ii = 1; ii < num_blocks; ii++) {
                sort(strings, mark, block_size);
                mark += block_size;
            }
            int leftover = count - mark;
            if (leftover > 0) {
                sort(strings, mark, leftover);
            }

            // Merge the n^(1/3) sorted arrays using a k-merger.
            List<CircularBuffer<Comparable>> inputs =
                    new ArrayList<CircularBuffer<Comparable>>(num_blocks + 1);
            mark = offset;
            for (int ii = 1; ii < num_blocks; ii++) {
                inputs.add(new CircularBuffer<Comparable>(strings, mark, block_size, false));
                mark += block_size;
            }
            leftover = count - mark;
            if (leftover > 0) {
                inputs.add(new CircularBuffer<Comparable>(strings, mark, leftover, false));
            }
            CircularBuffer<Comparable> output = new CircularBuffer<Comparable>(count);
            Kmerger merger = MergerFactory.createMerger(inputs, output);
            merger.merge();
            output.drain(strings, offset);
        } else {
            // For small subarrays, delegate to quicksort.
            Quicksort.sort(strings, offset, offset + count - 1);
        }
    }

    /**
     * A Kmerger merges one or more input streams into a single output
     * stream, sorting the elements in the process.
     */
    static interface Kmerger {

        /**
         * Return the reference to this merger's output buffer.
         *
         * @return  output buffer for this merger, or null if none.
         */
        CircularBuffer<Comparable> getOutput();

        /**
         * Indicates if this merger has more elements to output.
         *
         * @return  true if more elements are available, false otherwise.
         */
        boolean hasMore();

        /**
         * Merges k^3 elements from the inputs and writes them to the output.
         */
        void merge();
    }

    /**
     * MergerFactory creates instances of Kmerger based on the given inputs.
     */
    private static class MergerFactory {

        /**
         * Creates a new instance of Kmerger appropriate for the inputs.
         *
         * @param  inputs  streams of sorted input to be merged.
         * @param  output  buffer to which merged results are written.
         * @return  a Kmerger instance.
         */
        public static Kmerger createMerger(List<CircularBuffer<Comparable>> inputs,
                CircularBuffer<Comparable> output) {
            int k = inputs.size();
            if (k > 4) {
                return new BufferMerger(inputs, output);
            } else {
                return new InsertionMerger(inputs, output);
            }
        }

        /**
         * Creates a new instance of Kmerger appropriate for the inputs.
         *
         * @param  output   buffer to which merged results are written.
         * @param  mergers  input mergers to be merged into a single stream.
         * @return  a Kmerger instance.
         */
        public static Kmerger createMerger(CircularBuffer<Comparable> output,
                List<Kmerger> mergers) {
            int k = mergers.size();
            if (k > 4) {
                return new MergerMerger(output, mergers);
            } else {
                return new InsertionMerger(output, mergers);
            }
        }
    }

    /**
     * A BufferMerger divides up the input streams into k^(1/2) groups
     * each of size k^(1/2), creating additional mergers for those
     * groups, and ultimately merging their output into a single buffer.
     */
    private static class BufferMerger implements Kmerger {
        /** The output buffer for this merger. */
        private final CircularBuffer<Comparable> output;
        /** The right k-merger for merging the k^(1/2) input streams. */
        private final Kmerger R;
        /** The left k^(1/2) input streams each of size k^(1/2). */
        private final List<Kmerger> Li;

        /**
         * Creates a new instance of BufferMerger.
         *
         * @param  inputs  streams of sorted input to be merged.
         * @param  output  buffer to which merged results are written.
         */
        public BufferMerger(List<CircularBuffer<Comparable>> inputs,
                CircularBuffer<Comparable> output) {
            this.output = output;
            int k = inputs.size();
            int k3half = Math.round((float) Math.sqrt((double) k * k * k));
            // Rounding up avoids creating excessive numbers of mergers.
            int kroot = Math.round((float) Math.sqrt((double) k));
            int offset = 0;
            Li = new ArrayList<Kmerger>(kroot + 1);
            for (int ii = 1; ii < kroot; ii++) {
                List<CircularBuffer<Comparable>> li = inputs.subList(offset, offset + kroot);
                CircularBuffer<Comparable> buffer = new CircularBuffer<Comparable>(k3half);
                Li.add(MergerFactory.createMerger(li, buffer));
                offset += kroot;
            }
            if (inputs.size() > offset) {
                List<CircularBuffer<Comparable>> li = inputs.subList(offset, inputs.size());
                CircularBuffer<Comparable> buffer = new CircularBuffer<Comparable>(k3half);
                Li.add(MergerFactory.createMerger(li, buffer));
            }
            R = MergerFactory.createMerger(output, Li);
        }

        @Override
        public CircularBuffer<Comparable> getOutput() {
            return output;
        }

        @Override
        public boolean hasMore() {
            return R.hasMore();
        }

        @Override
        public void merge() {
            while (!output.isFull() && R.hasMore()) {
                // Make sure all of the buffers are non-empty.
                for (Kmerger merger : Li) {
                    CircularBuffer<Comparable> buf = merger.getOutput();
                    if (buf.isEmpty()) {
                        merger.merge();
                    }
                }
                R.merge();
            }
        }
    }

    /**
     * A MergerMerger divides up the input mergers into k^(1/2) groups
     * each of size k^(1/2), creating additional mergers for those
     * groups, and ultimately merging their output into a single buffer.
     */
    private static class MergerMerger implements Kmerger {
        /** The output buffer for this merger. */
        private final CircularBuffer<Comparable> output;
        /** The right k-merger for merging the k^(1/2) input streams. */
        private final Kmerger R;
        /** The left k^(1/2) input streams each of size k^(1/2). */
        private final List<Kmerger> Li;

        /**
         * Creates a new instance of MergerMerger.
         *
         * @param  output   buffer to which merged results are written.
         * @param  mergers  streams of sorted input to be merged.
         */
        public MergerMerger(CircularBuffer<Comparable> output, List<Kmerger> mergers) {
            this.output = output;
            int k = mergers.size();
            int k3half = Math.round((float) Math.sqrt((double) k * k * k));
            // Rounding up avoids creating excessive numbers of mergers.
            int kroot = Math.round((float) Math.sqrt((double) k));
            int offset = 0;
            Li = new ArrayList<Kmerger>(kroot + 1);
            for (int ii = 1; ii < kroot; ii++) {
                List<Kmerger> li = mergers.subList(offset, offset + kroot);
                CircularBuffer<Comparable> buffer = new CircularBuffer<Comparable>(k3half);
                Li.add(MergerFactory.createMerger(buffer, li));
                offset += kroot;
            }
            if (mergers.size() > offset) {
                List<Kmerger> li = mergers.subList(offset, mergers.size());
                CircularBuffer<Comparable> buffer = new CircularBuffer<Comparable>(k3half);
                Li.add(MergerFactory.createMerger(buffer, li));
            }
            R = MergerFactory.createMerger(output, Li);
        }

        @Override
        public CircularBuffer<Comparable> getOutput() {
            return R.getOutput();
        }

        @Override
        public boolean hasMore() {
            return R.hasMore();
        }

        @Override
        public void merge() {
            while (!output.isFull() && R.hasMore()) {
                // Make sure all of the buffers are non-empty.
                for (Kmerger merger : Li) {
                    CircularBuffer<Comparable> buf = merger.getOutput();
                    if (buf.isEmpty()) {
                        merger.merge();
                    }
                }
                R.merge();
            }
        }
    }

    /**
     * A k-merger that merges multiple inputs, whether those are mergers or
     * buffers, or a mix of both, using an insertion d-way mergesort.
     */
    static class InsertionMerger implements Kmerger {
        /** List of input buffers. */
        private List<CircularBuffer<Comparable>> buffers;
        /** Mergers associated with the circular buffers. Not all buffers
         * will necessarily have a merger. */
        private Map<CircularBuffer<Comparable>, Kmerger> mergers;
        /** The output buffer. */
        private CircularBuffer<Comparable> output;
        /** True if the input buffers have been initially sorted. */
        private boolean sorted;

        /**
         * Creates a new instance of InsertionMerger.
         *
         * @param  buffers  the list of input buffers.
         * @param  output   the output buffer.
         */
        public InsertionMerger(List<CircularBuffer<Comparable>> buffers,
                CircularBuffer<Comparable> output) {
            this.buffers = new ArrayList<CircularBuffer<Comparable>>(buffers);
            this.output = output;
            mergers = Collections.emptyMap();
        }

        /**
         * Creates a new instance of InsertionMerger.
         *
         * @param  output   the output buffer.
         * @param  mergers  the list of input mergers.
         */
        public InsertionMerger(CircularBuffer<Comparable> output,
                List<Kmerger> mergers) {
            this.output = output;
            this.mergers = new HashMap<CircularBuffer<Comparable>, Kmerger>();
            buffers = new ArrayList<CircularBuffer<Comparable>>();
            for (Kmerger merger : mergers) {
                CircularBuffer<Comparable> buffer = merger.getOutput();
                buffers.add(buffer);
                this.mergers.put(buffer, merger);
            }
        }

        @Override
        public CircularBuffer<Comparable> getOutput() {
            return output;
        }

        @Override
        public boolean hasMore() {
            for (CircularBuffer<Comparable> buffer : buffers) {
                if (!buffer.isEmpty()) {
                    return true;
                }
            }
            for (Kmerger merger : mergers.values()) {
                if (merger.hasMore()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void merge() {
            if (!sorted) {
                // Ensure all input buffers are populated, pruning any
                // buffers that are empty.
                Iterator<CircularBuffer<Comparable>> iter = buffers.iterator();
                while (iter.hasNext()) {
                    CircularBuffer<Comparable> buffer = iter.next();
                    if (buffer.isEmpty()) {
                        Kmerger merger = mergers.get(buffer);
                        if (merger != null) {
                            merger.merge();
                        }
                        if (buffer.isEmpty()) {
                            iter.remove();
                        }
                    }
                }
                // Perform an insertion sort of the buffers using the leading values.
                for (int i = 1; i < buffers.size(); i++) {
                    CircularBuffer<Comparable> tmp = buffers.get(i);
                    int j = i;
                    while (j > 0 && tmp.peek().compareTo(buffers.get(j - 1).peek()) < 0) {
                        buffers.set(j, buffers.get(j - 1));
                        j--;
                    }
                    buffers.set(j, tmp);
                }
                sorted = true;
            }

            // While output is not full...
            while (!output.isFull() && !buffers.isEmpty()) {
                if (buffers.size() == 1) {
                    // Copy remainder of last stream to output.
                    CircularBuffer<Comparable> a = buffers.get(0);
                    a.drain(output);
                    buffers.clear();
                    mergers.clear();
                } else if (buffers.size() == 2) {
                    // With only two streams, perform a faster merge.
                    CircularBuffer<Comparable> a = buffers.get(0);
                    CircularBuffer<Comparable> b = buffers.get(1);
                    while (!output.isFull() && !a.isEmpty() && !b.isEmpty()) {
                        if (a.peek().compareTo(b.peek()) < 0) {
                            output.add(a.remove());
                        } else {
                            output.add(b.remove());
                        }
                    }
                    if (!output.isFull() && !a.isEmpty()) {
                        a.drain(output);
                    }
                    if (!output.isFull() && !b.isEmpty()) {
                        b.drain(output);
                    }
                    buffers.clear();
                    mergers.clear();
                } else {
                    CircularBuffer<Comparable> t = buffers.get(0);
                    output.add(t.remove());
                    if (t.isEmpty()) {
                        Kmerger merger = mergers.get(t);
                        if (merger != null) {
                            merger.merge();
                        }
                        if (t.isEmpty()) {
                            // This stream has been exhausted.
                            buffers.remove(0);
                            mergers.remove(t);
                        }
                    } else {
                        // Insert new candidate into correct position in d.
                        Comparable s = t.peek();
                        int j = 1;
                        int length = buffers.size();
                        while (j < length && s.compareTo(buffers.get(j).peek()) > 0) {
                            buffers.set(j - 1, buffers.get(j));
                            j++;
                        }
                        buffers.set(j - 1, t);
                    }
                }
            }
        }
    }
}
