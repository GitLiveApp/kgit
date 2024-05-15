/*
 * Copyright (C) 2015, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.errors

/**
 * Thrown by DirCache code when entries overlap in impossible way.
 *
 * @since 4.2
 */
class DirCacheNameConflictException
/**
 * Construct an exception for a specific path.
 *
 * @param path1
 * one path that conflicts.
 * @param path2
 * another path that conflicts.
 */(
    /**
     * Get one of the paths that has a conflict
     *
     * @return one of the paths that has a conflict
     */
    val path1: String,
    /**
     * Get another path that has a conflict
     *
     * @return another path that has a conflict
     */
    val path2: String
) : IllegalStateException("$path1 $path2") {
    companion object {
        private const val serialVersionUID = 1L
    }
}
