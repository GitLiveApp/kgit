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
 * Wrap another comparator for use with
 * [org.eclipse.jgit.diff.Subsequence].
 *
 *
 * This comparator acts as a proxy for the real comparator, translating element
 * indexes on the fly by adding the subsequence's begin offset to them.
 * Comparators of this type must be used with a
 * [org.eclipse.jgit.diff.Subsequence].
 *
 * @param <S>
 * the base sequence type.
</S> */
class SubsequenceComparator<S : Sequence>
/**
 * Construct a comparator wrapping another comparator.
 *
 * @param cmp
 * the real comparator.
 */(private val cmp: SequenceComparator<in S>) : SequenceComparator<Subsequence<S>>() {
    override fun equals(a: Subsequence<S>, ai: Int, b: Subsequence<S>, bi: Int): Boolean {
        return cmp.equals(a.base, ai + a.begin, b.base, bi + b.begin)
    }

    override fun hash(seq: Subsequence<S>, ptr: Int): Int {
        return cmp.hash(seq.base, ptr + seq.begin)
    }
}
