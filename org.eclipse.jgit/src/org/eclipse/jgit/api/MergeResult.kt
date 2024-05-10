/*
 * Copyright (C) 2010, Stefan Lay <stefan.lay@sap.com>
 * Copyright (C) 2010-2012, Christian Halstrick <christian.halstrick@sap.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.api

import org.eclipse.jgit.internal.JGitText
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.merge.MergeChunk.ConflictState
import org.eclipse.jgit.merge.MergeResult
import org.eclipse.jgit.merge.MergeStrategy
import org.eclipse.jgit.merge.ResolveMerger.MergeFailureReason
import java.text.MessageFormat

/**
 * Encapsulates the result of a [org.eclipse.jgit.api.MergeCommand].
 */
class MergeResult {
    /**
     * The status the merge resulted in.
     */
    enum class MergeStatus {
        /**
         * Merge is a fast-forward
         */
        FAST_FORWARD {
            override fun toString(): String {
                return "Fast-forward" //$NON-NLS-1$
            }

            override fun isSuccessful(): Boolean {
                return true
            }
        },

        /**
         * Merge is a fast-forward, squashed
         *
         * @since 2.0
         */
        FAST_FORWARD_SQUASHED {
            override fun toString(): String {
                return "Fast-forward-squashed" //$NON-NLS-1$
            }

            override fun isSuccessful(): Boolean {
                return true
            }
        },

        /**
         * Already up to date, merge was a no-op
         */
        ALREADY_UP_TO_DATE {
            override fun toString(): String {
                return "Already-up-to-date" //$NON-NLS-1$
            }

            override fun isSuccessful(): Boolean {
                return true
            }
        },

        /**
         * Merge failed
         */
        FAILED {
            override fun toString(): String {
                return "Failed" //$NON-NLS-1$
            }

            override fun isSuccessful(): Boolean {
                return false
            }
        },

        /**
         * Merged
         */
        MERGED {
            override fun toString(): String {
                return "Merged" //$NON-NLS-1$
            }

            override fun isSuccessful(): Boolean {
                return true
            }
        },

        /**
         * Merged, squashed, not updating HEAD
         *
         * @since 2.0
         */
        MERGED_SQUASHED {
            override fun toString(): String {
                return "Merged-squashed" //$NON-NLS-1$
            }

            override fun isSuccessful(): Boolean {
                return true
            }
        },

        /**
         * Merged, squashed, not committed
         *
         * @since 3.0
         */
        MERGED_SQUASHED_NOT_COMMITTED {
            override fun toString(): String {
                return "Merged-squashed-not-committed" //$NON-NLS-1$
            }

            override fun isSuccessful(): Boolean {
                return true
            }
        },

        /**
         * Merge raised conflicts to be resolved
         */
        CONFLICTING {
            override fun toString(): String {
                return "Conflicting" //$NON-NLS-1$
            }

            override fun isSuccessful(): Boolean {
                return false
            }
        },

        /**
         * Merge was aborted
         *
         * @since 2.2
         */
        ABORTED {
            override fun toString(): String {
                return "Aborted" //$NON-NLS-1$
            }

            override fun isSuccessful(): Boolean {
                return false
            }
        },

        /**
         * Merged, not committed
         *
         * @since 3.0
         */
        MERGED_NOT_COMMITTED {
            override fun toString(): String {
                return "Merged-not-committed" //$NON-NLS-1$
            }

            override fun isSuccessful(): Boolean {
                return true
            }
        },

        /** Not yet supported  */
        NOT_SUPPORTED {
            override fun toString(): String {
                return "Not-yet-supported" //$NON-NLS-1$
            }

            override fun isSuccessful(): Boolean {
                return false
            }
        },

        /**
         * Status representing a checkout conflict, meaning that nothing could
         * be merged, as the pre-scan for the trees already failed for certain
         * files (i.e. local modifications prevent checkout of files).
         */
        CHECKOUT_CONFLICT {
            override fun toString(): String {
                return "Checkout Conflict" //$NON-NLS-1$
            }

            override fun isSuccessful(): Boolean {
                return false
            }
        };

        /**
         * Whether the merge was successful
         *
         * @return whether the status indicates a successful result
         */
        abstract fun isSuccessful(): Boolean
    }

    /**
     * Get the commits which have been merged
     *
     * @return all the commits which have been merged together
     */
    var mergedCommits: Array<ObjectId> = emptyArray()
        private set

