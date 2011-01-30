/*
 * Copyright 2009-2011 Nathan Fiedler. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package org.burstsort4j;

/**
 * Implementation of comb sort based on pseudo-code on Wikipedia,
 * in particular the Combsort11 algorithm.
 *
 * @author Nathan Fiedler
 */
public class Combsort {

    private Combsort() {
    }

    /**
     * Sort the input array using the Combsort11 algorithm.
     * Purportedly O(n*logn) running time.
     *
     * @param  <T>    type of comparable to be sorted.
     * @param  input  array of comparable objects to be sorted.
     */
    public static <T extends Comparable<? super T>> void sort(T[] input) {
        if (input == null || input.length < 2) {
            return;
        }

        int gap = input.length; //initialize gap size
        boolean swapped = true;

        while (gap > 1 || swapped) {
            // Update the gap value for the next comb.
            if (gap > 1) {
                gap = (gap * 10) / 13;
                if (gap == 10 || gap == 9) {
                    gap = 11;
                }
            }

            swapped = false;

            // a single "comb" over the input list
            for (int i = 0; i + gap < input.length; i++) {
                int j = i + gap;
                if (input[i].compareTo(input[j]) > 0) {
                    T tmp = input[i];
                    input[i] = input[j];
                    input[j] = tmp;
                    // Signal that the list is not guaranteed sorted.
                    swapped = true;
                }
            }
        }
    }
}
