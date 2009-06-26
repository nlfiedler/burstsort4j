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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Command-line interface to the various sort implementations.
 *
 * @author Nathan Fiedler
 */
public class Main {
    private static final String BURSTSORT = "--burstsort";
    private static final String MERGESORT = "--mergesort";
    private static final String QUICKSORT = "--quicksort";
    private static final String MULTIKEY = "--multikey";
    private static final String SHUFFLE = "--shuffle";
    /** String data to be sorted. */
    private List<String> data;

    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            printUsage();
            System.exit(1);
        }
        String input = null;
        String output = null;
        String sort = null;
        if (args.length == 3) {
            sort = args[0];
            input = args[1];
            output = args[2];
        } else {
            sort = BURSTSORT;
            input = args[0];
            output = args[1];
        }
        Main main = new Main();
        // Read in the input file.
        long r1 = System.currentTimeMillis();
        main.readFile(input);
        long r2 = System.currentTimeMillis();
        System.out.format("Read time: %dms\n", r2 - r1);

        if (sort.equals(MERGESORT)) {
            main.mergesort();
        } else if (sort.equals(BURSTSORT)) {
            main.burstsort();
        } else if (sort.equals(QUICKSORT)) {
            main.quicksort();
        } else if (sort.equals(MULTIKEY)) {
            main.multikey();
        } else if (sort.equals(SHUFFLE)) {
            main.shuffle();
        } else {
            System.err.println("Unrecognized sort option!");
            printUsage();
            System.exit(1);
        }

        // Write the results to the output file.
        long w1 = System.currentTimeMillis();
        main.writeFile(output);
        long w2 = System.currentTimeMillis();
        System.out.format("Write time: %dms\n", w2 - w1);
    }

    private static void printUsage() {
        System.out.format("Usage: java Main [%s|%s|%s|%s|%s] <input> <output>\n",
                BURSTSORT, MERGESORT, QUICKSORT, MULTIKEY, SHUFFLE);
        System.out.println("\tWithout a specified sort, default is burstsort.");
        System.out.println("\t--shuffle will in fact randomly shuffle the input file.");
        System.out.println("\t<input> is the name of the (unsorted) input file.");
        System.out.println("\t<output> is the name for the (sorted) output file.");
        System.out.println("\t(Note: all sort/shuffle actions are line-oriented.)");
    }

    private void mergesort() {
        long s1 = System.currentTimeMillis();
        Collections.sort(data);
        long s2 = System.currentTimeMillis();
        System.out.format("Sort time: %dms\n", s2 - s1);
    }

    private void multikey() {
        String[] arr = data.toArray(new String[data.size()]);
        long s1 = System.currentTimeMillis();
        // The median-of-three multikey quicksort is typically faster
        // than the standard version.
        MultikeyQuicksort.sort(arr);
        long s2 = System.currentTimeMillis();
        System.out.format("Sort time: %dms\n", s2 - s1);
        data.clear();
        for (String a : arr) {
            data.add(a);
        }
    }

    private void burstsort() {
        String[] arr = data.toArray(new String[data.size()]);
        long s1 = System.currentTimeMillis();
        Burstsort.sort(arr);
        long s2 = System.currentTimeMillis();
        System.out.format("Sort time: %dms\n", s2 - s1);
        data.clear();
        for (String a : arr) {
            data.add(a);
        }
    }

    private void quicksort() {
        String[] arr = data.toArray(new String[data.size()]);
        long s1 = System.currentTimeMillis();
        Quicksort.sort(arr);
        long s2 = System.currentTimeMillis();
        System.out.format("Sort time: %dms\n", s2 - s1);
        data.clear();
        for (String a : arr) {
            data.add(a);
        }
    }

    private void shuffle() {
        long s1 = System.currentTimeMillis();
        Collections.shuffle(data);
        long s2 = System.currentTimeMillis();
        System.out.format("Shuffle time: %dms\n", s2 - s1);
    }

    private void readFile(String name) {
        data = new ArrayList<String>();
        try {
            FileReader fr = new FileReader(name);
            BufferedReader br = new BufferedReader(fr);
            String line = br.readLine();
            while (line != null) {
                data.add(line);
                line = br.readLine();
            }
            br.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void writeFile(String name) {
        try {
            FileWriter fw = new FileWriter(name);
            BufferedWriter bw = new BufferedWriter(fw);
            for (String output : data) {
                bw.write(output);
                bw.newLine();
            }
            bw.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
