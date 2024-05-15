/*
 * Copyright (C) 2008, Google Inc.
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

import org.eclipse.jgit.annotations.Nullable
import org.eclipse.jgit.internal.JGitText
import org.eclipse.jgit.lib.AnyObjectId
import org.eclipse.jgit.lib.ObjectChecker
import org.eclipse.jgit.lib.ObjectId
import java.io.IOException
import java.text.MessageFormat

/**
 * Exception thrown when an object cannot be read from Git.
 */
class CorruptObjectException : IOException {
    /**
     * Specific error condition identified by
     * [org.eclipse.jgit.lib.ObjectChecker].
     *
     * @return error condition or null.
     * @since 4.2
     */
    @get:Nullable
    var errorType: ObjectChecker.ErrorType? = null
        private set

    /**
     * Report a specific error condition discovered in an object.
     *
     * @param type
     * type of error
     * @param id
     * identity of the bad object
     * @param why
     * description of the error.
     * @since 4.2
     */
    constructor(
        type: ObjectChecker.ErrorType, id: AnyObjectId,
        why: String?
    ) : super(
        MessageFormat.format(
            JGitText.get().objectIsCorrupt3,
            type.messageId, id.name(), why
        )
    ) {
        this.errorType = type
    }

    /**
     * Construct a CorruptObjectException for reporting a problem specified
     * object id
     *
     * @param id
     * a [org.eclipse.jgit.lib.AnyObjectId]
     * @param why
     * error message
     */
    constructor(id: AnyObjectId, why: String?) : super(
        MessageFormat.format(
            JGitText.get().objectIsCorrupt,
            id.name(),
            why
        )
    )

    /**
     * Construct a CorruptObjectException for reporting a problem specified
     * object id
     *
     * @param id
     * a [org.eclipse.jgit.lib.ObjectId]
     * @param why
     * error message
     */
    constructor(id: ObjectId, why: String?) : super(
        MessageFormat.format(
            JGitText.get().objectIsCorrupt,
            id.name(),
            why
        )
    )

    /**
     * Construct a CorruptObjectException for reporting a problem not associated
     * with a specific object id.
     *
     * @param why
     * error message
     */
    constructor(why: String?) : super(why)

    /**
     * Construct a CorruptObjectException for reporting a problem not associated
     * with a specific object id.
     *
     * @param why
     * message describing the corruption.
     * @param cause
     * optional root cause exception
     * @since 3.4
     */
    constructor(why: String?, cause: Throwable?) : super(why) {
        initCause(cause)
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
