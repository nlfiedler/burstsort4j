/*
 * Copyright 2008-2011 Nathan Fiedler. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        SMALL(333000),
        MEDIUM(1000000),
        LARGE(3000000);
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
        DataGenerator[] generators = new DataGenerator[]{
            new RandomGenerator(),
            new PseudoWordGenerator(),
            new RepeatGenerator(),
            new SmallAlphabetGenerator(),
            new RepeatCycleGenerator(),
            new GenomeGenerator()
        };
        SortRunner[] runners = null;
        if (Runtime.getRuntime().availableProcessors() > 1) {
            runners = new SortRunner[]{
                        new MergesortRunner(),
                        new QuicksortRunner(),
                        new MultikeyRunner(),
                        new BurstsortRunner(),
                        new BurstsortThreadPoolRunner(),
                        new RedesignedBurstsortRunner(),
                        new RedesignedBurstsortThreadPoolRunner(),
                        new LazyFunnelsortRunner(),
                        new ThreadedLazyFunnelsortRunner()
                    };
        } else {
            runners = new SortRunner[]{
                        new MergesortRunner(),
                        new QuicksortRunner(),
                        new MultikeyRunner(),
                        new BurstsortRunner(),
                        new RedesignedBurstsortRunner(),
                        new LazyFunnelsortRunner()
                    };
        }
        DataSize[] sizes = DataSize.values();
        if (args.length > 0) {
            // Parse the command line arguments.
            int i = 0;
            while (i < args.length) {
                if (args[i].equals("--comparable")) {
                    // Benchmark the Comparable-based sorters (i.e. those that
                    // sort instances of Comparable, without any assumptions
                    // about the input, such as String-based sorters).
                    runners = new SortRunner[]{
                                new MergesortRunner(),
                                new QuicksortRunner(),
                                new LazyFunnelsortRunner()
                            };
                } else if (args[i].equals("--data")) {
                    i++;
                    if (i >= args.length) {
                        usage("Missing --data argument");
                    }
                    Pattern p = Pattern.compile(args[i], Pattern.CASE_INSENSITIVE);
                    List<DataGenerator> list = new ArrayList<DataGenerator>();
                    for (DataGenerator generator : generators) {
                        Matcher m = p.matcher(generator.getDisplayName());
                        if (m.find()) {
                            list.add(generator);
                        }
                    }
                    generators = list.toArray(new DataGenerator[list.size()]);
                } else if (args[i].equals("--sort")) {
                    i++;
                    if (i >= args.length) {
                        usage("Missing --sort argument");
                    }
                    Pattern p = Pattern.compile(args[i], Pattern.CASE_INSENSITIVE);
                    List<SortRunner> list = new ArrayList<SortRunner>();
                    for (SortRunner runner : runners) {
                        Matcher m = p.matcher(runner.getDisplayName());
                        if (m.find()) {
                            list.add(runner);
                        }
                    }
                    runners = list.toArray(new SortRunner[list.size()]);
                } else if (args[i].equals("--list")) {
                    System.out.println("Data sets");
                    for (DataGenerator generator : generators) {
                        System.out.format("\t%s\n", generator.getDisplayName());
                    }
                    System.out.println("Sorting algorithms");
                    for (SortRunner runner : runners) {
                        System.out.format("\t%s\n", runner.getDisplayName());
                    }
                    System.exit(0);
                } else if (args[i].equals("--size")) {
                    i++;
                    if (i >= args.length) {
                        usage("Missing --size argument");
                    }
                    if (args[i].equals("small")) {
                        sizes = new DataSize[]{DataSize.SMALL};
                    } else if (args[i].equals("medium")) {
                        sizes = new DataSize[]{DataSize.MEDIUM};
                    } else if (args[i].equals("large")) {
                        sizes = new DataSize[]{DataSize.LARGE};
                    } else {
                        usage("Unrecognized --size argument");
                    }
                } else if (args[i].equals("--file")) {
                    i++;
                    if (i >= args.length) {
                        usage("Missing --file argument");
                    }
                    File file = new File(args[i]);
                    if (!file.exists()) {
                        usage("File not found: " + args[i]);
                    }
                    generators = new DataGenerator[]{
                                new FileGenerator(file)
                            };
                } else if (args[i].equals("--help")) {
                    usage();
                } else {
                    usage("Unrecognized option: " + args[i]);
                }
                i++;
            }
        }
        try {
            runsorts(generators, runners, sizes);
        } catch (GeneratorException ge) {
            ge.printStackTrace();
        }
    }

    /**
     * Display an error message and the usage information.
     */
    private static void usage(String msg) {
        System.out.println(msg);
        usage();
    }

    /**
     * Display a usage message.
     */
    private static void usage() {
        System.out.println("Usage: Benchmark [options]");
        System.out.println("\t--comparable");
        System.out.println("\t\tRun only the sorts that operate on Comparable.");
        System.out.println("\t\tNot compatible with the --sort option.");
        System.out.println("\t--data <regex>");
        System.out.println("\t\tSelect the data set whose name matches the regular expression.");
        System.out.println("\t\tFor example, '--data random' would use only the random data set.");
        System.out.println("\t\tNot compatible with the --file option.");
        System.out.println("\t--file <file>");
        System.out.println("\t\tUse the contents of the named file for sorting.");
        System.out.println("\t\tNot compatible with the --data option.");
        System.out.println("\t--help");
        System.out.println("\t\tDisplay this usage information.");
        System.out.println("\t--list");
        System.out.println("\t\tDisplay a list of the supported data sets and sorting algorithms.");
        System.out.println("\t--size <size>");
        System.out.println("\t\tIf given 'small', uses 333,000 inputs from data set.");
        System.out.println("\t\tIf given 'medium', uses 1,000,000 inputs from data set.");
        System.out.println("\t\tIf given 'large', uses 3,000,000 inputs from data set.");
        System.out.println("\t--sort <regex>");
        System.out.println("\t\tSelect the sort algorithms whose name matches the regular");
        System.out.println("\t\texpression. For example, '--sort (comb|insert)' would run");
        System.out.println("\t\tboth versions of the insertion and comb sort algorithms.");
        System.out.println("\t\tNot compatible with the --comparable option.");
        System.exit(0);
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

        // Warm up the JVM so that the code (hopefully) gets compiled.
        System.out.println("Warming up the system, please wait...");
        String[] input = new String[DataSize.SMALL.getValue()];
        for (DataGenerator generator : generators) {
            String[] dataSet = generator.generate(DataSize.SMALL);
            for (SortRunner runner : runners) {
                System.arraycopy(dataSet, 0, input, 0, input.length);
                runner.sort(input);
            }
        }

        // For each type of data set, and each data set size, and
        // each sort implementation, run the sort several times and
        // calculate the average run time.
        for (DataGenerator generator : generators) {
            System.out.format("%s...\n", generator.getDisplayName());
            for (DataSize size : sizes) {
                System.out.format("\t%s...\n", size.toString());
                String[] dataSet = generator.generate(size);
                input = new String[size.getValue()];
                for (SortRunner runner : runners) {
                    System.out.format("\t\t%-20s:\t", runner.getDisplayName());
                    long[] times = new long[RUN_COUNT];
                    for (int run = 0; run < times.length; run++) {
                        System.arraycopy(dataSet, 0, input, 0, input.length);
                        long t1 = System.currentTimeMillis();
                        runner.sort(input);
                        long t2 = System.currentTimeMillis();
                        times[run] = t2 - t1;
                    }

                    // Find the average of the run times. The run times
                    // should never be more than a couple of minutes,
                    // so these calculations will never overflow.
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
                    long average = total / RUN_COUNT;
                    System.out.format("%4d %4d %4d (low/avg/high) ms\n", lowest, average, highest);
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
        GeneratorException(String msg) {
            super(msg);
        }

        /**
         * GeneratorException with a cause.
         *
         * @param  cause  cause of the exception.
         */
        GeneratorException(Throwable cause) {
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
         * @return  array of test data.
         * @throws  GeneratorException  thrown if generation fails.
         */
        String[] generate(DataSize size) throws GeneratorException;

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
        FileGenerator(File file) {
            this.file = file;
        }

        @Override
        public String[] generate(DataSize size) throws GeneratorException {
            int count = size.getValue();
            String[] data = new String[count];
            try {
                FileReader fr = new FileReader(file);
                BufferedReader br = new BufferedReader(fr);
                String line = br.readLine();
                for (int ii = 0; count > 0; ii++, count--) {
                    if (line == null) {
                        break;
                    }
                    data[ii] = line;
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
        public String[] generate(DataSize size) throws GeneratorException {
            Random r = new Random();
            String[] list = new String[size.getValue()];
            StringBuilder sb = new StringBuilder();
            for (int ii = 0; ii < list.length; ii++) {
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
                list[ii] = sb.toString();
                sb.setLength(0);
            }
            return list;
        }

        @Override
        public String getDisplayName() {
            return "Genome FL9 C4";
        }
    }

    /**
     * Generates a set of pseudo words, comprised of at least one letter,
     * up to the length of the longest (real) English word, using only
     * the lower-case letters.
     */
    private static class PseudoWordGenerator implements DataGenerator {

        /** Longest (real) word in English: antidisestablishmentarianism */
        private static final int LONGEST = 28;
        /** Letters in the English alphabet (lower case only) */
        private static final int ALPHABET = 26;

        @Override
        public String[] generate(DataSize size) throws GeneratorException {
            Random r = new Random();
            String[] list = new String[size.getValue()];
            StringBuilder sb = new StringBuilder();
            for (int ii = 0; ii < list.length; ii++) {
                int length = r.nextInt(LONGEST) + 1;
                for (int jj = 0; jj < length; jj++) {
                    int d = r.nextInt(ALPHABET);
                    sb.append((char) ('a' + d));
                }
                list[ii] = sb.toString();
                sb.setLength(0);
            }
            return list;
        }

        @Override
        public String getDisplayName() {
            return "Pseudo words FL28 C26";
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
        public String[] generate(DataSize size) throws GeneratorException {
            Random r = new Random();
            String[] list = new String[size.getValue()];
            StringBuilder sb = new StringBuilder();
            for (int ii = 0; ii < list.length; ii++) {
                for (int jj = 0; jj < LENGTH; jj++) {
                    int d = r.nextInt(ALPHABET);
                    sb.append((char) (' ' + d));
                }
                list[ii] = sb.toString();
                sb.setLength(0);
            }
            return list;
        }

        @Override
        public String getDisplayName() {
            return "Random FL100 C95";
        }
    }

    /**
     * Generates a set of duplicate strings, comprised of an alphabet
     * of size one, where each string is 100 characters. One of three
     * pathological cases created to stress test the sort.
     */
    private static class RepeatGenerator implements DataGenerator {

        @Override
        public String[] generate(DataSize size) throws GeneratorException {
            int count = size.getValue();
            List<String> list = Collections.nCopies(count,
                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                    + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
            return list.toArray(new String[count]);
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
        public String[] generate(DataSize size) throws GeneratorException {
            String[] strs = new String[100];
            String seed = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                    + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
            for (int i = 0, l = 1; i < strs.length; i++, l++) {
                strs[i] = seed.substring(0, l);
            }
            String[] list = new String[size.getValue()];
            int c = 0;
            for (int ii = 0; ii < list.length; ii++) {
                list[ii] = strs[c];
                c++;
                if (c >= strs.length) {
                    c = 0;
                }
            }
            return list;
        }

        @Override
        public String getDisplayName() {
            return "Repeated 100-A decreasing cycle";
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
        public String[] generate(DataSize size) throws GeneratorException {
            Random r = new Random();
            String[] list = new String[size.getValue()];
            StringBuilder sb = new StringBuilder();
            for (int ii = 0; ii < list.length; ii++) {
                int length = r.nextInt(LONGEST) + 1;
                for (int jj = 0; jj < length; jj++) {
                    int d = r.nextInt(ALPHABET);
                    sb.append((char) ('a' + d));
                }
                list[ii] = sb.toString();
                sb.setLength(0);
            }
            return list;
        }

        @Override
        public String getDisplayName() {
            return "Small Alphabet FL100 C9";
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
                throw new RuntimeException(ie);
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
                throw new RuntimeException(ie);
            }
        }
    }

    /**
     * Runs the lazy funnelsort implementation.
     */
    private static class LazyFunnelsortRunner implements SortRunner {

        @Override
        public String getDisplayName() {
            return "LazyFunnelsort";
        }

        @Override
        public void sort(String[] data) {
            LazyFunnelsort.sort(data);
        }
    }

    /**
     * Runs the multi-threaded lazy funnelsort implementation.
     */
    private static class ThreadedLazyFunnelsortRunner implements SortRunner {

        @Override
        public String getDisplayName() {
            return "LazyFunnelsort|TP|";
        }

        @Override
        public void sort(String[] data) {
            LazyFunnelsort.sortThreaded(data);
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
