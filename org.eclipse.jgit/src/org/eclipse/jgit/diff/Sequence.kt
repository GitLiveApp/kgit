/*
 * Copyright (C) 2010, Google Inc.
 * Copyright (C) 2008-2009, Johannes E. Schindelin <johannes.schindelin@gmx.de> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.diff

/**
 * Arbitrary sequence of elements.
 *
 *
 * A sequence of elements is defined to contain elements in the index range
 * `[0, [.size])`, like a standard Java List implementation.
 * Unlike a List, the members of the sequence are not directly obtainable.
 *
 *
 * Implementations of Sequence are primarily intended for use in content
 * difference detection algorithms, to produce an
 * [org.eclipse.jgit.diff.EditList] of [org.eclipse.jgit.diff.Edit]
 * instances describing how two Sequence instances differ.
 *
 *
 * To be compared against another Sequence of the same type, a supporting
 * [org.eclipse.jgit.diff.SequenceComparator] must also be supplied.
 */
abstract class Sequence {
    /** @return total number of items in the sequence.
     */
    /**
     * Get size
     *
     * @return size
     */
    abstract fun size(): Int
}
