/*
 * Copyright (C) 2010, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.diff

/**
 * Equivalence function for a [org.eclipse.jgit.diff.Sequence] compared by
 * difference algorithm.
 *
 *
 * Difference algorithms can use a comparator to compare portions of two
 * sequences and discover the minimal edits required to transform from one
 * sequence to the other sequence.
 *
 *
 * Indexes within a sequence are zero-based.
 *
 * @param <S>
 * type of sequence the comparator supports.
</S> */
abstract class SequenceComparator<S : Sequence> {
    /**
     * Compare two items to determine if they are equivalent.
     *
     * It is permissible to compare sequence `a` with itself (by passing
     * `a` again in position `b`).
     *
     * @param a
     * the first sequence.
     * @param ai
     * item of `ai` to compare.
     * @param b
     * the second sequence.
     * @param bi
     * item of `bi` to compare.
     * @return true if the two items are identical according to this function's
     * equivalence rule.
     */
    abstract fun equals(a: S, ai: Int, b: S, bi: Int): Boolean

    /**
     * Get a hash value for an item in a sequence.
     *
     * If two items are equal according to this comparator's
     * [.equals] method, then this hash
     * method must produce the same integer result for both items.
     *
     * It is not required for two items to have different hash values if they
     * are unequal according to the `equals()` method.
     *
     * @param seq
     * the sequence.
     * @param ptr
     * the item to obtain the hash for.
     * @return hash the hash value.
     */
    abstract fun hash(seq: S, ptr: Int): Int

    /**
     * Modify the edit to remove common leading and trailing items.
     *
     * The supplied edit `e` is reduced in size by moving the beginning A
     * and B points so the edit does not cover any items that are in common
     * between the two sequences. The ending A and B points are also shifted to
     * remove common items from the end of the region.
     *
     * @param a
     * the first sequence.
     * @param b
     * the second sequence.
     * @param e
     * the edit to start with and update.
     * @return `e` if it was updated in-place, otherwise a new edit
     * containing the reduced region.
     */
    open fun reduceCommonStartEnd(a: S, b: S, e: Edit): Edit? {
        // Skip over items that are common at the start.
        //
        while (e.beginA < e.endA && e.beginB < e.endB && equals(a, e.beginA, b, e.beginB)) {
            e.beginA++
            e.beginB++
        }

        // Skip over items that are common at the end.
        //
        while (e.beginA < e.endA && e.beginB < e.endB && equals(a, e.endA - 1, b, e.endB - 1)) {
            e.endA--
            e.endB--
        }

        return e
    }
}
