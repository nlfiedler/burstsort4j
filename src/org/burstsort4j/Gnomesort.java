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
 * Implementation of Gnome sort based on psuedocode on Wikipedia.
 *
 * @author Nathan Fiedler
 */
public class Gnomesort {

    /**
     * Sort the input array using the Gnome sort algorithm.
     *  O(n^2) running time.
     *
     * @param  <T>    type of comparable to be sorted.
     * @param  input  array of comparable objects to be sorted.
     */
    public static <T extends Comparable<? super T>> void sort(T[] input) {
        if (input == null || input.length == 0) {
            return;
        }
        int i = 1;
        int j = 2;
        while (i < input.length) {
            if (input[i - 1].compareTo(input[i]) < 1) {
                i = j;
                j++;
            } else {
                // swap a[i-1] and a[i]
                T t = input[i - 1];
                input[i - 1] = input[i];
                input[i] = t;
                i--;
                if (i == 0) {
                    i = j;
                    j++;
                }
            }
        }
    }
}
