/*
 * Copyright 2009-2011 Nathan Fiedler. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package org.burstsort4j.benchmark;

/**
 * Size of the data sets used in testing sort performance.
 * For the median-of-three killer generator to work, the
 * sizes must be divisible by four.
 *
 * @author Nathan Fiedler
 */
public enum DataSize {

    N_12 {

        @Override
        public int getValue() {
            return 12;
        }
    },
    N_20 {

        @Override
        public int getValue() {
            return 20;
        }
    },
    N_52 {

        @Override
        public int getValue() {
            return 52;
        }
    },
    N_100 {

        @Override
        public int getValue() {
            return 100;
        }
    },
    N_400 {

        @Override
        public int getValue() {
            return 400;
        }
    },
    N_800 {

        @Override
        public int getValue() {
            return 800;
        }
    },
    SMALL {

        @Override
        public int getValue() {
            return 333000;
        }
    },
    MEDIUM {

        @Override
        public int getValue() {
            return 1000000;
        }
    },
    LARGE {

        @Override
        public int getValue() {
            return 3000000;
        }
    };

    /**
     * Returns the quantity for this data size.
     *
     * @return  quantity.
     */
    public abstract int getValue();
};
