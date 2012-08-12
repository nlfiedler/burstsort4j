/*
 * Copyright 2011-2012 Nathan Fiedler. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package org.burstsort4j;

/**
 * DualPivotQuickSort will sort the given slice of strings using the
 * two pivot value quicksort variation by Vladimir Yaroslavskiy. This
 * version if a translation of the Go version, which itself is a
 * translation of a Java version written by Yaroslavskiy.
 */
public class DualPivotQuicksort {

    private DualPivotQuicksort() {
    }

    /**
     * Sorts the given array of comparable objects using the dual-pivot
     * quicksort algorithm.
     *
     * @param  arr  an array of Comparable items to sort.
     */
    public static void sort(String[] arr) {
        if (arr != null && arr.length > 1) {
            sort(arr, 0, arr.length - 1);
        }
    }

    /**
     * Sorts the given array of comparable objects using the dual-pivot
     * quicksort algorithm within the given range of the array.
     *
     * @param  <T>    type of comparable to be sorted.
     * @param  arr    an array of Comparable items to sort.
     * @param  left   lower bound of range to be sorted.
     * @param  right  upper bound of range to be sorted.
     */
    private static void sort(String[] arr, int left, int right) {
        int len = right - left;

        // perform insertion sort on small ranges
        if (len < 17) {
            for (int i = left + 1; i <= right; i++) {
                for (int j = i; j > left && arr[j].compareTo(arr[j - 1]) < 0; j--) {
                    String t = arr[j - 1];
                    arr[j - 1] = arr[j];
                    arr[j] = t;
                }
            }
            return;
        }

        // compute indices of medians
        int sixth = len / 6;
        int m1 = left + sixth;
        int m2 = m1 + sixth;
        int m3 = m2 + sixth;
        int m4 = m3 + sixth;
        int m5 = m4 + sixth;

        // order the medians in preparation for partitioning
        if (arr[m1].compareTo(arr[m2]) > 0) {
            String t = arr[m1];
            arr[m1] = arr[m2];
            arr[m2] = t;
        }
        if (arr[m4].compareTo(arr[m5]) > 0) {
            String t = arr[m4];
            arr[m4] = arr[m5];
            arr[m4] = t;
        }
        if (arr[m1].compareTo(arr[m3]) > 0) {
            String t = arr[m1];
            arr[m1] = arr[m3];
            arr[m3] = t;
        }
        if (arr[m2].compareTo(arr[m3]) > 0) {
            String t = arr[m2];
            arr[m2] = arr[m3];
            arr[m3] = t;
        }
        if (arr[m1].compareTo(arr[m4]) > 0) {
            String t = arr[m1];
            arr[m1] = arr[m4];
            arr[m4] = t;
        }
        if (arr[m3].compareTo(arr[m4]) > 0) {
            String t = arr[m3];
            arr[m3] = arr[m4];
            arr[m4] = t;
        }
        if (arr[m2].compareTo(arr[m5]) > 0) {
            String t = arr[m2];
            arr[m2] = arr[m5];
            arr[m5] = t;
        }
        if (arr[m2].compareTo(arr[m3]) > 0) {
            String t = arr[m2];
            arr[m2] = arr[m3];
            arr[m3] = t;
        }
        if (arr[m4].compareTo(arr[m5]) > 0) {
            String t = arr[m4];
            arr[m4] = arr[m5];
            arr[m5] = t;
        }

        // select the pivots such that [ < pivot1 | pivot1 <= && <= pivot2 | > pivot2 ]
        String pivot1 = arr[m2];
        String pivot2 = arr[m4];

        boolean diffPivots = !pivot1.equals(pivot2);

        // move the pivots out of the away
        arr[m2] = arr[left];
        arr[m4] = arr[right];
        int less = left + 1;
        int more = right - 1;

        // partition the elements
        if (diffPivots) {
            for (int k = less; k <= more; k++) {
                String x = arr[k];
                if (x.compareTo(pivot2) > 0) {
                    while (arr[more].compareTo(pivot2) > 0 && k < more) {
                        more--;
                    }
                    arr[k] = arr[more];
                    arr[more] = x;
                    more--;
                    x = arr[k];
                }
                if (x.compareTo(pivot1) < 0) {
                    arr[k] = arr[less];
                    arr[less] = x;
                    less++;
                }
            }
        } else {
            for (int k = less; k <= more; k++) {
                String x = arr[k];
                if (x.equals(pivot1)) {
                    continue;
                }
                if (x.compareTo(pivot1) > 0) {
                    while (arr[more].compareTo(pivot2) > 0 && k < more) {
                        more--;
                    }
                    arr[k] = arr[more];
                    arr[more] = x;
                    more--;
                    x = arr[k];
                }
                if (x.compareTo(pivot1) < 0) {
                    arr[k] = arr[less];
                    arr[less] = x;
                    less++;
                }
            }
        }

        // swap the pivots back into position
        arr[left] = arr[less - 1];
        arr[less - 1] = pivot1;
        arr[right] = arr[more + 1];
        arr[more + 1] = pivot2;

        // recursively sort the left and right partitions
        sort(arr, left, less - 2);
        sort(arr, more + 2, right);

        // order the equal elements in the middle
        if (more - less > len - 13 && diffPivots) {
            for (int k = less; k <= more; k++) {
                String x = arr[k];
                if (x.equals(pivot2)) {
                    arr[k] = arr[more];
                    arr[more] = x;
                    more--;
                    x = arr[k];
                }
                if (x.equals(pivot1)) {
                    arr[k] = arr[less];
                    arr[less] = x;
                    less++;
                }
            }
        }

        // recursively sort the middle partition
        if (diffPivots) {
            sort(arr, less, more);
        }
    }
}
