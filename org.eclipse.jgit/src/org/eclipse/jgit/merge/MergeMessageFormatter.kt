/*
 * Copyright (C) 2010-2012, Robin Stocker <robin@nibor.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.merge

import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.util.ChangeIdUtil
import org.eclipse.jgit.util.StringUtils

/**
 * Formatter for constructing the commit message for a merge commit.
 *
 *
 * The format should be the same as C Git does it, for compatibility.
 */
class MergeMessageFormatter {
    /**
     * Construct the merge commit message.
     *
     * @param refsToMerge
     * the refs which will be merged
     * @param target
     * the branch ref which will be merged into
     * @return merge commit message
     */
    fun format(refsToMerge: List<Ref>, target: Ref): String {
        val sb = StringBuilder()
        sb.append("Merge ") //$NON-NLS-1$

        val branches: MutableList<String> = ArrayList()
        val remoteBranches: MutableList<String> = ArrayList()
        val tags: MutableList<String> = ArrayList()
        val commits: MutableList<String> = ArrayList()
        val others: MutableList<String> = ArrayList()
        for (ref in refsToMerge) {
            if (ref.name.startsWith(Constants.R_HEADS)) {
                branches.add(
                    "'" + Repository.shortenRefName(ref.name) //$NON-NLS-1$
                        + "'"
                ) //$NON-NLS-1$
            } else if (ref.name.startsWith(Constants.R_REMOTES)) {
                remoteBranches.add(
                    "'" //$NON-NLS-1$
                        + Repository.shortenRefName(ref.name) + "'"
                ) //$NON-NLS-1$
            } else if (ref.name.startsWith(Constants.R_TAGS)) {
                tags.add("'" + Repository.shortenRefName(ref.name) + "'") //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                val objectId = ref.objectId
                if (objectId != null && ref.name == objectId.name) {
                    commits.add("'" + ref.name + "'") //$NON-NLS-1$ //$NON-NLS-2$
                } else {
                    others.add(ref.name)
                }
            }
        }

        val listings: MutableList<String> = ArrayList()

        if (!branches.isEmpty()) listings.add(joinNames(branches, "branch", "branches")) //$NON-NLS-1$//$NON-NLS-2$


        if (!remoteBranches.isEmpty()) listings.add(
            joinNames(
                remoteBranches, "remote-tracking branch",  //$NON-NLS-1$
                "remote-tracking branches"
            )
        ) //$NON-NLS-1$


        if (!tags.isEmpty()) listings.add(joinNames(tags, "tag", "tags")) //$NON-NLS-1$ //$NON-NLS-2$


        if (!commits.isEmpty()) listings.add(joinNames(commits, "commit", "commits")) //$NON-NLS-1$ //$NON-NLS-2$


        if (!others.isEmpty()) listings.add(StringUtils.join(others, ", ", " and ")) //$NON-NLS-1$ //$NON-NLS-2$


        sb.append(StringUtils.join(listings, ", ")) //$NON-NLS-1$

        val targetName = target.leaf.name
        if (targetName != Constants.R_HEADS + Constants.MASTER) {
            val targetShortName = Repository.shortenRefName(targetName)
            sb.append(" into $targetShortName") //$NON-NLS-1$
        }

        return sb.toString()
    }

    /**
     * Add section with conflicting paths to merge message. Lines are prefixed
     * with a hash.
     *
     * @param message
     * the original merge message
     * @param conflictingPaths
     * the paths with conflicts
     * @return merge message with conflicting paths added
     */
    @Deprecated(
        """since 6.1; use
	              {@link #formatWithConflicts(String, Iterable, char)} instead"""
    )
    fun formatWithConflicts(
        message: String,
        conflictingPaths: List<String?>
    ): String {
        return formatWithConflicts(message, conflictingPaths, '#')
    }

    /**
     * Add section with conflicting paths to merge message.
     *
     * @param message
     * the original merge message
     * @param conflictingPaths
     * the paths with conflicts
     * @param commentChar
     * comment character to use for prefixing the conflict lines
     * @return merge message with conflicting paths added
     * @since 6.1
     */
    fun formatWithConflicts(
        message: String,
        conflictingPaths: Iterable<String?>, commentChar: Char
    ): String {
        val sb = StringBuilder()
        val lines = message.split("\n").toTypedArray() //$NON-NLS-1$
        val firstFooterLine = ChangeIdUtil.indexOfFirstFooterLine(lines)
        for (i in 0 until firstFooterLine) {
            sb.append(lines[i]).append('\n')
        }
        if (firstFooterLine == lines.size && message.isNotEmpty()) {
            sb.append('\n')
        }
        addConflictsMessage(conflictingPaths, sb, commentChar)
        if (firstFooterLine < lines.size) {
            sb.append('\n')
        }
        for (i in firstFooterLine until lines.size) {
            sb.append(lines[i]).append('\n')
        }
        return sb.toString()
    }

    companion object {
        private fun addConflictsMessage(
            conflictingPaths: Iterable<String?>,
            sb: StringBuilder, commentChar: Char
        ) {
            sb.append(commentChar).append(" Conflicts:\n") //$NON-NLS-1$
            for (conflictingPath in conflictingPaths) {
                sb.append(commentChar).append('\t').append(conflictingPath)
                    .append('\n')
            }
        }

        private fun joinNames(
            names: List<String>, singular: String,
            plural: String
        ): String {
            if (names.size == 1) {
                return singular + " " + names[0] //$NON-NLS-1$
            }
            return plural + " " + StringUtils.join(names, ", ", " and ") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }
}
