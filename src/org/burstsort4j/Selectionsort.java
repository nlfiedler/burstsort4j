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

/**
 * Basic selection sort implementation based on psuedocode on Wikipedia.
 *
 * @author  Nathan Fiedler
 */
public class Selectionsort {

    /**
     * Sort the input array using the selection sort algorithm.
     * O(n^2) running time.
     *
     * @param  <T>    type of comparable to be sorted.
     * @param  input  array of comparable objects to be sorted.
     * @param  low    low end of range to sort.
     * @param  high   high end of range to sort.
     */
    public static <T extends Comparable<? super T>> void sort(T[] input,
            int low, int high) {
        if (input == null || input.length < 2 || high <= low) {
            return;
        }

        for (int ii = low; ii < high; ii++) {
            int min = ii;
            for (int jj = ii + 1; jj < input.length; jj++) {
                if (input[jj].compareTo(input[min]) < 0) {
                    min = jj;
                }
            }
            if (ii != min) {
                T temp = input[ii];
                input[ii] = input[min];
                input[min] = temp;
            }
        }
    }
}
