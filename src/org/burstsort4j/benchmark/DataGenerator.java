/*
 * Copyright 2009-2011 Nathan Fiedler. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package org.burstsort4j.benchmark;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.burstsort4j.DualPivotQuicksort;

/**
 * Creates a set of data to be sorted.
 *
 * @author Nathan Fiedler
 */
public enum DataGenerator {

    /**
     * Generates a set of pseudo words, comprised of at least one letter,
     * up to the length of the longest (real) English word, using only
     * the lower-case letters.
     */
    PSEUDO_WORD {

        /** Longest (real) word in English: antidisestablishmentarianism */
        private static final int LONGEST = 28;
        /** Letters in the English alphabet (lower case only) */
        private static final int ALPHABET = 26;

        @Override
        public String[] generate(DataSize size) {
            Random r = new Random();
            String[] list = new String[size.getValue()];
            StringBuilder sb = new StringBuilder();
            for (int ii = 0; ii < list.length; ii++) {
                int length = r.nextInt(LONGEST) + 1;
                for (int jj = 0; jj < length; jj++) {
                    int d = r.nextInt(ALPHABET);
                    sb.append((char) ('a' + d));
                }
                list[ii] = sb.toString();
                sb.setLength(0);
            }
            return list;
        }

        @Override
        public String getDisplayName() {
            return "Pseudo words FL28 C26";
        }
    },
    /**
     * Generates strings of a fixed length, comprised of randomly selected
     * characters from the printable ASCII set (from 32 to 126).
     */
    RANDOM {

        /** Size of the randomly generated strings. */
        private static final int LENGTH = 100;
        /** All printable characters in US-ASCII. */
        private static final int ALPHABET = 95;

        @Override
        public String[] generate(DataSize size) {
            Random r = new Random();
            String[] list = new String[size.getValue()];
            StringBuilder sb = new StringBuilder();
            for (int ii = 0; ii < list.length; ii++) {
                for (int jj = 0; jj < LENGTH; jj++) {
                    int d = r.nextInt(ALPHABET);
                    sb.append((char) (' ' + d));
                }
                list[ii] = sb.toString();
                sb.setLength(0);
            }
            return list;
        }

        @Override
        public String getDisplayName() {
            return "Random FL100 C95";
        }
    },
    /**
     * Generates a set of duplicate strings, comprised of an alphabet
     * of size one, where each string is 100 characters. One of three
     * pathological cases created to stress test the sort.
     */
    REPEAT {

        @Override
        public String[] generate(DataSize size) {
            int count = size.getValue();
            List<String> list = Collections.nCopies(count,
                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                    + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
            return list.toArray(new String[count]);
        }

        @Override
        public String getDisplayName() {
            return "Repeated 100-A";
        }
    },
    /**
     * Generates a set of strings, comprised of an alphabet of size one,
     * where length increases from one to 100 characters in a cycle.
     * One of three pathological cases created to stress test the sort.
     */
    REPEAT_CYCLE {

        @Override
        public String[] generate(DataSize size) {
            String[] strs = new String[Math.min(size.getValue() / 4, 100)];
            String seed = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                    + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
            for (int i = 0; i < strs.length; i++) {
                strs[i] = seed.substring(0, i + 1);
            }
            String[] list = new String[size.getValue()];
            int c = 0;
            for (int ii = 0; ii < list.length; ii++) {
                list[ii] = strs[c];
                c++;
                if (c >= strs.length) {
                    c = 0;
                }
            }
            return list;
        }

        @Override
        public String getDisplayName() {
            return "Repeated 100-A decreasing cycle";
        }
    },
    /**
     * Generates a set of strings, comprised of one to 100 characters,
     * from an alphabet consisting of nine letters. One of three
     * pathological cases to stress test the sort.
     */
    SMALL_ALPHABET {

        /** Longest string to be created. */
        private static final int LONGEST = 100;
        /** Small alphabet size. */
        private static final int ALPHABET = 9;

        @Override
        public String[] generate(DataSize size) {
            Random r = new Random();
            String[] list = new String[size.getValue()];
            StringBuilder sb = new StringBuilder();
            for (int ii = 0; ii < list.length; ii++) {
                int length = r.nextInt(LONGEST) + 1;
                for (int jj = 0; jj < length; jj++) {
                    int d = r.nextInt(ALPHABET);
                    sb.append((char) ('a' + d));
                }
                list[ii] = sb.toString();
                sb.setLength(0);
            }
            return list;
        }

        @Override
        public String getDisplayName() {
            return "Small Alphabet FL100 C9";
        }
    },
    /**
     * Generates strings of a fixed length, comprised of randomly selected
     * characters from the genome alphabet.
     */
    GENOME {

        /** Size of the randomly generated strings. */
        private static final int LENGTH = 9;
        /** Size of the genome alphabet (a, c, g, t). */
        private static final int ALPHABET = 4;

        @Override
        public String[] generate(DataSize size) {
            Random r = new Random();
            String[] list = new String[size.getValue()];
            StringBuilder sb = new StringBuilder();
            for (int ii = 0; ii < list.length; ii++) {
                for (int jj = 0; jj < LENGTH; jj++) {
                    switch (r.nextInt(ALPHABET)) {
                        case 0:
                            sb.append('a');
                            break;
                        case 1:
                            sb.append('c');
                            break;
                        case 2:
                            sb.append('g');
                            break;
                        case 3:
                            sb.append('t');
                            break;
                    }
                }
                list[ii] = sb.toString();
                sb.setLength(0);
            }
            return list;
        }

        @Override
        public String getDisplayName() {
            return "Genome FL9 C4";
        }
    },
    /**
     * Generates a data set that would normally cause worst-case behavior
     * for a sorting algorithm such as quicksort, otherwise known as a
     * "median of 3 killer".
     */
    MEDIAN_OF_3_KILLER {

        @Override
        public String[] generate(DataSize size) {
            if (((size.getValue() / 2) % 2) == 1) {
                throw new IllegalArgumentException(
                        "cannot generate median-of-3 killer with given size");
            }
            // Generate a random data set and then sort it so we can
            // pluck values from it to generate our killer data set.
            String[] data = PSEUDO_WORD.generate(size);
            DualPivotQuicksort.sort(data);
            final int k = data.length / 2;
            String[] list = new String[data.length];
            for (int ii = 1; ii <= k; ii++) {
                if ((ii % 2) == 1) {
                    list[ii - 1] = data[ii - 1];
                    list[ii] = data[k + ii - 1];
                }
                list[k + ii - 1] = data[2 * ii - 1];
            }
            return list;
        }

        @Override
        public String getDisplayName() {
            return "Median3Killer";
        }
    },
    /**
     * A "generator" that reads data from a named file, returning
     * a particular number of lines based on the requested size.
     * The file must have sufficient data or an error occurs.
     */
    FILE {

        /** File from whence data is to be read. */
        private File file;

        @Override
        public void setFile(File file) {
            // Yeah, this is basically a static field, but I don't care.
            this.file = file;
        }

        @Override
        public String[] generate(DataSize size) {
            int count = size.getValue();
            String[] data = new String[count];
            try {
                FileReader fr = new FileReader(file);
                BufferedReader br = new BufferedReader(fr);
                String line = br.readLine();
                for (int ii = 0; count > 0; ii++, count--) {
                    if (line == null) {
                        break;
                    }
                    data[ii] = line;
                    line = br.readLine();
                }
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
            if (count > 0) {
                throw new RuntimeException(String.format(
                        "File '%s' has too few lines (%d more needed)",
                        file.getName(), count));
            }
            return data;
        }

        @Override
        public String getDisplayName() {
            return file.getName();
        }
    };

    /**
     * Generate data for testing the sort implementations.
     *
     * @param  size  size of the data to be generated.
     * @return  array of test data.
     */
    public abstract String[] generate(DataSize size);

    /**
     * Returns the display name for this generator.
     *
     * @return  display name.
     */
    public abstract String getDisplayName();

    /**
     * For certain data generators, a file is needed to produce the data.
     *
     * @param  file  that which contains test data.
     */
    public void setFile(File file) {
    }
}
