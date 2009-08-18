/*
 * Copyright (C) 2009  Nathan Fiedler
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

/**
 * A simple circular buffer of fixed length which has an empty and full
 * state. When full, the buffer will not accept any new entries.
 *
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a buffer concurrently, and at least one of
 * the threads modifies the buffer structurally, it must be synchronized
 * externally.</p>
 *
 * @param  <T>  type of elements in the buffer.
 * @author Nathan Fiedler
 */
public class CircularBuffer<T> {
    /** The circular buffer. */
    private final Object[] buffer;
    /** Offset of the first entry in the buffer. */
    private int start;
    /** Offset of the last entry in the buffer. */
    private int end;
    /** Number of elements in the buffer. Used to distinquish between
     * the full and empty case. */
    private int count;

    /**
     * Constructs a new instance of CircularBuffer with the given capacity.
     *
     * @param  capacity  maximum number of valid elements to contain.
     */
    public CircularBuffer(int capacity) {
        buffer = new Object[capacity];
    }

    /**
     * Adds the given object to the buffer.
     *
     * @param  o  object to be added.
     */
    public void add(T o) {
        if (count == buffer.length) {
            throw new IllegalStateException("buffer is full");
        }
        count++;
        buffer[end] = o;
        end++;
        if (end == buffer.length) {
            end = 0;
        }
    }

    /**
     * Drains the contents of the circular buffer into the given output
     * array in an efficient manner (uses System.arraycopy). Copies the
     * remaining elements of the circular buffer to the destination,
     * leaving the buffer empty.
     *
     * @param  output  destination for buffer contents.
     * @param  offset  position in output to which elements are copied.
     */
    public void drain(T[] output, int offset) {
        if (count == 0) {
            throw new IllegalStateException("buffer is empty");
        }
        if (end <= start) {
            // Buffer wraps around, must make two calls to arraycopy().
            int leading = buffer.length - start;
            System.arraycopy(buffer, start, output, offset, leading);
            System.arraycopy(buffer, 0, output, offset + leading, end);
        } else {
            // Buffer is in one contiguous region.
            System.arraycopy(buffer, start, output, offset, end - start);
        }
        start = 0;
        end = 0;
        count = 0;
    }

    /**
     * Returns the total number of elements this buffer can hold (the same
     * value passed to the constructor).
     *
     * @return  buffer capacity.
     */
    public int getCapacity() {
        return buffer.length;
    }

    /**
     * Indicates if the buffer is empty.
     *
     * @return  true if empty, false otherwise.
     */
    public boolean isEmpty() {
        return count == 0;
    }

    /**
     * Indicates if the buffer is full.
     *
     * @return  true if full, false otherwise.
     */
    public boolean isFull() {
        return count == buffer.length;
    }

    /**
     * Returns the first element in the buffer without removing it. The
     * buffer is not modified in any way by this operation.
     *
     * @return  first element in the buffer.
     */
    @SuppressWarnings("unchecked")
    public T peek() {
        if (count == 0) {
            throw new IllegalStateException("buffer is empty");
        }
        return (T) buffer[start];
    }

    /**
     * Removes the first element in the buffer.
     *
     * @return  first element in the buffer.
     */
    @SuppressWarnings("unchecked")
    public T remove() {
        if (count == 0) {
            throw new IllegalStateException("buffer is empty");
        }
        count--;
        Object o = buffer[start];
        start++;
        if (start == buffer.length) {
            start = 0;
        }
        return (T) o;
    }

    /**
     * Returns the number of elements stored in the buffer.
     *
     * @return  number of elements in buffer.
     */
    public int size() {
        return count;
    }
}
