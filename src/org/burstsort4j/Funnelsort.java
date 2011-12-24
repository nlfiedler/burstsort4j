/*
 * Copyright 2009-2011 Nathan Fiedler. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package org.burstsort4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * An implementation of the funnelsort algorithm as described by Frigo,
 * Leisersen, Prokop, and Ramachandran in "Cache Oblivious Algorithms"
 * (the extended abstract, based on the thesis by Prokop).
 *
 * @author Nathan Fiedler
 */
public class Funnelsort {

    /** Size of inputs for which insertion sort should be used. */
    private static final int INSERTSORT_THRESHOLD = 21;

    private Funnelsort() {
    }

    /**
     * Sorts the set of strings using the "lazy" funnelsort algorithm as
     * described by Brodal, Fagerberg, and Vinther.
     *
     * @param  <T>      type of comparable to be sorted.
     * @param  strings  array of strings to be sorted.
     */
    public static <T extends Comparable<? super T>> void sort(T[] strings) {
        if (strings == null || strings.length < 2) {
            return;
        }
        sort(strings, 0, strings.length);
    }

    /**
     * Sorts the elements within the array starting at the offset and
     * ending at offset plus the count.
     *
     * @param  <T>      type of comparable to be sorted.
     * @param  strings  array containing elements to be sorted.
     * @param  offset   first position within array to be sorted.
     * @param  count    number of elements from offset to be sorted.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Comparable<? super T>> void sort(T[] strings, int offset, int count) {
        if (count > INSERTSORT_THRESHOLD) {
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
            List<CircularBuffer<T>> inputs =
                    new ArrayList<CircularBuffer<T>>(num_blocks);
            mark = offset;
            for (int ii = 1; ii < num_blocks; ii++) {
                inputs.add(new CircularBuffer<T>(strings, mark, block_size, false));
                mark += block_size;
            }
            leftover = count - (mark - offset);
            if (leftover > 0) {
                inputs.add(new CircularBuffer<T>(strings, mark, leftover, false));
            }
            CircularBuffer<T> output = new CircularBuffer<T>(count);
            Kmerger<T> merger = MergerFactory.createMerger(inputs, output);
            merger.merge();
            output.drain(strings, offset);
        } else {
            // For small subarrays, delegate to insertion sort.
            Insertionsort.sort(strings, offset, offset + count - 1);
        }
    }

    /**
     * A Kmerger merges one or more input streams into a single output
     * stream, sorting the elements in the process.
     *
     * @param  <E>  type of elements being merged.
     */
    private static interface Kmerger<E extends Comparable<? super E>> {

        /**
         * Return the reference to this merger's output buffer.
         *
         * @return  output buffer for this merger, or null if none.
         */
        CircularBuffer<E> getOutput();

        /**
         * Merges k^3 elements from the inputs and writes them to the output.
         */
        void merge();
    }

    /**
     * MergerFactory creates instances of Kmerger based on the given inputs.
     */
    private static class MergerFactory {

        private MergerFactory() {
        }

        /**
         * Creates a new instance of Kmerger appropriate for the inputs.
         *
         * @param  inputs  streams of sorted input to be merged.
         * @param  output  buffer to which merged results are written.
         * @return  a Kmerger instance.
         */
        public static <T extends Comparable<? super T>> Kmerger<T> createMerger(
                List<CircularBuffer<T>> inputs,
                CircularBuffer<T> output) {
            int k = inputs.size();
            if (k == 1) {
                return new UnaryMerger<T>(inputs.get(0), output);
            } else if (k == 2) {
                CircularBuffer<T> lb = inputs.get(0);
                CircularBuffer<T> rb = inputs.get(1);
                return new BinaryMerger<T>(lb, rb, output);
            } else if (k == 3) {
                CircularBuffer<T> b1 = inputs.get(0);
                CircularBuffer<T> o1 = new CircularBuffer<T>(16);
                List<Kmerger<T>> mergers = new ArrayList<Kmerger<T>>(2);
                mergers.add(new UnaryMerger<T>(b1, o1));
                CircularBuffer<T> b2 = inputs.get(1);
                CircularBuffer<T> b3 = inputs.get(2);
                CircularBuffer<T> o2 = new CircularBuffer<T>(16);
                mergers.add(new BinaryMerger<T>(b2, b3, o2));
                return new BufferMerger<T>(output, mergers);
            } else {
                return new BufferMerger<T>(inputs, output);
            }
        }
    }

