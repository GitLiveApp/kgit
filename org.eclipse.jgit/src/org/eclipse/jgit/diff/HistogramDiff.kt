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

import org.eclipse.jgit.diff.Subsequence.Companion.a
import org.eclipse.jgit.diff.Subsequence.Companion.b
import org.eclipse.jgit.diff.Subsequence.Companion.toBase

/**
 * An extended form of Bram Cohen's patience diff algorithm.
 *
 *
 * This implementation was derived by using the 4 rules that are outlined in
 * Bram Cohen's [blog](http://bramcohen.livejournal.com/73318.html),
 * and then was further extended to support low-occurrence common elements.
 *
 *
 * The basic idea of the algorithm is to create a histogram of occurrences for
 * each element of sequence A. Each element of sequence B is then considered in
 * turn. If the element also exists in sequence A, and has a lower occurrence
 * count, the positions are considered as a candidate for the longest common
 * subsequence (LCS). After scanning of B is complete the LCS that has the
 * lowest number of occurrences is chosen as a split point. The region is split
 * around the LCS, and the algorithm is recursively applied to the sections
 * before and after the LCS.
 *
 *
 * By always selecting a LCS position with the lowest occurrence count, this
 * algorithm behaves exactly like Bram Cohen's patience diff whenever there is a
 * unique common element available between the two sequences. When no unique
 * elements exist, the lowest occurrence element is chosen instead. This offers
 * more readable diffs than simply falling back on the standard Myers' O(ND)
 * algorithm would produce.
 *
 *
 * To prevent the algorithm from having an O(N^2) running time, an upper limit
 * on the number of unique elements in a histogram bucket is configured by
 * [.setMaxChainLength]. If sequence A has more than this many
 * elements that hash into the same hash bucket, the algorithm passes the region
 * to [.setFallbackAlgorithm]. If no fallback algorithm is
 * configured, the region is emitted as a replace edit.
 *
 *
 * During scanning of sequence B, any element of A that occurs more than
 * [.setMaxChainLength] times is never considered for an LCS match
 * position, even if it is common between the two sequences. This limits the
 * number of locations in sequence A that must be considered to find the LCS,
 * and helps maintain a lower running time bound.
 *
 *
 * So long as [.setMaxChainLength] is a small constant (such as 64),
 * the algorithm runs in O(N * D) time, where N is the sum of the input lengths
 * and D is the number of edits in the resulting EditList. If the supplied
 * [org.eclipse.jgit.diff.SequenceComparator] has a good hash function,
 * this implementation typically out-performs
 * [org.eclipse.jgit.diff.MyersDiff], even though its theoretical running
 * time is the same.
 *
 *
 * This implementation has an internal limitation that prevents it from handling
 * sequences with more than 268,435,456 (2^28) elements.
 */
class HistogramDiff : LowLevelDiffAlgorithm() {
    /** Algorithm to use when there are too many element occurrences.  */
    var fallback: DiffAlgorithm? = MyersDiff.INSTANCE

    /**
     * Maximum number of positions to consider for a given element hash.
     *
     * All elements with the same hash are stored into a single chain. The chain
     * size is capped to ensure search is linear time at O(len_A + len_B) rather
     * than quadratic at O(len_A * len_B).
     */
    private var maxChainLength: Int = 64

    /**
     * Set the algorithm used when there are too many element occurrences.
     *
     * @param alg
     * the secondary algorithm. If null the region will be denoted as
     * a single REPLACE block.
     */
    fun setFallbackAlgorithm(alg: DiffAlgorithm?) {
        fallback = alg
    }

    /**
     * Maximum number of positions to consider for a given element hash.
     *
     * All elements with the same hash are stored into a single chain. The chain
     * size is capped to ensure search is linear time at O(len_A + len_B) rather
     * than quadratic at O(len_A * len_B).
     *
     * @param maxLen
     * new maximum length.
     */
    fun setMaxChainLength(maxLen: Int) {
        maxChainLength = maxLen
    }

    override fun <S : Sequence> diffNonCommon(
        edits: EditList,
        cmp: HashedSequenceComparator<S>, a: HashedSequence<S>,
        b: HashedSequence<S>, region: Edit
    ) {
        State<S>(edits, cmp, a, b).diffRegion(region)
    }

    private inner class State<S : Sequence>(
        /** Result edits we have determined that must be made to convert a to b.  */
        val edits: EditList, private val cmp: HashedSequenceComparator<S>,
        private val a: HashedSequence<S>, private val b: HashedSequence<S>
    ) {
        private val queue: MutableList<Edit> = ArrayList()

        fun diffRegion(r: Edit) {
            diffReplace(r)
            while (!queue.isEmpty()) diff(queue.removeAt(queue.size - 1))
        }

        private fun diffReplace(r: Edit) {
            val lcs = HistogramDiffIndex(maxChainLength, cmp, a, b, r)
                .findLongestCommonSequence()
            if (lcs != null) {
                // If we were given an edit, we can prove a result here.
                //
                if (lcs.isEmpty) {
                    // An empty edit indicates there is nothing in common.
                    // Replace the entire region.
                    //
                    edits.add(r)
                } else {
                    queue.add(r.after(lcs))
                    queue.add(r.before(lcs))
                }
            } else if (fallback is LowLevelDiffAlgorithm) {
                val fb = fallback as LowLevelDiffAlgorithm
                fb.diffNonCommon(edits, cmp, a, b, r)
            } else if (fallback != null) {
                val cs = subcmp()
                val `as` = a(a, r)
                val bs = b(b, r)

                val res = fallback!!.diffNonCommon(cs, `as`, bs)
                edits.addAll(toBase(res, `as`, bs))
            } else {
                edits.add(r)
            }
        }

        private fun diff(r: Edit) {
            when (r.type) {
                Edit.Type.INSERT, Edit.Type.DELETE -> edits.add(r)
                Edit.Type.REPLACE -> if (r.lengthA == 1 && r.lengthB == 1) edits.add(r)
                else diffReplace(r)

                Edit.Type.EMPTY -> throw IllegalStateException()
            }
        }

        private fun subcmp(): SubsequenceComparator<HashedSequence<S>> {
            return SubsequenceComparator(cmp)
        }
    }
}
