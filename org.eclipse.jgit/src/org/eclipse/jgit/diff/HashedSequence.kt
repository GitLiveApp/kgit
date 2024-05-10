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
 * Wraps a [org.eclipse.jgit.diff.Sequence] to assign hash codes to
 * elements.
 *
 *
 * This sequence acts as a proxy for the real sequence, caching element hash
 * codes so they don't need to be recomputed each time. Sequences of this type
 * must be used with a [org.eclipse.jgit.diff.HashedSequenceComparator].
 *
 *
 * To construct an instance of this type use
 * [org.eclipse.jgit.diff.HashedSequencePair].
 *
 * @param <S>
 * the base sequence type.
</S> */
class HashedSequence<S : Sequence> internal constructor(val base: S, val hashes: IntArray) : Sequence() {
    override fun size(): Int {
        return base.size()
    }
}
