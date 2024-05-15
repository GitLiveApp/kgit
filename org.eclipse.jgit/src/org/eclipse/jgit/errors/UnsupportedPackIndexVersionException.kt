/*
 * Copyright (C) 2017, Matthias Sohn <matthias.sohn@sap.com> and others
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
 * Thrown when a PackIndex uses an index version not supported by JGit.
 *
 * @since 4.5
 */
class UnsupportedPackIndexVersionException
/**
 * Construct an exception.
 *
 * @param version
 * pack index version
 */
    (version: Int) : IOException(
    MessageFormat.format(
        JGitText.get().unsupportedPackIndexVersion,
        version
    )
) {
    companion object {
        private const val serialVersionUID = 1L
    }
}
