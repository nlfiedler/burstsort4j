/*
 * Copyright 2009-2011 Nathan Fiedler. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package org.burstsort4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test the SelectionSort implementations.
 *
 * @author Nathan Fiedler
 */
public class SelectionsortTest {

    /** Maximum number of lines to test for any given file, which avoids
     * spending far too much time testing selection sort on data sets
     * that it will never actually be called upon to sort in practice. */
    private static final int MAX_LINES = 512;

    @Test
    public void testArguments() {
        Selectionsort.sort((String[]) null, 0, 0);
        Selectionsort.sort(new String[0], 0, 0);
        String[] arr = new String[]{"a"};
        Selectionsort.sort(arr, 0, arr.length - 1);
        arr = new String[]{"b", "a"};
        Selectionsort.sort(arr, 0, arr.length - 1);
        assertTrue(Tests.isSorted(arr));
        arr = new String[]{"c", "b", "a"};
        Selectionsort.sort(arr, 0, arr.length - 1);
        assertTrue(Tests.isSorted(arr));
    }

    @Test
    public void testComparable() {
        try {
            List<String> data = Tests.loadData("dictwords", false, MAX_LINES);
            Collections.shuffle(data);
            String[] arr = data.toArray(new String[data.size()]);
            Selectionsort.sort(arr, 0, arr.length - 1);
            assertTrue(Tests.isSorted(arr));
            // Test with sorted list
            Selectionsort.sort(arr, 0, arr.length - 1);
            assertTrue(Tests.isSorted(arr));
            // Test with reverse sorted list
            Collections.reverse(data);
            arr = data.toArray(new String[data.size()]);
            Selectionsort.sort(arr, 0, arr.length - 1);
            assertTrue(Tests.isSorted(arr));
            // Test with non-unique word list.
            data = Tests.loadData("hamletwords", false, MAX_LINES);
            Collections.shuffle(data);
            arr = data.toArray(new String[data.size()]);
            Selectionsort.sort(arr, 0, arr.length - 1);
            assertTrue(Tests.isSorted(arr));
            // Test with sorted list
            Selectionsort.sort(arr, 0, arr.length - 1);
            assertTrue(Tests.isSorted(arr));
            // Test with reverse sorted list
            Collections.reverse(data);
            arr = data.toArray(new String[data.size()]);
            Selectionsort.sort(arr, 0, arr.length - 1);
            assertTrue(Tests.isSorted(arr));
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
        // Test with repeated strings.
        String[] arr = new String[MAX_LINES];
        Arrays.fill(arr, "abcdefghijklmnopqrstuvwxyz");
        Selectionsort.sort(arr, 0, arr.length - 1);
        assertTrue(Tests.isRepeated(arr, "abcdefghijklmnopqrstuvwxyz"));
        // Test with randomly generated strings.
        List<String> data = Tests.generateData(MAX_LINES, 64);
        arr = data.toArray(new String[data.size()]);
        Selectionsort.sort(arr, 0, arr.length - 1);
        assertTrue(Tests.isSorted(arr));
    }
}
