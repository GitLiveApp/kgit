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
 * [org.eclipse.jgit.diff.HashedSequence].
 *
 *
 * This comparator acts as a proxy for the real comparator, evaluating the
 * cached hash code before testing the underlying comparator's equality.
 * Comparators of this type must be used with a
 * [org.eclipse.jgit.diff.HashedSequence].
 *
 *
 * To construct an instance of this type use
 * [org.eclipse.jgit.diff.HashedSequencePair].
 *
 * @param <S>
 * the base sequence type.
</S> */
class HashedSequenceComparator<S : Sequence> internal constructor(private val cmp: SequenceComparator<in S>) :
    SequenceComparator<HashedSequence<S>>() {

    override fun equals(
        a: HashedSequence<S>, ai: Int,  //
        b: HashedSequence<S>, bi: Int
    ): Boolean {
        return (a.hashes[ai] == b.hashes[bi]
            && cmp.equals(a.base, ai, b.base, bi))
    }

    override fun hash(seq: HashedSequence<S>, ptr: Int): Int {
        return seq.hashes[ptr]
    }
}
