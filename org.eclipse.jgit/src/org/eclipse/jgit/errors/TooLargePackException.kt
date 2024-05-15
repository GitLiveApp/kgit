/*
 * Copyright (C) 2014, Sasa Zivkov <sasa.zivkov@sap.com>, SAP AG and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.errors

import org.eclipse.jgit.internal.JGitText
import org.eclipse.jgit.transport.URIish
import java.text.MessageFormat

/**
 * Thrown when a pack exceeds a given size limit
 *
 * @since 3.3
 */
class TooLargePackException : TransportException {
    /**
     * Construct a too large pack exception.
     *
     * @param packSizeLimit
     * the pack size limit (in bytes) that was exceeded
     */
    constructor(packSizeLimit: Long) : super(
        MessageFormat.format(
            JGitText.get().receivePackTooLarge,
            packSizeLimit
        )
    )

    /**
     * Construct a too large pack exception.
     *
     * @param uri
     * URI used for transport
     * @param s
     * message
     * @since 4.0
     */
    constructor(uri: URIish, s: String) : super(uri.setPass(null).toString() + ": " + s) //$NON-NLS-1$

    companion object {
        private const val serialVersionUID = 1L
    }
}