    /**
     * A BufferMerger divides up the input streams into k^(1/2) groups
     * each of size k^(1/2), creating additional mergers for those
     * groups, and ultimately merging their output into a single buffer.
     *
     * @param  <E>  the type of the buffer elements.
     */
    private static class BufferMerger<E extends Comparable<? super E>>
            implements Kmerger<E>, Observer {

        /** The number of times to invoke the R merger to merge inputs. */
        private final int k3half;
        /** The right k-merger for merging the k^(1/2) input streams. */
        private final Kmerger<E> R;
        /** The left k^(1/2) input streams each of size k^(1/2). */
        private final List<Kmerger<E>> Li;

        /**
         * Creates a new instance of BufferMerger.
         *
         * @param  inputs  streams of sorted input to be merged.
         * @param  output  buffer to which merged results are written.
         */
        BufferMerger(CircularBuffer<E> output,
                List<Kmerger<E>> mergers) {
            int k = mergers.size() ^ 2;
            k3half = Math.round((float) Math.sqrt((double) k * k * k));
            List<CircularBuffer<E>> buffers =
                    new ArrayList<CircularBuffer<E>>();
            for (Kmerger<E> merger : mergers) {
                buffers.add(merger.getOutput());
            }
            Li = mergers;
            R = MergerFactory.createMerger(buffers, output);
            output.addObserver(this);
        }

        /**
         * Creates a new instance of BufferMerger.
         *
         * @param  inputs  streams of sorted input to be merged.
         * @param  output  buffer to which merged results are written.
         */
        BufferMerger(List<CircularBuffer<E>> inputs,
                CircularBuffer<E> output) {
            int k = inputs.size();
            // Rounding up avoids creating excessive numbers of mergers.
            int kroot = Math.round((float) Math.sqrt((double) k));
            k3half = Math.round((float) Math.sqrt((double) k * k * k));
            int twok3half = 2 * k3half;
            int offset = 0;
            // Set up the list of buffers for the right side merger.
            List<CircularBuffer<E>> buffers =
                    new ArrayList<CircularBuffer<E>>();
            // Create mergers for the left inputs by dividing up the
            // inputs into roughly equal-sized sublists.
            Li = new ArrayList<Kmerger<E>>(kroot);
            for (int ii = 1; ii < kroot; ii++) {
                List<CircularBuffer<E>> li = inputs.subList(offset, offset + kroot);
                CircularBuffer<E> buffer = new CircularBuffer<E>(twok3half);
                buffers.add(buffer);
                Li.add(MergerFactory.createMerger(li, buffer));
                offset += kroot;
            }
            if (inputs.size() > offset) {
                List<CircularBuffer<E>> li = inputs.subList(offset, inputs.size());
                CircularBuffer<E> buffer = new CircularBuffer<E>(twok3half);
                buffers.add(buffer);
                Li.add(MergerFactory.createMerger(li, buffer));
            }
            R = MergerFactory.createMerger(buffers, output);
            output.addObserver(this);
        }

        @Override
        public CircularBuffer<E> getOutput() {
            return R.getOutput();
        }

        @Override
        public void merge() {
            // Invoke the R merger k^(3/2) times to generate our output.
            for (int ii = 0; ii < k3half; ii++) {
                // Make sure all of the buffers are at least half full.
                for (Kmerger<E> merger : Li) {
                    CircularBuffer<E> buf = merger.getOutput();
                    if (buf.size() < (buf.capacity() / 2)) {
                        merger.merge();
                    }
                }
                R.merge();
            }
        }

        @Override
        public void update(Observable o, Object arg) {
            merge();
        }
    }

