/*
 * Copyright 2009-2011 Nathan Fiedler. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package org.burstsort4j;

import java.util.ArrayList;
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
        Selectionsort.sort((String[]) null);
        Selectionsort.sort(new String[0]);
        String[] arr = new String[]{"a"};
        Selectionsort.sort(arr);
        arr = new String[]{"b", "a"};
        Selectionsort.sort(arr);
        assertTrue(Tests.isSorted(arr));
        arr = new String[]{"c", "b", "a"};
        Selectionsort.sort(arr);
        assertTrue(Tests.isSorted(arr));
        // test with all empty input
        arr = new String[]{"", "", "", "", "", "", "", "", "", ""};
        Selectionsort.sort(arr);
        for (String s : arr) {
            assertEquals("", s);
        }
        // test with peculiar input
        arr = new String[]{"z", "m", "", "a", "d", "tt", "tt", "tt", "foo", "bar"};
        Selectionsort.sort(arr);
        assertTrue("peculiar input not sorted", Tests.isSorted(arr));
    }

    @Test
    public void testDictWords() {
        try {
            List<String> data = Tests.loadData("dictwords", false, MAX_LINES);
            Collections.shuffle(data);
            String[] arr = data.toArray(new String[data.size()]);
            Selectionsort.sort(arr);
            assertTrue(Tests.isSorted(arr));
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }

    @Test
    public void testSorted() {
        try {
            List<String> data = Tests.loadData("dictwords", false, MAX_LINES);
            Collections.sort(data);
            String[] arr = data.toArray(new String[data.size()]);
            Selectionsort.sort(arr);
            assertTrue(Tests.isSorted(arr));
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }

    @Test
    public void testReversed() {
        try {
            List<String> data = Tests.loadData("dictwords", false, MAX_LINES);
            Collections.sort(data);
            Collections.reverse(data);
            String[] arr = data.toArray(new String[data.size()]);
            Selectionsort.sort(arr);
            assertTrue(Tests.isSorted(arr));
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }

    @Test
    public void testRepeated() {
        String[] arr = new String[MAX_LINES];
        final String STR = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        Arrays.fill(arr, STR);
        Selectionsort.sort(arr);
        assertTrue(Tests.isRepeated(arr, STR));
    }

    @Test
    public void testRepeatedCycle() {
        String[] strs = new String[100];
        String seed = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        for (int i = 0, l = 1; i < strs.length; i++, l++) {
            strs[i] = seed.substring(0, l);
        }
        List<String> list = new ArrayList<String>();
        for (int c = MAX_LINES, i = 0; c > 0; i++, c--) {
            list.add(strs[i % strs.length]);
        }
        String[] arr = list.toArray(new String[list.size()]);
        Selectionsort.sort(arr);
        assertTrue(Tests.isSorted(arr));
    }

    @Test
    public void testRandom() {
        List<String> data = Tests.generateData(MAX_LINES, 100);
        String[] arr = data.toArray(new String[data.size()]);
        Selectionsort.sort(arr);
        assertTrue(Tests.isSorted(arr));
    }

    @Test
    public void testHamlet() {
        try {
            List<String> data = Tests.loadData("hamletwords", false, MAX_LINES);
            Collections.shuffle(data);
            String[] arr = data.toArray(new String[data.size()]);
            Selectionsort.sort(arr);
            assertTrue(Tests.isSorted(arr));
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }

    @Test
    public void testDictCalls() {
        try {
            List<String> data = Tests.loadData("dictcalls.gz", true, MAX_LINES);
            String[] arr = data.toArray(new String[data.size()]);
            Selectionsort.sort(arr);
            assertTrue(Tests.isSorted(arr));
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }
}
