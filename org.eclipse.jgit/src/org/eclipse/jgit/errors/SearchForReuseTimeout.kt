/*
 * Copyright (C) 2021, Fabio Ponciroli <ponch@gerritforge.com>
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
import java.time.Duration

/**
 * Thrown when the search for reuse phase times out.
 *
 * @since 5.13
 */
class SearchForReuseTimeout
/**
 * Construct a search for reuse timeout error.
 *
 * @param timeout
 * time exceeded during the search for reuse phase.
 */
    (timeout: Duration) : IOException(
    MessageFormat.format(
        JGitText.get().searchForReuseTimeout,
        timeout.seconds
    )
) {
    @Synchronized
    override fun fillInStackTrace(): Throwable {
        return this
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}