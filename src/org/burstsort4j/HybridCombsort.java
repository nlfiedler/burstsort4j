/*
 * Copyright (C) 2011  Nathan Fiedler
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
