/*
 * Copyright (C) 2008-2009, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.errors

import org.eclipse.jgit.dircache.DirCacheEntry
import org.eclipse.jgit.internal.JGitText
import java.io.IOException
import java.text.MessageFormat

/**
 * Indicates one or more paths in a DirCache have non-zero stages present.
 */
class UnmergedPathException
/**
 * Create a new unmerged path exception.
 *
 * @param dce
 * the first non-zero stage of the unmerged path.
 */(
    /**
     * Get the first non-zero stage of the unmerged path
     *
     * @return the first non-zero stage of the unmerged path
     */
    val dirCacheEntry: DirCacheEntry
) : IOException(MessageFormat.format(JGitText.get().unmergedPath, dirCacheEntry.pathString)) {
    companion object {
        private const val serialVersionUID = 1L
    }
}
