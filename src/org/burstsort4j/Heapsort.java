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
 * Binary heap sort implementation from code codex web site.
 *
 * @author  Nathan Fiedler
 */
public class Heapsort {

    /**
     * Sort the input array using the Combsort11 algorithm.
     * Purportedly O(n*logn) running time.
     *
     * @param  <T>    type of comparable to be sorted.
     * @param  input  array of comparable objects to be sorted.
     */
    public static <T extends Comparable<? super T>> void sort(T[] input) {
        if (input == null || input.length == 0) {
            return;
        }

        // Build the binary heap.
        for (int child = 1; child < input.length; child++) {
            int parent = (child - 1) / 2;
            while (parent >= 0 && input[parent].compareTo(input[child]) < 0) {
                T temp = input[parent];
                input[parent] = input[child];
                input[child] = temp;
                child = parent;
                parent = (child - 1) / 2;
            }
        }

        // Shrink the heap to sort the contents.
        for (int n = input.length - 1; n >= 0; n--) {
            T temp = input[0];
            input[0] = input[n];
            input[n] = temp;
            int parent = 0;
            while (true) {
                int leftChild = 2 * parent + 1;
                if (leftChild >= n) {
                    // no more children
                    break;
                }
                int rightChild = leftChild + 1;
                int maxChild = leftChild;
                if (rightChild < n && input[leftChild].compareTo(input[rightChild]) < 0) {
                    maxChild = rightChild;
                }
                if (input[parent].compareTo(input[maxChild]) < 0) {
                    temp = input[parent];
                    input[parent] = input[maxChild];
                    input[maxChild] = temp;
                    parent = maxChild;
                } else {
                    break;
                }
            }
        }
    }
}
