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
 * Wraps two [org.eclipse.jgit.diff.Sequence] instances to cache their
 * element hash codes.
 *
 *
 * This pair wraps two sequences that contain cached hash codes for the input
 * sequences.
 *
 * @param <S>
 * the base sequence type.
</S> */
class HashedSequencePair<S : Sequence>
/**
 * Construct a pair to provide fast hash codes.
 *
 * @param cmp
 * the base comparator for the sequence elements.
 * @param a
 * the A sequence.
 * @param b
 * the B sequence.
 */(private val cmp: SequenceComparator<in S>, private val baseA: S, private val baseB: S) {
    private var cachedA: HashedSequence<S>? = null

    private var cachedB: HashedSequence<S>? = null

    val comparator: HashedSequenceComparator<S>
        /**
         * Get comparator
         *
         * @return obtain a comparator that uses the cached hash codes
         */
        get() = HashedSequenceComparator(cmp)

    val a: HashedSequence<S>
        /**
         * Get A
         *
         * @return wrapper around A that includes cached hash codes
         */
        get() {
            if (cachedA == null) cachedA = wrap(baseA)
            return cachedA!!
        }

    val b: HashedSequence<S>
        /**
         * Get B
         *
         * @return wrapper around B that includes cached hash codes
         */
        get() {
            if (cachedB == null) cachedB = wrap(baseB)
            return cachedB!!
        }

    private fun wrap(base: S): HashedSequence<S> {
        val end = base!!.size()
        val hashes = IntArray(end)
        for (ptr in 0 until end) hashes[ptr] = cmp.hash(base, ptr)
        return HashedSequence(base, hashes)
    }
}
