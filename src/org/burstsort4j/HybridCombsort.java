/*
 * Copyright 2011 Nathan Fiedler. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package org.burstsort4j;

/**
 * An implementation of comb sort that delegates to insertion sort when
 * the gap value has dropped below a certain threshold. This variation
 * was proposed by David B. Ring of Palo Alto and demonstrated to be
 * 10 to 15 percent faster than traditional comb sort. This particular
 * implementation uses the Combsort11 variation for determining the
 * gap values.
 *
 * @author Nathan Fiedler
 */
public class HybridCombsort {

    private HybridCombsort() {
    }

    /**
     * Sort the input array using a hybrid of Combsort11 and Insertion sort.
     *
     * @param  <T>    type of comparable to be sorted.
     * @param  input  array of comparable objects to be sorted.
     */
    public static <T extends Comparable<? super T>> void sort(T[] input) {
        if (input == null || input.length < 2) {
            return;
        }

        int gap = input.length;
        while (gap > 8) {
            gap = (10 * gap) / 13;
            if (gap == 10 || gap == 9) {
                gap = 11;
            }
            for (int i = 0; i + gap < input.length; i++) {
                int j = i + gap;
                if (input[i].compareTo(input[j]) > 0) {
                    T tmp = input[i];
                    input[i] = input[j];
                    input[j] = tmp;
                }
            }
        }
        // At this point the input is nearly sorted, a case for which
        // insertion sort performs very well.
        Insertionsort.sort(input);
    }
}
