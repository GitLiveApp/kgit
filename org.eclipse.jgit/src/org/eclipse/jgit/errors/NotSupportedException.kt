/*
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

import java.io.IOException

/**
 * JGit encountered a case that it knows it cannot yet handle.
 */
class NotSupportedException : IOException {
    /**
     * Construct a NotSupportedException for some issue JGit cannot
     * yet handle.
     *
     * @param s message describing the issue
     */
    constructor(s: String?) : super(s)

    /**
     * Construct a NotSupportedException for some issue JGit cannot yet handle.
     *
     * @param s
     * message describing the issue
     * @param why
     * a lower level implementation specific issue.
     */
    constructor(s: String?, why: Throwable?) : super(s) {
        initCause(why)
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
