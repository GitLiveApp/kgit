/*
 * Copyright (C) 2016, Matthias Sohn <matthias.sohn@sap.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.errors

/**
 * Thrown when an external command failed
 *
 * @since 4.5
 */
class CommandFailedException : Exception {
    /**
     * Get return code returned by the command
     *
     * @return return code returned by the command
     */
    var returnCode: Int
        private set

    /**
     * Constructor for CommandFailedException
     *
     * @param returnCode
     * return code returned by the command
     * @param message
     * error message
     */
    constructor(returnCode: Int, message: String?) : super(message) {
        this.returnCode = returnCode
    }

    /**
     * Constructor for CommandFailedException
     *
     * @param returnCode
     * return code returned by the command
     * @param message
     * error message
     * @param cause
     * exception causing this exception
     */
    constructor(
        returnCode: Int, message: String?,
        cause: Throwable?
    ) : super(message, cause) {
        this.returnCode = returnCode
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
