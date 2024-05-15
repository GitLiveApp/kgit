/*
 * Copyright (C) 2009-2010, Google Inc. and others
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
import java.text.MessageFormat

/**
 * Indicates a local repository does not exist.
 */
class RepositoryNotFoundException : TransportException {
    /**
     * Constructs an exception indicating a local repository does not exist.
     *
     * @param location
     * description of the repository not found, usually file path.
     */
    constructor(location: File) : this(location.path)

    /**
     * Constructs an exception indicating a local repository does not exist.
     *
     * @param location
     * description of the repository not found, usually file path.
     * @param why
     * why the repository does not exist.
     */
    constructor(location: File, why: Throwable?) : this(location.path, why)

    /**
     * Constructs an exception indicating a local repository does not exist.
     *
     * @param location
     * description of the repository not found, usually file path.
     */
    constructor(location: String) : super(message(location))

    /**
     * Constructs an exception indicating a local repository does not exist.
     *
     * @param location
     * description of the repository not found, usually file path.
     * @param why
     * why the repository does not exist.
     */
    constructor(location: String, why: Throwable?) : super(message(location), why)

    companion object {
        private const val serialVersionUID = 1L

        private fun message(location: String): String {
            return MessageFormat.format(JGitText.get().repositoryNotFound, location)
        }
    }
}
