/*
 * Copyright (C) 2009, Christian Halstrick <christian.halstrick@sap.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.merge

//import org.eclipse.jgit.annotations.NonNull
import org.eclipse.jgit.diff.*
import org.eclipse.jgit.merge.MergeChunk.ConflictState
import org.jetbrains.annotations.NotNull
import kotlin.math.max
import kotlin.math.min

/**
 * Provides the merge algorithm which does a three-way merge on content provided
 * as RawText. By default [org.eclipse.jgit.diff.HistogramDiff] is used as
 * diff algorithm.
 */
class MergeAlgorithm
/**
 * Creates a new MergeAlgorithm which uses
 * [org.eclipse.jgit.diff.HistogramDiff] as diff algorithm
 */ @JvmOverloads constructor(private val diffAlg: DiffAlgorithm = HistogramDiff()) {
    @NotNull
    private var strategy = ContentMergeStrategy.CONFLICT

    @get:NotNull
    var contentMergeStrategy: ContentMergeStrategy?
        /**
         * Retrieves the [ContentMergeStrategy].
         *
         * @return the [ContentMergeStrategy] in effect
         * @since 5.12
         */
        get() = strategy
        /**
         * Sets the [ContentMergeStrategy].
         *
         * @param strategy
         * [ContentMergeStrategy] to set; if `null`, set
         * [ContentMergeStrategy.CONFLICT]
         * @since 5.12
         */
        set(strategy) {
            this.strategy = strategy ?: ContentMergeStrategy.CONFLICT
        }

    /**
     * Creates a new MergeAlgorithm
     *
     * @param diffAlg
     * the diff algorithm used by this merge
     */

    /**
     * Does the three way merge between a common base and two sequences.
     *
     * @param <S>
     * type of the sequences
     * @param cmp
     * comparison method for this execution.
     * @param base
     * the common base sequence
     * @param ours
     * the first sequence to be merged
     * @param theirs
     * the second sequence to be merged
     * @return the resulting content
    </S> */
    fun <S : Sequence> merge(
        cmp: SequenceComparator<S>, base: S, ours: S, theirs: S
    ): MergeResult<S> {
        val sequences: MutableList<S> = ArrayList(3)
        sequences.add(base)
        sequences.add(ours)
        sequences.add(theirs)
        val result = MergeResult(sequences)

        if (ours.size() == 0) {
            if (theirs.size() != 0) {
                val theirsEdits = diffAlg.diff(cmp, base, theirs)
                if (!theirsEdits.isEmpty()) {
                    // we deleted, they modified
                    when (strategy) {
                        ContentMergeStrategy.OURS -> result.add(1, 0, 0, ConflictState.NO_CONFLICT)
                        ContentMergeStrategy.THEIRS -> result.add(
                            2, 0, theirs.size(),
                            ConflictState.NO_CONFLICT
                        )

                        else -> {
                            // Let their complete content conflict with empty text
                            result.add(
                                1, 0, 0,
                                ConflictState.FIRST_CONFLICTING_RANGE
                            )
                            result.add(
                                0, 0, base!!.size(),
                                ConflictState.BASE_CONFLICTING_RANGE
                            )
                            result.add(
                                2, 0, theirs.size(),
                                ConflictState.NEXT_CONFLICTING_RANGE
                            )
                        }
                    }
                } else {
                    // we deleted, they didn't modify -> Let our deletion win
                    result.add(1, 0, 0, ConflictState.NO_CONFLICT)
                }
            } else {
                // we and they deleted -> return a single chunk of nothing
                result.add(1, 0, 0, ConflictState.NO_CONFLICT)
            }
            return result
        } else if (theirs!!.size() == 0) {
            val oursEdits = diffAlg.diff(cmp, base, ours)
            if (!oursEdits.isEmpty()) {
                // we modified, they deleted
                when (strategy) {
                    ContentMergeStrategy.OURS -> result.add(1, 0, ours.size(), ConflictState.NO_CONFLICT)
                    ContentMergeStrategy.THEIRS -> result.add(2, 0, 0, ConflictState.NO_CONFLICT)
                    else -> {
                        // Let our complete content conflict with empty text
                        result.add(
                            1, 0, ours.size(),
                            ConflictState.FIRST_CONFLICTING_RANGE
                        )
                        result.add(
                            0, 0, base!!.size(),
                            ConflictState.BASE_CONFLICTING_RANGE
                        )
                        result.add(2, 0, 0, ConflictState.NEXT_CONFLICTING_RANGE)
                    }
                }
            } else {
                // they deleted, we didn't modify -> Let their deletion win
                result.add(2, 0, 0, ConflictState.NO_CONFLICT)
            }
            return result
        }

        val oursEdits = diffAlg.diff(cmp, base, ours)
        val baseToOurs: Iterator<Edit> = oursEdits.iterator()
        val theirsEdits = diffAlg.diff(cmp, base, theirs)
        val baseToTheirs: Iterator<Edit> = theirsEdits.iterator()
        var current = 0 // points to the next line (first line is 0) of base
        // which was not handled yet
        var oursEdit = nextEdit(baseToOurs)
        var theirsEdit = nextEdit(baseToTheirs)

        // iterate over all edits from base to ours and from base to theirs
        // leave the loop when there are no edits more for ours or for theirs
        // (or both)
        while (!isEndEdit(theirsEdit) || !isEndEdit(oursEdit)) {
            if (oursEdit.endA < theirsEdit.beginA) {
                // something was changed in ours not overlapping with any change
                // from theirs. First add the common part in front of the edit
                // then the edit.
                if (current != oursEdit.beginA) {
                    result.add(
                        0, current, oursEdit.beginA,
                        ConflictState.NO_CONFLICT
                    )
                }
                result.add(
                    1, oursEdit.beginB, oursEdit.endB,
                    ConflictState.NO_CONFLICT
                )
                current = oursEdit.endA
                oursEdit = nextEdit(baseToOurs)
            } else if (theirsEdit.endA < oursEdit.beginA) {
                // something was changed in theirs not overlapping with any
                // from ours. First add the common part in front of the edit
                // then the edit.
                if (current != theirsEdit.beginA) {
                    result.add(
                        0, current, theirsEdit.beginA,
                        ConflictState.NO_CONFLICT
                    )
                }
                result.add(
                    2, theirsEdit.beginB, theirsEdit.endB,
                    ConflictState.NO_CONFLICT
                )
                current = theirsEdit.endA
                theirsEdit = nextEdit(baseToTheirs)
            } else {
                // here we found a real overlapping modification

                // if there is a common part in front of the conflict add it

                if (oursEdit.beginA != current
                    && theirsEdit.beginA != current
                ) {
                    result.add(
                        0, current, min(
                            oursEdit.beginA.toDouble(),
                            theirsEdit.beginA.toDouble()
                        ).toInt(), ConflictState.NO_CONFLICT
                    )
                }

                // set some initial values for the ranges in A and B which we
                // want to handle
                var oursBeginA = oursEdit.beginA
                var theirsBeginA = theirsEdit.beginA
                var oursBeginB = oursEdit.beginB
                var theirsBeginB = theirsEdit.beginB
                // harmonize the start of the ranges in A and B
                if (oursEdit.beginA < theirsEdit.beginA) {
                    theirsBeginA -= (theirsEdit.beginA
                        - oursEdit.beginA)
                    theirsBeginB -= (theirsEdit.beginA
                        - oursEdit.beginA)
                } else {
                    oursBeginA -= oursEdit.beginA - theirsEdit.beginA
                    oursBeginB -= oursEdit.beginA - theirsEdit.beginA
                }

                // combine edits:
                // Maybe an Edit on one side corresponds to multiple Edits on
                // the other side. Then we have to combine the Edits of the
                // other side - so in the end we can merge together two single
                // edits.
                //
                // It is important to notice that this combining will extend the
                // ranges of our conflict always downwards (towards the end of
                // the content). The starts of the conflicting ranges in ours
                // and theirs are not touched here.
                //
                // This combining is an iterative process: after we have
                // combined some edits we have to do the check again. The
                // combined edits could now correspond to multiple edits on the
                // other side.
                //
                // Example: when this combining algorithm works on the following
                // edits
                // oursEdits=((0-5,0-5),(6-8,6-8),(10-11,10-11)) and
                // theirsEdits=((0-1,0-1),(2-3,2-3),(5-7,5-7))
                // it will merge them into
                // oursEdits=((0-8,0-8),(10-11,10-11)) and
                // theirsEdits=((0-7,0-7))
                //
                // Since the only interesting thing to us is how in ours and
                // theirs the end of the conflicting range is changing we let
                // oursEdit and theirsEdit point to the last conflicting edit
                var nextOursEdit = nextEdit(baseToOurs)
                var nextTheirsEdit = nextEdit(baseToTheirs)
                while (true) {
                    if (oursEdit.endA >= nextTheirsEdit.beginA) {
                        theirsEdit = nextTheirsEdit
                        nextTheirsEdit = nextEdit(baseToTheirs)
                    } else if (theirsEdit.endA >= nextOursEdit.beginA) {
                        oursEdit = nextOursEdit
                        nextOursEdit = nextEdit(baseToOurs)
                    } else {
                        break
                    }
                }

                // harmonize the end of the ranges in A and B
                var oursEndA = oursEdit.endA
                var theirsEndA = theirsEdit.endA
                var oursEndB = oursEdit.endB
                var theirsEndB = theirsEdit.endB
                if (oursEdit.endA < theirsEdit.endA) {
                    oursEndA += theirsEdit.endA - oursEdit.endA
                    oursEndB += theirsEdit.endA - oursEdit.endA
                } else {
                    theirsEndA += oursEdit.endA - theirsEdit.endA
                    theirsEndB += oursEdit.endA - theirsEdit.endA
                }

                // A conflicting region is found. Strip off common lines in
                // in the beginning and the end of the conflicting region

                // Determine the minimum length of the conflicting areas in OURS
                // and THEIRS. Also determine how much bigger the conflicting
                // area in THEIRS is compared to OURS. All that is needed to
                // limit the search for common areas at the beginning or end
                // (the common areas cannot be bigger then the smaller
                // conflicting area. The delta is needed to know whether the
                // complete conflicting area is common in OURS and THEIRS.
                var minBSize = oursEndB - oursBeginB
                val BSizeDelta = minBSize - (theirsEndB - theirsBeginB)
                if (BSizeDelta > 0) minBSize -= BSizeDelta

                var commonPrefix = 0
                while (commonPrefix < minBSize
                    && cmp.equals(
                        ours, oursBeginB + commonPrefix, theirs,
                        theirsBeginB + commonPrefix
                    )
                ) commonPrefix++
                minBSize -= commonPrefix
                var commonSuffix = 0
                while (commonSuffix < minBSize
                    && cmp.equals(
                        ours, oursEndB - commonSuffix - 1, theirs,
                        theirsEndB - commonSuffix - 1
                    )
                ) commonSuffix++
                minBSize -= commonSuffix

                // Add the common lines at start of conflict
                if (commonPrefix > 0) result.add(
                    1, oursBeginB, oursBeginB + commonPrefix,
                    ConflictState.NO_CONFLICT
                )

                // Add the conflict (Only if there is a conflict left to report)
                if (minBSize > 0 || BSizeDelta != 0) {
                    when (strategy) {
                        ContentMergeStrategy.OURS -> result.add(
                            1, oursBeginB + commonPrefix,
                            oursEndB - commonSuffix,
                            ConflictState.NO_CONFLICT
                        )

                        ContentMergeStrategy.THEIRS -> result.add(
                            2, theirsBeginB + commonPrefix,
                            theirsEndB - commonSuffix,
                            ConflictState.NO_CONFLICT
                        )

                        else -> {
                            result.add(
                                1, oursBeginB + commonPrefix,
                                oursEndB - commonSuffix,
                                ConflictState.FIRST_CONFLICTING_RANGE
                            )

                            val baseBegin = (min(oursBeginA.toDouble(), theirsBeginA.toDouble()) + commonPrefix).toInt()
                            val baseEnd = (min(
                                base!!.size().toDouble(),
                                max(oursEndA.toDouble(), theirsEndA.toDouble())
                            ) - commonSuffix).toInt()
                            result.add(
                                0, baseBegin, baseEnd,
                                ConflictState.BASE_CONFLICTING_RANGE
                            )

                            result.add(
                                2, theirsBeginB + commonPrefix,
                                theirsEndB - commonSuffix,
                                ConflictState.NEXT_CONFLICTING_RANGE
                            )
                        }
                    }
                }

                // Add the common lines at end of conflict
                if (commonSuffix > 0) result.add(
                    1, oursEndB - commonSuffix, oursEndB,
                    ConflictState.NO_CONFLICT
                )

                current = max(oursEdit.endA.toDouble(), theirsEdit.endA.toDouble()).toInt()
                oursEdit = nextOursEdit
                theirsEdit = nextTheirsEdit
            }
        }
        // maybe we have a common part behind the last edit: copy it to the
        // result
        if (current < base!!.size()) {
            result.add(0, current, base.size(), ConflictState.NO_CONFLICT)
        }
        return result
    }

    companion object {
        // An special edit which acts as a sentinel value by marking the end the
        // list of edits
        private val END_EDIT = Edit(
            Int.MAX_VALUE,
            Int.MAX_VALUE
        )

        private fun isEndEdit(edit: Edit): Boolean {
            return edit === END_EDIT
        }

        /**
         * Helper method which returns the next Edit for an Iterator over Edits.
         * When there are no more edits left this method will return the constant
         * END_EDIT.
         *
         * @param it
         * the iterator for which the next edit should be returned
         * @return the next edit from the iterator or END_EDIT if there no more
         * edits
         */
        private fun nextEdit(it: Iterator<Edit>): Edit {
            return (if (it.hasNext()) it.next() else END_EDIT)
        }
    }
}
