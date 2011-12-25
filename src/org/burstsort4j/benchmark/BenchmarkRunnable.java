/*
 * Copyright 2011 Nathan Fiedler. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package org.burstsort4j.benchmark;

/**
 * That which is run in a single benchmark test.
 *
 * @author Nathan Fiedler
 */
public interface BenchmarkRunnable {

    /**
     * Run the benchmark the number of times specified in {@code b}.
     *
     * @param  b  provides the number of iterations.
     */
    void run(BenchmarkData b);
}
