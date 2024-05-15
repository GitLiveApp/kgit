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

import org.eclipse.jgit.internal.JGitText

/**
 * Indicates a checked exception was thrown inside of
 * [org.eclipse.jgit.revwalk.RevWalk].
 *
 *
 * Usually this exception is thrown from the Iterator created around a RevWalk
 * instance, as the Iterator API does not allow checked exceptions to be thrown
 * from hasNext() or next(). The [java.lang.Exception.getCause] of this
 * exception is the original checked exception that we really wanted to throw
 * back to the application for handling and recovery.
 */
class RevWalkException
/**
 * Create a new walk exception an original cause.
 *
 * @param cause
 * the checked exception that describes why the walk failed.
 */
    (cause: Throwable?) : RuntimeException(JGitText.get().walkFailure, cause) {
    companion object {
        private const val serialVersionUID = 1L
    }
}
