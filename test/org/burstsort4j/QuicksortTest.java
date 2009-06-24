/*
 * Copyright (C) 2008-2009  Nathan Fiedler
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
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the Quicksort class.
 *
 * @author Nathan Fiedler
 */
public class QuicksortTest {

    @Test
    public void testArguments() {
        Quicksort.sort(null);
        Quicksort.sort(new String[0]);
        String[] arr = new String[] { "a" };
        Quicksort.sort(arr);
        arr = new String[] { "b", "a" };
        Quicksort.sort(arr);
        assertTrue(Tests.isSorted(arr));
        arr = new String[] { "c", "b", "a" };
        Quicksort.sort(arr);
        assertTrue(Tests.isSorted(arr));
    }

    @Test
    public void testQuicksort() {
        try {
            List<String> data = Tests.loadData();
            Collections.shuffle(data);
            String[] arr = data.toArray(new String[data.size()]);
            Quicksort.sort(arr);
            assertTrue(Tests.isSorted(arr));
            // Test with sorted list
            Quicksort.sort(arr);
            assertTrue(Tests.isSorted(arr));
            // Test with reverse sorted list
            Collections.reverse(data);
            arr = data.toArray(new String[data.size()]);
            Quicksort.sort(arr);
            assertTrue(Tests.isSorted(arr));
            // Test with non-unique word list.
            data = Tests.loadData("hamletwords");
            Collections.shuffle(data);
            arr = data.toArray(new String[data.size()]);
            Quicksort.sort(arr);
            assertTrue(Tests.isSorted(arr));
            // Test with sorted list
            Quicksort.sort(arr);
            assertTrue(Tests.isSorted(arr));
            // Test with reverse sorted list
            Collections.reverse(data);
            arr = data.toArray(new String[data.size()]);
            Quicksort.sort(arr);
            assertTrue(Tests.isSorted(arr));
            // Test with dict calls data
            data = Tests.loadData("dictcalls.gz", true);
            arr = data.toArray(new String[data.size()]);
            Quicksort.sort(arr);
            assertTrue(Tests.isSorted(arr));
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }
}
