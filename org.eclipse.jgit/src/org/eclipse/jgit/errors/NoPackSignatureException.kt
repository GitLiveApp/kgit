/*
 * Copyright (C) 2017, Matthias Sohn <matthias.sohn@sap.com> and others
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
 * Thrown when a Pack is found not to contain the pack signature defined by git.
 *
 * @since 4.5
 */
class NoPackSignatureException
/**
 * Construct an exception.
 *
 * @param why
 * description of the type of error.
 */
    (why: String?) : IOException(why) {
    companion object {
        private const val serialVersionUID = 1L
    }
}
