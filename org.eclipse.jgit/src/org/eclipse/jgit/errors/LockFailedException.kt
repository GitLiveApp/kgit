/*
 * Copyright (C) 2011, GitHub Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.errors

import org.eclipse.jgit.internal.JGitText
import java.io.File
import java.io.IOException
import java.text.MessageFormat

/**
 * An exception occurring when a file cannot be locked
 */
class LockFailedException : IOException {
    /**
     * Get the file that could not be locked
     *
     * @return file
     */
    var file: File
        private set

    /**
     * Constructor for LockFailedException
     *
     * @param file
     * file that could not be locked
     * @param message
     * exception message
     * @param cause
     * cause, for later retrieval by
     * [java.lang.Throwable.getCause]
     * @since 4.1
     */
    constructor(file: File, message: String?, cause: Throwable?) : super(message, cause) {
        this.file = file
    }

    /**
     * Construct a CannotLockException for the given file and message
     *
     * @param file
     * file that could not be locked
     * @param message
     * exception message
     */
    /**
     * Construct a CannotLockException for the given file
     *
     * @param file
     * file that could not be locked
     */
    @JvmOverloads
    constructor(file: File, message: String? = MessageFormat.format(JGitText.get().cannotLock, file)) : super(message) {
        this.file = file
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
