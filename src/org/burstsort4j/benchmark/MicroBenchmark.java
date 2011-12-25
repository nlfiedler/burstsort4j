/*
 * Copyright 2009-2011 Nathan Fiedler. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package org.burstsort4j.benchmark;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs performance tests over several kinds of data for each of the
 * sort implementations, collecting run times and displaying the results.
 *
 * @author Nathan Fiedler
 */
public class MicroBenchmark {

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
        DataGenerator[] generators = {
            DataGenerator.RANDOM,
            DataGenerator.REPEAT,
            DataGenerator.REPEAT_CYCLE,
            DataGenerator.PSEUDO_WORD,
            DataGenerator.GENOME,
            DataGenerator.SMALL_ALPHABET,
            DataGenerator.MEDIAN_OF_3_KILLER
        };
        SortRunner[] runners = {
            SortRunner.BINARY_INSERTION,
            SortRunner.INSERTION,
            SortRunner.COMB,
            SortRunner.HYBRID_COMB,
            SortRunner.GNOME,
            SortRunner.HEAP,
            SortRunner.INTRO,
            SortRunner.QUICK,
            SortRunner.SELECTION,
            SortRunner.SHELL
        };
        DataSize[] sizes = {
            DataSize.N_12,
            DataSize.N_20,
            DataSize.N_52,
            DataSize.N_100,
            DataSize.N_400,
            DataSize.N_800
        };

        if (args.length > 0) {
            // Parse the command line arguments.
            int i = 0;
            while (i < args.length) {
                if (args[i].equals("--data")) {
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
                } else if (args[i].equals("--help")) {
                    usage();
                } else {
                    usage("Unrecognized option: " + args[i]);
                }
                i++;
            }
        }

        // Warm up the JVM so that the code (hopefully) gets compiled.
        System.out.println("Warming up the system, please wait...");
        for (DataGenerator generator : generators) {
            String[] dataSet = generator.generate(DataSize.N_400);
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
            for (DataSize size : sizes) {
                // Must generate the data for each size since some
                // generators use it to form a pattern.
                final String[] dataSet = generator.generate(size);
                System.out.format("\t%s...\n", size.toString());
                final String[] input = inputSets.get(size);
                for (final SortRunner runner : runners) {
                    System.out.format("\t\t%-20s:\t", runner.getDisplayName());
                    final SortRunner func = runner;
                    BenchmarkRunnable r = new BenchmarkRunnable() {

                        @Override
                        public void run(BenchmarkData b) {
                            for (int i = 0; i < b.count(); i++) {
                                b.stopTimer();
                                System.arraycopy(dataSet, 0, input, 0, input.length);
                                b.startTimer();
                                func.sort(input);
                            }
                        }
                    };
                    BenchmarkData bench = new BenchmarkData(r);
                    BenchmarkResult result = bench.run();
                    System.out.format("%8d\t%10d ns/op\n", result.count(), result.nsPerOp());
                }
            }
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
     * Display the usage information.
     */
    private static void usage() {
        System.out.println("Usage: MicroBenchmark [options]");
        System.out.println("\t--data <regex>");
        System.out.println("\t\tSelect the data set whose name matches the regular expression.");
        System.out.println("\t\tFor example, '--data random' would use only the random data set.");
        System.out.println("\t--help");
        System.out.println("\t\tDisplay this usage information.");
        System.out.println("\t--list");
        System.out.println("\t\tDisplay a list of the supported data sets and sorting algorithms.");
        System.out.println("\t--sort <regex>");
        System.out.println("\t\tSelect the sort algorithms whose name matches the regular");
        System.out.println("\t\texpression. For example, '--sort (comb|insert)' would run");
        System.out.println("\t\tboth versions of the insertion and comb sort algorithms.");
        System.exit(0);
    }
}
