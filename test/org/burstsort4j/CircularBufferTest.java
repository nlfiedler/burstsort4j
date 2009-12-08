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

import java.util.NoSuchElementException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit test for CircularBuffer class.
 *
 * @author Nathan Fiedler
 */
public class CircularBufferTest {

    @Test
    public void testAdd() {
        CircularBuffer<String> instance = new CircularBuffer<String>(5);
        instance.add("abc123");
        assertEquals(5, instance.capacity());
        assertEquals(1, instance.size());
        assertFalse(instance.isEmpty());
        assertFalse(instance.isFull());
        instance.add("abc123");
        instance.offer("abc123");
        instance.add("abc123");
        instance.add("abc123");
        assertFalse(instance.isEmpty());
        assertTrue(instance.isFull());
        assertEquals(5, instance.size());
        try {
            instance.add("abc123");
            fail("should not allow add to full buffer");
        } catch (IllegalStateException ise) {
            // expected
        }

        // Test mixed add and remove operations beyond the capacity.
        instance = new CircularBuffer<String>(10);
        for (int ii = 1; ii <= 10; ii++) {
            instance.add(String.valueOf(ii));
        }
        assertFalse(instance.isEmpty());
        assertTrue(instance.isFull());
        assertEquals(10, instance.size());
        for (int ii = 0; ii < 6; ii++) {
            instance.remove();
        }
        assertFalse(instance.isEmpty());
        assertFalse(instance.isFull());
        assertEquals(4, instance.size());
        for (int ii = 10; ii < 16; ii++) {
            instance.add(String.valueOf(ii));
        }
        assertFalse(instance.isEmpty());
        assertTrue(instance.isFull());
        assertEquals(10, instance.size());
        try {
            instance.add("abc123");
            fail("should not allow add to full buffer");
        } catch (IllegalStateException ise) {
            // expected
        }
        assertFalse(instance.offer("foo"));
        while (!instance.isEmpty()) {
            instance.remove();
        }
        assertTrue(instance.isEmpty());
        assertFalse(instance.isFull());
        assertEquals(0, instance.size());
    }

    @Test
    public void testInitial() {
        Integer[] data = new Integer[10];
        for (int i = 1; i <= 10; i++) {
            data[i - 1] = new Integer(i);
        }
        CircularBuffer<Integer> instance = new CircularBuffer<Integer>(data);
        assertFalse(instance.isEmpty());
        assertTrue(instance.isFull());
        assertEquals(10, instance.size());
        for (int i = 1; i <= 10; i++) {
            Integer v = instance.remove();
            assertEquals(i, v.intValue());
        }
        assertTrue(instance.isEmpty());
        assertFalse(instance.isFull());
        assertEquals(0, instance.size());

        data = new Integer[20];
        for (int i = 1; i <= data.length; i++) {
            data[i - 1] = new Integer(i);
        }
        instance = new CircularBuffer<Integer>(data, 4, 10, false);
        assertFalse(instance.isEmpty());
        assertTrue(instance.isFull());
        assertEquals(10, instance.size());
        assertEquals(10, instance.capacity());
        for (int i = 5; !instance.isEmpty(); i++) {
            Integer v = instance.remove();
            assertEquals(i, v.intValue());
        }
        assertTrue(instance.isEmpty());
        assertFalse(instance.isFull());
        assertEquals(0, instance.size());
        assertEquals(10, instance.capacity());
    }

    @Test
    public void testCorrectness() {
        CircularBuffer<Integer> instance = new CircularBuffer<Integer>(10);
        assertTrue(instance.isEmpty());
        assertFalse(instance.isFull());
        assertEquals(0, instance.size());
        for (int i = 1; i <= 10; i++) {
            instance.add(i);
        }
        assertFalse(instance.isEmpty());
        assertTrue(instance.isFull());
        assertEquals(10, instance.size());
        for (int i = 1; i <= 10; i++) {
            Integer v = instance.remove();
            assertEquals(i, v.intValue());
        }
        assertTrue(instance.isEmpty());
        assertFalse(instance.isFull());
        assertEquals(0, instance.size());
    }

