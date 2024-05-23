/*
 * Copyright (C) 2008, Google Inc.
 * Copyright (C) 2009, Johannes Schindelin <johannes.schindelin@gmx.de> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.util

/**
 * A more efficient List&lt;Integer&gt; using a primitive integer array.
 */
class IntList constructor(capacity: Int = 10) {
    private var entries: IntArray

    private var count = 0

    /**
     * Create an empty list with the specified capacity.
     *
     * @param capacity
     * number of entries the list can initially hold.
     */
    /**
     * Create an empty list with a default capacity.
     */
    init {
        entries = IntArray(capacity)
    }

    /**
     * Get number of entries in this list.
     *
     * @return number of entries in this list.
     */
    fun size(): Int {
        return count
    }

    /**
     * Check if an entry appears in this collection.
     *
     * @param value
     * the value to search for.
     * @return true of `value` appears in this list.
     * @since 4.9
     */
    fun contains(value: Int): Boolean {
        for (i in 0 until count) if (entries[i] == value) return true
        return false
    }

    /**
     * Get the value at the specified index
     *
     * @param i
     * index to read, must be in the range [0, [.size]).
     * @return the number at the specified index
     * @throws java.lang.IndexOutOfBoundsException
     * the index outside the valid range
     */
    operator fun get(i: Int): Int {
        if (count <= i) throw IndexOutOfBoundsException(i.toString())
        return entries[i]
    }

    /**
     * Empty this list
     */
    fun clear() {
        count = 0
    }

    /**
     * Add an entry to the end of the list.
     *
     * @param n
     * the number to add.
     */
    fun add(n: Int) {
        if (count == entries.size) grow()
        entries[count++] = n
    }

    /**
     * Assign an entry in the list.
     *
     * @param index
     * index to set, must be in the range [0, [.size]).
     * @param n
     * value to store at the position.
     */
    operator fun set(index: Int, n: Int) {
        if (count < index) throw IndexOutOfBoundsException(index.toString())
        else if (count == index) add(n)
        else entries[index] = n
    }

    /**
     * Pad the list with entries.
     *
     * @param toIndex
     * index position to stop filling at. 0 inserts no filler. 1
     * ensures the list has a size of 1, adding `val` if
     * the list is currently empty.
     * @param val
     * value to insert into padded positions.
     */
    fun fillTo(toIndex: Int, `val`: Int) {
        while (count < toIndex) add(`val`)
    }

    /**
     * Sort the entries of the list in-place, according to the comparator.
     *
     * @param comparator
     * provides the comparison values for sorting the entries
     * @since 6.6
     */
    fun sort(comparator: IntComparator) {
        quickSort(0, count - 1, comparator)
    }

    /**
     * Quick sort has average time complexity of O(n log n) and O(log n) space
     * complexity (for recursion on the stack).
     *
     *
     * Implementation based on https://www.baeldung.com/java-quicksort.
     *
     * @param begin
     * the index to begin partitioning at, inclusive
     * @param end
     * the index to end partitioning at, inclusive
     * @param comparator
     * provides the comparison values for sorting the entries
     */
    private fun quickSort(begin: Int, end: Int, comparator: IntComparator) {
        if (begin < end) {
            val partitionIndex = partition(begin, end, comparator)

            quickSort(begin, partitionIndex - 1, comparator)
            quickSort(partitionIndex + 1, end, comparator)
        }
    }

    private fun partition(begin: Int, end: Int, comparator: IntComparator): Int {
        val pivot = entries[end]
        var writeSmallerIdx = (begin - 1)

        for (findSmallerIdx in begin until end) {
            if (comparator.compare(entries[findSmallerIdx], pivot) <= 0) {
                writeSmallerIdx++

                val biggerVal = entries[writeSmallerIdx]
                entries[writeSmallerIdx] = entries[findSmallerIdx]
                entries[findSmallerIdx] = biggerVal
            }
        }

        val pivotIdx = writeSmallerIdx + 1
        entries[end] = entries[pivotIdx]
        entries[pivotIdx] = pivot

        return pivotIdx
    }

    private fun grow() {
        val n = IntArray((entries.size + 16) * 3 / 2)
        entries.copyInto(n, 0, 0, count)
        entries = n
    }

    override fun toString(): String {
        val r = StringBuilder()
        r.append('[')
        for (i in 0 until count) {
            if (i > 0) r.append(", ") //$NON-NLS-1$

            r.append(entries[i])
        }
        r.append(']')
        return r.toString()
    }

    /**
     * A comparator of primitive ints.
     *
     * @since 6.6
     */
    interface IntComparator {
        /**
         * Compares the two int arguments for order.
         *
         * @param first
         * the first int to compare
         * @param second
         * the second int to compare
         * @return a negative number if first &lt; second, 0 if first == second, or
         * a positive number if first &gt; second
         */
        fun compare(first: Int, second: Int): Int
    }

    companion object {
        /**
         * Create a list initialized with the values of the given range.
         *
         * @param start
         * the beginning of the range, inclusive
         * @param end
         * the end of the range, exclusive
         * @return the list initialized with the given range
         * @since 6.6
         */
        fun filledWithRange(start: Int, end: Int): IntList {
            val list = IntList(end - start)
            for (`val` in start until end) {
                list.add(`val`)
            }
            return list
        }
    }
}
