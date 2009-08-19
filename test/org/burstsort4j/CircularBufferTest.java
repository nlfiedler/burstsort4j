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
        assertEquals(5, instance.getCapacity());
        assertEquals(1, instance.size());
        assertFalse(instance.isEmpty());
        assertFalse(instance.isFull());
        instance.add("abc123");
        instance.add("abc123");
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
    public void drain() {
        CircularBuffer<Integer> instance = new CircularBuffer<Integer>(10);
        try {
            instance.peek();
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
            instance.peek();
            fail("peek of empty buffer should fail");
        } catch (IllegalStateException ise) {
            // expected
        }
        instance.add("abc");
        assertEquals("abc", instance.peek());
        instance.add("123");
        assertEquals("abc", instance.peek());
        assertTrue(instance.isFull());
        assertFalse(instance.isEmpty());
        instance.remove();
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
        while (!instance.isEmpty()) {
            instance.remove();
        }
        assertFalse(instance.isFull());
        assertTrue(instance.isEmpty());
        assertEquals(0, instance.size());
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
