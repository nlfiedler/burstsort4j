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

/**
 * An implementation of the funnelsort algorithm as described by Frigo,
 * Leisersen, Prokop, and Ramachandran in "Cache Oblivious Algorithms"
 * (the extended abstract, based on the thesis by Prokop).
 *
 * @author Nathan Fiedler
 */
public class Funnelsort {

    /**
     * Sorts the set of strings using the "lazy" funnelsort algorithm as
     * described by Brodal, Fagerberg, and Vinther.
     *
     * @param  strings  array of strings to be sorted.
     */
    public static void sort(String[] strings) { // TODO: use Comparable instead of String
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
    private static void sort(String[] strings, int offset, int count) {
        // For arrays of trivial length, delegate to insertion sort.
        if (count < 21) {
            Insertionsort.sort(strings, offset, offset + count - 1);
        } else {

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
            List<CircularBuffer<String>> inputs =
                    new ArrayList<CircularBuffer<String>>(num_blocks + 1);
            mark = offset;
            for (int ii = 1; ii < num_blocks; ii++) {
                inputs.add(new CircularBuffer<String>(strings, mark, block_size, false));
                mark += block_size;
            }
            leftover = count - mark;
            if (leftover > 0) {
                inputs.add(new CircularBuffer<String>(strings, mark, leftover, false));
            }
            CircularBuffer<String> output = new CircularBuffer<String>(count);
            Kmerger merger = MergerFactory.createMerger(inputs, output);
            merger.merge();
            output.drain(strings, offset);
        }
    }

    /**
     * A Kmerger merges one or more input streams into a single output
     * stream, sorting the elements in the process.
     */
    private static interface Kmerger {

        /**
         * Return the reference to this merger's output buffer.
         *
         * @return  output buffer for this merger, or null if none.
         */
        CircularBuffer<String> getOutput();

        /**
         * Merges k^3 elements from the inputs and writes them to the output.
         */
        void merge();

        /**
         * Set the k value for the merger to dictate how many elements are
         * to be output during each merge operation.
         *
         * @param  k  the k value for the merger.
         */
        void setK(int k);
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
        public static Kmerger createMerger(List<CircularBuffer<String>> inputs,
                CircularBuffer<String> output) {
            int k = inputs.size();
            if (k == 1) {
                throw new IllegalArgumentException("cannot merge a single stream");
            } else if (k == 2) {
                CircularBuffer<String> lb = inputs.get(0);
                CircularBuffer<String> rb = inputs.get(1);
                return new BinaryMerger(null, lb, null, rb, output);
            } else if (k == 3) {
                CircularBuffer<String> b1 = inputs.get(0);
                CircularBuffer<String> b2 = inputs.get(1);
                // A two-way merger will output 8 elements, and output
                // buffers must be twice the expected output.
                CircularBuffer<String> out = new CircularBuffer<String>(16);
                Kmerger ma = new BinaryMerger(null, b1, null, b2, out);
                CircularBuffer<String> b3 = inputs.get(2);
                return new BinaryMerger(null, b3, ma, null, output);
            } else {
                return new BufferMerger(inputs, output);
            }
        }

        /**
         * Creates a new instance of Kmerger appropriate for the inputs.
         *
         * @param  output   buffer to which merged results are written.
         * @param  mergers  input mergers to be merged into a single stream.
         * @return  a Kmerger instance.
         */
        public static Kmerger createMerger(CircularBuffer<String> output,
                List<Kmerger> mergers) {
            int k = mergers.size();
            if (k == 1) {
                throw new IllegalArgumentException("cannot merge a single stream");
            } else if (k == 2) {
                Kmerger lm = mergers.get(0);
                Kmerger rm = mergers.get(1);
                return new BinaryMerger(lm, null, rm, null, output);
            } else if (k == 3) {
                Kmerger m1 = mergers.get(0);
                Kmerger m2 = mergers.get(1);
                // A two-way merger will output 8 elements, and output
                // buffers must be twice the expected output.
                CircularBuffer<String> out = new CircularBuffer<String>(16);
                Kmerger ma = new BinaryMerger(m1, null, m2, null, out);
                Kmerger m3 = mergers.get(2);
                return new BinaryMerger(ma, null, m3, null, output);
            } else {
                return new MergerMerger(output, mergers);
            }
        }
    }

    /**
     * A BufferMerger divides up the input streams into k^(1/2) groups
     * each of size k^(1/2), creating additional mergers for those
     * groups, and ultimately merging their output into a single buffer.
     */
    private static class BufferMerger implements Kmerger {
        /** The size of this k-merger. */
        private int k;
        /** The number of times to invoke the R merger to merge inputs. */
        private int k3half;
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
        public BufferMerger(List<CircularBuffer<String>> inputs,
                CircularBuffer<String> output) {
            setK(inputs.size());
            // Rounding up avoids creating excessive numbers of mergers.
            int kroot = Math.round((float) Math.sqrt((double) k));
            int twok3half = 2 * k3half;
            int offset = 0;
            Li = new ArrayList<Kmerger>(kroot + 1);
            for (int ii = 1; ii < kroot; ii++) {
                List<CircularBuffer<String>> li = inputs.subList(offset, offset + kroot);
                CircularBuffer<String> buffer = new CircularBuffer<String>(twok3half);
                Li.add(MergerFactory.createMerger(li, buffer));
                offset += kroot;
            }
            if (inputs.size() > offset) {
                List<CircularBuffer<String>> li = inputs.subList(offset, inputs.size());
                CircularBuffer<String> buffer = new CircularBuffer<String>(twok3half);
                Li.add(MergerFactory.createMerger(li, buffer));
            }
            R = MergerFactory.createMerger(output, Li);
            // Tell the R merger how many elements it should output.
            R.setK(kroot + 1);
        }

        @Override
        public CircularBuffer<String> getOutput() {
            return R.getOutput();
        }

        @Override
        public void merge() {
            // Invoke the R merger k^(3/2) times to generate our output.
            for (int ii = 0; ii < k3half; ii++) {
                // Make sure all of the buffers are at least half full.
                for (Kmerger merger : Li) {
                    CircularBuffer<String> buf = merger.getOutput();
                    if (buf.size() < k3half) {
                        merger.merge();
                    }
                }
                R.merge();
            }
        }

        @Override
        public void setK(int k) {
            this.k = k;
            k3half = Math.round((float) Math.sqrt((double) k * k * k));
        }
    }

    /**
     * A MergerMerger divides up the input mergers into k^(1/2) groups
     * each of size k^(1/2), creating additional mergers for those
     * groups, and ultimately merging their output into a single buffer.
     */
    private static class MergerMerger implements Kmerger {
        /** The size of this k-merger. */
        private int k;
        /** The number of times to invoke the R merger to merge inputs. */
        private int k3half;
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
        public MergerMerger(CircularBuffer<String> output, List<Kmerger> mergers) {
            setK(mergers.size());
            // Rounding up avoids creating excessive numbers of mergers.
            int kroot = Math.round((float) Math.sqrt((double) k));
            int twok3half = 2 * k3half;
            int offset = 0;
            Li = new ArrayList<Kmerger>(kroot + 1);
            for (int ii = 1; ii < kroot; ii++) {
                List<Kmerger> li = mergers.subList(offset, offset + kroot);
                CircularBuffer<String> buffer = new CircularBuffer<String>(twok3half);
                Li.add(MergerFactory.createMerger(buffer, li));
                offset += kroot;
            }
            if (mergers.size() > offset) {
                List<Kmerger> li = mergers.subList(offset, mergers.size());
                CircularBuffer<String> buffer = new CircularBuffer<String>(twok3half);
                Li.add(MergerFactory.createMerger(buffer, li));
            }
            R = MergerFactory.createMerger(output, Li);
            // Tell the R merger how many elements it should output.
            R.setK(kroot + 1);
        }

        @Override
        public CircularBuffer<String> getOutput() {
            return R.getOutput();
        }

        @Override
        public void merge() {
            // Invoke the R merger k^(3/2) times to generate our output.
            for (int ii = 0; ii < k3half; ii++) {
                // Make sure all of the buffers are at least half full.
                for (Kmerger merger : Li) {
                    CircularBuffer<String> buf = merger.getOutput();
                    if (buf.size() < k3half) {
                        merger.merge();
                    }
                }
                R.merge();
            }
        }

        @Override
        public void setK(int k) {
            this.k = k;
            k3half = Math.round((float) Math.sqrt((double) k * k * k));
        }
    }

    /**
     * A k-merger that merges two inputs, whether those are mergers or
     * buffers, or a mix of both.
     */
    private static class BinaryMerger implements Kmerger {
        /** The "left" k-merger for populating the input buffer, or null if none. */
        private Kmerger leftMerger;
        /** The "left" input buffer, possibly associated with the left merger,
         * if there is an associated merger. */
        private CircularBuffer<String> leftBuffer;
        /** The "right" k-merger for populating the input buffer, or null if none. */
        private Kmerger rightMerger;
        /** The "right" input buffer, possibly associated with the left merger,
         * if there is an associated merger. */
        private CircularBuffer<String> rightBuffer;
        /** The output buffer. */
        private CircularBuffer<String> output;
        /** The number of elements expected for each merge operation. */
        private int kpow3;
        /** Number of elements that should be in the input buffers,
         * otherwise the input mergers will be invoked. */
        private int k3half;

        /**
         * Creates a new instance of BinaryMerger.
         *
         * @param  leftMerger   the left input merger, if there is one.
         * @param  leftBuffer   the left input buffer, or null if leftMerger is non-null.
         * @param  rightMerger  the right input merger, if there is one.
         * @param  rightBuffer  the left input buffer, or null if leftMerger is non-null.
         * @param  output       the output buffer.
         */
        public BinaryMerger(Kmerger leftMerger, CircularBuffer<String> leftBuffer,
                Kmerger rightMerger, CircularBuffer<String> rightBuffer,
                CircularBuffer<String> output) {
            setK(2);
            this.leftMerger = leftMerger;
            if (leftMerger != null) {
                this.leftBuffer = leftMerger.getOutput();
            } else {
                this.leftBuffer = leftBuffer;
            }
            this.rightMerger = rightMerger;
            if (rightMerger != null) {
                this.rightBuffer = rightMerger.getOutput();
            } else {
                this.rightBuffer = rightBuffer;
            }
            this.output = output;
        }

        @Override
        public CircularBuffer<String> getOutput() {
            return output;
        }

        @Override
        public void merge() {
            // We assume our output buffer is sized appropriately such
            // that it has a capacity of 16 or greater.
            if (leftBuffer.size() < k3half && leftMerger != null) {
                leftMerger.merge();
            }
            if (rightBuffer.size() < k3half && rightMerger != null) {
                rightMerger.merge();
            }
            // Output k^3 elements from the two buffers using a simple merge.
            int count = kpow3;
            while (count > 0 && !leftBuffer.isEmpty() && !rightBuffer.isEmpty()) {
                if (leftBuffer.peek().compareTo(rightBuffer.peek()) < 0) {
                    output.add(leftBuffer.remove());
                } else {
                    output.add(rightBuffer.remove());
                }
                count--;
            }
            int n = Math.min(leftBuffer.size(), count);
            if (n > 0) {
                leftBuffer.move(output, n);
                count -= n;
            }
            n = Math.min(rightBuffer.size(), count);
            if (n > 0) {
                rightBuffer.move(output, n);
            }
        }

        @Override
        public void setK(int k) {
            kpow3 = k * k * k;
            // Binary merger differs in what it expects of its inputs.
            k3half = kpow3;
        }
    }
}
