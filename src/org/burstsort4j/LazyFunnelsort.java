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

import java.util.List;

/**
 * An implementation of the lazy funnelsort algorithm as described by Brodal,
 * Fagerberg, and Vinther, which itself is adapted from the original
 * algorithm by Frigo, Leiserson, Prokop, and Ramachandran.
 *
 * @author Nathan Fiedler
 */
public class LazyFunnelsort {

    /**
     * Merges the given list of arrays into a single array using an
     * insertion d-way merge as described by Moruz and Brodal in WADS05.
     *
     * @param  inputs  list of arrays to be merged.
     * @param  output  where merged results are stored.
     * @param  offset  starting position in output.
     */
    public static void insertionMerge(List<String[]> inputs, String[] output, int offset) {
// TODO: change all arrays to circular buffers
        // Set up the array (d) of inputs and indices within those inputs.
        String[][] d = new String[inputs.size()][];
        int[] di = new int[d.length];
        for (int i = 0; i < di.length; i++) {
            d[i] = inputs.get(i);
            di[i] = 0;
        }

        // Perform an insertion sort of the streams using the leading values.
        for (int i = 1; i < d.length; i++) {
            String[] tmp = d[i];
            int j = i;
            while (j > 0 && tmp[0].compareTo(d[j - 1][0]) < 0) {
                d[j] = d[j - 1];
                j--;
            }
            d[j] = tmp;
        }

        // While there are streams to be processed...
        while (d.length > 0) {
            if (d.length == 1) {
                // Copy remainder of last stream to output.
                System.arraycopy(d[0], di[0], output, offset, d[0].length - di[0]);
                d = new String[0][];
//            } else if (d.length == 2) {
//                // TODO: just two streams, do the usual merge
            } else {
                output[offset] = d[0][di[0]];
                offset++;
                di[0]++;
                if (di[0] == d[0].length) {
                    // This stream has been exhausted, remove it from the pool.
                    String[][] td = new String[d.length - 1][];
                    System.arraycopy(d, 1, td, 0, td.length);
                    int[] tdi = new int[di.length - 1];
                    System.arraycopy(di, 1, tdi, 0, tdi.length);
                    d = td;
                    di = tdi;
                } else {
                    // Insert new candidate into correct position in d,
                    // keeping the indices in di in corresponding order.
                    String[] t = d[0];
                    int ti = di[0];
                    int j = 1;
                    while (j < d.length && t[ti].compareTo(d[j][di[j]]) > 0) {
                        d[j - 1] = d[j];
                        di[j - 1] = di[j];
                        j++;
                    }
                    d[j - 1] = t;
                    di[j - 1] = ti;
                }
            }
        }
    }
}
