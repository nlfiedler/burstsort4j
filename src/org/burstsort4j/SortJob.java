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

import java.util.concurrent.Callable;

/**
 * A sort job to be completed after the trie traversal phase. Each job
 * is given a single bucket to be a processed. A sort job first sorts the
 * the string "tails" and then copies the references to the output array.
 *
 * @author  Nathan Fiedler
 */
public class SortJob implements Callable<Object> {
    /** True if this job has already been completed. */
    private volatile boolean completed;
    /** The array from the trie bucket containing unsorted strings. */
    private final String[] input;
    /** The number of elements in the input array. */
    private final int count;
    /** The array to which the sorted strings are written. */
    private final String[] output;
    /** The position within the strings array at which to store the
     * sorted results. */
    private final int offset;
    /** The depth at which to sort the strings (i.e. the strings often
     * have a common prefix, and depth is the length of that prefix and
     * thus the sort routines can ignore those characters). */
    private final int depth;

    /**
     * Constructs an instance of Job which will sort and then copy the
     * input strings to the output array.
     *
     * @param  input   input array; all elements are copied.
     * @param  count   number of elements from input to consider.
     * @param  output  output array; only a subset should be modified.
     * @param  offset  offset within output array to which sorted
     *                 strings will be written.
     * @param  depth   number of charaters in strings to be ignored
     *                 when sorting (i.e. the common prefix).
     */
    public SortJob(String[] input, int count, String[] output, int offset, int depth) {
        this.input = input;
        this.count = count;
        this.output = output;
        this.offset = offset;
        this.depth = depth;
    }

    /**
     * Indicates if this job has been completed or not.
     *
     * @return
     */
    public boolean isCompleted() {
        return completed;
    }

    @Override
    public Object call() throws Exception {
        if (count > 0) {
            if (count > 1) {
                // Sort the strings from the bucket.
                if (count < 20) {
                    Insertionsort.sort(input, 0, count, depth);
                } else {
                    MultikeyQuicksort.sort(input, 0, count, depth);
                }
            }
            // Copy the sorted strings to the destination array.
            System.arraycopy(input, 0, output, offset, count);
        }
        completed = true;
        return null;
    }
}
