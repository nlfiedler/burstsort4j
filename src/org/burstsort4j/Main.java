/*
 * Copyright 2008-2011 Nathan Fiedler. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package org.burstsort4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 * Command-line interface to the various sort implementations.
 *
 * @author Nathan Fiedler
 */
public class Main {

    private static final String BURSTSORT = "--burstsort";
    private static final String FUNNELSORT = "--funnelsort";
    private static final String MULTIKEY = "--multikey";

    private Main() {
    }

    /**
     * Main entry point for the sorter.
     *
     * @param  args  command line arguments.
     */
    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            usage();
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

        // Read in the input file.
        long r1 = System.currentTimeMillis();
        String[] data = readFile(input);
        long r2 = System.currentTimeMillis();
        System.out.format("Read time: %dms\n", r2 - r1);

        // Sort the data using the selected sort.
        long s1 = System.currentTimeMillis();
        if (sort.equals(FUNNELSORT)) {
            LazyFunnelsort.sort(data);
        } else if (sort.equals(BURSTSORT)) {
            Burstsort.sort(data);
        } else if (sort.equals(MULTIKEY)) {
            MultikeyQuicksort.sort(data);
        } else {
            usage();
        }
        long s2 = System.currentTimeMillis();
        System.out.format("Sort time: %dms\n", s2 - s1);

        // Write the results to the output file.
        long w1 = System.currentTimeMillis();
        writeFile(output, data);
        long w2 = System.currentTimeMillis();
        System.out.format("Write time: %dms\n", w2 - w1);
    }

    /**
     * Display usage information and exit.
     */
    private static void usage() {
        System.out.println("Usage: Main [options] <input> <output>\n");
        System.out.println("\t<input> is the name of the (unsorted) input file.");
        System.out.println("\t<output> is the name for the (sorted) output file.");
        System.out.println("\tGzip compression will be used on any file having a \".gz\" suffix\n");
        System.out.println("\t--burstsort");
        System.out.println("\t\tSort using the original Burstsort algorithm.");
        System.out.println("\t\tThis is the default sort if none is specified.\n");
        System.out.println("\t--funnelsort");
        System.out.println("\t\tSort using the Lazy Funnelsort algorithm.\n");
        System.out.println("\t--multikey");
        System.out.println("\t\tSort using the Multikey Quicksort algorithm.");
        System.exit(0);
    }

    /**
     * Reads the contents of the named file into an array of strings.
     *
     * @param  name  file to be read.
     * @return  the lines of text from the file.
     */
    private static String[] readFile(String name) {
        List<String> data = new ArrayList<String>();
        try {
            Reader fr = null;
            if (name.toLowerCase().endsWith(".gz")) {
              FileInputStream fis = new FileInputStream(name);
              GZIPInputStream gis = new GZIPInputStream(fis);
              fr = new InputStreamReader(gis);
            } else {
              fr = new FileReader(name);
            }
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
        return data.toArray(new String[data.size()]);
    }

    /**
     * Write the given array of strings to the named file.
     *
     * @param  name  file name to write to.
     * @param  data  the strings to be written.
     */
    private static void writeFile(String name, String[] data) {
        try {
            Writer fw = null;
            if (name.toLowerCase().endsWith(".gz")) {
              FileOutputStream fos = new FileOutputStream(name);
              GZIPOutputStream gos = new GZIPOutputStream(fos);
              fw = new OutputStreamWriter(gos);
            } else {
              fw = new FileWriter(name);
            }
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
