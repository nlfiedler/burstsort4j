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
 * A copy job to be completed after the trie traversal phase. Each job
 * is given a single bucket to be a processed. A copy job simply copies
 * the string references from the null bucket to the string output array.
 *
 * @author  Nathan Fiedler
 */
public class CopyJob implements Callable<Object> {
    /** True if this job has already been completed. */
    private volatile boolean completed;
    /** The array from the null trie bucket containing strings as Object
     * references; not to be sorted. */
    private final Object[] input;
    /** The number of elements in the input array. */
    private final int count;
    /** The array to which the sorted strings are written. */
    private final CharSequence[] output;
    /** The position within the strings array at which to store the
     * sorted results. */
    private final int offset;

    /**
     * Constructs an instance of Job which merely copies the objects
     * from the input array to the output array. The input objects
     * must be of type CharSequence in order for the copy to succeed.
     *
     * @param  input   input array.
     * @param  count   number of elements from input to consider.
     * @param  output  output array; only a subset should be modified.
     * @param  offset  offset within output array to which sorted
     *                 strings will be written.
     */
    public CopyJob(Object[] input, int count, CharSequence[] output, int offset) {
        this.input = input;
        this.count = count;
        this.output = output;
        this.offset = offset;
    }

    /**
     * Indicates if this job has been completed or not.
     *
     * @return  true if job has been completed, false otherwise.
     */
    public boolean isCompleted() {
        return completed;
    }

    @Override
    public Object call() throws Exception {
        System.arraycopy(input, 0, output, offset, count);
        completed = true;
        return null;
    }
}
