/*
 * Copyright 2011 Nathan Fiedler. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package org.burstsort4j.benchmark;

/**
 * The data regarding a benchmark, including the elapsed time and
 * a timer for tracking the current run.
 *
 * @author Nathan Fiedler
 */
public class BenchmarkData {

    /** Number of nanoseconds in one second. */
    private static final int ONE_BILLION = 1000000000;
    private final BenchmarkRunnable func;
    private int count;
    private long start;
    private long ns;

    public BenchmarkData(BenchmarkRunnable r) {
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
