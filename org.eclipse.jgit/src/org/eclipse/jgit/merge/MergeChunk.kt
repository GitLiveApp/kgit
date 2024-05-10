/*
 * Copyright (C) 2009, Christian Halstrick <christian.halstrick@sap.com>
 * Copyright (C) 2010, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.merge

/**
 * One chunk from a merge result. Each chunk contains a range from a
 * single sequence. In case of conflicts multiple chunks are reported for one
 * conflict. The conflictState tells when conflicts start and end.
 */
class MergeChunk
/**
 * Creates a new empty MergeChunk
 *
 * @param sequenceIndex
 * determines to which sequence this chunks belongs to. Same as
 * in [org.eclipse.jgit.merge.MergeResult.add]
 * @param begin
 * the first element from the specified sequence which should be
 * included in the merge result. Indexes start with 0.
 * @param end
 * specifies the end of the range to be added. The element this
 * index points to is the first element which not added to the
 * merge result. All elements between begin (including begin) and
 * this element are added.
 * @param conflictState
 * the state of this chunk. See
 * [org.eclipse.jgit.merge.MergeChunk.ConflictState]
 */(
	/**
     * Get the index of the sequence to which this sequence chunks belongs to.
     *
     * @return the index of the sequence to which this sequence chunks belongs
     * to. Same as in [org.eclipse.jgit.merge.MergeResult.add]
     */
	@JvmField val sequenceIndex: Int,
	/**
     * Get the first element from the specified sequence which should be
     * included in the merge result.
     *
     * @return the first element from the specified sequence which should be
     * included in the merge result. Indexes start with 0.
     */
	@JvmField val begin: Int,
	/**
     * Get the end of the range of this chunk.
     *
     * @return the end of the range of this chunk. The element this index points
     * to is the first element which not added to the merge result. All
     * elements between begin (including begin) and this element are
     * added.
     */
	@JvmField val end: Int,
	/**
     * Get the state of this chunk.
     *
     * @return the state of this chunk. See
     * [org.eclipse.jgit.merge.MergeChunk.ConflictState]
     */
	@JvmField val conflictState: ConflictState
) {
    /**
     * A state telling whether a MergeChunk belongs to a conflict or not. The
     * first chunk of a conflict is reported with a special state to be able to
     * distinguish the border between two consecutive conflicts
     */
    enum class ConflictState {
        /**
         * This chunk does not belong to a conflict
         */
        NO_CONFLICT,

        /**
         * This chunk does belong to a conflict and is the ours section of the
         * conflicting chunks
         */
        FIRST_CONFLICTING_RANGE,

        /**
         * This chunk does belong to a conflict and is the base section of the
         * conflicting chunks
         *
         * @since 6.7
         */
        BASE_CONFLICTING_RANGE,

        /**
         * This chunk does belong to a conflict and is the theirs section of
         * the conflicting chunks. It's a subsequent one.
         */
        NEXT_CONFLICTING_RANGE
    }
}
