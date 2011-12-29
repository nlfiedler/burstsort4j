/*
 * Copyright 2009-2011 Nathan Fiedler. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package org.burstsort4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.burstsort4j.LazyFunnelsort.Kmerger;
import org.burstsort4j.LazyFunnelsort.MergerFactory;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the LazyFunnelsort class.
 *
 * @author Nathan Fiedler
 */
public class LazyFunnelsortTest {

    @Test
    public void test_Mergers_Dictwords() {
        try {
            // Use the large dictionary rather than the trivial one.
            List<String> data = Tests.loadData("dictwords.gz", true);
            Collections.shuffle(data);
            test_Mergers(data);
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }

    @Test
    public void test_Mergers_Sorted() {
        try {
            List<String> data = Tests.loadData();
            Collections.sort(data);
            test_Mergers(data);
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }

    @Test
    public void test_Mergers_Reversed() {
        try {
            List<String> data = Tests.loadData();
            Collections.sort(data);
            Collections.reverse(data);
            test_Mergers(data);
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }

    @Test
    public void test_Mergers_Repeated() {
        String[] arr = new String[25000];
        String seed = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        Arrays.fill(arr, seed);
        List<String> data = new ArrayList<String>();
        for (String s : arr) {
            data.add(s);
        }
        test_Mergers(data);
    }

    @Test
    public void test_Mergers_RepeatedCycle() {
        String[] strs = new String[100];
        String seed = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        for (int i = 0, l = 1; i < strs.length; i++, l++) {
            strs[i] = seed.substring(0, l);
        }
        List<String> data = new ArrayList<String>();
        for (int c = 25000, i = 0; c > 0; i++, c--) {
            data.add(strs[i % strs.length]);
        }
        test_Mergers(data);
    }

    /**
     * Tests the merger implementations for correctness using the given
     * data. This only tests the case where the inputs are circular buffers.
     *
     * @param  data  data to use for testing.
     */
    private void test_Mergers(List<String> data) {
        // Test using different numbers of partitions.
        test_Mergers(data, 4);
        test_Mergers(data, 8);
        test_Mergers(data, 16);
        test_Mergers(data, 32);
        test_Mergers(data, 64);
    }

    /**
     * Tests the merger implementations for correctness using the given
     * data. This only tests the case where the inputs are circular buffers.
     *
     * @param  data        data to use for testing (will not be modified).
     * @param  partitions  number of partitions to create, sort and merge
     */
    private void test_Mergers(List<String> data, int partitions) {
        String[] arr = data.toArray(new String[data.size()]);
        // Split up the inputs for the insertion d-way merger.
        List<CircularBuffer<Comparable>> inputs = new ArrayList<CircularBuffer<Comparable>>();
        int size = arr.length / partitions;
        int offset = 0;
        while (offset < arr.length) {
            if (offset + size > arr.length) {
                size = arr.length - offset;
            }
            Arrays.sort(arr, offset, offset + size);
            inputs.add(new CircularBuffer<Comparable>(arr, offset, size, false));
            offset += size;
        }
        CircularBuffer<Comparable> output = new CircularBuffer<Comparable>(arr.length);
        // Test the merger.
        Kmerger merger = MergerFactory.createMerger(inputs, 0, inputs.size(), output);
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
        String[] arr = new String[]{"a"};
        LazyFunnelsort.sort(arr);
        arr = new String[]{"b", "a"};
        LazyFunnelsort.sort(arr);
        assertEquals("a", arr[0]);
        assertEquals("b", arr[1]);
        arr = new String[]{"c", "b", "a"};
        LazyFunnelsort.sort(arr);
        assertEquals("a", arr[0]);
        assertEquals("b", arr[1]);
        assertEquals("c", arr[2]);
        arr = new String[]{"c", "d", "b", "e", "a"};
        LazyFunnelsort.sort(arr);
        assertEquals("a", arr[0]);
        assertEquals("b", arr[1]);
        assertEquals("c", arr[2]);
        assertEquals("d", arr[3]);
        assertEquals("e", arr[4]);
        arr = new String[]{"j", "f", "c", "b", "i", "g", "a", "d", "e", "h"};
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
        // test with all empty input
        arr = new String[]{"", "", "", "", "", "", "", "", "", ""};
        LazyFunnelsort.sort(arr);
        for (String s : arr) {
            assertEquals("", s);
        }
        // test with peculiar input
        arr = new String[]{"z", "m", "", "a", "d", "tt", "tt", "tt", "foo", "bar"};
        LazyFunnelsort.sort(arr);
        assertTrue("peculiar input not sorted", Tests.isSorted(arr));
    }

    @Test
    public void testSmallReversed() {
        try {
            List<String> data = Tests.loadData();
            Collections.shuffle(data);
            data = data.subList(0, 1024);
            Collections.sort(data);
            Collections.reverse(data);
            String[] arr = data.toArray(new String[data.size()]);
            LazyFunnelsort.sort(arr);
            assertTrue(Tests.isSorted(arr));
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
    public void testRepeated() {
        String[] arr = new String[1310720];
        final String STR = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        Arrays.fill(arr, STR);
        LazyFunnelsort.sort(arr);
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
        for (int c = 3162300, i = 0; c > 0; i++, c--) {
            list.add(strs[i % strs.length]);
        }
        String[] arr = list.toArray(new String[list.size()]);
        LazyFunnelsort.sort(arr);
        assertTrue(Tests.isSorted(arr));
    }

    @Test
    public void testRandom() {
        List<String> data = Tests.generateData(1000000, 100);
        String[] arr = data.toArray(new String[data.size()]);
        LazyFunnelsort.sort(arr);
        assertTrue(Tests.isSorted(arr));
    }

    @Test
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

    @Test
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

    @Test
    public void testSmallShuffledThreaded() {
        try {
            List<String> data = Tests.loadData();
            Collections.shuffle(data);
            data = data.subList(0, 1024);
            String[] arr = data.toArray(new String[data.size()]);
            LazyFunnelsort.sortThreaded(arr);
            assertTrue(Tests.isSorted(arr));
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }

    @Test
    public void testDictWordsThreaded() {
        try {
            // Use the large dictionary rather than the trivial one.
            List<String> data = Tests.loadData("dictwords.gz", true);
            Collections.shuffle(data);
            String[] arr = data.toArray(new String[data.size()]);
            LazyFunnelsort.sortThreaded(arr);
            assertTrue(Tests.isSorted(arr));
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }
}