    /**
     * Get the common base
     *
     * @return base the common base which was used to produce a content-merge.
     * May be `null` if the merge-result was produced without
     * computing a common base
     */
    var base: ObjectId? = null
        private set

    /**
     * Get the object the head points at after the merge
     *
     * @return the object the head points at after the merge
     */
    var newHead: ObjectId? = null
        private set

    private var conflicts: MutableMap<String?, Array<IntArray>>? = null

    /**
     * Get the merge status
     *
     * @return the status the merge resulted in
     */
    var mergeStatus: MergeStatus?
        private set

    private var description: String? = null

    private var mergeStrategy: MergeStrategy? = null

    /**
     * Returns a list of paths causing this merge to fail as returned by
     * [org.eclipse.jgit.merge.ResolveMerger.getFailingPaths]
     *
     * @return the list of paths causing this merge to fail or `null`
     * if no failure occurred
     */
    var failingPaths: Map<String, MergeFailureReason>? = null
        private set

    /**
     * Returns a list of paths that cause a checkout conflict. These paths
     * prevent the operation from even starting.
     *
     * @return the list of files that caused the checkout conflict.
     */
    var checkoutConflicts: List<String>? = null
        private set

    /**
     * Constructor for MergeResult.
     *
     * @param newHead
     * the object the head points at after the merge
     * @param base
     * the common base which was used to produce a content-merge. May
     * be `null` if the merge-result was produced without
     * computing a common base
     * @param mergedCommits
     * all the commits which have been merged together
     * @param mergeStatus
     * the status the merge resulted in
     * @param mergeStrategy
     * the used [org.eclipse.jgit.merge.MergeStrategy]
     * @param lowLevelResults
     * merge results as returned by
     * [org.eclipse.jgit.merge.ResolveMerger.getMergeResults]
     * @param description
     * a user friendly description of the merge result
     */
    /**
     * Constructor for MergeResult.
     *
     * @param newHead
     * the object the head points at after the merge
     * @param base
     * the common base which was used to produce a content-merge. May
     * be `null` if the merge-result was produced without
     * computing a common base
     * @param mergedCommits
     * all the commits which have been merged together
     * @param mergeStatus
     * the status the merge resulted in
     * @param mergeStrategy
     * the used [org.eclipse.jgit.merge.MergeStrategy]
     * @param lowLevelResults
     * merge results as returned by
     * [org.eclipse.jgit.merge.ResolveMerger.getMergeResults]
     * @since 2.0
     */
    @JvmOverloads
    constructor(
        newHead: ObjectId?, base: ObjectId?,
        mergedCommits: Array<ObjectId>, mergeStatus: MergeStatus?,
        mergeStrategy: MergeStrategy?,
        lowLevelResults: Map<String?, MergeResult<*>>?,
        description: String? = null
    ) : this(
        newHead, base, mergedCommits, mergeStatus, mergeStrategy,
        lowLevelResults, null, description
    )

    /**
     * Constructor for MergeResult.
     *
     * @param newHead
     * the object the head points at after the merge
     * @param base
     * the common base which was used to produce a content-merge. May
     * be `null` if the merge-result was produced without
     * computing a common base
     * @param mergedCommits
     * all the commits which have been merged together
     * @param mergeStatus
     * the status the merge resulted in
     * @param mergeStrategy
     * the used [org.eclipse.jgit.merge.MergeStrategy]
     * @param lowLevelResults
     * merge results as returned by
     * [org.eclipse.jgit.merge.ResolveMerger.getMergeResults]
     * @param failingPaths
     * list of paths causing this merge to fail as returned by
     * [org.eclipse.jgit.merge.ResolveMerger.getFailingPaths]
     * @param description
     * a user friendly description of the merge result
     */
    constructor(
        newHead: ObjectId?, base: ObjectId?,
        mergedCommits: Array<ObjectId>, mergeStatus: MergeStatus?,
        mergeStrategy: MergeStrategy?,
        lowLevelResults: Map<String?, MergeResult<*>>?,
        failingPaths: Map<String, MergeFailureReason>?, description: String?
    ) {
        this.newHead = newHead
        this.mergedCommits = mergedCommits
        this.base = base
        this.mergeStatus = mergeStatus
        this.mergeStrategy = mergeStrategy
        this.description = description
        this.failingPaths = failingPaths
        if (lowLevelResults != null) for ((key, value) in lowLevelResults) addConflict(key, value)
    }

    /**
     * Creates a new result that represents a checkout conflict before the
     * operation even started for real.
     *
     * @param checkoutConflicts
     * the conflicting files
     */
    constructor(checkoutConflicts: List<String>?) {
        this.checkoutConflicts = checkoutConflicts
        this.mergeStatus = MergeStatus.CHECKOUT_CONFLICT
    }

