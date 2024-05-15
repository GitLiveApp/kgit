/*
 * Copyright (C) 2013, Christian Halstrick <christian.halstrick@sap.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.errors

import org.eclipse.jgit.internal.JGitText
import java.io.IOException
import java.text.MessageFormat

/**
 * Exception thrown if a merge fails because no merge base could be determined.
 *
 * @since 3.0
 */
class NoMergeBaseException : IOException {
    /**
     * Get the reason why no merge base could be found
     *
     * @return the reason why no merge base could be found
     */
    var reason: MergeBaseFailureReason
        private set

    /**
     * An enum listing the different reason why no merge base could be
     * determined.
     */
    enum class MergeBaseFailureReason {
        /**
         * Multiple merge bases have been found (e.g. the commits to be merged
         * have multiple common predecessors) but the merge strategy doesn't
         * support this (e.g. ResolveMerge)
         */
        MULTIPLE_MERGE_BASES_NOT_SUPPORTED,

        /**
         * The number of merge bases exceeds [RecursiveMerger.MAX_BASES]
         */
        TOO_MANY_MERGE_BASES,

        /**
         * In order to find a single merge base it may required to merge
         * together multiple common predecessors. If during these merges
         * conflicts occur the merge fails with this reason
         */
        CONFLICTS_DURING_MERGE_BASE_CALCULATION
    }


    /**
     * Construct a NoMergeBase exception
     *
     * @param reason
     * the reason why no merge base could be found
     * @param message
     * a text describing the problem
     */
    constructor(reason: MergeBaseFailureReason, message: String?) : super(
        MessageFormat.format(
            JGitText.get().noMergeBase,
            reason.toString(), message
        )
    ) {
        this.reason = reason
    }

    /**
     * Construct a NoMergeBase exception
     *
     * @param reason
     * the reason why no merge base could be found
     * @param message
     * a text describing the problem
     * @param why
     * an exception causing this error
     */
    constructor(
        reason: MergeBaseFailureReason, message: String?,
        why: Throwable?
    ) : super(
        MessageFormat.format(
            JGitText.get().noMergeBase,
            reason.toString(), message
        )
    ) {
        this.reason = reason
        initCause(why)
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