    @Test
    public void testDrain() {
        CircularBuffer<Integer> instance = new CircularBuffer<Integer>(10);
        try {
            instance.drain(new Integer[10], 0);
            fail("drain of empty buffer should fail");
        } catch (IllegalStateException ise) {
            // expected
        }
        for (int i = 1; i <= 8; i++) {
            instance.add(i);
        }
        Integer[] arr = new Integer[instance.size()];
        instance.drain(arr, 0);
        for (int i = 0; i < arr.length; i++) {
            assertEquals(i + 1, arr[i].intValue());
        }
        assertTrue(instance.isEmpty());
        assertEquals(0, instance.size());
        for (int i = 1; i <= 8; i++) {
            instance.add(i);
        }
        for (int i = 1; i <= 3; i++) {
            instance.remove();
        }
        for (int i = 9; i <= 13; i++) {
            instance.add(i);
        }
        arr = new Integer[instance.size()];
        instance.drain(arr, 0);
        for (int i = 0; i < arr.length; i++) {
            assertEquals(i + 4, arr[i].intValue());
        }
        assertTrue(instance.isEmpty());
        assertEquals(0, instance.size());
    }

    @Test
    public void testDrainBuffer() {
        CircularBuffer<Integer> source = new CircularBuffer<Integer>(10);
        CircularBuffer<Integer> sink = new CircularBuffer<Integer>(1);

        // Empty source case.
        try {
            source.drain(sink);
            fail("drain of empty buffer should fail");
        } catch (IllegalStateException ise) {
            // expected
        }

        // Full sink case.
        source.add(1);
        sink.add(1);
        try {
            source.drain(sink);
            fail("drain to full buffer should fail");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        // Both buffers contiguous free regions case.
        source = new CircularBuffer<Integer>(8);
        sink = new CircularBuffer<Integer>(8);
        for (int i = 1; !source.isFull(); i++) {
            source.add(i);
        }
        source.drain(sink);
        assertTrue(source.isEmpty());
        assertEquals(0, source.size());
        assertTrue(sink.isFull());
        assertEquals(8, sink.size());
        for (int i = 1; !sink.isEmpty(); i++) {
            assertEquals(i, sink.remove().intValue());
        }

        // Contiguous source but insufficient upper free region in sink case.
        for (int i = 1; i <= 6; i++) {
            sink.add(i);
        }
        sink.remove();
        sink.remove();
        for (int i = 7; i <= 10; i++) {
            source.add(i);
        }
        source.drain(sink);
        assertTrue(sink.isFull());
        assertEquals(8, sink.size());
        for (int i = 3; !sink.isEmpty(); i++) {
            assertEquals(i, sink.remove().intValue());
        }

        // Source buffer contiguous, sink buffer contiguous free region
        // with end below start case.
        source = new CircularBuffer<Integer>(10);
        sink = new CircularBuffer<Integer>(10);
        for (int i = 1; i <= 10; i++) {
            sink.add(i);
        }
        for (int i = 1; i <= 8; i++) {
            sink.remove();
        }
        sink.add(11);
        sink.add(12);
        for (int i = 13; i <= 16; i++) {
            source.add(i);
        }
        source.drain(sink);
        assertFalse(sink.isFull());
        assertEquals(8, sink.size());
        for (int i = 9; !sink.isEmpty(); i++) {
            assertEquals(i, sink.remove().intValue());
        }

        // Source buffer not contiguous and sink buffer contiguous case.
        source = new CircularBuffer<Integer>(10);
        sink = new CircularBuffer<Integer>(10);
        for (int i = 1; i <= 10; i++) {
            source.add(i);
        }
        for (int i = 1; i <= 8; i++) {
            source.remove();
        }
        source.add(11);
        source.add(12);
        source.drain(sink);
        assertFalse(sink.isFull());
        assertEquals(4, sink.size());
        for (int i = 9; !sink.isEmpty(); i++) {
            assertEquals(i, sink.remove().intValue());
        }

        // Source and sink buffers are not contiguous, direct-map case.
        source = new CircularBuffer<Integer>(10);
        for (int i = 1; i <= 10; i++) {
            source.add(i);
        }
        for (int i = 1; i <= 8; i++) {
            source.remove();
        }
        for (int i = 11; i <= 14; i++) {
            source.add(i);
        }
        sink = new CircularBuffer<Integer>(10);
        for (int i = 1; i <= 8; i++) {
            sink.add(i);
        }
        for (int i = 1; i <= 5; i++) {
            sink.remove();
        }
        source.drain(sink);
        assertFalse(sink.isFull());
        assertEquals(9, sink.size());
        for (int i = 6; !sink.isEmpty(); i++) {
            assertEquals(i, sink.remove().intValue());
        }

        // Source and sink buffers not contiguous, extra space case.
        source = new CircularBuffer<Integer>(10);
        for (int i = 1; i <= 10; i++) {
            source.add(i);
        }
        for (int i = 1; i <= 8; i++) {
            source.remove();
        }
        for (int i = 11; i <= 16; i++) {
            source.add(i);
        }
        sink = new CircularBuffer<Integer>(10);
        for (int i = 3; i <= 8; i++) {
            sink.add(i);
        }
        for (int i = 1; i <= 4; i++) {
            sink.remove();
        }
        source.drain(sink);
        assertTrue(sink.isFull());
        assertEquals(10, sink.size());
        for (int i = 7; !sink.isEmpty(); i++) {
            assertEquals(i, sink.remove().intValue());
        }

        // Source and sink buffers not contiguous, insufficient space case.
        source = new CircularBuffer<Integer>(10);
        for (int i = 1; i <= 10; i++) {
            source.add(i);
        }
        for (int i = 1; i <= 4; i++) {
            source.remove();
        }
        sink = new CircularBuffer<Integer>(10);
        for (int i = -2; i <= 4; i++) {
            sink.add(i);
        }
        for (int i = 1; i <= 3; i++) {
            sink.remove();
        }
        source.drain(sink);
        assertTrue(sink.isFull());
        assertEquals(10, sink.size());
        for (int i = 1; !sink.isEmpty(); i++) {
            assertEquals(i, sink.remove().intValue());
        }
    }

    @Test
    public void testMove() {
        // No need for exhaustive tests here since we know the drain()
        // method delegates to move() and thus it is already well tested.

        // Both buffers contiguous free regions case.
        CircularBuffer<Integer> source = new CircularBuffer<Integer>(8);
        CircularBuffer<Integer> sink = new CircularBuffer<Integer>(8);
        for (int i = 1; !source.isFull(); i++) {
            source.add(i);
        }
        source.move(sink, 4);
        assertFalse(source.isEmpty());
        assertEquals(4, source.size());
        for (int i = 5; !source.isEmpty(); i++) {
            assertEquals(i, source.remove().intValue());
        }
        assertFalse(sink.isFull());
        assertEquals(4, sink.size());
        for (int i = 1; !sink.isEmpty(); i++) {
            assertEquals(i, sink.remove().intValue());
        }
    }

    @Test
    public void testIsEmpty() {
        CircularBuffer<String> instance = new CircularBuffer<String>(2);
        assertTrue(instance.isEmpty());
        instance.add("abc123");
        assertFalse(instance.isEmpty());
    }

    @Test
    public void testIsFull() {
        CircularBuffer<String> instance = new CircularBuffer<String>(2);
        assertFalse(instance.isFull());
        instance.add("abc123");
        instance.add("abc123");
        assertTrue(instance.isFull());
    }

    @Test
    public void testPeek() {
        CircularBuffer<String> instance = new CircularBuffer<String>(2);
        assertFalse(instance.isFull());
        assertTrue(instance.isEmpty());
        try {
            instance.element();
            fail("element() of empty buffer should fail");
        } catch (NoSuchElementException nsee) {
            // expected
        }
        instance.add("abc");
        assertEquals("abc", instance.element());
        assertEquals("abc", instance.peek());
        instance.add("123");
        assertEquals("abc", instance.element());
        assertEquals("abc", instance.peek());
        assertTrue(instance.isFull());
        assertFalse(instance.isEmpty());
        instance.remove();
        assertEquals("123", instance.element());
        assertEquals("123", instance.peek());
    }

    @Test
    public void testRemove() {
        CircularBuffer<String> instance = new CircularBuffer<String>(6);
        instance.add("a");
        instance.add("b");
        instance.add("c");
        instance.add("1");
        instance.add("2");
        instance.add("3");
        assertEquals(6, instance.size());
        assertTrue(instance.isFull());
        assertEquals("a", instance.poll());
        while (!instance.isEmpty()) {
            instance.remove();
        }
        assertFalse(instance.isFull());
        assertTrue(instance.isEmpty());
        assertEquals(0, instance.size());
        assertNull(instance.poll());
        // Test adding again.
        instance.add("a");
        instance.add("b");
        instance.add("c");
        instance.add("1");
        instance.add("2");
        instance.add("3");
        assertEquals(6, instance.size());
        assertTrue(instance.isFull());
    }
}
