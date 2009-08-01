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
import java.util.List;
import java.util.Random;

/**
 * Runs performance tests over several kinds of data for each of the
 * sort implementations, collecting run times and displaying the results.
 *
 * @author Nathan Fiedler
 */
public class MicroBenchmark {
    /** Size of the data sets used in testing sort performance. */
    private static enum DataSize {
        TINY    (10, 100000),
        SMALL   (50,  20000),
        MEDIUM (100,  10000),
        LARGE  (500,   2000);
        /** The quantity for this data size. */
        private final int value;
        /** Number of times to run this particular test. */
        private final int count;

        /**
         * Constructs a DataSize with a particular quantity.
         *
         * @param  value  the quantity.
         * @param  count  number of times to test this size.
         */
        DataSize(int value, int count) {
            this.value = value;
            this.count = count;
        }

        /**
         * Returns the number of test iterations to be performed for
         * this data size.
         *
         * @return  run count.
         */
        public int getCount() {
            return count;
        }

        /**
         * Returns the quantity for this data size.
         *
         * @return  quantity.
         */
        public int getValue() {
            return value;
        }
    };

    /**
     * Creates a new instance of MicroBenchmark.
     */
    private MicroBenchmark() {
    }

    /**
     * Command-line interface to benchmark driver.
     *
     * @param  args  command-line arguments.
     */
    public static void main(String[] args) {
        DataGenerator[] generators = new DataGenerator[]{
                    new RandomGenerator(),
                    new PsuedoWordGenerator(),
                    new RepeatGenerator(),
                    new RepeatCycleGenerator()
                };
        SortRunner[] runners = new SortRunner[]{
                    new CombsortRunner(),
                    new GnomesortRunner(),
                    new HeapsortRunner(),
                    new InsertionsortRunner(),
                    new SelectionsortRunner(),
                    new ShellsortRunner()
                };
        DataSize[] sizes = DataSize.values();
        try {
            runsorts(generators, runners, sizes);
        } catch (GeneratorException ge) {
            ge.printStackTrace();
        }
    }

    /**
     * Runs a set of sort routines over test data, as provided by the
     * given data generators. Performs a warmup run first to get all
     * of the classes compiled by the JVM, to avoid skewing the resuls.
     *
     * @param  generators  set of data generators to use.
     * @param  runners     set of sorters to compare.
     * @param  sizes       data sizes to be run.
     * @throws  GeneratorException  thrown if one of the generators fails.
     */
    private static void runsorts(DataGenerator[] generators,
            SortRunner[] runners, DataSize[] sizes) throws GeneratorException {
        // Warm up the JVM so that the classes get compiled and the
        // CPU comes up to full speed.
        System.out.println("Warming up the system, please wait...");
        for (DataGenerator generator : generators) {
            List<String> data = generator.generate(DataSize.LARGE);
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
            for (DataSize size : sizes) {
                int runCount = size.getCount();
                System.out.format("\t%s...\n", size.toString());
                List<String> data = generator.generate(size);
                for (SortRunner runner : runners) {
                    System.out.format("\t\t%-20s:\t", runner.getDisplayName());
                    // Track the time for running the test, which includes
                    // some overhead but that should be fine as it is the
                    // same for all of the tests.
                    long t1 = System.currentTimeMillis();
                    for (int run = 0; run < runCount; run++) {
                        String[] arr = data.toArray(new String[data.size()]);
                        runner.sort(arr);
                        if (run == 0) {
                            // Verify the results are actually sorted, just
                            // in case the unit tests missed something.
                            for (int ii = 1; ii < arr.length; ii++) {
                                if (arr[ii - 1].compareTo(arr[ii]) > 0) {
                                    System.err.format("\n\nSort %s failed!\n", runner.getDisplayName());
                                    System.err.format("%s > %s @ %d\n", arr[ii - 1], arr[ii], ii);
                                    System.exit(1);
                                }
                            }
                        }
                    }
                    long t2 = System.currentTimeMillis();
                    System.out.format("%d ms\n", t2 - t1);
                }
            }
        }
    }

