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

import java.io.IOException

/**
 * Thrown when a Pack no longer matches the PackIndex.
 */
class PackMismatchException
/**
 * Construct a pack modification error.
 *
 * @param why
 * description of the type of error.
 */
    (why: String?) : IOException(why) {
    /**
     * Check if this is a permanent problem
     *
     * @return if this is a permanent problem and repeatedly scanning the
     * packlist couldn't fix it
     * @since 5.9.1
     */
    /**
     * Set the type of the exception
     *
     * @param permanent
     * whether the exception is considered permanent
     * @since 5.9.1
     */
    var isPermanent: Boolean = false

    companion object {
        private const val serialVersionUID = 1L
    }
}
