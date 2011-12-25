/*
 * Copyright 2011 Nathan Fiedler. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package org.burstsort4j;

/**
 * Implementation of the introspective sort algorithm, developed by
 * David Musser; implementation copied from the paper on introsort
 * by Ralph Unden, with some modifications.
 *
 * @author Nathan Fiedler
 */
public class Introsort {

    /** As with typical quicksort implementations, delegate to insertion
     * sort for ranges of size below 16. */
    private static final int THRESHOLD = 16;

    private Introsort() {
    }

    /**
     * Sort the array of comparables. Uses an introspective sort
     * algorithm, so expect O(log(n)) running time.
     *
     * @param  <T>   type of comparable to be sorted.
     * @param  arr   comparables to be sorted.
     */
    public static <T extends Comparable<? super T>> void sort(T[] arr) {
        if (arr != null && arr.length > 1) {
            int floor = (int) (Math.floor(Math.log(arr.length) / Math.log(2)));
            introsort_loop(0, arr.length, 2 * floor, arr);
            insertionsort(0, arr.length, arr);
        }
    }

    /**
     * Sort the array of comparables within the given range of elements.
     * Uses an introspective sort algorithm, so expect O(log(n)) running
     * time.
     *
     * @param  <T>   type of comparable to be sorted.
     * @param  arr   comparables to be sorted.
     * @param  low   low end of range to sort (inclusive).
     * @param  high  high end of range to sort (inclusive).
     */
    public static <T extends Comparable<? super T>> void sort(T[] arr, int low, int high) {
        if (arr != null && arr.length > 1 && low >= 0 && low < high) {
            int floor = (int) (Math.floor(Math.log(high - low) / Math.log(2)));
            introsort_loop(low, high, 2 * floor, arr);
            insertionsort(low, high, arr);
        }
    }

    /**
     * A modified quicksort that delegates to heapsort when the depth
     * limit has been reached. Does not sort the array if the range is
     * below the threshold.
     *
     * @param  <T>          type of comparable to be sorted.
     * @param  arr          comparables to be sorted.
     * @param  low          low end of range to sort (inclusive).
     * @param  high         high end of range to sort (inclusive).
     * @param  depth_limit  if zero, will delegate to heapsort.
     */
    private static <T extends Comparable<? super T>> void introsort_loop(
            int low, int high, int depth_limit, T[] arr) {
        while (high - low > THRESHOLD) {
            if (depth_limit == 0) {
                // perform a basic heap sort
                int n = high - low;
                for (int i = n / 2; i >= 1; i--) {
                    T d = arr[low + i - 1];
                    int j = i;
                    while (j <= n / 2) {
                        int child = 2 * j;
                        if (child < n && arr[low + child - 1].compareTo(arr[low + child]) < 0) {
                            child++;
                        }
                        if (d.compareTo(arr[low + child - 1]) >= 0) {
                            break;
                        }
                        arr[low + j - 1] = arr[low + child - 1];
                        j = child;
                    }
                    arr[low + j - 1] = d;
                }
                for (int i = n; i > 1; i--) {
                    T t = arr[low];
                    arr[low] = arr[low + i - 1];
                    arr[low + i - 1] = t;
                    T d = arr[low + i - 1];
                    int j = 1;
                    int m = i - 1;
                    while (j <= m / 2) {
                        int child = 2 * j;
                        if (child < m && arr[low + child - 1].compareTo(arr[low + child]) < 0) {
                            child++;
                        }
                        if (d.compareTo(arr[low + child - 1]) >= 0) {
                            break;
                        }
                        arr[low + j - 1] = arr[low + child - 1];
                        j = child;
                    }
                    arr[low + j - 1] = d;
                }
                return;
            }
            depth_limit--;
            int p = partition(low, high, medianOf3(low, low + ((high - low) / 2) + 1, high - 1, arr), arr);
            introsort_loop(p, high, depth_limit, arr);
            high = p;
        }
    }

    /**
     * Partitions the elements in the given range such that elements
     * less than the pivot appear before those greater than the pivot.
     * 
     * @param  <T>   type of comparable to be sorted.
     * @param  low   low end of range to sort (inclusive).
     * @param  high  high end of range to sort (inclusive).
     * @param  x     pivot to compare to.
     * @param  arr   comparables to be sorted.
     * @return  midpoint of partitioned values.
     */
    private static <T extends Comparable<? super T>> int partition(int low, int high, T x, T[] arr) {
        int i = low;
        int j = high;
        while (true) {
            while (arr[i].compareTo(x) < 0) {
                i++;
            }
            j--;
            while (x.compareTo(arr[j]) < 0) {
                j--;
            }
            if (i >= j) {
                return i;
            }
            T t = arr[i];
            arr[i] = arr[j];
            arr[j] = t;
            i++;
        }
    }

    /**
     * Finds the median of three element in the given range.
     *
     * @param  <T>   type of comparable to be sorted.
     * @param  low   low end of range to sort (inclusive).
     * @param  mid   midpoint of the range.
     * @param  high  high end of range to sort (inclusive).
     * @param  arr   comparables to be sorted.
     * @return  the median of three element.
     */
    private static <T extends Comparable<? super T>> T medianOf3(int low, int mid, int high, T[] arr) {
        if (arr[mid].compareTo(arr[low]) < 0) {
            if (arr[high].compareTo(arr[mid]) < 0) {
                return arr[mid];
            } else {
                if (arr[high].compareTo(arr[low]) < 0) {
                    return arr[high];
                } else {
                    return arr[low];
                }
            }
        } else {
            if (arr[high].compareTo(arr[mid]) < 0) {
                if (arr[high].compareTo(arr[low]) < 0) {
                    return arr[low];
                } else {
                    return arr[high];
                }
            } else {
                return arr[mid];
            }
        }
    }

    /**
     * A simple insertion sort that operates on the given range.
     * 
     * @param  <T>   type of comparable to be sorted.
     * @param  low   low end of range to heapify (inclusive).
     * @param  high  high end of range to sort (inclusive).
     * @param  arr   comparables to be sorted.
     */
    private static <T extends Comparable<? super T>> void insertionsort(int low, int high, T[] arr) {
        for (int i = low; i < high; i++) {
            int j = i;
            T t = arr[i];
            while (j != low && t.compareTo(arr[j - 1]) < 0) {
                arr[j] = arr[j - 1];
                j--;
            }
            arr[j] = t;
        }
    }
}
