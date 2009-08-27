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

        // Divide input strings into n^(1/3) arrays of size n^(2/3).
        int n13 = Math.round((float) Math.cbrt((double) strings.length));
        //int n23 = n13 * n13;
        int n23 = strings.length / n13;
        int offset = 0;
        List<CircularBuffer<String>> inputs = new ArrayList<CircularBuffer<String>>(n13);
        for (int ii = 1; ii < n13; ii++) {
            inputs.add(new CircularBuffer<String>(strings, offset, n23, false));
            offset += n23;
        }
        int leftover = strings.length - offset;
        if (leftover > 0) {
            inputs.add(new CircularBuffer<String>(strings, offset, leftover, false));
        }

        // Recursively sort the n^(1/3) arrays.
        CircularBuffer<String> output = new CircularBuffer<String>(strings.length);
        Kmerger kmerger = MergerFactory.createMerger(inputs, output);
// XXX: this seems hackish, isn't there a better way?
        kmerger = MergerFactory.createRoot(Collections.singletonList(kmerger), output);
        kmerger.merge();

        // Copy sorted output to strings array.
        output.drain(strings, 0);
    }

    /**
     * A Kmerger merges one or more input streams into a single output
     * stream, sorting the elements in the process.
     */
    private static interface Kmerger {

        /**
         * Returns the size of this k-merger.
         *
         * @return  the 'k' value for this merger.
         */
        int getK();

        /**
         * Merges k^3 elements from the inputs and writes them to the output.
         */
        void merge();

        /**
         * Set the destination of the output of this merger.
         *
         * @param  output  buffer to which merged output is written.
         */
        void setOutput(CircularBuffer<String> output);
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
            if (inputs.get(0).isEmpty()) {
                return new RightMerger(inputs, output);
            } else {
                int k = inputs.size();
                if (k == 1) {
                    return new SingleSortingMerger(inputs.get(0), output);
                } else if (k == 2) {
                    return new SortingMerger(inputs.get(0), inputs.get(1), output);
                } else {
                    return new RightMerger(inputs, output);
                }
            }
        }

        /**
         * Creates a new instance of Kmerger appropriate for the inputs.
         *
         * @param  inputs  streams of sorted input to be merged.
         * @param  output  buffer to which merged results are written.
         * @return  a Kmerger instance.
         */
        public static Kmerger createMerger(Kmerger input,
                CircularBuffer<String> output) {
            return new SingleMerger(input, output);
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

        public static Kmerger createRoot(List<Kmerger> inputs,
                CircularBuffer<String> output) {
            return new RootMerger(inputs, output);
        }
    }

    /**
     * A k-merger that simply pipes an input buffer to an output buffer,
     * acting like a k-merger but not performing any merging. The inputs
     * are sorted using a comparison-based string sorting algorithm.
     */
    private static class SingleSortingMerger implements Kmerger {
        /** The input buffer. */
        private CircularBuffer<String> input;
        /** The output buffer. */
        private CircularBuffer<String> output;
        /** True if the input has already been sorted. */
        private boolean sorted;

        /**
         * Creates a new instance of SingleSortingMerger.
         *
         * @param  input   the input buffer.
         * @param  output  the output buffer.
         */
        public SingleSortingMerger(CircularBuffer<String> input,
                CircularBuffer<String> output) {
            this.input = input;
            this.output = output;
        }

        @Override
        public int getK() {
            // Even though this is a single, it pretends to be a 2-way.
            return 2;
        }

        @Override
        public void merge() {
            // Lazily sort the input buffer the first time we're asked
            // to produce output.
            if (!sorted) {
                String[] arr = new String[input.size()];
                input.drain(arr, 0);
                Quicksort.sort(arr);
                input = new CircularBuffer<String>(arr, false);
                sorted = true;
            }
            // Copy k^3 elements from the input buffer to the output.
            final int kpow3 = 8;
            int n = Math.min(input.size(), kpow3);
            if (n > 0) {
                input.move(output, n);
            }
        }

        @Override
        public void setOutput(CircularBuffer<String> output) {
            this.output = output;
        }
    }

    /**
     * A simple k-merger that merges two inputs streams into a single
     * output stream. The input streams are sorted prior to merging.
     */
    private static class SortingMerger implements Kmerger {
        /** The "left" input buffer. */
        private CircularBuffer<String> left;
        /** The "right" input buffer. */
        private CircularBuffer<String> right;
        /** The output buffer. */
        private CircularBuffer<String> output;
        /** True if the input has already been sorted. */
        private boolean sorted;

        /**
         * Creates a new instance of SortingMerger.
         *
         * @param  left    the left input buffer.
         * @param  right   the right input buffer.
         * @param  output  the output buffer.
         */
        public SortingMerger(CircularBuffer<String> left,
                CircularBuffer<String> right,
                CircularBuffer<String> output) {
            this.left = left;
            this.right = right;
            this.output = output;
        }

        @Override
        public int getK() {
            return 2;
        }

        @Override
        public void merge() {
            // Lazily sort the input buffer the first time we're asked
            // to produce output.
            if (!sorted) {
                String[] arr = new String[left.size()];
                left.drain(arr, 0);
                Quicksort.sort(arr);
                left = new CircularBuffer<String>(arr, false);
                arr = new String[right.size()];
                right.drain(arr, 0);
                Quicksort.sort(arr);
                right = new CircularBuffer<String>(arr, false);
                sorted = true;
            }
            // Output k^3 elements from the two buffers using a simple merge.
            final int kpow3 = 8;
            int written = 0;
            while (written < kpow3 && !left.isEmpty() && !right.isEmpty()) {
                if (left.peek().compareTo(right.peek()) < 0) {
                    output.add(left.remove());
                } else {
                    output.add(right.remove());
                }
                written++;
            }
            int n = Math.min(left.size(), kpow3 - written);
            if (n > 0) {
                left.move(output, n);
                written += n;
            }
            n = Math.min(right.size(), kpow3 - written);
            if (n > 0) {
                right.move(output, n);
            }
        }

        @Override
        public void setOutput(CircularBuffer<String> output) {
            this.output = output;
        }
    }

    /**
     * An RightMerger divides up the input streams into k^(1/2) groups
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
        private final Map<CircularBuffer<String>, Kmerger> Li;

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
// XXX: seems this could be more efficient in handling small numbers of inputs,
//      such that odd inputs are merged directly into the R merger without an
//      intermediate "copy" merger
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
            if (kroot == 1) {
                Kmerger merger = Li.get(buffers.get(0));
                R = MergerFactory.createMerger(merger, output);
            } else if (kroot == 2) {
                Kmerger left = Li.get(buffers.get(0));
                Kmerger right = Li.get(buffers.get(1));
                R = MergerFactory.createMerger(left, right, output);
            } else {
                R = MergerFactory.createMerger(buffers, output);
            }
        }

        @Override
        public int getK() {
            return k;
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

        @Override
        public void setOutput(CircularBuffer<String> output) {
        }
    }

    /**
     * A k-merger that simply pipes an input buffer to an output buffer,
     * acting like a k-merger but not performing any merging.
     */
    private static class SingleMerger implements Kmerger {
        /** The number of times to invoke the input merger. */
        private final int k3half;
        /** The "left" k-merger for populating the input buffer. */
        private final Kmerger merger;
        /** The input buffer. */
        private final CircularBuffer<String> input;
        /** The output buffer. */
        private CircularBuffer<String> output;

        /**
         * Creates a new instance of SingleMerger.
         *
         * @param  input   the input merger.
         * @param  output  the output buffer.
         */
        public SingleMerger(Kmerger merger,
                CircularBuffer<String> output) {
            this.merger = merger;
            int k = merger.getK();
            int kpow3 = k * k * k;
            k3half = Math.round((float) Math.sqrt((double) kpow3));
            int twok3half = 2 * k3half;
            input = new CircularBuffer<String>(twok3half);
            this.output = output;
        }

        @Override
        public int getK() {
            // Even though this is a single, it pretends to be a 2-way.
            return 2;
        }

        @Override
        public void merge() {
            // Invoke the left merger to populate the input buffer.
            if (input.size() < k3half) {
                merger.merge();
            }
            // Copy k^3 elements from the input buffer to the output.
            final int kpow3 = 8;
            int n = Math.min(input.size(), kpow3);
            if (n > 0) {
                input.move(output, n);
            }
        }

        @Override
        public void setOutput(CircularBuffer<String> output) {
            this.output = output;
        }
    }

    /**
     * A k-merger that merges two input buffers to an output buffer.
     */
    private static class TwoWayMerger implements Kmerger {
        /** The number of times to invoke the input mergers. */
        private int k3half;
        /** The "left" k-merger for populating the input buffer. */
        private Kmerger leftMerger;
        /** The "right" k-merger for populating the input buffer. */
        private Kmerger rightMerger;
        /** The "left" input buffer. */
        private CircularBuffer<String> leftBuffer;
        /** The "right" input buffer. */
        private CircularBuffer<String> rightBuffer;
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
            int k = left.getK();
            int kpow3 = k * k * k;
            k3half = Math.round((float) Math.sqrt((double) kpow3));
            int twok3half = 2 * k3half;
            leftBuffer = new CircularBuffer<String>(twok3half);
            k = left.getK();
            kpow3 = k * k * k;
            twok3half = 2 * Math.round((float) Math.sqrt((double) kpow3));
            rightBuffer = new CircularBuffer<String>(twok3half);
            this.output = output;
        }

        @Override
        public int getK() {
            return 2;
        }

        @Override
        public void merge() {
            if (leftBuffer.size() < k3half) {
                leftMerger.merge();
            }
            if (rightBuffer.size() < k3half) {
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

        @Override
        public void setOutput(CircularBuffer<String> output) {
            this.output = output;
        }
    }

    /**
     * A form of intermediate merger that encapsulates whatever has been
     * constructed by the factory to ensure the child mergers are invoked
     * a sufficient number of times to produce the entire output.
     */
    private static class RootMerger implements Kmerger {
        /** The size of this merger. */
        private final int k;
        /** The number of times to invoke the R merger to merge inputs. */
        private final int k3half;
        /** The right k-merger for merging the k^(1/2) input streams. */
        private final Kmerger R;
        /** The left k^(1/2) input streams each of size k^(1/2). */
        private final Map<CircularBuffer<String>, Kmerger> Li;

        public RootMerger(List<Kmerger> inputs, CircularBuffer<String> output) {
            int sqrt_k = inputs.size();
            k = sqrt_k * sqrt_k;
            int kpow3 = k * k * k;
            k3half = Math.round((float) Math.sqrt((double) kpow3));
            int twok3half = 2 * k3half;
            List<CircularBuffer<String>> buffers =
                    new ArrayList<CircularBuffer<String>>(sqrt_k);
            Li = new HashMap<CircularBuffer<String>, Kmerger>();
            for (Kmerger merger : inputs) {
                CircularBuffer<String> buffer = new CircularBuffer<String>(twok3half);
                buffers.add(buffer);
                merger.setOutput(buffer);
                Li.put(buffer, merger);
            }
            if (sqrt_k == 1) {
                Kmerger merger = Li.get(buffers.get(0));
                R = MergerFactory.createMerger(merger, output);
            } else if (sqrt_k == 2) {
                Kmerger left = Li.get(buffers.get(0));
                Kmerger right = Li.get(buffers.get(1));
                R = MergerFactory.createMerger(left, right, output);
            } else {
                R = MergerFactory.createMerger(buffers, output);
            }
        }

        @Override
        public int getK() {
            return k;
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

        @Override
        public void setOutput(CircularBuffer<String> output) {
        }
    }
}
