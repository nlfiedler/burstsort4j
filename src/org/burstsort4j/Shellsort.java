/*
 * Copyright (C) 2009-2010  Nathan Fiedler
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
 * Shell sort implementation based on pseudocode from Wikipedia.
 *
 * @author  Nathan Fiedler
 */
public class Shellsort {

    private Shellsort() {
    }

    /**
     * Sort the input array using the shell sort algorithm with the gap
     * sequence suggested by Gonnet and Baeza-Yates.
     *
     * @param  <T>    type of comparable to be sorted.
     * @param  input  array of comparable objects to be sorted.
     */
    public static <T extends Comparable<? super T>> void sort(T[] input) {
        if (input == null || input.length < 2) {
            return;
        }

        int inc = input.length / 2;
        while (inc > 0) {
            for (int ii = inc; ii < input.length; ii++) {
                T temp = input[ii];
                int jj = ii;
                while (jj >= inc && input[jj - inc].compareTo(temp) > 0) {
                    input[jj] = input[jj - inc];
                    jj -= inc;
                }
                input[jj] = temp;
            }
            // Another way of dividing by 2.2 to get an integer.
            inc = inc == 2 ? 1 : inc * 5 / 11;
        }
    }
}
