/*
 * Copyright (C) 2010, Christian Halstrick <christian.halstrick@sap.com>,
 * Copyright (C) 2010, Stefan Lay <stefan.lay@sap.com> and others
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
 * An exception thrown when a [org.eclipse.jgit.transport.CredentialItem]
 * is requested from a [org.eclipse.jgit.transport.CredentialsProvider]
 * which is not supported by this provider.
 */
class UnsupportedCredentialItem
/**
 * Constructs an UnsupportedCredentialItem with the specified detail message
 * prefixed with provided URI.
 *
 * @param uri
 * URI used for transport
 * @param s
 * message
 */
    (uri: URIish, s: String) : RuntimeException(uri.setPass(null).toString() + ": " + s) {
    companion object {
        private const val serialVersionUID = 1L
    }
}
