/*
 * Copyright (C) 2013, Robin Rosenberg <robin.rosenberg@dewire.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.lib

import java.io.IOException

/**
 * Utility for reading reflog entries
 *
 * @since 3.0
 */
interface ReflogReader {
	@get:Throws(IOException::class)
    val lastEntry: ReflogEntry?

	@get:Throws(IOException::class)
    val reverseEntries: List<ReflogEntry?>?

    /**
     * Get specific entry in the reflog relative to the last entry which is
     * considered entry zero.
     *
     * @param number
     * a int.
     * @return reflog entry or null if not found
     * @throws java.io.IOException
     * if an IO error occurred
     */
    @Throws(IOException::class)
    fun getReverseEntry(number: Int): ReflogEntry?

    /**
     * Get all reflog entries in reverse order
     *
     * @param max
     * max number of entries to read
     * @return all reflog entries in reverse order
     * @throws java.io.IOException
     * if an IO error occurred
     */
    @Throws(IOException::class)
    fun getReverseEntries(max: Int): List<ReflogEntry?>?
}
