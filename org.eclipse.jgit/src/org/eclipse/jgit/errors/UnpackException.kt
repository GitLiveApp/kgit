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
import java.io.IOException

/**
 * Indicates a ReceivePack failure while scanning the pack stream.
 */
class UnpackException(why: Throwable?) : IOException(JGitText.get().unpackException) {
    /**
     * Creates an exception with a root cause.
     *
     * @param why
     * the root cause of the unpacking failure.
     */
    init {
        initCause(why)
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
