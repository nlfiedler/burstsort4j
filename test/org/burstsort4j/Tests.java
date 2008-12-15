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
     * Loads the test data into a list.
     *
     * @return  test data as a list.
     * @throws  java.io.IOException
     *          if a problem occurs.
     */
    public static List<String> loadData() throws IOException {
        InputStream is = Tests.class.getResourceAsStream("dictwords");
        List<String> list = new ArrayList<String>();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        for (String ln = br.readLine(); ln != null; ln = br.readLine()) {
            list.add(ln);
        }
        br.close();
        return list;
    }
}
