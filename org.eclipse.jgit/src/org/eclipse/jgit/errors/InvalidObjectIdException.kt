/*
 * Copyright (C) 2009, Jonas Fonseca <fonseca@diku.dk>
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.errors

import org.eclipse.jgit.internal.JGitText
import java.nio.charset.StandardCharsets
import java.text.MessageFormat

/**
 * Thrown when an invalid object id is passed in as an argument.
 */
class InvalidObjectIdException : IllegalArgumentException {
    /**
     * Create exception with bytes of the invalid object id.
     *
     * @param bytes containing the invalid id.
     * @param offset in the byte array where the error occurred.
     * @param length of the sequence of invalid bytes.
     */
    constructor(bytes: ByteArray, offset: Int, length: Int) : super(msg(bytes, offset, length))

    /**
     * Constructor for InvalidObjectIdException
     *
     * @param id
     * the invalid id.
     * @since 4.1
     */
    constructor(id: String?) : super(MessageFormat.format(JGitText.get().invalidId, id))

    companion object {
        private const val serialVersionUID = 1L

        private fun msg(bytes: ByteArray, offset: Int, length: Int): String {
            return try {
                MessageFormat.format(
                    JGitText.get().invalidId,
                    String(bytes, offset, length, StandardCharsets.US_ASCII)
                )
            } catch (e: StringIndexOutOfBoundsException) {
                JGitText.get().invalidId0
            }
        }
    }
}
