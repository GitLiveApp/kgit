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
 * Compares two sequences primarily based upon hash codes.
 */
abstract class LowLevelDiffAlgorithm : DiffAlgorithm() {
    override fun <S : Sequence> diffNonCommon(
        cmp: SequenceComparator<in S>, a: S, b: S
    ): EditList {
        var p: HashedSequencePair<S>? = HashedSequencePair(cmp, a, b)
        val hc = p!!.comparator
        val ha = p.a
        val hb = p.b
        p = null

        val res = EditList()
        val region = Edit(0, a.size(), 0, b.size())
        diffNonCommon(res, hc, ha, hb, region)
        return res
    }

    /**
     * Compare two sequences and identify a list of edits between them.
     *
     * This method should be invoked only after the two sequences have been
     * proven to have no common starting or ending elements. The expected
     * elimination of common starting and ending elements is automatically
     * performed by the [.diff]
     * method, which invokes this method using
     * [org.eclipse.jgit.diff.Subsequence]s.
     *
     * @param <S>
     * type of Sequence compared
     * @param edits
     * result list to append the region's edits onto.
     * @param cmp
     * the comparator supplying the element equivalence function.
     * @param a
     * the first (also known as old or pre-image) sequence. Edits
     * returned by this algorithm will reference indexes using the
     * 'A' side: [org.eclipse.jgit.diff.Edit.getBeginA],
     * [org.eclipse.jgit.diff.Edit.getEndA].
     * @param b
     * the second (also known as new or post-image) sequence. Edits
     * returned by this algorithm will reference indexes using the
     * 'B' side: [org.eclipse.jgit.diff.Edit.getBeginB],
     * [org.eclipse.jgit.diff.Edit.getEndB].
     * @param region
     * the region being compared within the two sequences.
    </S> */
    abstract fun <S : Sequence> diffNonCommon(
        edits: EditList?,
        cmp: HashedSequenceComparator<S>?, a: HashedSequence<S>?,
        b: HashedSequence<S>?, region: Edit?
    )
}
