/*
 * Copyright 2009-2011 Nathan Fiedler. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package org.burstsort4j;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Observable;
import java.util.Observer;
import java.util.Queue;

/**
 * A simple circular buffer of fixed length which has an empty and full
 * state. When full, the buffer will not accept any new entries.
 *
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a buffer concurrently, and at least one of
 * the threads modifies the buffer structurally, it must be synchronized
 * externally.</p>
 *
 * @param  <E>  type of elements in the buffer.
 * @author Nathan Fiedler
 */
public class CircularBuffer<E> implements Collection, Queue {

    /** Observable for notifying listeners of changes. */
    private final BufferObservable observable;
    /** The circular buffer. */
    private final Object[] buffer;
    /** Lower limit within the buffer, equal to the first usable position. */
    private final int lower;
    /** Upper limit within the buffer, just past the last usable position. */
    private final int upper;
    /** The element capacity of the buffer. */
    private final int capacity;
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
        observable = new BufferObservable(this);
        buffer = new Object[capacity];
        lower = 0;
        upper = capacity;
        this.capacity = capacity;
    }

    /**
     * Constructs a new instance of CircularBuffer with the given data.
     * The given array data will be copied to a new array to prevent
     * accidental modification of the buffer.
     *
     * @param  initial  data to be stored in buffer initially.
     */
    public CircularBuffer(E[] initial) {
        this(initial, true);
    }

    /**
     * Constructs a new instance of CircularBuffer with the given data.
     * All entries in the array are assumed to be valid data such that
     * the buffer count will be equal to the length of the given array.
     *
     * @param  initial  data to be stored in buffer initially.
     * @param  copy     if true, data will be copied to a new array,
     *                  otherwise the given array will be used as-is.
     */
    public CircularBuffer(E[] initial, boolean copy) {
        this(initial, 0, initial.length, copy);
    }

    /**
     * Constructs a new instance of CircularBuffer with the given data.
     * The entries in the array from the offset up to <tt>offset + count</tt>
     * are assumed to be valid data such that the buffer count will be equal
     * to the count argument.
     *
     * @param  initial  data to be stored in buffer initially.
     * @param  offset   first offset within array to be used.
     * @param  count    number of elements from offset to be used.
     * @param  copy     if true, data will be copied to a new array,
     *                  otherwise the given array will be used as-is.
     */
    public CircularBuffer(E[] initial, int offset, int count, boolean copy) {
        observable = new BufferObservable(this);
        if (copy) {
            buffer = new Object[count];
            System.arraycopy(initial, offset, buffer, 0, count);
            offset = 0;
        } else {
            buffer = initial;
        }
        this.count = count;
        lower = offset;
        upper = offset + count;
        capacity = count;
        start = lower;
        end = lower;
    }

    @Override
    public boolean add(Object o) {
        if (count == capacity) {
            throw new IllegalStateException("buffer is full");
        }
        count++;
        buffer[end] = o;
        end++;
        if (end == upper) {
            end = lower;
        }
        return true;
    }

    @Override
    public boolean addAll(Collection c) {
        Iterator i = c.iterator();
        while (i.hasNext()) {
            add(i.next());
        }
        return true;
    }

    /**
     * Adds an observer to the set of observers for this object, provided
     * that it is not the same as some observer already in the set. The
     * order in which notifications will be delivered to multiple observers
     * is not specified. See the class comment.
     *
     * <p>The {@code Observer} will be notified when this buffer becomes
     * empty (that is, the last element is removed).</p>
     *
     * @param  o  an observer to be added.
     */
    public void addObserver(Observer o) {
        observable.addObserver(o);
    }

    /**
     * Returns the total number of elements this buffer can hold (the same
     * value passed to the constructor).
     *
     * @return  buffer capacity.
     */
    public int capacity() {
        return capacity;
    }

    @Override
    public void clear() {
        start = lower;
        end = lower;
        count = 0;
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean containsAll(Collection c) {
        throw new UnsupportedOperationException("Not supported yet.");
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
        move(sink, count);
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
    public void drain(E[] sink, int offset) {
        if (count == 0) {
            throw new IllegalStateException("buffer is empty");
        }
        if (sink.length - offset < count) {
            throw new IllegalArgumentException("destination too small");
        }
        if (end <= start) {
            // Buffer wraps around, must make two calls to arraycopy().
            int leading = upper - start;
            System.arraycopy(buffer, start, sink, offset, leading);
            System.arraycopy(buffer, lower, sink, offset + leading, end - lower);
        } else {
            // Buffer is in one contiguous region.
            System.arraycopy(buffer, start, sink, offset, end - start);
        }
        start = lower;
        end = lower;
        count = 0;
        observable.setAndNotify();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object element() {
        if (count == 0) {
            throw new NoSuchElementException("buffer is empty");
        }
        return (E) buffer[start];
    }

    @Override
    public boolean isEmpty() {
        return count == 0;
    }

    /**
     * Indicates if the buffer is full.
     *
     * @return  true if full, false otherwise.
     */
    public boolean isFull() {
        return count == capacity;
    }

    @Override
    public Iterator iterator() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean offer(Object o) {
        if (count == capacity) {
            return false;
        }
        count++;
        buffer[end] = o;
        end++;
        if (end == upper) {
            end = lower;
        }
        return true;
    }

    /**
     * Moves the given number of elements from this circular buffer into
     * the sink buffer in an efficient manner (using System.arraycopy).
     * This is equivalent to repeatedly removing elements from this buffer
     * and adding them to the sink.
     *
     * @param  sink  destination for buffer contents.
     * @param  n     number of elements to be moved.
     */
    @SuppressWarnings("unchecked")
    public void move(CircularBuffer sink, int n) {
        if (count < n) {
            throw new IllegalStateException("source has too few items");
        }
        if (sink.upper - sink.lower - sink.count < n) {
            throw new IllegalArgumentException("sink has insufficient space");
        }

        int tocopy = n;
        while (tocopy > 0) {
            int desired = Math.min(tocopy, Math.max(end - start, upper - start));
            int willfit = sink.start <= sink.end ? sink.upper - sink.end : sink.start - sink.end;
            int copied = Math.min(desired, willfit);
            System.arraycopy(buffer, start, sink.buffer, sink.end, copied);
            sink.end += copied;
            if (sink.end >= sink.upper) {
                sink.end -= (sink.upper - sink.lower);
            }
            start += copied;
            if (start >= upper) {
                start -= capacity;
            }
            tocopy -= copied;
        }
        sink.count += n;
        count -= n;
        if (count == 0) {
            observable.setAndNotify();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public E peek() {
        return count == 0 ? null : (E) buffer[start];
    }

    @Override
    @SuppressWarnings("unchecked")
    public E poll() {
        if (count == 0) {
            return null;
        }
        count--;
        Object o = buffer[start];
        start++;
        if (start == upper) {
            start = lower;
        }
        if (count == 0) {
            observable.setAndNotify();
        }
        return (E) o;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E remove() {
        if (count == 0) {
            throw new NoSuchElementException("buffer is empty");
        }
        count--;
        Object o = buffer[start];
        start++;
        if (start == upper) {
            start = lower;
        }
        if (count == 0) {
            observable.setAndNotify();
        }
        return (E) o;
    }

    /**
     * Deletes an observer from the set of observers of this object.
     * Passing {@code null} to this method will have no effect. 
     *
     * @param  o  the observer to be removed.
     */
    public void removeObserver(Observer o) {
        observable.deleteObserver(o);
    }

    /**
     * Returns the number of empty spaces within this buffer (i.e. how many
     * times {@link #add(Object)} can be called before the buffer is full).
     *
     * @return  number of empty spaces in buffer.
     */
    public int remaining() {
        return capacity - count;
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Not supported on circular buffer.");
    }

    @Override
    public boolean removeAll(Collection c) {
        throw new UnsupportedOperationException("Not supported on circular buffer.");
    }

    @Override
    public boolean retainAll(Collection c) {
        throw new UnsupportedOperationException("Not supported on circular buffer.");
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException("Use drain() instead.");
    }

    @Override
    public Object[] toArray(Object[] a) {
        throw new UnsupportedOperationException("Use drain() instead.");
    }

    @Override
    public int size() {
        return count;
    }

    /**
     * An {@code Observable} that makes it convenient to set the state as
     * changed and notify the listeners, passing them a reference to the
     * CircularBuffer associated with this instance.
     */
    private static class BufferObservable extends Observable {

        /** The buffer whose state changes from time to time. */
        private CircularBuffer buffer;

        /**
         * @param  buffer  circular buffer to send to listeners.
         */
        public BufferObservable(CircularBuffer buffer) {
            this.buffer = buffer;
        }

        /**
         * Set the observable state to changed and notify listeners.
         */
        public void setAndNotify() {
            setChanged();
            notifyObservers(buffer);
        }
    }
}
