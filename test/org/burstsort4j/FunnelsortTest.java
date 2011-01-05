/*
 * Copyright (C) 2009-2011  Nathan Fiedler
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
 * Unit tests for the Funnelsort class.
 *
 * @author Nathan Fiedler
 */
public class FunnelsortTest {

    @Test
    public void testArguments() {
        Funnelsort.sort((String[]) null);
        Funnelsort.sort(new String[0]);
        String[] arr = new String[] { "a" };
        Funnelsort.sort(arr);
        arr = new String[] { "b", "a" };
        Funnelsort.sort(arr);
        assertEquals("a", arr[0]);
        assertEquals("b", arr[1]);
        arr = new String[] { "c", "b", "a" };
        Funnelsort.sort(arr);
        assertEquals("a", arr[0]);
        assertEquals("b", arr[1]);
        assertEquals("c", arr[2]);
        arr = new String[] { "c", "d", "b", "e", "a" };
        Funnelsort.sort(arr);
        assertEquals("a", arr[0]);
        assertEquals("b", arr[1]);
        assertEquals("c", arr[2]);
        assertEquals("d", arr[3]);
        assertEquals("e", arr[4]);
        arr = new String[] { "j", "f", "c", "b", "i", "g", "a", "d", "e", "h" };
        Funnelsort.sort(arr);
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

//    @Test
    public void testSmallReversed() {
        try {
            List<String> data = Tests.loadData();
            data = data.subList(0, 1024);
            Collections.reverse(data);
            String[] arr = data.toArray(new String[data.size()]);
            Funnelsort.sort(arr);
            assertTrue(Tests.isSorted(arr));
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }

//    @Test
    public void testTinyShuffled() {
        try {
            List<String> data = Tests.loadData();
            Collections.shuffle(data);
            data = data.subList(0, 100);
            String[] arr = data.toArray(new String[data.size()]);
            Funnelsort.sort(arr);
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
            Funnelsort.sort(arr);
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
            Funnelsort.sort(arr);
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
            Funnelsort.sort(arr);
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
            Funnelsort.sort(arr);
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
        Funnelsort.sort(arr);
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
        Funnelsort.sort(arr);
        assertTrue(Tests.isSorted(arr));
    }

//    @Test
    public void testRandom() {
        List<String> data = Tests.generateData(1000000, 100);
        String[] arr = data.toArray(new String[data.size()]);
        Funnelsort.sort(arr);
        assertTrue(Tests.isSorted(arr));
    }

//    @Test
    public void testHamlet() {
        try {
            List<String> data = Tests.loadData("hamletwords");
            Collections.shuffle(data);
            String[] arr = data.toArray(new String[data.size()]);
            Funnelsort.sort(arr);
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
            Funnelsort.sort(arr);
            assertTrue(Tests.isSorted(arr));
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }
}
