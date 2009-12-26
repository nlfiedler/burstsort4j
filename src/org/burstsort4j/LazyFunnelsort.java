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
import java.util.ListIterator;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of the lazy funnelsort algorithm as described by Brodal,
 * Fagerberg, and Vinther, which itself is adapted from the original
 * algorithm by Frigo, Leiserson, Prokop, and Ramachandran. This is not a
 * String-based sort like some of the others in this package, but rather
 * sorts instances of {@code Comparable}, much like mergesort and quicksort.
 *
 * @author Nathan Fiedler
 */
public class LazyFunnelsort {

    /** Brodal, Fagerberg, and Vinther found through experimentation that
     * delegating to quicksort for arrays of size 400 or less improved
     * overall performance of funnelsort. */
    private static final int QUICKSORT_THRESHOLD = 400;
    //
    // Tried threshold of 200 and 800 and no noticeable difference.
    //

    /**
     * Sorts the set of Comparables using the "lazy" funnelsort algorithm
     * as described by Brodal, Fagerberg, and Vinther.
     *
     * @param  inputs  array of Comparables to be sorted.
     */
    public static void sort(Comparable[] inputs) {
        if (inputs == null || inputs.length < 2) {
            return;
        }
        sort(inputs, 0, inputs.length, false);
    }

    /**
     * Sorts the set of Comparables using the "lazy" funnelsort algorithm
     * as described by Brodal, Fagerberg, and Vinther. The sorting is
     * performed using multiple threads in order to shorten the overall
     * sort time.
     *
     * @param  inputs  array of Comparables to be sorted.
     */
    public static void sortThreaded(Comparable[] inputs) {
        if (inputs == null || inputs.length < 2) {
            return;
        }
        sort(inputs, 0, inputs.length, true);
    }

    /**
     * Sorts the elements within the array starting at the offset and
     * ending at offset plus the count.
     *
     * @param  inputs    array containing elements to be sorted.
     * @param  offset    first position within array to be sorted.
     * @param  count     number of elements from offset to be sorted.
     * @param  threaded  true to use multiple threads, false otherwise.
     */
    @SuppressWarnings("unchecked")
    private static void sort(final Comparable[] inputs, final int offset,
            final int count, boolean threaded) {
        if (count > QUICKSORT_THRESHOLD) {
            // Divide input into n^(1/3) arrays of size n^(2/3) and sort each.
            final int num_blocks = Math.round((float) Math.cbrt((double) count));
            final int block_size = count / num_blocks;
            int mark = offset;
            if (threaded) {
                // In multi-threaded mode, create a set of jobs to sort each
                // of the subarrays and run those jobs on multiple threads.
                List<Callable<Object>> jobs = new ArrayList<Callable<Object>>();
                for (int ii = 1; ii < num_blocks; ii++) {
                    jobs.add(new SortJob(inputs, mark, block_size));
                    mark += block_size;
                }
                final int leftover = count - (mark - offset);
                if (leftover > 0) {
                    jobs.add(new SortJob(inputs, mark, leftover));
                }
                // Use the number of available processors to determine the
                // size of the thread pool, since having more threads does
                // not improve overall performance with a CPU-intensive task.
                ExecutorService executor = Executors.newFixedThreadPool(
                        Runtime.getRuntime().availableProcessors());
                try {
                    executor.invokeAll(jobs);
                    // Shut down to let the VM exit normally.
                    executor.shutdown();
                    executor.awaitTermination(1, TimeUnit.DAYS);
                } catch (InterruptedException ie) {
                    throw new RuntimeException("Sorters interrupted!", ie);
                }
                // Once the subarrays have been sorted, we merge them in a
                // single thread, just as in the single-threaded case.
            } else {
                // In single-threaded mode, just sort the subarrays
                // sequentially in the current thread.
                for (int ii = 1; ii < num_blocks; ii++) {
                    sort(inputs, mark, block_size, false);
                    mark += block_size;
                }
                int leftover = count - (mark - offset);
                if (leftover > 0) {
                    sort(inputs, mark, leftover, false);
                }
            }

            // Merge the n^(1/3) sorted arrays using a k-merger.
            List<CircularBuffer<Comparable>> buffers =
                    new ArrayList<CircularBuffer<Comparable>>(num_blocks);
            mark = offset;
            for (int ii = 1; ii < num_blocks; ii++) {
                buffers.add(new CircularBuffer<Comparable>(inputs, mark, block_size, false));
                mark += block_size;
            }
            int leftover = count - (mark - offset);
            if (leftover > 0) {
                buffers.add(new CircularBuffer<Comparable>(inputs, mark, leftover, false));
            }
            CircularBuffer<Comparable> output = new CircularBuffer<Comparable>(count);
            Kmerger merger = MergerFactory.createMerger(buffers, 0, buffers.size(), output);
            merger.merge();
            output.drain(inputs, offset);
            //
            // The above code is re-using the input buffer and creating a
            // new destination buffer. Tried copying the input buffers and
            // re-using the destination buffer but that made the performance
            // slightly slower.
            //
        } else {
            // For small subarrays, delegate to quicksort.
            Quicksort.sort(inputs, offset, offset + count - 1);
        }
    }

