/*
 * Copyright (C) 2008-2009  Nathan Fiedler
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    private static enum DataSize {
        SMALL   (333000),
        MEDIUM (1000000),
        LARGE  (3000000);
        /** The quantity for this data size. */
        private final int value;

        /**
         * Constructs a DataSize with a particular quantity.
         *
         * @param  value  the quantity.
         */
        DataSize(int value) {
            this.value = value;
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
     * Creates a new instance of Benchmark.
     */
    private Benchmark() {
    }

    /**
     * Command-line interface to benchmark driver.
     *
     * @param  args  command-line arguments.
     */
    public static void main(String[] args) {
        DataGenerator[] generators = null;
        SortRunner[] runners = null;
        DataSize[] sizes = null;
        if (args.length == 0) {
            // With no arguments, run the random data generators and all
            // of the data sizes.
            generators = new DataGenerator[] {
                        new RandomGenerator(),
                        new PsuedoWordGenerator(),
                        new RepeatGenerator(),
                        new SmallAlphabetGenerator(),
                        new RepeatCycleGenerator(),
                        new GenomeGenerator()
            };
            runners = new SortRunner[] {
                        new MergesortRunner(),
                        new QuicksortRunner(),
                        new MultikeyRunner(),
                        new BurstsortRunner(),
                        new RedesignedBurstsortRunner()
            };
            sizes = DataSize.values();
        } else if (args.length == 1) {
            if (args[0].equals("--parallel")) {
                generators = new DataGenerator[] {
                            new RandomGenerator(),
                            new PsuedoWordGenerator(),
                            new RepeatGenerator(),
                            new SmallAlphabetGenerator(),
                            new RepeatCycleGenerator(),
                            new GenomeGenerator()
                };
                sizes = DataSize.values();
                runners = new SortRunner[] {
                            new BurstsortRunner(),
                            new BurstsortThreadPoolRunner(),
                            new RedesignedBurstsortRunner(),
                            new RedesignedBurstsortThreadPoolRunner()
                };
            } else if (args[0].equals("--burstsort")) {
                generators = new DataGenerator[] {
                            new RandomGenerator(),
                            new PsuedoWordGenerator(),
                            new RepeatGenerator(),
                            new SmallAlphabetGenerator(),
                            new RepeatCycleGenerator(),
                            new GenomeGenerator()
                };
                sizes = DataSize.values();
                runners = new SortRunner[] {
                            new BurstsortRunner(),
                            new RedesignedBurstsortRunner()
                };
            } else if (args[0].equals("--only-parallel-large")) {
                generators = new DataGenerator[] {
                            new RandomGenerator(),
                            new PsuedoWordGenerator(),
                            new RepeatGenerator(),
                            new SmallAlphabetGenerator(),
                            new RepeatCycleGenerator(),
                            new GenomeGenerator()
                };
                sizes = new DataSize[]{DataSize.LARGE};
                runners = new SortRunner[] {
                            new BurstsortThreadPoolRunner(),
                            new RedesignedBurstsortThreadPoolRunner()
                };
            } else {
                usage();
                System.exit(1);
            }
        } else if (args.length == 2) {
            // Must provide size argument followed by file name.
            String size = args[0];
            if (size.equals("--3")) {
                sizes = DataSize.values();
            } else if (size.equals("--2")) {
                sizes = new DataSize[]{DataSize.SMALL, DataSize.MEDIUM};
            } else if (size.equals("--1")) {
                sizes = new DataSize[]{DataSize.SMALL};
            } else {
                System.err.println("First argument must be size (--1, --2, or --3)");
                System.exit(1);
            }
            File file = new File(args[1]);
            if (!file.exists()) {
                System.err.format("File '%s' not found!\n", args[1]);
                System.exit(1);
            }
            generators = new DataGenerator[] {
                        new FileGenerator(file)
            };
            runners = new SortRunner[] {
                        new MergesortRunner(),
                        new QuicksortRunner(),
                        new MultikeyRunner(),
                        new BurstsortRunner(),
                        new BurstsortThreadPoolRunner(),
                        new RedesignedBurstsortRunner(),
                        new RedesignedBurstsortThreadPoolRunner()
            };
        } else {
            usage();
            System.exit(1);
        }
        try {
            runsorts(generators, runners, sizes);
        } catch (GeneratorException ge) {
            ge.printStackTrace();
        }
    }

    /**
     * Display a usage message.
     */
    private static void usage() {
        System.out.println("Usage: Benchmark [<options>]|[--1|--2|--3 <file>]");
        System.out.println("\t--burstsort: run only the single-threaded burstsort tests.");
        System.out.println("\t--parallel: run only burstsort tests, both single and multithreaded.");
        System.out.println("\t--only-parallel-large: run only parallel sorts with large data");
        System.out.println("\t--1: load 333k lines from file and benchmark.");
        System.out.println("\t--2: load 1m lines from file and benchmark.");
        System.out.println("\t--3: load 3m lines from file and benchmark.");
        System.out.println("\t\tFor the file benchmarks, all tests are run.");
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
            for (DataSize size : sizes) {
                System.out.format("\t%s...\n", size.toString());
                List<String> data = generator.generate(size);
                for (SortRunner runner : runners) {
                    System.out.format("\t\t%-20s:\t", runner.getDisplayName());
                    long[] times = new long[RUN_COUNT];
                    for (int run = 0; run < times.length; run++) {
                        String[] arr = data.toArray(new String[data.size()]);
                        long t1 = System.currentTimeMillis();
                        runner.sort(arr);
                        long t2 = System.currentTimeMillis();
                        times[run] = t2 - t1;
                        if (run == 0) {
                            // Verify the results are actually sorted, just
                            // in case the unit tests missed something.
                            for (int ii = 1; ii < arr.length; ii++) {
                                if (arr[ii - 1].compareTo(arr[ii]) > 0) {
                                    System.err.format("Sort %s failed!\n", runner.getDisplayName());
                                    System.err.format("%s > %s @ %d\n", arr[ii - 1], arr[ii], ii);
                                    System.exit(1);
                                }
                            }
                        }
                    }
                    // Find the average of the run times, dropping the
                    // high and low values. The run times should never
                    // be more than a couple of minutes, so these
                    // calculations will never overflow.
                    long total = 0;
                    long highest = Long.MIN_VALUE;
                    long lowest = Long.MAX_VALUE;
                    for (int run = 0; run < RUN_COUNT; run++) {
                        total += times[run];
                        if (times[run] > highest) {
                            highest = times[run];
                        }
                        if (times[run] < lowest) {
                            lowest = times[run];
                        }
                    }
                    total -= lowest;
                    total -= highest;
                    long average = total / (RUN_COUNT - 2);
                    System.out.format("%d ms\n", average);
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
     * A "generator" that reads data from a named file, returning
     * a particular number of lines based on the requested size.
     * The file must have sufficient data or an error occurs.
     */
    private static class FileGenerator implements DataGenerator {
        /** File from whence data is to be read. */
        private File file;

        /**
         * Constructor for FileGenerator, which reads from the given file.
         *
         * @param  file  that which contains test data.
         */
        public FileGenerator(File file) {
            this.file = file;
        }

        @Override
        public List<String> generate(DataSize size) throws GeneratorException {
            int count = size.getValue();
            List<String> data = new ArrayList<String>(count);
            try {
                FileReader fr = new FileReader(file);
                BufferedReader br = new BufferedReader(fr);
                String line = br.readLine();
                while (line != null && count > 0) {
                    data.add(line);
                    count--;
                    line = br.readLine();
                }
            } catch (IOException ioe) {
                throw new GeneratorException(ioe);
            }
            if (count > 0) {
                throw new GeneratorException(String.format(
                        "File '%s' has too few lines (%d more needed)",
                        file.getName(), count));
            }
            return data;
        }

        @Override
        public String getDisplayName() {
            return file.getName();
        }
    }

    /**
     * Generates strings of a fixed length, comprised of randomly selected
     * characters from the genome alphabet.
     */
    private static class GenomeGenerator implements DataGenerator {
        /** Size of the randomly generated strings. */
        private static final int LENGTH = 9;
        /** Size of the genome alphabet (a, c, g, t). */
        private static final int ALPHABET = 4;

        @Override
        public List<String> generate(DataSize size) throws GeneratorException {
            int count = size.getValue();
            Random r = new Random();
            List<String> list = new ArrayList<String>();
            StringBuilder sb = new StringBuilder();
            for (int ii = 0; ii < count; ii++) {
                for (int jj = 0; jj < LENGTH; jj++) {
                    switch (r.nextInt(ALPHABET)) {
                        case 0:
                            sb.append('a');
                            break;
                        case 1:
                            sb.append('c');
                            break;
                        case 2:
                            sb.append('g');
                            break;
                        case 3:
                            sb.append('t');
                            break;
                    }
                }
                list.add(sb.toString());
                sb.setLength(0);
            }
            return list;
        }

        @Override
        public String getDisplayName() {
            return "Genome";
        }
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
            return "Artificial A";
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
            return "Artificial C";
        }
    }

    /**
     * Generates a set of strings, comprised of one to 100 characters,
     * from an alphabet consisting of nine letters. One of three
     * pathological cases to stress test the sort.
     */
    private static class SmallAlphabetGenerator implements DataGenerator {
        /** Longest string to be created. */
        private static final int LONGEST = 100;
        /** Small alphabet size. */
        private static final int ALPHABET = 9;

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
            return "Artificial B";
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
     * Runs the redesigned burstsort implementation.
     */
    private static class RedesignedBurstsortRunner implements SortRunner {

        @Override
        public String getDisplayName() {
            return "BR-Burstsort";
        }

        @Override
        public void sort(String[] data) {
            RedesignedBurstsort.sort(data);
        }
    }

    /**
     * Runs the parallel (thread pool) burstsort implementation.
     */
    private static class BurstsortThreadPoolRunner implements SortRunner {

        @Override
        public String getDisplayName() {
            return "Burstsort|TP|";
        }

        @Override
        public void sort(String[] data) {
            try {
                Burstsort.sortThreadPool(data);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }

    /**
     * Runs the parallel (thread pool) redesigned burstsort implementation.
     */
    private static class RedesignedBurstsortThreadPoolRunner implements SortRunner {

        @Override
        public String getDisplayName() {
            return "BR-Burstsort|TP|";
        }

        @Override
        public void sort(String[] data) {
            try {
                RedesignedBurstsort.sortThreadPool(data);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
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
     * Runs the preferred multikey quicksort implementation.
     */
    private static class MultikeyRunner implements SortRunner {

        @Override
        public String getDisplayName() {
            return "MultikeyQS";
        }

        @Override
        public void sort(String[] data) {
            MultikeyQuicksort.sort(data);
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
