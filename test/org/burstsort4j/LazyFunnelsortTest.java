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
        List<CircularBuffer<Comparable>> inputs = new ArrayList<CircularBuffer<Comparable>>();
        int size = arr.length / 4;
        int offset = 0;
        Arrays.sort(arr, offset, offset + size);
        inputs.add(new CircularBuffer<Comparable>(arr, offset, size, false));
        offset += size;
        Arrays.sort(arr, offset, offset + size);
        inputs.add(new CircularBuffer<Comparable>(arr, offset, size, false));
        offset += size;
        Arrays.sort(arr, offset, offset + size);
        inputs.add(new CircularBuffer<Comparable>(arr, offset, size, false));
        offset += size;
        Arrays.sort(arr, offset, offset + size);
        inputs.add(new CircularBuffer<Comparable>(arr, offset, size, false));
        CircularBuffer<Comparable> output = new CircularBuffer<Comparable>(arr.length);
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

    @Test
    public void test_InsertionMerger_Dictwords() {
        try {
            List<String> data = Tests.loadData();
            Collections.shuffle(data);
            test_InsertionMerger(data);
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }

    @Test
    public void test_InsertionMerger_Sorted() {
        try {
            List<String> data = Tests.loadData();
            Collections.sort(data);
            test_InsertionMerger(data);
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }

    @Test
    public void test_InsertionMerger_Reversed() {
        try {
            List<String> data = Tests.loadData();
            Collections.sort(data);
            Collections.reverse(data);
            test_InsertionMerger(data);
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }

    @Test
    public void test_InsertionMerger_Repeated() {
        String[] arr = new String[25000];
        String seed = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        Arrays.fill(arr, seed);
        List<String> data = new ArrayList<String>();
        for (String s : arr) {
            data.add(s);
        }
        test_InsertionMerger(data);
    }

    @Test
    public void test_InsertionMerger_RepeatedCycle() {
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
        test_InsertionMerger(data);
    }

    /**
     * Tests the insertion d-way merge algorithm for correctness using
     * the given data.
     *
     * @param  data  data to use for testing.
     */
    private void test_InsertionMerger(List<String> data) {
        // Ensure data size is a multiple of four to make debugging easier.
        if (data.size() < 25000) {
            throw new IllegalArgumentException("input too small");
        }
        if (data.size() > 25000) {
            data = data.subList(0, 25000);
        }
        String[] arr = data.toArray(new String[data.size()]);
        // Split up the inputs for the insertion d-way merger.
        List<CircularBuffer<Comparable>> inputs = new ArrayList<CircularBuffer<Comparable>>();
        int size = arr.length / 4;
        int offset = 0;
        Arrays.sort(arr, offset, offset + size);
        inputs.add(new CircularBuffer<Comparable>(arr, offset, size, false));
        offset += size;
        Arrays.sort(arr, offset, offset + size);
        inputs.add(new CircularBuffer<Comparable>(arr, offset, size, false));
        offset += size;
        Arrays.sort(arr, offset, offset + size);
        inputs.add(new CircularBuffer<Comparable>(arr, offset, size, false));
        offset += size;
        Arrays.sort(arr, offset, offset + size);
        inputs.add(new CircularBuffer<Comparable>(arr, offset, size, false));
        CircularBuffer<Comparable> output = new CircularBuffer<Comparable>(arr.length);
        // Test the merger.
        LazyFunnelsort.Kmerger merger = new LazyFunnelsort.InsertionMerger(inputs, output);
        merger.merge();
        String[] results = new String[arr.length];
        output.drain(results, 0);
        assertTrue(Tests.isSorted(results));
        Arrays.sort(arr);
        for (int ii = 0; ii < arr.length; ii++) {
            assertEquals(arr[ii], results[ii]);
        }
    }

    @Test
    public void testArguments() {
        LazyFunnelsort.sort(null);
        LazyFunnelsort.sort(new String[0]);
        String[] arr = new String[] { "a" };
        LazyFunnelsort.sort(arr);
        arr = new String[] { "b", "a" };
        LazyFunnelsort.sort(arr);
        assertEquals("a", arr[0]);
        assertEquals("b", arr[1]);
        arr = new String[] { "c", "b", "a" };
        LazyFunnelsort.sort(arr);
        assertEquals("a", arr[0]);
        assertEquals("b", arr[1]);
        assertEquals("c", arr[2]);
        arr = new String[] { "c", "d", "b", "e", "a" };
        LazyFunnelsort.sort(arr);
        assertEquals("a", arr[0]);
        assertEquals("b", arr[1]);
        assertEquals("c", arr[2]);
        assertEquals("d", arr[3]);
        assertEquals("e", arr[4]);
        arr = new String[] { "j", "f", "c", "b", "i", "g", "a", "d", "e", "h" };
        LazyFunnelsort.sort(arr);
        assertEquals("a", arr[0]);
        assertEquals("b", arr[1]);
        assertEquals("c", arr[2]);
        assertEquals("d", arr[3]);
        assertEquals("e", arr[4]);
        assertEquals("f", arr[5]);
        assertEquals("g", arr[6]);
        assertEquals("h", arr[7]);
        assertEquals("i", arr[8]);
        assertEquals("j", arr[9]);
    }

    @Test
    public void testSmallReversed() {
        try {
            List<String> data = Tests.loadData();
            data = data.subList(0, 1024);
            Collections.reverse(data);
            String[] arr = data.toArray(new String[data.size()]);
            LazyFunnelsort.sort(arr);
            assertTrue(Tests.isSorted(arr));
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }

//    @Test
    public void testSmallShuffled() {
        try {
            List<String> data = Tests.loadData();
            Collections.shuffle(data);
            data = data.subList(0, 1024);
            String[] arr = data.toArray(new String[data.size()]);
            LazyFunnelsort.sort(arr);
            assertTrue(Tests.isSorted(arr));
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }

//    @Test
    public void testDictWords() {
        try {
            // Use the large dictionary rather than the trivial one.
            List<String> data = Tests.loadData("dictwords.gz", true);
            Collections.shuffle(data);
            String[] arr = data.toArray(new String[data.size()]);
            LazyFunnelsort.sort(arr);
            assertTrue(Tests.isSorted(arr));
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }

//    @Test
    public void testSorted() {
        try {
            List<String> data = Tests.loadData();
            Collections.sort(data);
            String[] arr = data.toArray(new String[data.size()]);
            LazyFunnelsort.sort(arr);
            assertTrue(Tests.isSorted(arr));
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }

//    @Test
    public void testReversed() {
        try {
            List<String> data = Tests.loadData();
            Collections.sort(data);
            Collections.reverse(data);
            String[] arr = data.toArray(new String[data.size()]);
            LazyFunnelsort.sort(arr);
            assertTrue(Tests.isSorted(arr));
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }

//    @Test
    public void testRepeated() {
        // Make the size of the set large enough to burst buckets.
        String[] arr = new String[1310720];
        final String STR = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        Arrays.fill(arr, STR);
        LazyFunnelsort.sort(arr);
        assertTrue(Tests.isRepeated(arr, STR));
    }

//    @Test
    public void testRepeatedCycle() {
        String[] strs = new String[100];
        String seed = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        for (int i = 0, l = 1; i < strs.length; i++, l++) {
            strs[i] = seed.substring(0, l);
        }
        List<String> list = new ArrayList<String>();
        for (int c = 3162300, i = 0; c > 0; i++, c--) {
            list.add(strs[i % strs.length]);
        }
        String[] arr = list.toArray(new String[list.size()]);
        LazyFunnelsort.sort(arr);
        assertTrue(Tests.isSorted(arr));
    }

//    @Test
    public void testRandom() {
        List<String> data = Tests.generateData(1000000, 100);
        String[] arr = data.toArray(new String[data.size()]);
        LazyFunnelsort.sort(arr);
        assertTrue(Tests.isSorted(arr));
    }

//    @Test
    public void testHamlet() {
        try {
            List<String> data = Tests.loadData("hamletwords");
            Collections.shuffle(data);
            String[] arr = data.toArray(new String[data.size()]);
            LazyFunnelsort.sort(arr);
            assertTrue(Tests.isSorted(arr));
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }

//    @Test
    public void testDictCalls() {
        try {
            List<String> data = Tests.loadData("dictcalls.gz", true);
            String[] arr = data.toArray(new String[data.size()]);
            LazyFunnelsort.sort(arr);
            assertTrue(Tests.isSorted(arr));
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }
}
