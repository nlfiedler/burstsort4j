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
     * Merges the given list of buffers into a single buffer using an
     * insertion d-way merge as described by Moruz and Brodal in WADS05.
     *
     * @param  inputs  list of buffers to be merged.
     * @param  output  where merged results are stored.
     */
    @SuppressWarnings("unchecked")
    public static void insertionMerge(List<CircularBuffer<String>> inputs,
            CircularBuffer<String> output) {
        // Set up the array (d) of input buffers.
        CircularBuffer<String>[] d = new CircularBuffer[inputs.size()];
        d = inputs.toArray(d);

        // Perform an insertion sort of the buffers using the leading values.
        for (int i = 1; i < d.length; i++) {
            CircularBuffer<String> tmp = d[i];
            int j = i;
            while (j > 0 && tmp.peek().compareTo(d[j - 1].peek()) < 0) {
                d[j] = d[j - 1];
                j--;
            }
            d[j] = tmp;
        }

        // While there are streams to be processed...
        while (d.length > 0) {
            if (d.length == 1) {
                // Copy remainder of last stream to output.
                d[0].drain(output);
                d = new CircularBuffer[0];
            } else if (d.length == 2) {
                // With only two streams, perform a faster merge.
                CircularBuffer<String> a = d[0];
                CircularBuffer<String> b = d[1];
                while (!a.isEmpty() && !b.isEmpty()) {
                    if (a.peek().compareTo(b.peek()) < 0) {
                        output.add(a.remove());
                    } else {
                        output.add(b.remove());
                    }
                }
                if (!a.isEmpty()) {
                    a.drain(output);
                }
                if (!b.isEmpty()) {
                    b.drain(output);
                }
                d = new CircularBuffer[0];
            } else {
                output.add(d[0].remove());
                if (d[0].isEmpty()) {
                    // This stream has been exhausted, remove it from the pool.
                    CircularBuffer[] td = new CircularBuffer[d.length - 1];
                    System.arraycopy(d, 1, td, 0, td.length);
                    d = td;
                } else {
                    // Insert new candidate into correct position in d.
                    CircularBuffer<String> t = d[0];
                    String s = t.peek();
                    int j = 1;
                    while (j < d.length && s.compareTo(d[j].peek()) > 0) {
                        d[j - 1] = d[j];
                        j++;
                    }
                    d[j - 1] = t;
                }
            }
        }
    }
}
