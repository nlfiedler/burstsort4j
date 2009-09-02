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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            if (k < 4) {
                throw new IllegalArgumentException("cannot merge so few streams");
            } else {
                return new RightMerger(inputs, output);
            }
        }

        /**
         * Creates a new instance of Kmerger appropriate for the inputs.
         *
         * @param  inputs  streams of sorted input to be merged.
         * @param  output  buffer to which merged results are written.
         * @return  a Kmerger instance.
         */
        public static Kmerger createMerger(Kmerger input1, Kmerger input2,
                CircularBuffer<String> output) {
            return new TwoWayMerger(input1, input2, output);
        }

        /**
         * Creates a new instance of Kmerger appropriate for the inputs.
         *
         * @param  m1      first of the mergers to merge.
         * @param  m2      second of the mergers to merge.
         * @param  m3      third of the mergers to merge.
         * @param  output  buffer to which merged results are written.
         * @return  a Kmerger instance.
         */
        public static Kmerger createMerger(Kmerger m1, Kmerger m2, Kmerger m3,
                CircularBuffer<String> output) {
            CircularBuffer<String> out = new CircularBuffer<String>(8);
            Kmerger ma = new TwoWayMerger(m1, m2, out);
            return new TwoWayMerger(m3, ma, output);
        }
    }

    /**
     * A RightMerger divides up the input streams into k^(1/2) groups
     * each of size k^(1/2), creating additional mergers for those
     * groups, and ultimately merging their output into a sngle buffer.
     */
    private static class RightMerger implements Kmerger {
        /** The size of this k-merger. */
        private final int k;
        /** The number of times to invoke the R merger to merge inputs. */
        private final int k3half;
        /** The right k-merger for merging the k^(1/2) input streams. */
        private final Kmerger R;
        /** The left k^(1/2) input streams each of size k^(1/2). */
        private final Map<CircularBuffer<String>, Kmerger> Li; // TODO: change to a list of Kmerger, use Kmerger.getOutput()

        /**
         * Creates a new instance of RightMerger.
         *
         * @param  inputs  streams of sorted input to be merged.
         * @param  output  buffer to which merged results are written.
         */
        public RightMerger(List<CircularBuffer<String>> inputs,
                CircularBuffer<String> output) {
            k = inputs.size();
            int kpow3 = k * k * k;
            k3half = Math.round((float) Math.sqrt((double) kpow3));
            // Rounding up avoids creating excessive numbers of mergers.
            int kroot = Math.round((float) Math.sqrt((double) k));
            List<CircularBuffer<String>> buffers =
                    new ArrayList<CircularBuffer<String>>(kroot + 1);
            int twok3half = 2 * k3half;
            int offset = 0;
            Li = new HashMap<CircularBuffer<String>, Kmerger>();
            for (int ii = 1; ii < kroot; ii++) {
                List<CircularBuffer<String>> li = inputs.subList(offset, offset + kroot);
                CircularBuffer<String> buffer = new CircularBuffer<String>(twok3half);
                buffers.add(buffer);
                Li.put(buffer, MergerFactory.createMerger(li, buffer));
                offset += kroot;
            }
            if (inputs.size() > offset) {
                List<CircularBuffer<String>> li = inputs.subList(offset, inputs.size());
                CircularBuffer<String> buffer = new CircularBuffer<String>(twok3half);
                buffers.add(buffer);
                Li.put(buffer, MergerFactory.createMerger(li, buffer));
            }
            if (kroot == 2) {
                Kmerger left = Li.get(buffers.get(0));
                Kmerger right = Li.get(buffers.get(1));
                R = MergerFactory.createMerger(left, right, output);
            } else if (kroot == 3) {
                Kmerger m1 = Li.get(buffers.get(0));
                Kmerger m2 = Li.get(buffers.get(1));
                Kmerger m3 = Li.get(buffers.get(2));
                R = MergerFactory.createMerger(m1, m2, m3, output);
            } else {
                R = MergerFactory.createMerger(buffers, output);
            }
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
                for (Map.Entry<CircularBuffer<String>, Kmerger> entry : Li.entrySet()) {
                    CircularBuffer<String> buf = entry.getKey();
                    if (buf.size() < k3half) {
                        Kmerger lk = entry.getValue();
                        lk.merge();
                    }
                }
                R.merge();
            }
        }
    }

    /**
     * A k-merger that merges the output of two k-mergers to a single output.
     */
    private static class TwoWayMerger implements Kmerger {
        /** The "left" k-merger for populating the input buffer. */
        private Kmerger leftMerger;
        /** The "right" k-merger for populating the input buffer. */
        private Kmerger rightMerger;
        /** The output buffer. */
        private CircularBuffer<String> output;

        /**
         * Creates a new instance of TwoWayMerger.
         *
         * @param  left    the left input merger.
         * @param  right   the right input merger.
         * @param  output  the output buffer.
         */
        public TwoWayMerger(Kmerger left, Kmerger right,
                CircularBuffer<String> output) {
            leftMerger = left;
            rightMerger = right;
            this.output = output;
        }

        @Override
        public CircularBuffer<String> getOutput() {
            return output;
        }

        @Override
        public void merge() {
            CircularBuffer<String> leftBuffer = leftMerger.getOutput();
            if (leftBuffer.size() < 8) {
                leftMerger.merge();
            }
            CircularBuffer<String> rightBuffer = rightMerger.getOutput();
            if (rightBuffer.size() < 8) {
                rightMerger.merge();
            }
            // Output k^3 elements from the two buffers using a simple merge.
            final int kpow3 = 8;
            int written = 0;
            while (written < kpow3 && !leftBuffer.isEmpty() && !rightBuffer.isEmpty()) {
                if (leftBuffer.peek().compareTo(rightBuffer.peek()) < 0) {
                    output.add(leftBuffer.remove());
                } else {
                    output.add(rightBuffer.remove());
                }
                written++;
            }
            int n = Math.min(leftBuffer.size(), kpow3 - written);
            if (n > 0) {
                leftBuffer.move(output, n);
                written += n;
            }
            n = Math.min(rightBuffer.size(), kpow3 - written);
            if (n > 0) {
                leftBuffer.move(output, n);
            }
        }
    }
}
