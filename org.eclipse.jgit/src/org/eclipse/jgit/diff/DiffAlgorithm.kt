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

import org.eclipse.jgit.diff.EditList.Companion.singleton
import org.eclipse.jgit.diff.Subsequence.Companion.a
import org.eclipse.jgit.diff.Subsequence.Companion.b
import org.eclipse.jgit.diff.Subsequence.Companion.toBase

/**
 * Compares two [org.eclipse.jgit.diff.Sequence]s to create an
 * [org.eclipse.jgit.diff.EditList] of changes.
 *
 *
 * An algorithm's `diff` method must be callable from concurrent threads
 * without data collisions. This permits some algorithms to use a singleton
 * pattern, with concurrent invocations using the same singleton. Other
 * algorithms may support parameterization, in which case the caller can create
 * a unique instance per thread.
 */
abstract class DiffAlgorithm {
    /**
     * Supported diff algorithm
     */
    enum class SupportedAlgorithm {
        /**
         * Myers diff algorithm
         */
        MYERS,

        /**
         * Histogram diff algorithm
         */
        HISTOGRAM
    }

    /**
     * Compare two sequences and identify a list of edits between them.
     *
     * @param <S>
     * type of sequence being compared.
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
     * @return a modifiable edit list comparing the two sequences. If empty, the
     * sequences are identical according to `cmp`'s rules. The
     * result list is never null.
    </S> */
    fun <S : Sequence> diff(
        cmp: SequenceComparator<in S>, a: S, b: S
    ): EditList {
        val region = cmp.reduceCommonStartEnd(a, b, coverEdit(a, b))

        when (region!!.type) {
            Edit.Type.INSERT, Edit.Type.DELETE -> return singleton(region)

            Edit.Type.REPLACE -> {
                if (region.lengthA == 1 && region.lengthB == 1) return singleton(region)

                val cs = SubsequenceComparator(cmp)
                val `as` = a(a, region)
                val bs = b(b, region)
                val e: EditList = toBase<S>(diffNonCommon(cs, `as`, bs), `as`, bs)
                return normalize(cmp, e, a, b)
            }

            Edit.Type.EMPTY -> return EditList(0)

            else -> throw IllegalStateException()
        }
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
     * type of sequence being compared.
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
     * @return a modifiable edit list comparing the two sequences.
    </S> */
    abstract fun <S : Sequence> diffNonCommon(
        cmp: SequenceComparator<in S>, a: S, b: S
    ): EditList

    companion object {
        /**
         * Get diff algorithm
         *
         * @param alg
         * the diff algorithm for which an implementation should be
         * returned
         * @return an implementation of the specified diff algorithm
         */
		fun getAlgorithm(alg: SupportedAlgorithm?): DiffAlgorithm {
            return when (alg) {
                SupportedAlgorithm.MYERS -> MyersDiff.INSTANCE
                SupportedAlgorithm.HISTOGRAM -> HistogramDiff()
                else -> throw IllegalArgumentException()
            }
        }

        private fun <S : Sequence?> coverEdit(a: S, b: S): Edit {
            return Edit(0, a!!.size(), 0, b!!.size())
        }

        /**
         * Reorganize an [EditList] for better diff consistency.
         *
         *
         * `DiffAlgorithms` may return [Edit.Type.INSERT] or
         * [Edit.Type.DELETE] edits that can be "shifted". For
         * example, the deleted section
         * <pre>
         * -a
         * -b
         * -c
         * a
         * b
         * c
        </pre> *
         * can be shifted down by 1, 2 or 3 locations.
         *
         *
         * To avoid later merge issues, we shift such edits to a
         * consistent location. `normalize` uses a simple strategy of
         * shifting such edits to their latest possible location.
         *
         *
         * This strategy may not always produce an aesthetically pleasing
         * diff. For instance, it works well with
         * <pre>
         * function1 {
         * ...
         * }
         *
         * +function2 {
         * + ...
         * +}
         * +
         * function3 {
         * ...
         * }
        </pre> *
         * but less so for
         * <pre>
         * #
         * # comment1
         * #
         * function1() {
         * }
         *
         * #
         * +# comment3
         * +#
         * +function3() {
         * +}
         * +
         * +#
         * # comment2
         * #
         * function2() {
         * }
        </pre> *
         * [More
 * sophisticated strategies](https://github.com/mhagger/diff-slider-tools) are possible, say by calculating a
         * suitable "aesthetic cost" for each possible position and using
         * the lowest cost, but `normalize` just shifts edits
         * to the end as much as possible.
         *
         * @param <S>
         * type of sequence being compared.
         * @param cmp
         * the comparator supplying the element equivalence function.
         * @param e
         * a modifiable edit list comparing the provided sequences.
         * @param a
         * the first (also known as old or pre-image) sequence.
         * @param b
         * the second (also known as new or post-image) sequence.
         * @return a modifiable edit list with edit regions shifted to their
         * latest possible location. The result list is never null.
         * @since 4.7
        </S> */
        private fun <S : Sequence> normalize(
            cmp: SequenceComparator<in S>, e: EditList, a: S, b: S
        ): EditList {
            var prev: Edit? = null
            for (i in e.indices.reversed()) {
                val cur = e[i]
                val curType = cur.type

                val maxA = if ((prev == null)) a.size() else prev.beginA
                val maxB = if ((prev == null)) b.size() else prev.beginB

                if (curType == Edit.Type.INSERT) {
                    while (cur.endA < maxA && cur.endB < maxB && cmp.equals(b, cur.beginB, b, cur.endB)) {
                        cur.shift(1)
                    }
                } else if (curType == Edit.Type.DELETE) {
                    while (cur.endA < maxA && cur.endB < maxB && cmp.equals(a, cur.beginA, a, cur.endA)) {
                        cur.shift(1)
                    }
                }
                prev = cur
            }
            return e
        }
    }
}