    /**
     * A k-merger that merges two input buffers.
     */
    private static class BinaryMerger<E extends Comparable<? super E>>
            implements Kmerger<E>, Observer {

        /** The "left" input buffer. */
        private final CircularBuffer<E> leftBuffer;
        /** The "right" input buffer. */
        private final CircularBuffer<E> rightBuffer;
        /** The output buffer. */
        private final CircularBuffer<E> output;

        /**
         * Creates a new instance of BinaryMerger.
         *
         * @param  leftBuffer   the left input buffer.
         * @param  rightBuffer  the right input buffer.
         * @param  output       the output buffer.
         */
        BinaryMerger(CircularBuffer<E> leftBuffer,
                CircularBuffer<E> rightBuffer,
                CircularBuffer<E> output) {
            this.leftBuffer = leftBuffer;
            this.rightBuffer = rightBuffer;
            this.output = output;
            output.addObserver(this);
        }

        @Override
        public CircularBuffer<E> getOutput() {
            return output;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void merge() {
            int count = output.remaining();
//            if (leftBuffer.isEmpty()) {
//                int n = Math.min(rightBuffer.size(), count);
//                if (n > 0) {
//                    rightBuffer.move(output, n);
//                }
//            } else if (rightBuffer.isEmpty()) {
//                int n = Math.min(leftBuffer.size(), count);
//                if (n > 0) {
//                    leftBuffer.move(output, n);
//                    count -= n;
//                }
//            } else {
                // Try to fill the output buffer with whatever input is
                // available, so long as neither input becomes empty.
                while (count > 0 && !leftBuffer.isEmpty() && !rightBuffer.isEmpty()) {
                    if (leftBuffer.peek().compareTo(rightBuffer.peek()) < 0) {
                        output.add(leftBuffer.remove());
                    } else {
                        output.add(rightBuffer.remove());
                    }
                    count--;
                }
//            }
//            // Output k^3 elements from the two buffers using a simple merge.
//            int count = output.remaining();
//            while (count > 0 && !leftBuffer.isEmpty() && !rightBuffer.isEmpty()) {
//                if (leftBuffer.peek().compareTo(rightBuffer.peek()) < 0) {
//                    output.add(leftBuffer.remove());
//                } else {
//                    output.add(rightBuffer.remove());
//                }
//                count--;
//            }
//            int n = Math.min(leftBuffer.size(), count);
//            if (n > 0) {
//                leftBuffer.move(output, n);
//                count -= n;
//            }
//            n = Math.min(rightBuffer.size(), count);
//            if (n > 0) {
//                rightBuffer.move(output, n);
//            }
        }

        @Override
        public void update(Observable o, Object arg) {
            merge();
        }
    }

    /**
     * A k-merger that streams a single input buffer to its output buffer.
     */
    private static class UnaryMerger<E extends Comparable<? super E>>
            implements Kmerger<E>, Observer {

        /** The input buffer. */
        private final CircularBuffer<E> input;
        /** The output buffer. */
        private final CircularBuffer<E> output;

        /**
         * Creates a new instance of UnaryMerger.
         *
         * @param  input   the input buffer.
         * @param  output  the output buffer.
         */
        UnaryMerger(CircularBuffer<E> input,
                CircularBuffer<E> output) {
            this.input = input;
            this.output = output;
            output.addObserver(this);
        }

        @Override
        public CircularBuffer<E> getOutput() {
            return output;
        }

        @Override
        public void merge() {
            // Fill the output buffer with whatever is available.
            int n = Math.min(input.size(), output.remaining());
            if (n > 0) {
                input.move(output, n);
            }
        }

        @Override
        public void update(Observable o, Object arg) {
            merge();
        }
    }
}
