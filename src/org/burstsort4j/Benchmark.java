/*
 * Copyright (C) 2008  Nathan Fiedler
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
 * $Id: $
 */

package org.burstsort4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Runs performance tests over several kinds of data for each of the
 * sort implementations, collecting run times and displaying the results.
 *
 * @author Nathan Fiedler
 */
public class Benchmark {
    /** Number of times each sort implementation is run for each data set. */
    private static final int RUN_COUNT = 5;
    /** Size of the data sets used in testing sort performance. */
    public enum DataSize { SMALL, MEDIUM, LARGE };

    /**
     * Command-line interface to benchmark driver.
     *
     * @param  args  command-line arguments.
     */
    public static void main(String[] args) {
        DataGenerator[] generators = new DataGenerator[] {
            new RandomGenerator(),
            new PsuedoWordGenerator(),
        };
        SortRunner[] runners = new SortRunner[] {
            new MergesortRunner(),
            new QuicksortRunner(),
            new Multikey1Runner(),
            new Multikey2Runner(),
            new BurstsortRunner(),
        };
        // Warm up the JVM so that the classes get compiled and the
        // CPU comes up to full speed.
        System.out.println("Warming up the system, please wait...");
        for (DataGenerator generator : generators) {
            List<String> data = generator.generate(DataSize.SMALL);
            for (SortRunner runner : runners) {
                String[] arr = data.toArray(new String[data.size()]);
                runner.sort(arr);
            }
        }

        // For each type of data set, and each data set size, and
        // each sort implementation, run the sort several times and
        // calculate an average run time.
        for (DataGenerator generator : generators) {
            System.out.format("%s...\n", generator.getDisplayName());
            for (DataSize size : DataSize.values()) {
                System.out.format("\t%s...\n", size.toString());
                List<String> data = generator.generate(size);
                for (SortRunner runner : runners) {
                    System.out.format("\t\t%s:\t", runner.getDisplayName());
                    long[] times = new long[RUN_COUNT];
                    for (int run = 0; run < times.length; run++) {
                        String[] arr = data.toArray(new String[data.size()]);
                        long t1 = System.currentTimeMillis();
                        runner.sort(arr);
                        long t2 = System.currentTimeMillis();
                        times[run] = t2 - t1;
                    }
                    // Find the average of the run times, dropping the
                    // high and low values. The run times should never
                    // be more than a couple of minutes, so these
                    // calculations will never overflow.
                    Arrays.sort(times);
                    long total = 0;
                    for (int run = 1; run < RUN_COUNT - 1; run++) {
                        total += times[run];
                    }
                    long average = total / (RUN_COUNT - 2);
                    System.out.format("%d ms\n", average);
                }
            }
        }
    }

    /**
     * Creates a set of data to be sorted.
     */
    private static interface DataGenerator {

        /**
         * Generate data for testing the sort implementations.
         *
         * @param  size  size of the data to be generated.
         * @return  list of strings.
         */
        List<String> generate(DataSize size);

        /**
         * Returns the display name for this generator.
         *
         * @return  display name.
         */
        String getDisplayName();
    }

    /**
     * Generates a set of psuedo words, comprised of at least one letter,
     * up to the length of the longest (real) English word, using only
     * the lower-case letters.
     */
    private static class PsuedoWordGenerator implements DataGenerator {
        /** Longest (real) word in English: antidisestablishmentarianism */
        private static final int LONGEST = 28;
        /** Letters in the English alphabet (lower case only) */
        private static final int ALPHABET = 26;

        @Override
        public List<String> generate(DataSize size) {
            int count = 0;
            switch (size) {
                case SMALL:
                    count = 100000;
                    break;
                case MEDIUM:
                    count = 1000000;
                    break;
                case LARGE:
                    count = 3000000;
                    break;
            }
            Random r = new Random();
            List<String> list = new ArrayList<String>();
            StringBuilder sb = new StringBuilder();
            for (int ii = 0; ii < count; ii++) {
                int length = r.nextInt(LONGEST) + 1;
                for (int jj = 0; jj < length; jj++) {
                    int d = r.nextInt(ALPHABET);
                    sb.append((char) ('a' + d));
                }
                list.add(sb.toString());
                sb.setLength(0);
            }
            return list;
        }

        @Override
        public String getDisplayName() {
            return "Psuedo words";
        }
    }

    /**
     * Generates strings of a fixed length, comprised of randomly selected
     * letters (mixed case) and numbers.
     */
    private static class RandomGenerator implements DataGenerator {
        /** Size of the randomly generated strings. */
        private static final int LENGTH = 64;
        /** Upper/lowercase letters, digits */
        private static final int ALPHABET = 62;

        @Override
        public List<String> generate(DataSize size) {
            int count = 0;
            switch (size) {
                case SMALL:
                    count = 100000;
                    break;
                case MEDIUM:
                    count = 1000000;
                    break;
                case LARGE:
                    count = 3000000;
                    break;
            }
            Random r = new Random();
            List<String> list = new ArrayList<String>();
            StringBuilder sb = new StringBuilder();
            for (int ii = 0; ii < count; ii++) {
                for (int jj = 0; jj < LENGTH; jj++) {
                    int d = r.nextInt(ALPHABET);
                    if (d < 10) {
                        sb.append((char) ('0' + d));
                    } else if (d < 36) {
                        sb.append((char) ('A' + (d - 10)));
                    } else {
                        sb.append((char) ('a' + (d - 36)));
                    }
                }
                list.add(sb.toString());
                sb.setLength(0);
            }
            return list;
        }

        @Override
        public String getDisplayName() {
            return "Random";
        }
    }

    /**
     * Runs a particular sort implementation.
     */
    private static interface SortRunner {

        /**
         * Returns the display name for this runner.
         *
         * @return  display name.
         */
        String getDisplayName();

        /**
         * Sort the given array of strings.
         *
         * @param  data  strings to be sorted.
         */
        void sort(String[] data);
    }

    /**
     * Runs the burstsort implementation.
     */
    private static class BurstsortRunner implements SortRunner {

        @Override
        public String getDisplayName() {
            return "Burstsort";
        }

        @Override
        public void sort(String[] data) {
            Burstsort.sort(data);
        }
    }

    /**
     * Runs the mergesort implementation in java.util.Arrays. This is
     * here simply to provide a basis for comparing everything else,
     * and because it is part of the Java core classes, with a typical
     * runtime of n*log(n).
     */
    private static class MergesortRunner implements SortRunner {

        @Override
        public String getDisplayName() {
            return "Mergesort";
        }

        @Override
        public void sort(String[] data) {
            // This uses a merge sort.
            Arrays.sort(data);
        }
    }

    /**
     * Runs the less-than-optimal multikey quicksort implementation.
     */
    private static class Multikey1Runner implements SortRunner {

        @Override
        public String getDisplayName() {
            return "Multikey 1";
        }

        @Override
        public void sort(String[] data) {
            MultikeyQuicksort.multikey1(data);
        }
    }

    /**
     * Runs the preferred multikey quicksort implementation.
     */
    private static class Multikey2Runner implements SortRunner {

        @Override
        public String getDisplayName() {
            return "Multikey 2";
        }

        @Override
        public void sort(String[] data) {
            MultikeyQuicksort.multikey2(data);
        }
    }

    /**
     * Runs the basic quicksort implementation.
     */
    private static class QuicksortRunner implements SortRunner {

        @Override
        public String getDisplayName() {
            return "Quicksort";
        }

        @Override
        public void sort(String[] data) {
            Quicksort.sort(data);
        }
    }
}
