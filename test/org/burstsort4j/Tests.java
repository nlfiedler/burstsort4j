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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.GZIPInputStream;

/**
 * Collection of utility methods to support the unit tests.
 *
 * @author nfiedler
 */
public class Tests {

    /**
     * Generates a set of <em>n</em> strings in a List consisting of
     * <em>l</em> randomly selected alphanumeric characters (mixed case).
     *
     * @param  n  number of random strings to generate.
     * @param  l  length of each random string.
     * @return  the list of randomly generated strings.
     */
    public static List<String> generateData(int n, int l) {
        Random r = new Random();
        List<String> list = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        for (int ii = 0; ii < n; ii++) {
            for (int jj = 0; jj < l; jj++) {
                int d = r.nextInt(62);
                if (d < 10) {
                    sb.append((char) ('0' + d));
                } else if (d < 36) {
                    sb.append((char) ('A' + (d - 10)));
                } else {
                    sb.append((char) ('a' + (d - 36)));
                }
            }
            list.add(sb.toString());
            sb.setLength(0);
        }
        return list;
    }

    /**
     * Loads the default test data into a list.
     *
     * @return  test data as a list.
     * @throws  java.io.IOException
     *          if a problem occurs.
     */
    public static List<String> loadData() throws IOException {
        return loadData("dictwords", false);
    }

    /**
     * Loads the specified test data into a list of strings.
     *
     * @param  file  name of file to be loaded (must be present in classpath).
     * @return  test data as a list.
     * @throws  java.io.IOException
     *          if a problem occurs.
     */
    public static List<String> loadData(String file) throws IOException {
        return loadData(file, false);
    }

    /**
     * Loads the specified test data into a list of strings.
     *
     * @param  file  name of file to be loaded (must be present in classpath).
     * @param  gzip  if true, stream will be decompressed using gzip.
     * @return  test data as a list.
     * @throws  java.io.IOException
     *          if a problem occurs.
     */
    public static List<String> loadData(String file, boolean gzip) throws IOException {
        InputStream is = Tests.class.getResourceAsStream(file);
        if (gzip) {
            is = new GZIPInputStream(is);
        }
        List<String> list = new ArrayList<String>();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        for (String ln = br.readLine(); ln != null; ln = br.readLine()) {
            list.add(ln);
        }
        br.close();
        return list;
    }

    /**
     * Tests if the given array of strings is a repeating sequence.
     *
     * @param  arr  array of strings to test.
     * @param  s    the expected repeated value.
     * @return  true if repeating, false otherwise.
     */
    public static boolean isRepeated(String[] arr, String s) {
        for (int ii = 0; ii < arr.length; ii++) {
            if (!arr[ii].equals(s)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests if the given array of strings is in sorted order.
     *
     * @param  arr  array of strings to test.
     * @return  true if sorted, false otherwise.
     */
    public static boolean isSorted(String[] arr) {
        for (int ii = 1; ii < arr.length; ii++) {
            if (arr[ii - 1].compareTo(arr[ii]) > 0) {
                return false;
            }
        }
        return true;
    }
}
