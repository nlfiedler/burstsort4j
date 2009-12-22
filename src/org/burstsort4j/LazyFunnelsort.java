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
import java.util.List;
import java.util.Observable;
import java.util.Observer;

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
            final int num_blocks = Math.round((float) Math.cbrt((double) count));
            final int block_size = count / num_blocks;
            int mark = offset;
            for (int ii = 1; ii < num_blocks; ii++) {
                sort(strings, mark, block_size);
                mark += block_size;
            }
            int leftover = count - (mark - offset);
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
            leftover = count - (mark - offset);
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
         * Merges k^3 elements from the inputs and writes them to the output.
         */
        void merge();
    }

    /**
     * MergerFactory creates instances of Kmerger based on the given inputs.
     */
    static class MergerFactory {

        /**
         * Creates a new instance of Kmerger to merge the input buffers.
         *
         * @param  inputs  streams of sorted input to be merged.
         * @param  output  buffer to which merged results are written.
         * @return  a Kmerger instance.
         */
        public static Kmerger createMerger(
                List<CircularBuffer<Comparable>> inputs,
                CircularBuffer<Comparable> output) {
            int k = inputs.size();
            if (k > 4) {
                return new BufferMerger(inputs, output);
            } else {
                return new InsertionMerger(inputs, output);
            }
        }
    }

    /**
     * A BufferMerger divides up the input streams into k^(1/2) groups
     * each of size k^(1/2), creating additional mergers for those
     * groups, and ultimately merging their output into a single buffer.
     */
    private static class BufferMerger implements Kmerger, Observer {

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
            output.addObserver(this);
            int k = inputs.size();
            int k3half2 = Math.round((float) Math.sqrt((double) k * k * k)) * 2;
            double k2 = Math.sqrt((double) k);
            // The kroot is used to determine the number of mergers to
            // create, which we want to be reasonable, so we round.
            int kroot = Math.round((float) k2);
            // The kspread value is how many inputs are sent to each
            // merger, of which we want to have an even distribution,
            // and so the ceiling of this value is taken.
            int kspread = (int) Math.ceil(k2);
            int offset = 0;
            Li = new ArrayList<Kmerger>(kroot);
            List<CircularBuffer<Comparable>> buffers =
                    new ArrayList<CircularBuffer<Comparable>>();
            for (int ii = 1; ii < kroot; ii++) {
                List<CircularBuffer<Comparable>> li = inputs.subList(offset, offset + kspread);
                CircularBuffer<Comparable> buffer = new CircularBuffer<Comparable>(k3half2);
                buffers.add(buffer);
                Li.add(MergerFactory.createMerger(li, buffer));
                offset += kspread;
            }
            if (inputs.size() > offset) {
                List<CircularBuffer<Comparable>> li = inputs.subList(offset, inputs.size());
                CircularBuffer<Comparable> buffer = new CircularBuffer<Comparable>(k3half2);
                buffers.add(buffer);
                Li.add(MergerFactory.createMerger(li, buffer));
            }
            R = MergerFactory.createMerger(buffers, output);
        }

        @Override
        public void merge() {
            // Do not loop, just make one attempt to populate the output.
            if (!output.isFull()) {
                // Even with the buffer empty notification, need to at least
                // populate the buffers once to prime them for merging.
                for (Kmerger merger : Li) {
                    merger.merge();
                }
                R.merge();
            }
        }

        @Override
        public void update(Observable o, Object arg) {
            // The buffer that we are observing has become empty,
            // try to fill it again.
            merge();
        }
    }

    /**
     * A k-merger that merges multiple inputs, using an efficient
     * insertion d-way mergesort.
     */
    private static class InsertionMerger implements Kmerger, Observer {

        /** List of input buffers. */
        private List<CircularBuffer<Comparable>> buffers;
        /** The output buffer. */
        private CircularBuffer<Comparable> output;

        /**
         * Creates a new instance of InsertionMerger.
         *
         * @param  buffers  the list of input buffers.
         * @param  output   the output buffer.
         */
        public InsertionMerger(List<CircularBuffer<Comparable>> buffers,
                CircularBuffer<Comparable> output) {
            // Copy the input list so it can be modified during merging.
            this.buffers = new ArrayList<CircularBuffer<Comparable>>(buffers);
            this.output = output;
            output.addObserver(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void merge() {
            // We assume the buffers have been populated. An exception will
            // be thrown if that is not the case.

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

            // While output is not full...
            while (!output.isFull() && !buffers.isEmpty()) {
                if (buffers.size() == 1) {
                    // Copy remainder of last stream to output.
                    CircularBuffer<Comparable> t = buffers.get(0);
                    int n = Math.min(t.size(), output.remaining());
                    if (n > 0) {
                        t.move(output, n);
                        if (t.isEmpty()) {
                            buffers.remove(0);
                        }
                    }
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
                    if (b.isEmpty()) {
                        buffers.remove(1);
                    }
                    if (a.isEmpty()) {
                        buffers.remove(0);
                    }
                } else {
                    CircularBuffer<Comparable> t = buffers.get(0);
                    output.add(t.remove());
                    if (t.isEmpty()) {
                        // This stream has been exhausted.
                        buffers.remove(0);
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

        @Override
        public void update(Observable o, Object arg) {
            // Attempt to populate the recently emptied output buffer.
            merge();
        }
    }
}
