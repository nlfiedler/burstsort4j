/*
 * Copyright 2011 Nathan Fiedler. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package org.burstsort4j.benchmark;

/**
 * Data regarding the run of a benchmark, including the number of
 * iterations and the number of nanoseconds per iteration.
 *
 * @author Nathan Fiedler
 */
public class BenchmarkResult {

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
