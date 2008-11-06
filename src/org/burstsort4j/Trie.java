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

import java.io.PrintStream;

/**
 * A simple trie structure for storing strings for fast retrieval.
 * An optional object reference can be stored in the trie nodes,
 * turning the trie into a form of map.
 *
 * <p>Note that a trie structure consumes a lot of memory. Each
 * object is on the order of 32 bytes, and each holds an array
 * that is at least 2,048 bytes. Storing a few thousand words
 * will easily consume more than 50Mb of memory.</p>
 *
 * @author nfiedler
 */
public class Trie {
    /** Size of the large alphabet this trie supports. */
    private static final int ALPHABET_SIZE_L = 65536;
    /** Size of the small alphabet this trie supports. */
    private static final int ALPHABET_SIZE_S = 256;
    /** Number of trie nodes created. */
    private static long trieCount;
    /** Number of small children arrays created. */
    private static long smallCount;
    /** Number of large children arrays created. */
    private static long largeCount;
    /** Child node, indexed by the character itself. */
    private Trie[] children;
    /** If true, this node contains an entire word. There may be
     * additional words below, but this so far is a word. */
    private boolean terminal;
    /** The value stored in this node. */
    private Object value;

    /**
     * Adds a string to the trie structure.
     *
     * @param  str  string to add to the trie.
     */
    public void add(CharSequence str) {
        add(str, null);
    }

    /**
     * Adds a string to the trie structure.
     *
     * @param  str    string to add to the trie.
     * @param  value  object to store in the new trie node.
     */
    public void add(CharSequence str, Object value) {
        Trie node = this;
        int limit = str.length();
        for (int idx = 0; idx < limit; idx++) {
            int ch = str.charAt(idx);
            addChildren(node, ch);
            if (node.children[ch] == null) {
                node.children[ch] = new Trie();
                trieCount++;
            }
            node = node.children[ch];
        }
        node.terminal = true;
        node.value = value;
    }

    /**
     * Adds a string to the trie structure.
     *
     * @param  str     array of characters containing string to add.
     * @param  offset  first position within array from which to add.
     * @param  length  number of characters to add.
     */
    public void add(char[] str, int offset, int length) {
        add(str, offset, length, null);
    }

    /**
     * Adds a string to the trie structure.
     *
     * @param  str     array of characters containing string to add.
     * @param  offset  first position within array from which to add.
     * @param  length  number of characters to add.
     * @param  value   object to store in the new trie node.
     */
    public void add(char[] str, int offset, int length, Object value) {
        Trie node = this;
        int limit = offset + length;
        for (int idx = offset; idx < limit; idx++) {
            int ch = str[idx];
            addChildren(node, ch);
            if (node.children[ch] == null) {
                node.children[ch] = new Trie();
                trieCount++;
            }
            node = node.children[ch];
        }
        node.terminal = true;
        node.value = value;
    }

    /**
     * Add the children array to the give node, if necessary. Determines
     * the size of the array based on the character. If the array already
     * exists and is too small, it will be resized appropriately.
     *
     * @param node
     * @param ch
     */
    private static void addChildren(Trie node, int ch) {
        if (node.children == null) {
            // Lazily allocate the array to reduce memory usage.
            if (ch < ALPHABET_SIZE_S) {
                node.children = new Trie[ALPHABET_SIZE_S];
                smallCount++;
            } else {
                node.children = new Trie[ALPHABET_SIZE_L];
                largeCount++;
            }
        }
        if (ch >= node.children.length) {
            // Have to grow the children array.
            Trie[] temp = new Trie[ALPHABET_SIZE_L];
            System.arraycopy(node.children, 0, temp, 0, ALPHABET_SIZE_S);
            node.children = temp;
            largeCount++;
            smallCount--;
        }
    }

    /**
     * Determine if the given string is in the trie.
     *
     * @param  str  string to find.
     * @return  true if string is in the trie; false otherwise.
     */
    public boolean contains(CharSequence str) {
        Trie node = this;
        int limit = str.length();
        for (int idx = 0; idx < limit; idx++) {
            int ch = str.charAt(idx);
            if (node.children == null || ch >= node.children.length ||
                    node.children[ch] == null) {
                return false;
            }
            node = node.children[ch];
        }
        return node.terminal;
    }

    /**
     * Determine if the given string is in the trie.
     *
     * @param  str     array of characters containing string to find.
     * @param  offset  first position within array from which to find.
     * @param  length  number of characters to find.
     * @return  true if string is in the trie; false otherwise.
     */
    public boolean contains(char[] str, int offset, int length) {
        Trie node = this;
        int limit = offset + length;
        for (int idx = offset; idx < limit; idx++) {
            int ch = str[idx];
            if (node.children == null || ch >= node.children.length ||
                    node.children[ch] == null) {
                return false;
            }
            node = node.children[ch];
        }
        return node.terminal;
    }

    /**
     * Retrieve the value associated with the given string.
     * 
     * @param  str  the string to find in the trie.
     * @return  the associated value, or null if none.
     */
    public Object get(CharSequence str) {
        Trie node = this;
        int limit = str.length();
        for (int idx = 0; idx < limit; idx++) {
            int ch = str.charAt(idx);
            if (node.children == null || ch >= node.children.length ||
                node.children[ch] == null) {
                return null;
            }
            node = node.children[ch];
        }
        return node.value;
    }

    /**
     * Retrieve the value associated with the given string.
     * 
     * @param  str     array of characters containing string to find.
     * @param  offset  first position within array from which to find.
     * @param  length  number of characters to find.
     * @return  the associated value, or null if none.
     */
    public Object get(char[] str, int offset, int length) {
        Trie node = this;
        int limit = offset + length;
        for (int idx = offset; idx < limit; idx++) {
            int ch = str[idx];
            if (node.children == null || ch >= node.children.length ||
                node.children[ch] == null) {
                return null;
            }
            node = node.children[ch];
        }
        return node.value;
    }

    /**
     * Print a report of the object allocation for this class.
     *
     * @param  out  stream on which to print report.
     */
    public static void reportStats(PrintStream out) {
        out.format("Trie allocation count: %d\n", trieCount);
        out.format("Trie small array count: %d\n", smallCount);
        out.format("Trie large array count: %d\n", largeCount);
    }

    /**
     * Reset the object allocation statistics for this class.
     */
    public static void resetStats() {
        trieCount = 0;
        smallCount = 0;
        largeCount = 0;
    }
}
