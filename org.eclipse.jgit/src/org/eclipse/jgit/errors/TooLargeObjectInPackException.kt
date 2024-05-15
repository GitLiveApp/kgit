/*
 * Copyright (C) 2011, Sasa Zivkov <sasa.zivkov@sap.com> and others
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
 * Thrown when PackParser finds an object larger than a predefined limit
 */
class TooLargeObjectInPackException : TransportException {
    /**
     * Construct a too large object in pack exception when the exact size of the
     * too large object is not available. This will be used when we find out
     * that a delta sequence is already larger than the maxObjectSizeLimit but
     * don't want to inflate the delta just to find out the exact size of the
     * resulting object.
     *
     * @param maxObjectSizeLimit
     * the maximum object size limit
     */
    constructor(maxObjectSizeLimit: Long) : super(
        MessageFormat.format(
            JGitText.get().receivePackObjectTooLarge1,
            maxObjectSizeLimit
        )
    )

    /**
     * Construct a too large object in pack exception when the exact size of the
     * too large object is known.
     *
     * @param objectSize
     * a long.
     * @param maxObjectSizeLimit
     * a long.
     */
    constructor(
        objectSize: Long,
        maxObjectSizeLimit: Long
    ) : super(
        MessageFormat.format(
            JGitText.get().receivePackObjectTooLarge2,
            objectSize, maxObjectSizeLimit
        )
    )

    /**
     * Construct a too large object in pack exception.
     *
     * @param uri
     * URI used for transport
     * @param s
     * message
     * @since 4.4
     */
    constructor(uri: URIish, s: String) : super(uri.setPass(null).toString() + ": " + s) //$NON-NLS-1$

    companion object {
        private const val serialVersionUID = 1L
    }
}