    override fun toString(): String {
        var first = true
        val commits = StringBuilder()
        for (commit in mergedCommits) {
            if (!first) commits.append(", ")
            else first = false
            commits.append(ObjectId.toString(commit))
        }
        return MessageFormat.format(
            JGitText.get().mergeUsingStrategyResultedInDescription,
            commits, ObjectId.toString(base), mergeStrategy!!.name,
            mergeStatus, (if (description == null) "" else ", $description")
        )
    }

    /**
     * Set conflicts
     *
     * @param conflicts
     * the conflicts to set
     */
    fun setConflicts(conflicts: MutableMap<String?, Array<IntArray>>?) {
        this.conflicts = conflicts
    }

    /**
     * Add a conflict
     *
     * @param path
     * path of the file to add a conflict for
     * @param conflictingRanges
     * the conflicts to set
     */
    fun addConflict(path: String?, conflictingRanges: Array<IntArray>) {
        if (conflicts == null) conflicts = HashMap()
        conflicts!![path] = conflictingRanges
    }

    /**
     * Add a conflict
     *
     * @param path
     * path of the file to add a conflict for
     * @param lowLevelResult
     * a [org.eclipse.jgit.merge.MergeResult]
     */
    fun addConflict(path: String?, lowLevelResult: MergeResult<*>) {
        if (!lowLevelResult.containsConflicts()) return
        if (conflicts == null) conflicts = HashMap()
        var nrOfConflicts = 0
        // just counting
        for (mergeChunk in lowLevelResult) {
            if (mergeChunk.conflictState == ConflictState.FIRST_CONFLICTING_RANGE) {
                nrOfConflicts++
            }
        }
        var currentConflict = -1
        val ret = Array(nrOfConflicts) { IntArray(mergedCommits.size + 1) }
        for (mergeChunk in lowLevelResult) {
            // to store the end of this chunk (end of the last conflicting range)
            var endOfChunk = 0
            if (mergeChunk.conflictState == ConflictState.FIRST_CONFLICTING_RANGE) {
                if (currentConflict > -1) {
                    // there was a previous conflicting range for which the end
                    // is not set yet - set it!
                    ret[currentConflict][mergedCommits.size] = endOfChunk
                }
                currentConflict++
                endOfChunk = mergeChunk.end
                ret[currentConflict][mergeChunk.sequenceIndex] = mergeChunk.begin
            }
            if (mergeChunk.conflictState == ConflictState.NEXT_CONFLICTING_RANGE) {
                if (mergeChunk.end > endOfChunk) endOfChunk = mergeChunk.end
                ret[currentConflict][mergeChunk.sequenceIndex] = mergeChunk.begin
            }
        }
        conflicts!![path] = ret
    }

    /**
     * Returns information about the conflicts which occurred during a
     * [org.eclipse.jgit.api.MergeCommand]. The returned value maps the
     * path of a conflicting file to a two-dimensional int-array of line-numbers
     * telling where in the file conflict markers for which merged commit can be
     * found.
     *
     *
     * If the returned value contains a mapping "path"-&gt;[x][y]=z then this
     * means
     *
     *  * the file with path "path" contains conflicts
     *  * if y &lt; "number of merged commits": for conflict number x in this
     * file the chunk which was copied from commit number y starts on line
     * number z. All numberings and line numbers start with 0.
     *  * if y == "number of merged commits": the first non-conflicting line
     * after conflict number x starts at line number z
     *
     *
     *
     * Example code how to parse this data:
     *
     * <pre>
     * MergeResult m=...;
     * Map&lt;String, int[][]&gt; allConflicts = m.getConflicts();
     * for (String path : allConflicts.keySet()) {
     * int[][] c = allConflicts.get(path);
     * System.out.println("Conflicts in file " + path);
     * for (int i = 0; i &lt; c.length; ++i) {
     * System.out.println("  Conflict #" + i);
     * for (int j = 0; j &lt; (c[i].length) - 1; ++j) {
     * if (c[i][j] &gt;= 0)
     * System.out.println("    Chunk for "
     * + m.getMergedCommits()[j] + " starts on line #"
     * + c[i][j]);
     * }
     * }
     * }
    </pre> *
     *
     * @return the conflicts or `null` if no conflict occurred
     */
    fun getConflicts(): Map<String?, Array<IntArray>>? {
        return conflicts
    }
}
