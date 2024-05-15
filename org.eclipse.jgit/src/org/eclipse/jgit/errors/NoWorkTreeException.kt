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

import org.eclipse.jgit.internal.JGitText

/**
 * Indicates a [org.eclipse.jgit.lib.Repository] has no working directory,
 * and is thus bare.
 */
class NoWorkTreeException : IllegalStateException(JGitText.get().bareRepositoryNoWorkdirAndIndex) {
    companion object {
        private const val serialVersionUID = 1L
    }
}
