/*
 * Copyright 2009-2011 Nathan Fiedler. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package org.burstsort4j;

/**
 * Basic selection sort implementation based on pseudocode on Wikipedia.
 *
 * @author  Nathan Fiedler
 */
public class Selectionsort {

    private Selectionsort() {
    }

    /**
     * Sort the input array using the selection sort algorithm.
     * O(n^2) running time.
     *
     * @param  <T>    type of comparable to be sorted.
     * @param  input  array of comparable objects to be sorted.
     */
    public static <T extends Comparable<? super T>> void sort(T[] input) {
        if (input != null && input.length > 1) {
            sort(input, 0, input.length - 1);
        }
    }

    /**
     * Sort the input array using the selection sort algorithm.
     * O(n^2) running time.
     *
     * @param  <T>    type of comparable to be sorted.
     * @param  input  array of comparable objects to be sorted.
     * @param  low    low end of range to sort.
     * @param  high   high end of range to sort (inclusive).
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
