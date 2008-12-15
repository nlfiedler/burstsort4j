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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the Burstsort class.
 *
 * @author nfiedler
 */
public class BurstsortTest {
    private static List<String> data;

    public BurstsortTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        try {
            data = Tests.loadData();
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }

    @Before
    public void setUp() {
        Collections.shuffle(data);
    }

    @Test
    public void testDictWords() {
        String[] arr = data.toArray(new String[data.size()]);
        Burstsort.sort(arr);
        assertTrue(Tests.isSorted(arr));
    }

    @Test
    public void testSorted() {
        Collections.sort(data);
        String[] arr = data.toArray(new String[data.size()]);
        Burstsort.sort(arr);
        assertTrue(Tests.isSorted(arr));
    }

    @Test
    public void testReversed() {
        Collections.sort(data);
        Collections.reverse(data);
        String[] arr = data.toArray(new String[data.size()]);
        Burstsort.sort(arr);
        assertTrue(Tests.isSorted(arr));
    }

    @Test
    public void testRepeated() {
        // Make the size of the set large enough to burst buckets.
        String[] arr = new String[16384];
        Arrays.fill(arr, "abcdefghijklmnopqrstuvwxyz");
        Burstsort.sort(arr);
        assertTrue(Tests.isRepeated(arr, "abcdefghijklmnopqrstuvwxyz"));
    }

    @Test
    public void testRandom() {
        List<String> rlist = Tests.generateData(32768, 64);
        String[] arr = rlist.toArray(new String[rlist.size()]);
        Burstsort.sort(arr);
        assertTrue(Tests.isSorted(arr));
    }
}
