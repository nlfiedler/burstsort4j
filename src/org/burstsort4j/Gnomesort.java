/*
 * Copyright 2009-2011 Nathan Fiedler. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package org.burstsort4j;

/**
 * Implementation of Gnome sort based on pseudocode on Wikipedia.
 *
 * @author Nathan Fiedler
 */
public class Gnomesort {

    private Gnomesort() {
    }

    /**
     * Sort the input array using the Gnome sort algorithm.
     *  O(n^2) running time.
     *
     * @param  <T>    type of comparable to be sorted.
     * @param  input  array of comparable objects to be sorted.
     */
    public static <T extends Comparable<? super T>> void sort(T[] input) {
        if (input == null || input.length < 2) {
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
