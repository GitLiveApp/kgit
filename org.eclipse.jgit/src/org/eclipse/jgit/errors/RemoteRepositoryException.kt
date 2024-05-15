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

import org.eclipse.jgit.transport.URIish

/**
 * Contains a message from the remote repository indicating a problem.
 *
 *
 * Some remote repositories may send customized error messages describing why
 * they cannot be accessed. These messages are wrapped up in this exception and
 * thrown to the caller of the transport operation.
 */
class RemoteRepositoryException
/**
 * Constructs a RemoteRepositoryException for a message.
 *
 * @param uri
 * URI used for transport
 * @param message
 * message, exactly as supplied by the remote repository. May
 * contain LFs (newlines) if the remote formatted it that way.
 */
    (uri: URIish, message: String) : TransportException(uri, message) {
    companion object {
        private const val serialVersionUID = 1L
    }
}
