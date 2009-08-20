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
     * Constructs a new instance of CircularBuffer with the given data.
     * The given array data will be copied to a new array to prevent
     * accidental modification of the buffer.
     *
     * @param  initial  data to be stored in buffer initially.
     */
    public CircularBuffer(T[] initial) {
        this(initial, true);
    }

    /**
     * Constructs a new instance of CircularBuffer with the given data.
     * All entries in the array are assumed to be valid data such that
     * the buffer count will be equal to the length of the given array.
     *
     * <p>You may wish, for multi-thread safety, to have the data copied
     * to a new array by passing <tt>true</tt> for the <em>copy</em>
     * argument. Otherwise, for memory efficiency you can pass <tt>false</tt>
     * and the given array will be used as the buffer. It is then your
     * responsibility to prevent concurrent modifications to that array.</p>
     *
     * @param  initial  data to be stored in buffer initially.
     * @param  copy     if true, data will be copied to a new array,
     *                  otherwise the given array will be used as-is.
     */
    public CircularBuffer(T[] initial, boolean copy) {
        if (copy) {
            buffer = new Object[initial.length];
            System.arraycopy(initial, 0, buffer, 0, initial.length);
        } else {
            buffer = initial;
        }
        count = buffer.length;
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
     * Drains the contents of the circular buffer into the given sink
     * in an efficient manner (using System.arraycopy). Copies the
     * remaining elements of this circular buffer to the destination,
     * leaving this buffer empty.
     *
     * @param  sink  destination for buffer contents.
     */
    public void drain(CircularBuffer sink) {
        if (count == 0) {
            throw new IllegalStateException("buffer is empty");
        }
        if (sink.buffer.length - sink.count < count) {
            throw new IllegalArgumentException("sink too small");
        }
        Object[] output = sink.buffer;
        if (end <= start) {
            // Source buffer is not contiguous.
            if (sink.buffer.length - sink.end < count) {
                // Destination buffer will wrap around after this call.
                int tocopy = buffer.length - start;
                int willfit = sink.buffer.length - sink.end;
                if (tocopy == willfit) {
                    // Source buffer regions map directly onto open regions
                    // of sink buffer.
                    System.arraycopy(buffer, start, output, sink.end, tocopy);
                    System.arraycopy(buffer, 0, output, 0, end);
                } else if (tocopy < willfit) {
                    // Sink buffer has extra space in the upper open region.
                    System.arraycopy(buffer, start, output, sink.end, tocopy);
                    System.arraycopy(buffer, 0, output, sink.end + tocopy, willfit - tocopy);
                    tocopy = willfit - tocopy;
                    System.arraycopy(buffer, tocopy, output, 0, end - tocopy);
                } else {
                    // Upper free region of sink buffer is too small.
                    System.arraycopy(buffer, start, output, sink.end, willfit);
                    tocopy -= willfit;
                    System.arraycopy(buffer, start + willfit, output, 0, tocopy);
                    System.arraycopy(buffer, 0, output, tocopy, end);
                }
            } else {
                // Destination has a contiguous open region.
                int leading = buffer.length - start;
                System.arraycopy(buffer, start, output, sink.end, leading);
                System.arraycopy(buffer, 0, output, sink.end + leading, end);
            }
        } else {
            // Source buffer is contiguous.
            if (sink.buffer.length - sink.end < count) {
                // Destination buffer will wrap around after this call.
                int leading = sink.buffer.length - sink.end;
                System.arraycopy(buffer, start, output, sink.end, leading);
                System.arraycopy(buffer, start + leading, output, 0, count - leading);
            } else {
                // Both buffers have contiguous open regions.
                System.arraycopy(buffer, start, output, sink.end, count);
            }
        }
        sink.end += count;
        if (sink.end >= sink.buffer.length) {
            sink.end -= sink.buffer.length;
        }
        sink.count += count;
        start = 0;
        end = 0;
        count = 0;
    }

    /**
     * Drains the contents of the circular buffer into the given output
     * array in an efficient manner (using System.arraycopy). Copies the
     * remaining elements of this circular buffer to the destination,
     * leaving this buffer empty.
     *
     * @param  sink    destination for buffer contents.
     * @param  offset  position in output to which elements are copied.
     */
    public void drain(T[] sink, int offset) {
        if (count == 0) {
            throw new IllegalStateException("buffer is empty");
        }
        if (sink.length - offset < count) {
            throw new IllegalArgumentException("destination too small");
        }
        if (end <= start) {
            // Buffer wraps around, must make two calls to arraycopy().
            int leading = buffer.length - start;
            System.arraycopy(buffer, start, sink, offset, leading);
            System.arraycopy(buffer, 0, sink, offset + leading, end);
        } else {
            // Buffer is in one contiguous region.
            System.arraycopy(buffer, start, sink, offset, end - start);
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
