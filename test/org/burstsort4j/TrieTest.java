/*
 * Copyright (C) 2008  Nathan Fiedler
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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test the Trie class.
 *
 * @author Nathan Fiedler
 */
public class TrieTest {

    public TrieTest() {
    }

    @Test
    public void testTrie() {
        Trie trie = new Trie();
        if (trie.contains("")) {
            fail("Empty string found but not stored!");
        }
        trie.add("");
        if (!trie.contains("")) {
            fail("Failed to store empty string!");
        }
        String[] found = new String[] {
            "bat", "barn", "bark", "by", "byte", "bytes",
            "wane", "way", "while"
        };
        trie = new Trie();
        for (String s : found) {
            trie.add(s);
        }
        String str = "extra fox";
        trie.add(str.toCharArray(), 0, str.length());
        assertTrue(trie.contains("extra fox"));
        for (String s : found) {
            assertTrue(trie.contains(s));
        }
        String[] missing = new String[] {
            "barking", "foobar", "sizzle", "wan"
        };
        for (String s : missing) {
            assertFalse(trie.contains(s));
        }
        assertTrue(trie.contains(str.toCharArray(), 0, str.length()));
    }

    @Test
    public void testTrieValues() {
        Trie trie = new Trie();
        String[] found = new String[] {
            "bat", "barn", "bark", "by", "byte", "bytes",
            "wane", "way", "while"
        };
        trie = new Trie();
        for (String s : found) {
            trie.add(s, s.hashCode());
        }
        String str = "extra fox";
        trie.add(str.toCharArray(), 0, str.length(), str.hashCode());
        assertEquals(new Integer(str.hashCode()), trie.get("extra fox"));
        for (String s : found) {
            assertEquals(new Integer(s.hashCode()), trie.get(s));
        }
        String[] missing = new String[] {
            "barking", "foobar", "sizzle", "wan"
        };
        for (String s : missing) {
            assertNull(trie.get(s));
        }
        assertEquals(new Integer(str.hashCode()), trie.get(str.toCharArray(), 0, str.length()));
    }

    @Test
    public void testTrieWords() {
        List<String> data = null;
        try {
            data = Tests.loadData();
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
        Collections.shuffle(data);
        Trie trie = new Trie();
        Trie.resetStats();
        int count = 0;
        try {
            for (String s : data) {
                trie.add(s);
                count++;
            }
        } catch (OutOfMemoryError oome) {
            System.out.format("Words added: %d\n", count);
            Trie.reportStats(System.out);
            throw oome;
        }
        Collections.shuffle(data);
        for (String s : data) {
            assertTrue(trie.contains(s));
        }
    }
}
