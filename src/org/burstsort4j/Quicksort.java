/*
 * Copyright 2008-2011 Nathan Fiedler. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
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

    /** As with GCC std::sort delegate to insertion sort for ranges of
     * size below 16. */
    private static final int THRESHOLD = 16;

    /**
     * Creates a new instance of Quicksort.
     */
    private Quicksort() {
    }

    /**
     * Sorts the given array of comparable objects using the standard
     * quicksort algorithm. This sort is <em>not</em> stable. The objects
     * are sorted in place with constant additional memory (not counting
     * the stack due to recursion).
     *
     * @param  <T>  type of comparable to be sorted.
     * @param  arr  an array of Comparable items to sort.
     */
    public static <T extends Comparable<? super T>> void sort(T[] arr) {
        if (arr != null && arr.length > 1) {
            sort(arr, 0, arr.length - 1);
        }
    }

    /**
     * Basic implementation of quicksort. Uses median-of-three partitioning
     * and a cutoff at which point insertion sort is used.
     *
     * @param  <T>   type of comparable to be sorted.
     * @param  arr   an array of Comparable items.
     * @param  low   the left-most index of the subarray.
     * @param  high  the right-most index of the subarray.
     */
    public static <T extends Comparable<? super T>> void sort(T[] arr, int low, int high) {
        if (low + THRESHOLD > high) {
            // Insertion sort for small partitions.
            BinaryInsertionsort.sort(arr, low, high);
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
            T pivot = arr[high - 1];

            // Order the elements such that those below the pivot
            // point appear earlier, and those higher than the pivot
            // appear later.
            int i = low;
            int j = high - 1;
            while (true) {
                while (arr[++i].compareTo(pivot) < 0) {
                }
                while (pivot.compareTo(arr[--j]) < 0) {
                }
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
    private static void swap(Object[] a, int x, int y) {
        Object tmp = a[x];
        a[x] = a[y];
        a[y] = tmp;
    }
}
