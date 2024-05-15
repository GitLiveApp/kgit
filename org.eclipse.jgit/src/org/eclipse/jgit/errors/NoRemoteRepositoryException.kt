/*
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.errors

import org.eclipse.jgit.transport.URIish

/**
 * Indicates a remote repository does not exist.
 */
class NoRemoteRepositoryException : TransportException {
    /**
     * Constructs an exception indicating a repository does not exist.
     *
     * @param uri
     * URI used for transport
     * @param s
     * message
     */
    constructor(uri: URIish, s: String) : super(uri, s)

    /**
     * Constructs an exception indicating a repository does not exist.
     *
     * @param uri
     * URI used for transport
     * @param s
     * message
     * @param cause
     * root cause exception
     * @since 5.13.1
     */
    constructor(uri: URIish, s: String, cause: Throwable?) : super(uri, s, cause)

    companion object {
        private const val serialVersionUID = 1L
    }
}
