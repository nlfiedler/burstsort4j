/*
 * Copyright 2009-2011 Nathan Fiedler. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package org.burstsort4j;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Runs performance tests over several kinds of data for each of the
 * sort implementations, collecting run times and displaying the results.
 *
 * @author Nathan Fiedler
 */
public class MicroBenchmark {

    /** Number of nanoseconds in one second. */
    private static final int ONE_BILLION = 1000000000;

    /** Size of the data sets used in testing sort performance. */
    private static enum DataSize {

        N_10(10),
        N_20(20),
        N_50(50),
        N_100(100),
        N_400(400);
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
// TODO: if --sort argument given, treat as regex to select sorts to measure
//       e.g. "--sort comb" will run only sorts that have "comb" in the name
// TODO: if --data argument given, treat as regex to select data set to use
        DataGenerator[] generators = new DataGenerator[]{
            new RepeatGenerator(),
            new RepeatCycleGenerator(),
            new RandomGenerator(),
            new PseudoWordGenerator(),
            new SmallAlphabetGenerator(),
            new GenomeGenerator()
        };
        SortRunner[] runners = new SortRunner[]{
            new BinaryInsertionsortRunner(),
            new InsertionsortRunner(),
            new CombsortRunner(),
            new HybridCombsortRunner(),
            new GnomesortRunner(),
            new HeapsortRunner(),
            new QuicksortRunner(),
            new SelectionsortRunner(),
            new ShellsortRunner()
        };

        // Generate the data sets once and reuse hereafter.
        Map<DataGenerator, String[]> dataSets = new HashMap<DataGenerator, String[]>();
        for (DataGenerator generator : generators) {
            dataSets.put(generator, generator.generate(DataSize.N_400));
        }

        // Warm up the JVM so that the code (hopefully) gets compiled.
        System.out.println("Warming up the system, please wait...");
        for (DataGenerator generator : generators) {
            String[] dataSet = dataSets.get(generator);
            String[] input = new String[dataSet.length];
            for (SortRunner runner : runners) {
                for (int i = 0; i < 1000; i++) {
                    System.arraycopy(dataSet, 0, input, 0, input.length);
                    runner.sort(input);
                }
            }
        }

        // Avoid recreating the input arrays over and over again.
        Map<DataSize, String[]> inputSets = new EnumMap<DataSize, String[]>(DataSize.class);
        for (DataSize size : DataSize.values()) {
            inputSets.put(size, new String[size.getValue()]);
        }

        // For each type of data set, and each data set size, and
        // each sort implementation, run the sort many times and
        // calculate an average.
        for (DataGenerator generator : generators) {
            System.out.format("%s...\n", generator.getDisplayName());
            final String[] dataSet = dataSets.get(generator);
            for (DataSize size : DataSize.values()) {
                System.out.format("\t%s...\n", size.toString());
                final String[] input = inputSets.get(size);
                for (final SortRunner runner : runners) {
                    System.out.format("\t\t%-20s:\t", runner.getDisplayName());
                    final SortRunner func = runner;
                    BenchRunnable r = new BenchRunnable() {

                        @Override
                        public void run(BenchData b) {
                            for (int i = 0; i < b.count(); i++) {
                                b.stopTimer();
                                System.arraycopy(dataSet, 0, input, 0, input.length);
                                b.startTimer();
                                func.sort(input);
                            }
                        }
                    };
                    BenchData bench = new BenchData(r);
                    BenchmarkResult result = bench.run();
                    System.out.format("%8d\t%10d ns/op\n", result.count(), result.nsPerOp());
                }
            }
        }
    }

    /**
     * That which is run in a single benchmark test.
     */
    private static interface BenchRunnable {

        /**
         * Run the benchmark the number of times specified in {@code b}.
         *
         * @param  b  provides the number of iterations.
         */
        void run(BenchData b);
    }

    /**
     * The data regarding a benchmark, including the elapsed time and
     * a timer for tracking the current run.
     */
    private static class BenchData {