    /**
     * Checked exception for the data generators.
     */
    private static class GeneratorException extends Exception {
        /** silence compiler warnings */
        private static final long serialVersionUID = 1L;

        /**
         * GeneratorException with a message.
         *
         * @param  msg  explanatory message.
         */
        public GeneratorException(String msg) {
            super(msg);
        }

        /**
         * GeneratorException with a cause.
         *
         * @param  cause  cause of the exception.
         */
        public GeneratorException(Throwable cause) {
            super(cause);
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
         * @throws  GeneratorException  thrown if generation fails.
         */
        List<String> generate(DataSize size) throws GeneratorException;

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
        public List<String> generate(DataSize size) throws GeneratorException {
            int count = size.getValue();
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
     * characters from the printable ASCII set (from 32 to 126).
     */
    private static class RandomGenerator implements DataGenerator {
        /** Size of the randomly generated strings. */
        private static final int LENGTH = 100;
        /** All printable characters in US-ASCII. */
        private static final int ALPHABET = 95;

        @Override
        public List<String> generate(DataSize size) throws GeneratorException {
            int count = size.getValue();
            Random r = new Random();
            List<String> list = new ArrayList<String>();
            StringBuilder sb = new StringBuilder();
            for (int ii = 0; ii < count; ii++) {
                for (int jj = 0; jj < LENGTH; jj++) {
                    int d = r.nextInt(ALPHABET);
                    sb.append((char) (' ' + d));
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
     * Generates a set of duplicate strings, comprised of an alphabet
     * of size one, where each string is 100 characters. One of three
     * pathological cases created to stress test the sort.
     */
    private static class RepeatGenerator implements DataGenerator {

        @Override
        public List<String> generate(DataSize size) throws GeneratorException {
            int count = size.getValue();
            List<String> list = Collections.nCopies(count,
                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
            return list;
        }

        @Override
        public String getDisplayName() {
            return "Repeated 100-A";
        }
    }

    /**
     * Generates a set of strings, comprised of an alphabet of size one,
     * where length increases from one to 100 characters in a cycle.
     * One of three pathological cases created to stress test the sort.
     */
    private static class RepeatCycleGenerator implements DataGenerator {

        @Override
        public List<String> generate(DataSize size) throws GeneratorException {
            String[] strs = new String[100];
            String seed = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
            for (int i = 0, l = 1; i < strs.length; i++, l++) {
                strs[i] = seed.substring(0, l);
            }
            List<String> list = new ArrayList<String>();
            for (int c = size.getValue(), i = 0; c > 0; i++, c--) {
                list.add(strs[i % strs.length]);
            }
            return list;
        }

        @Override
        public String getDisplayName() {
            return "Repeated 100-A decreasing cycle";
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

    private static class CombsortRunner implements SortRunner {

        @Override
        public String getDisplayName() {
            return "Combsort";
        }

        @Override
        public void sort(String[] data) {
            Combsort.sort(data);
        }
    }

    private static class GnomesortRunner implements SortRunner {

        @Override
        public String getDisplayName() {
            return "Gnomesort";
        }

        @Override
        public void sort(String[] data) {
            Gnomesort.sort(data);
        }
    }

    private static class HeapsortRunner implements SortRunner {

        @Override
        public String getDisplayName() {
            return "Heapsort";
        }

        @Override
        public void sort(String[] data) {
            Heapsort.sort(data);
        }
    }

    private static class InsertionsortRunner implements SortRunner {

        @Override
        public String getDisplayName() {
            return "Insertionsort";
        }

        @Override
        public void sort(String[] data) {
            Insertionsort.sort(data, 0, data.length - 1);
        }
    }

    private static class SelectionsortRunner implements SortRunner {

        @Override
        public String getDisplayName() {
            return "Selectionsort";
        }

        @Override
        public void sort(String[] data) {
            Selectionsort.sort(data, 0, data.length - 1);
        }
    }

    private static class ShellsortRunner implements SortRunner {

        @Override
        public String getDisplayName() {
            return "Shellsort";
        }

        @Override
        public void sort(String[] data) {
            Shellsort.sort(data);
        }
    }
}
