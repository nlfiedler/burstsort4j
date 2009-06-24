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
 * This is the traditional quicksort algorithm. It is comparison-based,
 * which for strings, is considerably less efficient than a radix-based
 * sort, and in particular, multikey quicksort.
 *
 * <p>If you are sorting arrays of primitives, consider using the
 * <code>java.util.Arrays</code> class instead, which uses additional
 * heuristics to improve performance.</p>
 *
 * @author Nathan Fiedler
 */
public class Quicksort {

    /**
     * Creates a new instance of Quicksort.
     */
    public Quicksort() {
    }

    /**
     * Sorts the given array of comparable objects using the standard
     * quicksort algorithm. This sort is <em>not</em> stable. The objects
     * are sorted in place with constant additional memory (not counting
     * the stack due to recursion).
     *
     * @param  arr  an array of Comparable items to sort.
     */
    public static void sort(Comparable[] arr) {
        if (arr != null && arr.length > 1) {
            sort(arr, 0, arr.length - 1);
        }
    }

    /**
     * Internal quicksort method that makes recursive calls.
     * Uses median-of-three partitioning and a cutoff at which
     * point insertion sort is used.
     *
     * @param  arr   an array of Comparable items.
     * @param  low   the left-most index of the subarray.
     * @param  high  the right-most index of the subarray.
     */
    @SuppressWarnings("unchecked")
    private static void sort(Comparable[] arr, int low, int high) {
        if (low + 7 > high) {
            // Insertion sort for small partitions.
            Insertionsort.sort(arr, low, high);
        } else {
            // Choose a partition element
            int middle = (low + high) / 2;
            // Order the low, middle, and high elements
            if (arr[middle].compareTo(arr[low]) < 0) {
                swap(arr, low, middle);
            }
            if (arr[high].compareTo(arr[low]) < 0) {
                swap(arr, low, high);
            }
            if (arr[high].compareTo(arr[middle]) < 0) {
                swap(arr, middle, high);
            }
            // Place pivot element at the high end in preparation
            // for the ensuing swapping of elements.
            swap(arr, middle, high - 1);
            Comparable pivot = arr[high - 1];

            // Order the elements such that those below the pivot
            // point appear earlier, and those higher than the pivot
            // appear later.
            int i = low;
            int j = high - 1;
            while (true) {
                while (arr[++i].compareTo(pivot) < 0) { }
                while (pivot.compareTo(arr[--j]) < 0) { }
                if (i >= j) {
                    break;
                }
                swap(arr, i, j);
            }

            // Restore pivot element to its rightful position.
            swap(arr, i, high - 1);
            // Sort low partition recursively.
            sort(arr, low, i - 1);
            // Sort high partition recursively.
            sort(arr, i + 1, high);
        }
    }

    /**
     * Method to swap to elements in an array.
     *
     * @param  a  an array of objects.
     * @param  x  the index of the first object.
     * @param  y  the index of the second object.
     */
    private static final void swap(Object[] a, int x, int y) {
        Object tmp = a[x];
        a[x] = a[y];
        a[y] = tmp;
    }
}
