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
 * Wraps a [org.eclipse.jgit.diff.Sequence] to have a narrower range of
 * elements.
 *
 *
 * This sequence acts as a proxy for the real sequence, translating element
 * indexes on the fly by adding `begin` to them. Sequences of this type
 * must be used with a [org.eclipse.jgit.diff.SubsequenceComparator].
 *
 * @param <S>
 * the base sequence type.
</S> */
class Subsequence<S : Sequence>(@JvmField val base: S, @JvmField val begin: Int, end: Int) : Sequence() {
    private val size = end - begin

    override fun size(): Int {
        return size
    }

    companion object {
        /**
         * Construct a subsequence around the A region/base sequence.
         *
         * @param <S>
         * type of returned Sequence
         * @param a
         * the A sequence.
         * @param region
         * the region of `a` to create a subsequence around.
         * @return subsequence of `base` as described by A in `region`.
        </S> */
        fun <S : Sequence> a(a: S, region: Edit): Subsequence<S> {
            return Subsequence(a, region.beginA, region.endA)
        }

        /**
         * Construct a subsequence around the B region/base sequence.
         *
         * @param <S>
         * type of returned Sequence
         * @param b
         * the B sequence.
         * @param region
         * the region of `b` to create a subsequence around.
         * @return subsequence of `base` as described by B in `region`.
        </S> */
        fun <S : Sequence> b(b: S, region: Edit): Subsequence<S> {
            return Subsequence(b, region.beginB, region.endB)
        }

        /**
         * Adjust the Edit to reflect positions in the base sequence.
         *
         * @param <S>
         * type of returned Sequence
         * @param e
         * edit to adjust in-place. Prior to invocation the indexes are
         * in terms of the two subsequences; after invocation the indexes
         * are in terms of the base sequences.
         * @param a
         * the A sequence.
         * @param b
         * the B sequence.
        </S> */
        fun <S : Sequence> toBase(
            e: Edit, a: Subsequence<S>,
            b: Subsequence<S>
        ) {
            e.beginA += a.begin
            e.endA += a.begin

            e.beginB += b.begin
            e.endB += b.begin
        }

        /**
         * Adjust the Edits to reflect positions in the base sequence.
         *
         * @param <S>
         * type of returned Sequence
         * @param edits
         * edits to adjust in-place. Prior to invocation the indexes are
         * in terms of the two subsequences; after invocation the indexes
         * are in terms of the base sequences.
         * @param a
         * the A sequence.
         * @param b
         * the B sequence.
         * @return always `edits` (as the list was updated in-place).
        </S> */
		@JvmStatic
		fun <S : Sequence> toBase(
            edits: EditList,
            a: Subsequence<S>, b: Subsequence<S>
        ): EditList {
            for (e in edits) toBase(e, a, b)
            return edits
        }
    }
}
