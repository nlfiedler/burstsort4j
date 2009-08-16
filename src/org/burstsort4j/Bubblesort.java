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
 * Implementation of Bubble sort based on psuedocode on Wikipedia.
 *
 * @author Nathan Fiedler
 */
public class Bubblesort {

    /**
     * Sort the input array using the Bubble sort algorithm.
     * Worst and average case O(n^2) running time.
     *
     * @param  <T>    type of comparable to be sorted.
     * @param  input  array of comparable objects to be sorted.
     */
    public static <T extends Comparable<? super T>> void sort(T[] input) {
        if (input == null || input.length == 0) {
            return;
        }
        int n = input.length;
        boolean swapped;
        do {
            swapped = false;
            n--;
            for (int i = 0; i < n; i++) {
                if (input[i].compareTo(input[i + 1]) > 0) {
                    T temp = input[i];
                    input[i] = input[i + 1];
                    input[i + 1] = temp;
                    swapped = true;
                }
            }
        } while (swapped);
    }
}
