/*
 * Copyright (C) 2009, Google Inc. and others
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
 * Thrown when a Pack previously failed and is known to be unusable
 */
class PackInvalidException
/**
 * Construct a pack invalid error with cause.
 *
 * @param path
 * path of the invalid pack file.
 * @param cause
 * cause of the pack file becoming invalid.
 * @since 4.5.7
 */
    (path: String?, cause: Throwable?) :
    IOException(MessageFormat.format(JGitText.get().packFileInvalid, path), cause) {
    /**
     * Construct a pack invalid error.
     *
     * @param path
     * path of the invalid pack file.
     */
    @Deprecated("Use {@link #PackInvalidException(File, Throwable)}.")
    constructor(path: File) : this(path, null)

    /**
     * Construct a pack invalid error with cause.
     *
     * @param path
     * path of the invalid pack file.
     * @param cause
     * cause of the pack file becoming invalid.
     * @since 4.5.7
     */
    constructor(path: File, cause: Throwable?) : this(path.absolutePath, cause)

    /**
     * Construct a pack invalid error.
     *
     * @param path
     * path of the invalid pack file.
     */
    @Deprecated("Use {@link #PackInvalidException(String, Throwable)}.")
    constructor(path: String?) : this(path, null)

    companion object {
        private const val serialVersionUID = 1L
    }
}
