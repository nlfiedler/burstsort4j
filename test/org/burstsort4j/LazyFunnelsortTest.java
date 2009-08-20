/*
 * Copyright (C) 2009  Nathan Fiedler
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id$
 */

package org.burstsort4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the LazyFunnelsort class.
 *
 * @author Nathan Fiedler
 */
public class LazyFunnelsortTest {

    @Test
    public void test_InsertionMerge_Dictwords() {
        try {
            List<String> data = Tests.loadData();
            Collections.shuffle(data);
            test_InsertionMerge(data);
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }

    @Test
    public void test_InsertionMerge_Sorted() {
        try {
            List<String> data = Tests.loadData();
            Collections.sort(data);
            test_InsertionMerge(data);
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }

    @Test
    public void test_InsertionMerge_Reversed() {
        try {
            List<String> data = Tests.loadData();
            Collections.sort(data);
            Collections.reverse(data);
            test_InsertionMerge(data);
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }

    @Test
    public void test_InsertionMerge_Repeated() {
        String[] arr = new String[25000];
        String seed = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        Arrays.fill(arr, seed);
        List<String> data = new ArrayList<String>();
        for (String s : arr) {
            data.add(s);
        }
        test_InsertionMerge(data);
    }

    @Test
    public void test_InsertionMerge_RepeatedCycle() {
        String[] strs = new String[100];
        String seed = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        for (int i = 0, l = 1; i < strs.length; i++, l++) {
            strs[i] = seed.substring(0, l);
        }
        List<String> data = new ArrayList<String>();
        for (int c = 25000, i = 0; c > 0; i++, c--) {
            data.add(strs[i % strs.length]);
        }
        test_InsertionMerge(data);
    }

    /**
     * Tests the insertion d-way merge algorithm for correctness using
     * the given data.
     *
     * @param  data  data to use for testing.
     */
    private void test_InsertionMerge(List<String> data) {
        // Ensure data size is a multiple of four to make debugging easier.
        if (data.size() < 25000) {
            throw new IllegalArgumentException("input too small");
        }
        if (data.size() > 25000) {
            data = data.subList(0, 25000);
        }
        String[] arr = data.toArray(new String[data.size()]);
        // Split up the inputs for the insertion d-way merger.
        List<CircularBuffer<String>> inputs = new ArrayList<CircularBuffer<String>>();
        int size = arr.length / 4;
        int offset = 0;
        Arrays.sort(arr, offset, offset + size);
        inputs.add(new CircularBuffer<String>(arr, offset, size, false));
        offset += size;
        Arrays.sort(arr, offset, offset + size);
        inputs.add(new CircularBuffer<String>(arr, offset, size, false));
        offset += size;
        Arrays.sort(arr, offset, offset + size);
        inputs.add(new CircularBuffer<String>(arr, offset, size, false));
        offset += size;
        Arrays.sort(arr, offset, offset + size);
        inputs.add(new CircularBuffer<String>(arr, offset, size, false));
        CircularBuffer<String> output = new CircularBuffer<String>(arr.length);
        // Test the merger.
        LazyFunnelsort.insertionMerge(inputs, output);
        String[] results = new String[arr.length];
        output.drain(results, 0);
        assertTrue(Tests.isSorted(results));
        Arrays.sort(arr);
        for (int ii = 0; ii < arr.length; ii++) {
            assertEquals(arr[ii], results[ii]);
        }
    }

//    @Test
//    public void testArguments() {
//        Burstsort.sort(null);
//        Burstsort.sort(new String[0]);
//        String[] arr = new String[] { "a" };
//        Burstsort.sort(arr);
//        arr = new String[] { "b", "a" };
//        Burstsort.sort(arr);
//        assertTrue(Tests.isSorted(arr));
//        arr = new String[] { "c", "b", "a" };
//        Burstsort.sort(arr);
//        assertTrue(Tests.isSorted(arr));
//    }
//
//    @Test
//    public void testDictWords() {
//        try {
//            // Use the large dictionary rather than the trivial one.
//            List<String> data = Tests.loadData("dictwords.gz", true);
//            Collections.shuffle(data);
//            String[] arr = data.toArray(new String[data.size()]);
//            System.out.format("\nDictionary words (large):\n");
//            Burstsort.sort(arr, System.out);
//            assertTrue(Tests.isSorted(arr));
//        } catch (IOException ioe) {
//            fail(ioe.toString());
//        }
//    }
//
//    @Test
//    public void testDictWordsParallel() {
//        try {
//            // Use the large dictionary rather than the trivial one.
//            List<String> data = Tests.loadData("dictwords.gz", true);
//            Collections.shuffle(data);
//            String[] arr = data.toArray(new String[data.size()]);
//            try {
//                Burstsort.sortThreadPool(arr);
//            } catch (InterruptedException ie) {
//                fail(ie.toString());
//            }
//            assertTrue(Tests.isSorted(arr));
//        } catch (IOException ioe) {
//            fail(ioe.toString());
//        }
//    }
//
//    @Test
//    public void testSorted() {
//        try {
//            List<String> data = Tests.loadData();
//            Collections.sort(data);
//            String[] arr = data.toArray(new String[data.size()]);
//            System.out.format("\nDictionary words (sorted):\n");
//            Burstsort.sort(arr, System.out);
//            assertTrue(Tests.isSorted(arr));
//        } catch (IOException ioe) {
//            fail(ioe.toString());
//        }
//    }
//
//    @Test
//    public void testReversed() {
//        try {
//            List<String> data = Tests.loadData();
//            Collections.sort(data);
//            Collections.reverse(data);
//            String[] arr = data.toArray(new String[data.size()]);
//            System.out.format("\nDictionary words (reversed):\n");
//            Burstsort.sort(arr, System.out);
//            assertTrue(Tests.isSorted(arr));
//        } catch (IOException ioe) {
//            fail(ioe.toString());
//        }
//    }
//
//    @Test
//    public void testRepeated() {
//        // Make the size of the set large enough to burst buckets.
//        String[] arr = new String[1310720];
//        final String STR = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
//                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
//        Arrays.fill(arr, STR);
//        System.out.format("\nRepeated 100-A string:\n");
//        Burstsort.sort(arr, System.out);
//        assertTrue(Tests.isRepeated(arr, STR));
//    }
//
//    @Test
//    public void testRepeatedParallel() {
//        // Make the size of the set large enough to burst buckets.
//        String[] arr = new String[1310720];
//        final String STR = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
//                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
//        Arrays.fill(arr, STR);
//        try {
//            Burstsort.sortThreadPool(arr);
//        } catch (InterruptedException ie) {
//            fail(ie.toString());
//        }
//        assertTrue(Tests.isRepeated(arr, STR));
//    }
//
//    @Test
//    public void testRepeatedCycle() {
//        String[] strs = new String[100];
//        String seed = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
//                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
//        for (int i = 0, l = 1; i < strs.length; i++, l++) {
//            strs[i] = seed.substring(0, l);
//        }
//        List<String> list = new ArrayList<String>();
//        for (int c = 3162300, i = 0; c > 0; i++, c--) {
//            list.add(strs[i % strs.length]);
//        }
//        System.out.format("\nRepeated A strings (cycle):\n");
//        String[] arr = list.toArray(new String[list.size()]);
//        Burstsort.sort(arr, System.out);
//        assertTrue(Tests.isSorted(arr));
//    }
//
//    @Test
//    public void testRepeatedCycleParallel() {
//        String[] strs = new String[100];
//        String seed = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
//                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
//        for (int i = 0, l = 1; i < strs.length; i++, l++) {
//            strs[i] = seed.substring(0, l);
//        }
//        List<String> list = new ArrayList<String>();
//        for (int c = 3162300, i = 0; c > 0; i++, c--) {
//            list.add(strs[i % strs.length]);
//        }
//        String[] arr = list.toArray(new String[list.size()]);
//        try {
//            Burstsort.sortThreadPool(arr);
//        } catch (InterruptedException ie) {
//            fail(ie.toString());
//        }
//        assertTrue(Tests.isSorted(arr));
//    }
//
//    @Test
//    public void testRandom() {
//        List<String> data = Tests.generateData(1000000, 100);
//        String[] arr = data.toArray(new String[data.size()]);
//        System.out.format("\nRandom strings:\n");
//        Burstsort.sort(arr, System.out);
//        assertTrue(Tests.isSorted(arr));
//    }
//
//    @Test
//    public void testHamlet() {
//        try {
//            List<String> data = Tests.loadData("hamletwords");
//            Collections.shuffle(data);
//            String[] arr = data.toArray(new String[data.size()]);
//            System.out.format("\nHamlet words:\n");
//            Burstsort.sort(arr, System.out);
//            assertTrue(Tests.isSorted(arr));
//        } catch (IOException ioe) {
//            fail(ioe.toString());
//        }
//    }
//
//    @Test
//    public void testDictCalls() {
//        try {
//            List<String> data = Tests.loadData("dictcalls.gz", true);
//            String[] arr = data.toArray(new String[data.size()]);
//            System.out.format("\nLibrary calls:\n");
//            Burstsort.sort(arr, System.out);
//            assertTrue(Tests.isSorted(arr));
//        } catch (IOException ioe) {
//            fail(ioe.toString());
//        }
//    }
}
