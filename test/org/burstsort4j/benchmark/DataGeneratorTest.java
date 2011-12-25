/*
 * Copyright 2011 Nathan Fiedler. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package org.burstsort4j.benchmark;

import org.burstsort4j.Quicksort;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the DataGenerator class.
 *
 * @author Nathan Fiedler
 */
public class DataGeneratorTest {

    @Test
    public void testMedianOf3Killer() {
        DataGenerator generator = DataGenerator.MEDIAN_OF_3_KILLER;
        String[] results = generator.generate(DataSize.N_20);
        String[] sorted = new String[results.length];
        System.arraycopy(results, 0, sorted, 0, results.length);
        Quicksort.sort(sorted);
        int[] order = new int[]{0, 10, 2, 12, 4, 14, 6, 16, 8, 18,
            1, 3, 5, 7, 9, 11, 13, 15, 17, 19};
        assertEquals(order.length, results.length);
        for (int ii = 0; ii < order.length; ii++) {
            assertEquals(sorted[order[ii]], results[ii]);
        }
    }
}
