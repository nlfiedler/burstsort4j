/*
 * Copyright (C) 2010  Nathan Fiedler
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
 * $Id: $
 */
package org.burstsort4j;

/**
 * Implementation of binary insertion sort algorithm borrowed from tim sort,
 * with some minor modifications.
 *
 * @author Nathan Fiedler
 */
public class BinaryInsertionsort {

    private BinaryInsertionsort() {
    }

    /**
     * Sorts the specified portion of the array using a binary insertion sort.
     * This is the best method for sorting small numbers of elements.
     * It requires O(n log n) compares, but O(n^2) data movement (worst case).
     *
     * @param  <T>   type of comparable to be sorted.
     * @param  arr   the array in which a range is to be sorted.
     * @param  low   the index of the first element in the range to be sorted.
     * @param  high  the index of the last element in the range to be sorted.
     */
    @SuppressWarnings("fallthrough")
    public static <T extends Comparable<? super T>> void sort(
            T[] arr, int low, int high) {
        if (arr == null || arr.length < 2 || low < 0 || high <= low) {
            return;
        }
        for (int ii = low; ii <= high; ii++) {
            T pivot = arr[ii];

            // Set left (and right) to the index where a[start] (pivot) belongs
            int left = low;
            int right = ii;
            // Invariants:
            //   pivot >= all in [lo, left).
            //   pivot <  all in [right, start).
            while (left < right) {
                int mid = (left + right) >>> 1;
                if (pivot.compareTo(arr[mid]) < 0) {
                    right = mid;
                } else {
                    left = mid + 1;
                }
            }

            // The invariants above still hold, so pivot belongs at left.
            // Note that if there are elements equal to pivot, left points
            // to the first slot after them -- that's why this sort is stable.
            // Slide elements over to make room to make room for pivot.
            int count = ii - left;
            // Switch is just an optimization for arraycopy in default case.
            switch (count) {
                case 2:
                    arr[left + 2] = arr[left + 1];
                case 1:
                    arr[left + 1] = arr[left];
                    break;
                default:
                    System.arraycopy(arr, left, arr, left + 1, count);
            }
            arr[left] = pivot;
        }
    }

    /**
     * Sorts the specified portion of the array using a binary insertion sort.
     * This is the best method for sorting small numbers of elements.
     * It requires O(n log n) compares, but O(n^2) data movement (worst case).
     *
     * @param  arr    the array in which a range is to be sorted.
     * @param  low    low offset into the array (inclusive).
     * @param  high   high offset into the array (exclusive).
     * @param  depth  offset of first character in each string to compare.
     */
    @SuppressWarnings("fallthrough")
    public static void sort(CharSequence[] arr, int low, int high, int depth) {
        if (arr == null || arr.length < 2 || low < 0 || high <= low || depth < 0) {
            return;
        }
        for (int ii = low; ii < high; ii++) {
            CharSequence pivot = arr[ii];

            // Set left (and right) to the index where a[start] (pivot) belongs
            int left = low;
            int right = ii;
            // Invariants:
            //   pivot >= all in [lo, left).
            //   pivot <  all in [right, start).
            while (left < right) {
                int mid = (left + right) >>> 1;
                if (compare(pivot, arr[mid], depth) < 0) {
                    right = mid;
                } else {
                    left = mid + 1;
                }
            }

            // The invariants above still hold, so pivot belongs at left.
            // Note that if there are elements equal to pivot, left points
            // to the first slot after them -- that's why this sort is stable.
            // Slide elements over to make room to make room for pivot.
            int count = ii - left;
            // Switch is just an optimization for arraycopy in default case.
            switch (count) {
                case 2:
                    arr[left + 2] = arr[left + 1];
                case 1:
                    arr[left + 1] = arr[left];
                    break;
                default:
                    System.arraycopy(arr, left, arr, left + 1, count);
            }
            arr[left] = pivot;
        }
    }

    /**
     * Compare two character sequences, starting with the characters at
     * offset <code>depth</code> (assumes the leading characters are the
     * same in both sequences).
     *
     * @param  a      first character sequence to compare.
     * @param  b      second character sequence to compare.
     * @param  depth  offset of first character in each string to compare.
     * @return  a negative integer, zero, or a positive integer as the first
     *          argument is less than, equal to, or greater than the second.
     */
    private static int compare(CharSequence a, CharSequence b, int depth) {
        int idx = depth;
        char s = idx < a.length() ? a.charAt(idx) : 0;
        char t = idx < b.length() ? b.charAt(idx) : 0;
        while (s == t && idx < a.length()) {
            idx++;
            s = idx < a.length() ? a.charAt(idx) : 0;
            t = idx < b.length() ? b.charAt(idx) : 0;
        }
        return s - t;
    }
}
