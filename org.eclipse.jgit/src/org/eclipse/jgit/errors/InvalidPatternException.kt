/*
 * Copyright (C) 2008, Florian Köberle <florianskarten@web.de>
 * Copyright (C) 2009, Vasyl' Vavrychuk <vvavrychuk@gmail.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.errors

/**
 * Thrown when a pattern passed in an argument was wrong.
 */
open class InvalidPatternException
/**
 * Constructor for InvalidPatternException
 *
 * @param message
 * explains what was wrong with the pattern.
 * @param pattern
 * the invalid pattern.
 */(
    message: String?,
    /**
     * Get the invalid pattern
     *
     * @return the invalid pattern.
     */
    val pattern: String
) : Exception(message) {
    /**
     * Constructor for InvalidPatternException
     *
     * @param message
     * explains what was wrong with the pattern.
     * @param pattern
     * the invalid pattern.
     * @param cause
     * the cause.
     * @since 4.10
     */
    constructor(
        message: String?, pattern: String,
        cause: Throwable?
    ) : this(message, pattern) {
        initCause(cause)
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