    /**
     * A SortJob is a simple Callable that invokes the sort() method with
     * the values given in the constructor.
     */
    private static class SortJob implements Callable<Object> {

        /** The inputs to be sorted. */
        private final Comparable[] inputs;
        /** Offset of the first element to be sorted. */
        private final int offset;
        /** Number of elements to be sorted. */
        private final int count;

        /**
         * Creates a new instance of SortJob.
         *
         * @param  inputs  array containing elements to be sorted.
         * @param  offset  first position within array to be sorted.
         * @param  count   number of elements from offset to be sorted.
         */
        public SortJob(Comparable[] inputs, int offset, int count) {
            this.inputs = inputs;
            this.offset = offset;
            this.count = count;
        }

        @Override
        public Object call() throws Exception {
            sort(inputs, offset, count, false);
            return null;
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
         * @param  offset  first element in list to be merged.
         * @param  count   number of elements in list to be merged, starting
         *                 from the {@code offset} position.
         * @param  output  buffer to which merged results are written.
         * @return  a Kmerger instance.
         */
        @SuppressWarnings("unchecked")
        public static Kmerger createMerger(
                List<CircularBuffer<Comparable>> inputs, int offset,
                int count, CircularBuffer<Comparable> output) {
            int k = count;
            // Tests indicate the values of 8 and 16 do not help performance.
            if (k > 4) {
                return new BufferMerger(inputs, offset, count, output);
            } else {
                // Convert the sublist to an array for insertion merger.
                CircularBuffer[] buffers = new CircularBuffer[count];
                ListIterator<CircularBuffer<Comparable>> li =
                        inputs.listIterator(offset);
                int c = 0;
                while (c < count) {
                    buffers[c++] = li.next();
                }
                return new InsertionMerger(buffers, output);
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
         * @param  offset  first element in list to be merged.
         * @param  count   number of elements in list to be merged, starting
         *                 from the {@code offset} position.
         * @param  output  buffer to which merged results are written.
         */
        public BufferMerger(List<CircularBuffer<Comparable>> inputs,
                int offset, int count, CircularBuffer<Comparable> output) {
            this.output = output;
            output.addObserver(this);
            int k = count;
            int k3half2 = Math.round((float) Math.sqrt((double) k * k * k)) * 2;
            double k2 = Math.sqrt((double) k);
            // The kroot is used to determine the number of mergers to
            // create, which we want to be reasonable, so we round.
            int kroot = Math.round((float) k2);
            // The kspread value is how many inputs are sent to each
            // merger, of which we want to have an even distribution,
            // and so the ceiling of this value is taken.
            int kspread = (int) Math.ceil(k2);
            Li = new ArrayList<Kmerger>(kroot);
            // Set up the list of buffers for the right side merger.
            List<CircularBuffer<Comparable>> buffers =
                    new ArrayList<CircularBuffer<Comparable>>();
            // Create mergers for the left inputs by dividing up the
            // inputs into roughly equal-sized sublists.
            int mark = offset;
            for (int ii = 1; ii < kroot; ii++) {
                CircularBuffer<Comparable> buffer = new CircularBuffer<Comparable>(k3half2);
                buffers.add(buffer);
                Li.add(MergerFactory.createMerger(inputs, mark, kspread, buffer));
                mark += kspread;
            }
            int leftover = count - (mark - offset);
            if (leftover > 0) {
                CircularBuffer<Comparable> buffer = new CircularBuffer<Comparable>(k3half2);
                buffers.add(buffer);
                Li.add(MergerFactory.createMerger(inputs, mark, leftover, buffer));
            }
            R = MergerFactory.createMerger(buffers, 0, buffers.size(), output);
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
        private CircularBuffer[] buffers;
        /** Number of active buffers in the {@code buffers} array. */
        private int bufferCount;
        /** The output buffer. */
        private CircularBuffer<Comparable> output;

        /**
         * Creates a new instance of InsertionMerger.
         *
         * @param  buffers  the list of input buffers.
         * @param  output   the output buffer.
         */
        public InsertionMerger(CircularBuffer[] buffers,
                CircularBuffer<Comparable> output) {
            // Convert the list to an array for fast access and compaction.
            bufferCount = buffers.length;
            this.buffers = buffers;
            this.output = output;
            output.addObserver(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void merge() {
            // We assume the buffers have been populated. An exception will
            // be thrown if that is not the case.

            // Perform an insertion sort of the buffers using the leading values.
            for (int i = 1; i < bufferCount; i++) {
                CircularBuffer<Comparable> tmp = buffers[i];
                int j = i;
                while (j > 0 && tmp.peek().compareTo(buffers[j - 1].peek()) < 0) {
                    buffers[j] = buffers[j - 1];
                    j--;
                }
                buffers[j] = tmp;
            }

            // While output is not full and we have buffers to merge...
            while (!output.isFull() && bufferCount > 0) {
                if (bufferCount == 1) {
                    // Copy remainder of last stream to output.
                    CircularBuffer<Comparable> t = buffers[0];
                    int n = Math.min(t.size(), output.remaining());
                    if (n > 0) {
                        t.move(output, n);
                        if (t.isEmpty()) {
                            shift();
                        }
                    }
                } else if (bufferCount == 2) {
                    // With only two streams, perform a faster merge.
                    CircularBuffer<Comparable> a = buffers[0];
                    CircularBuffer<Comparable> b = buffers[1];
                    int count = output.remaining();
                    while (count > 0 && !a.isEmpty() && !b.isEmpty()) {
                        if (a.peek().compareTo(b.peek()) < 0) {
                            output.add(a.remove());
                        } else {
                            output.add(b.remove());
                        }
                        count--;
                    }
                    if (b.isEmpty()) {
                        buffers[1] = buffers[0];
                        shift();
                    }
                    if (a.isEmpty()) {
                        shift();
                    }
                } else {
                    // Add the first comparable to the output.
                    CircularBuffer<Comparable> t = buffers[0];
                    output.add(t.remove());
                    if (t.isEmpty()) {
                        // This stream has been exhausted.
                        shift();
                    } else {
                        // Insert new candidate into correct position in array.
                        Comparable s = t.peek();
                        int j = 1;
                        while (j < bufferCount && s.compareTo(buffers[j].peek()) > 0) {
                            buffers[j - 1] = buffers[j];
                            j++;
                        }
                        buffers[j - 1] = t;
                    }
                }
            }
        }

        /**
         * Remove the first buffer from the buffers array by moving all of
         * the other elements forward one position. Decrements the buffer
         * count by one.
         */
        private void shift() {
            for (int i = 1; i < bufferCount; i++) {
                buffers[i - 1] = buffers[i];
            }
            bufferCount--;
        }

        @Override
        public void update(Observable o, Object arg) {
            // Attempt to populate the recently emptied output buffer.
            merge();
        }
    }
}
