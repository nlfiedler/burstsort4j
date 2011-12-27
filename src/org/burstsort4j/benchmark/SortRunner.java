/*
 * Copyright 2009-2011 Nathan Fiedler. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package org.burstsort4j.benchmark;

import java.util.Arrays;
import org.burstsort4j.BinaryInsertionsort;
import org.burstsort4j.Burstsort;
import org.burstsort4j.Combsort;
import org.burstsort4j.DualPivotQuicksort;
import org.burstsort4j.Gnomesort;
import org.burstsort4j.Heapsort;
import org.burstsort4j.HybridCombsort;
import org.burstsort4j.Insertionsort;
import org.burstsort4j.Introsort;
import org.burstsort4j.LazyFunnelsort;
import org.burstsort4j.MultikeyQuicksort;
import org.burstsort4j.Quicksort;
import org.burstsort4j.RedesignedBurstsort;
import org.burstsort4j.Selectionsort;
import org.burstsort4j.Shellsort;

/**
 * Runs a particular sort implementation.
 *
 * @author Nathan Fiedler
 */
public enum SortRunner {

    COMB {

        @Override
        public String getDisplayName() {
            return "Combsort";
        }

        @Override
        public void sort(String[] data) {
            Combsort.sort(data);
        }
    },
    HYBRID_COMB {

        @Override
        public String getDisplayName() {
            return "HybridComb";
        }

        @Override
        public void sort(String[] data) {
            HybridCombsort.sort(data);
        }
    },
    GNOME {

        @Override
        public String getDisplayName() {
            return "Gnomesort";
        }

        @Override
        public void sort(String[] data) {
            Gnomesort.sort(data);
        }
    },
    HEAP {

        @Override
        public String getDisplayName() {
            return "Heapsort";
        }

        @Override
        public void sort(String[] data) {
            Heapsort.sort(data);
        }
    },
    INSERTION {

        @Override
        public String getDisplayName() {
            return "Insertionsort";
        }

        @Override
        public void sort(String[] data) {
            Insertionsort.sort(data);
        }
    },
    BINARY_INSERTION {

        @Override
        public String getDisplayName() {
            return "BinInsertionsort";
        }

        @Override
        public void sort(String[] data) {
            BinaryInsertionsort.sort(data);
        }
    },
    INTRO {

        @Override
        public String getDisplayName() {
            return "Introsort";
        }

        @Override
        public void sort(String[] data) {
            Introsort.sort(data);
        }
    },
    QUICK {

        @Override
        public String getDisplayName() {
            return "Quicksort";
        }

        @Override
        public void sort(String[] data) {
            Quicksort.sort(data);
        }
    },
    QUICK_2_PIVOT {

        @Override
        public String getDisplayName() {
            return "2PivotQk";
        }

        @Override
        public void sort(String[] data) {
            DualPivotQuicksort.sort(data);
        }
    },
    SELECTION {

        @Override
        public String getDisplayName() {
            return "Selectionsort";
        }

        @Override
        public void sort(String[] data) {
            Selectionsort.sort(data, 0, data.length - 1);
        }
    },
    SHELL {

        @Override
        public String getDisplayName() {
            return "Shellsort";
        }

        @Override
        public void sort(String[] data) {
            Shellsort.sort(data);
        }
    },
    BURST {

        @Override
        public String getDisplayName() {
            return "Burstsort";
        }

        @Override
        public void sort(String[] data) {
            Burstsort.sort(data);
        }
    },
    REDESIGNED_BURST {

        @Override
        public String getDisplayName() {
            return "BR-Burstsort";
        }

        @Override
        public void sort(String[] data) {
            RedesignedBurstsort.sort(data);
        }
    },
    BURST_THREADPOOL {

        @Override
        public String getDisplayName() {
            return "Burstsort|TP|";
        }

        @Override
        public void sort(String[] data) {
            try {
                Burstsort.sortThreadPool(data);
            } catch (InterruptedException ie) {
                throw new RuntimeException(ie);
            }
        }
    },
    REDESIGNED_BURST_THREADPOOL {

        @Override
        public String getDisplayName() {
            return "BR-Burstsort|TP|";
        }

        @Override
        public void sort(String[] data) {
            try {
                RedesignedBurstsort.sortThreadPool(data);
            } catch (InterruptedException ie) {
                throw new RuntimeException(ie);
            }
        }
    },
    LAZY_FUNNEL {

        @Override
        public String getDisplayName() {
            return "LazyFunnelsort";
        }

        @Override
        public void sort(String[] data) {
            LazyFunnelsort.sort(data);
        }
    },
    THREADED_LAZY_FUNNEL {

        @Override
        public String getDisplayName() {
            return "LazyFunnelsort|TP|";
        }

        @Override
        public void sort(String[] data) {
            LazyFunnelsort.sortThreaded(data);
        }
    },
    MERGE {

        @Override
        public String getDisplayName() {
            return "Mergesort";
        }

        @Override
        public void sort(String[] data) {
            // This uses a merge sort.
            Arrays.sort(data);
        }
    },
    MULTIKEY {

        @Override
        public String getDisplayName() {
            return "MultikeyQS";
        }

        @Override
        public void sort(String[] data) {
            MultikeyQuicksort.sort(data);
        }
    };

    /**
     * Returns the display name for this runner.
     *
     * @return  display name.
     */
    public abstract String getDisplayName();

    /**
     * Sort the given array of strings.
     *
     * @param  data  strings to be sorted.
     */
    public abstract void sort(String[] data);
}