        private final BenchRunnable func;
        private int count;
        private long start;
        private long ns;

        BenchData(BenchRunnable r) {
            func = r;
        }

        public int count() {
            return count;
        }

        public void startTimer() {
            start = System.nanoTime();
        }

        public void stopTimer() {
            if (start > 0) {
                ns += System.nanoTime() - start;
            }
            start = 0;
        }

        public void resetTimer() {
            start = 0;
            ns = 0;
        }

        public long nsPerOp() {
            if (count <= 0) {
                return 0;
            }
            return ns / count;
        }

        private void runN(int n) {
            count = n;
            resetTimer();
            startTimer();
            func.run(this);
            stopTimer();
        }

        private int roundDown10(int n) {
            int tens = 0;
            while (n > 10) {
                n /= 10;
                tens++;
            }
            int result = 1;
            for (int i = 0; i < tens; i++) {
                result *= 10;
            }
            return result;
        }

        private int roundUp(int n) {
            int base = roundDown10(n);
            if (n < (2 * base)) {
                return 2 * base;
            }
            if (n < (5 * base)) {
                return 5 * base;
            }
            return 10 * base;
        }

        /**
         * Run the benchmark function a sufficient number of times to
         * get a timing of at least one second.
         *
         * @return  the results of the benchmark run.
         */
        public BenchmarkResult run() {
            // This code is a translation of that found in the Go testing package.
            // Run the benchmark for a single iteration in case it's expensive.
            int n = 1;
            runN(n);
            // Run the benchmark for at least a second.
            while (ns < ONE_BILLION && n < ONE_BILLION) {
                int last = n;
                // Predict iterations/sec.
                if (nsPerOp() == 0) {
                    n = ONE_BILLION;
                } else {
                    n = ONE_BILLION / (int) nsPerOp();
                }
                // Run more iterations than we think we'll need for a second (1.5x).
                // Don't grow too fast in case we had timing errors previously.
                // Be sure to run at least one more than last time.
                n = Math.max(Math.min(n + n / 2, 100 * last), last + 1);
                // Round up to something easy to read.
                n = roundUp(n);
                runN(n);
            }
            return new BenchmarkResult(count, ns);

        }
    }

    /**
     * Data regarding the run of a benchmark, including the number of
     * iterations and the number of nanoseconds per iteration.
     */
    private static class BenchmarkResult {

        private final int count;
        private final long ns;

        BenchmarkResult(int count, long ns) {
            this.count = count;
            this.ns = ns;
        }

        public int count() {
            return count;
        }

        public long nsPerOp() {
            if (count <= 0) {
                return 0;
            }
            return ns / count;
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
         */
        String[] generate(DataSize size);

        /**
         * Returns the display name for this generator.
         *
         * @return  display name.
         */
        String getDisplayName();
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
        public String[] generate(DataSize size) {
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
            return "Pseudo words";
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
        public String[] generate(DataSize size) {
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
        public String[] generate(DataSize size) {
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
        public String[] generate(DataSize size) {
            String[] strs = new String[100];
            String seed = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                    + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
            for (int i = 0; i < strs.length; i++) {
                strs[i] = seed.substring(0, i + 1);
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
        public String[] generate(DataSize size) {
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
            return "Small Alphabet";
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
        public String[] generate(DataSize size) {
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
            return "Genome";
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

    private static class HybridCombsortRunner implements SortRunner {

        @Override
        public String getDisplayName() {
            return "HybridComb";
        }

        @Override
        public void sort(String[] data) {
            HybridCombsort.sort(data);
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
            Insertionsort.sort(data);
        }
    }

    private static class BinaryInsertionsortRunner implements SortRunner {

        @Override
        public String getDisplayName() {
            return "BinInsertionsort";
        }

        @Override
        public void sort(String[] data) {
            BinaryInsertionsort.sort(data);
        }
    }

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
