/*
 * Copyright (C) 2017 Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.errors

/**
 * BinaryBlobException is used to signal that binary data was found
 * in a context that requires text (eg. for generating textual diffs).
 *
 * @since 4.10
 */
class BinaryBlobException
/**
 * Construct a BinaryBlobException.
 */
    : Exception() {
    @Synchronized
    override fun fillInStackTrace(): Throwable {
        return this
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
