/*
 * Copyright (C) 2009, Google Inc.
 * Copyright (C) 2008, Jonas Fonseca <fonseca@diku.dk>
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006-2007, Shawn O. Pearce <spearce@spearce.org> and others
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
import org.eclipse.jgit.lib.Constants.typeString
import org.eclipse.jgit.lib.ObjectId
import java.io.IOException
import java.text.MessageFormat

/**
 * An expected object is missing.
 */
class MissingObjectException : IOException {
    /**
     * Get the ObjectId that was not found
     *
     * @return the ObjectId that was not found
     */
    val objectId: ObjectId?

    /**
     * Construct a MissingObjectException for the specified object id.
     * Expected type is reported to simplify tracking down the problem.
     *
     * @param id SHA-1
     * @param type object type
     */
    constructor(id: ObjectId, type: String?) : super(
        MessageFormat.format(
            JGitText.get().missingObject,
            type,
            id.name()
        )
    ) {
        objectId = id.copy()
    }

    /**
     * Construct a MissingObjectException for the specified object id.
     * Expected type is reported to simplify tracking down the problem.
     *
     * @param id SHA-1
     * @param type object type
     */
    constructor(id: ObjectId, type: Int) : this(id, typeString(type))

    /**
     * Construct a MissingObjectException for the specified object id. Expected
     * type is reported to simplify tracking down the problem.
     *
     * @param id
     * SHA-1
     * @param type
     * object type
     */
    constructor(id: AbbreviatedObjectId, type: Int) : super(
        MessageFormat.format(
            JGitText.get().missingObject,
            typeString(type),
            id.name()
        )
    ) {
        objectId = null
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
