/*
 * Copyright (C) 2012, IBM Corporation and others. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.merge

import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.util.GitDateFormatter

/**
 * Formatter for constructing the commit message for a squashed commit.
 *
 *
 * The format should be the same as C Git does it, for compatibility.
 */
class SquashMessageFormatter {
    private val dateFormatter = GitDateFormatter(GitDateFormatter.Format.DEFAULT)

    /**
     * Construct the squashed commit message.
     *
     * @param squashedCommits
     * the squashed commits
     * @param target
     * the target branch
     * @return squashed commit message
     */
    fun format(squashedCommits: List<RevCommit>, target: Ref?): String {
        val sb = StringBuilder()
        sb.append("Squashed commit of the following:\n") //$NON-NLS-1$
        for (c in squashedCommits) {
            sb.append("\ncommit ") //$NON-NLS-1$
            sb.append(c.name)
            sb.append("\n") //$NON-NLS-1$
            sb.append(toString(c.authorIdent))
            sb.append("\n\t") //$NON-NLS-1$
            sb.append(c.shortMessage)
            sb.append("\n") //$NON-NLS-1$
        }
        return sb.toString()
    }

    private fun toString(author: PersonIdent): String {
        val a = StringBuilder()

        a.append("Author: ") //$NON-NLS-1$
        a.append(author.name)
        a.append(" <") //$NON-NLS-1$
        a.append(author.emailAddress)
        a.append(">\n") //$NON-NLS-1$
        a.append("Date:   ") //$NON-NLS-1$
        a.append(dateFormatter.formatDate(author))
        a.append("\n") //$NON-NLS-1$

        return a.toString()
    }
}
