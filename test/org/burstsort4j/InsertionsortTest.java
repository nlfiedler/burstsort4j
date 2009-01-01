/*
 * Copyright (C) 2008  Nathan Fiedler
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test the insertion sort implementations.
 *
 * @author Nathan Fiedler
 */
public class InsertionsortTest {

    @Test
    public void testArguments() {
        Insertionsort.sort(null, 0, 0);
        Insertionsort.sort(new String[0], 0, 0);
        String[] arr = new String[] { "a" };
        Insertionsort.sort(arr, 0, arr.length - 1);
        arr = new String[] { "b", "a" };
        Insertionsort.sort(arr, 0, arr.length - 1);
        assertTrue(Tests.isSorted(arr));
        arr = new String[] { "c", "b", "a" };
        Insertionsort.sort(arr, 0, arr.length - 1);
        assertTrue(Tests.isSorted(arr));
    }

    @Test
    public void testComparable() {
        try {
            List<String> data = Tests.loadData();
            Collections.shuffle(data);
            String[] arr = data.toArray(new String[data.size()]);
            Insertionsort.sort(arr, 0, arr.length - 1);
            assertTrue(Tests.isSorted(arr));
            // Test with sorted list
            Insertionsort.sort(arr, 0, arr.length - 1);
            assertTrue(Tests.isSorted(arr));
            // Test with reverse sorted list
            Collections.reverse(data);
            arr = data.toArray(new String[data.size()]);
            Insertionsort.sort(arr, 0, arr.length - 1);
            assertTrue(Tests.isSorted(arr));
            // Test with non-unique word list.
            data = Tests.loadData("hamletwords");
            Collections.shuffle(data);
            arr = data.toArray(new String[data.size()]);
            Insertionsort.sort(arr, 0, arr.length - 1);
            assertTrue(Tests.isSorted(arr));
            // Test with sorted list
            Insertionsort.sort(arr, 0, arr.length - 1);
            assertTrue(Tests.isSorted(arr));
            // Test with reverse sorted list
            Collections.reverse(data);
            arr = data.toArray(new String[data.size()]);
            Insertionsort.sort(arr, 0, arr.length - 1);
            assertTrue(Tests.isSorted(arr));
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
        // Test with repeated strings.
        String[] arr = new String[16384];
        Arrays.fill(arr, "abcdefghijklmnopqrstuvwxyz");
        Insertionsort.sort(arr, 0, arr.length - 1);
        assertTrue(Tests.isRepeated(arr, "abcdefghijklmnopqrstuvwxyz"));
        // Test with randomly generated strings.
        List<String> data = Tests.generateData(16384, 64);
        arr = data.toArray(new String[data.size()]);
        Insertionsort.sort(arr, 0, arr.length - 1);
        assertTrue(Tests.isSorted(arr));
    }

    @Test
    public void testStrings() {
        try {
            List<String> data = Tests.loadData();
            Collections.shuffle(data);
            String[] arr = data.toArray(new String[data.size()]);
            Insertionsort.sort(arr, 0, arr.length, 0);
            assertTrue(Tests.isSorted(arr));
            // Test with sorted list
            Insertionsort.sort(arr, 0, arr.length, 0);
            assertTrue(Tests.isSorted(arr));
            // Test with reverse sorted list
            Collections.reverse(data);
            arr = data.toArray(new String[data.size()]);
            Insertionsort.sort(arr, 0, arr.length, 0);
            assertTrue(Tests.isSorted(arr));
            // Test with non-unique word list.
            data = Tests.loadData("hamletwords");
            Collections.shuffle(data);
            arr = data.toArray(new String[data.size()]);
            Insertionsort.sort(arr, 0, arr.length, 0);
            assertTrue(Tests.isSorted(arr));
            // Test with sorted list
            Insertionsort.sort(arr, 0, arr.length, 0);
            assertTrue(Tests.isSorted(arr));
            // Test with reverse sorted list
            Collections.reverse(data);
            arr = data.toArray(new String[data.size()]);
            Insertionsort.sort(arr, 0, arr.length, 0);
            assertTrue(Tests.isSorted(arr));
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
        // Test with repeated strings.
        String[] arr = new String[16384];
        Arrays.fill(arr, "abcdefghijklmnopqrstuvwxyz");
        Insertionsort.sort(arr, 0, arr.length, 0);
        assertTrue(Tests.isRepeated(arr, "abcdefghijklmnopqrstuvwxyz"));
        // Test with randomly generated strings.
        List<String> data = Tests.generateData(16384, 64);
        arr = data.toArray(new String[data.size()]);
        Insertionsort.sort(arr, 0, arr.length, 0);
        assertTrue(Tests.isSorted(arr));
    }
}
