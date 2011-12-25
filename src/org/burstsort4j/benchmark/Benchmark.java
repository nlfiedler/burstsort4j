/*
 * Copyright 2008-2011 Nathan Fiedler. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package org.burstsort4j.benchmark;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
        DataGenerator[] generators = {
            DataGenerator.RANDOM,
            DataGenerator.REPEAT,
            DataGenerator.REPEAT_CYCLE,
            DataGenerator.PSEUDO_WORD,
            DataGenerator.GENOME,
            DataGenerator.SMALL_ALPHABET,
            DataGenerator.MEDIAN_OF_3_KILLER
        };
        SortRunner[] runners = null;
        if (Runtime.getRuntime().availableProcessors() > 1) {
            runners = new SortRunner[]{
                SortRunner.MERGE,
                SortRunner.QUICK,
                SortRunner.MULTIKEY,
                SortRunner.BURST,
                SortRunner.BURST_THREADPOOL,
                SortRunner.REDESIGNED_BURST,
                SortRunner.REDESIGNED_BURST_THREADPOOL,
                SortRunner.LAZY_FUNNEL,
                SortRunner.THREADED_LAZY_FUNNEL
            };
        } else {
            runners = new SortRunner[]{
                SortRunner.MERGE,
                SortRunner.QUICK,
                SortRunner.MULTIKEY,
                SortRunner.BURST,
                SortRunner.REDESIGNED_BURST,
                SortRunner.LAZY_FUNNEL
            };
        }
        DataSize[] sizes = {
            DataSize.SMALL,
            DataSize.MEDIUM,
            DataSize.LARGE
        };
        if (args.length > 0) {
            // Parse the command line arguments.
            int i = 0;
            while (i < args.length) {
                if (args[i].equals("--comparable")) {
                    // Benchmark the Comparable-based sorters (i.e. those that
                    // sort instances of Comparable, without any assumptions
                    // about the input, such as String-based sorters).
                    runners = new SortRunner[]{
                        SortRunner.MERGE,
                        SortRunner.QUICK,
                        SortRunner.LAZY_FUNNEL
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
                    DataGenerator fgen = DataGenerator.FILE;
                    fgen.setFile(file);
                    generators = new DataGenerator[]{fgen};
                } else if (args[i].equals("--help")) {
                    usage();
                } else {
                    usage("Unrecognized option: " + args[i]);
                }
                i++;
            }
        }
        runsorts(generators, runners, sizes);
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
     */
    private static void runsorts(DataGenerator[] generators,
            SortRunner[] runners, DataSize[] sizes) {

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
}
