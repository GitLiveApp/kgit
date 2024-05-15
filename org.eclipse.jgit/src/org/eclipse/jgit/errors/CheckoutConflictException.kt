/*
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006, Shawn O. Pearce <spearce@spearce.org> and others
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
 * Exception thrown if a conflict occurs during a merge checkout.
 */
class CheckoutConflictException : IOException {
    /**
     * Get the relative paths of the conflicting files
     *
     * @return the relative paths of the conflicting files (relative to the
     * working directory root).
     * @since 4.4
     */
    val conflictingFiles: Array<String>

    /**
     * Construct a CheckoutConflictException for the specified file
     *
     * @param file
     * relative path of a file
     */
    constructor(file: String) : super(MessageFormat.format(JGitText.get().checkoutConflictWithFile, file)) {
        conflictingFiles = arrayOf(file)
    }

    /**
     * Construct a CheckoutConflictException for the specified set of files
     *
     * @param files
     * an array of relative file paths
     */
    constructor(files: Array<String>) : super(
        MessageFormat.format(
            JGitText.get().checkoutConflictWithFiles,
            buildList(files)
        )
    ) {
        conflictingFiles = files
    }

    companion object {
        private const val serialVersionUID = 1L

        private fun buildList(files: Array<String>): String {
            val builder = StringBuilder()
            for (f in files) {
                builder.append("\n") //$NON-NLS-1$
                builder.append(f)
            }
            return builder.toString()
        }
    }
}
