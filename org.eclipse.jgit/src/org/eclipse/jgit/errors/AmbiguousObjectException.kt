/*
 * Copyright (C) 2010, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.errors

import org.eclipse.jgit.internal.JGitText
import org.eclipse.jgit.lib.AbbreviatedObjectId
import org.eclipse.jgit.lib.ObjectId
import java.io.IOException
import java.text.MessageFormat

/**
 * An [org.eclipse.jgit.lib.AbbreviatedObjectId] cannot be extended.
 */
class AmbiguousObjectException
/**
 * Construct a MissingObjectException for the specified object id. Expected
 * type is reported to simplify tracking down the problem.
 *
 * @param id
 * SHA-1
 * @param candidates
 * the candidate matches returned by the ObjectReader.
 */(
    /**
     * Get the `AbbreviatedObjectId` that has more than one result
     *
     * @return the `AbbreviatedObjectId` that has more than one result
     */
    val abbreviatedObjectId: AbbreviatedObjectId,
    /**
     * Get the matching candidates (or at least a subset of them)
     *
     * @return the matching candidates (or at least a subset of them)
     */
    val candidates: Collection<ObjectId>
) : IOException(
    MessageFormat.format(
        JGitText.get().ambiguousObjectAbbreviation,
        abbreviatedObjectId.name()
    )
) {
    companion object {
        private const val serialVersionUID = 1L
    }
}
