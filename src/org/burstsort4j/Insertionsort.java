/*
 * Copyright (C) 2008-2009  Nathan Fiedler
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
 * Simple implementation of insertion sort algorithm. Used by the
 * multikey quicksort implementation for small subarrays.
 *
 * @author Nathan Fiedler
 */
public class Insertionsort {

//    public static void sort(int[] A) {
//        int a, j;
//        for (int i = 1; i < A.length; i++) {
//            a = A[i];
//            for (j = i - 1; j >= 0 && A[j] > a; j--) {
//                A[j + 1] = A[j];
//            }
//            A[j + 1] = a;
//        }
//    }

    /**
     * Sort the array of comparables within the given range of elements.
     * Uses a simple insertion sort algorithm, so expect O(n^2) running
     * time.
     *
     * @param  <T>   type of comparable to be sorted.
     * @param  arr   comparables to be sorted.
     * @param  low   low end of range to sort.
     * @param  high  high end of range to sort.
     */
    public static <T extends Comparable<? super T>> void sort(T[] arr, int low, int high) {
        for (int i = low + 1; i <= high; i++) {
            T tmp = arr[i];
            int j = i;
            while (j > low && tmp.compareTo(arr[j - 1]) < 0) {
                arr[j] = arr[j - 1];
                j--;
            }
            arr[j] = tmp;
        }
    }

    /**
     * Sort the strings in the array using an insertion sort, but only
     * consider the characters in the strings starting from the given
     * offset <em>d</em>. That is, the method will ignore all characters
     * appearing before the <em>d</em>th character.
     *
     * @param  strings  array of strings to sort.
     * @param  low      low offset into the array (inclusive).
     * @param  high     high offset into the array (exclusive).
     * @param  depth    offset of first character in each string to compare.
     */
    public static void sort(CharSequence[] strings, int low, int high, int depth) {
        if (strings == null) {
            throw new IllegalArgumentException("strings must be non-null");
        }
        if (low < 0 || high < low || depth < 0) {
            throw new IllegalArgumentException("indices out of bounds");
        }
        for (int i = low + 1; i < high; i++) {
            for (int j = i; j > low; j--) {
                int idx = depth;
                char s = idx < strings[j - 1].length() ? strings[j - 1].charAt(idx) : 0;
                char t = idx < strings[j].length() ? strings[j].charAt(idx) : 0;
                while (s == t && idx < strings[j - 1].length()) {
                    idx++;
                    s = idx < strings[j - 1].length() ? strings[j - 1].charAt(idx) : 0;
                    t = idx < strings[j].length() ? strings[j].charAt(idx) : 0;
                }
                if (s <= t) {
                    break;
                }
                CharSequence tmp = strings[j];
                strings[j] = strings[j - 1];
                strings[j - 1] = tmp;
            }
        }
    }
}
