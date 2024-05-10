/*
 * Copyright (C) 2011-2013, Robin Rosenberg <robin.rosenberg@dewire.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.lib

/**
 * Parsed reflog entry
 *
 * @since 3.0
 */
interface ReflogEntry {
    /**
     * Get the commit id before the change
     *
     * @return the commit id before the change
     */
    val oldId: ObjectId?

    /**
     * Get the commit id after the change
     *
     * @return the commit id after the change
     */
    val newId: ObjectId?

    /**
     * Get user performing the change
     *
     * @return user performing the change
     */
    val who: PersonIdent?

    /**
     * Get textual description of the change
     *
     * @return textual description of the change
     */
    val comment: String?

    /**
     * Parse checkout
     *
     * @return a [org.eclipse.jgit.lib.CheckoutEntry] with parsed
     * information about a branch switch, or null if the entry is not a
     * checkout
     */
    fun parseCheckout(): CheckoutEntry?

    companion object {
        /**
         * Prefix used in reflog messages when the ref was first created.
         *
         *
         * Does not have a corresponding constant in C git, but is untranslated like
         * the other constants.
         *
         * @since 4.9
         */
        const val PREFIX_CREATED: String = "created" //$NON-NLS-1$

        /**
         * Prefix used in reflog messages when the ref was updated with a fast
         * forward.
         *
         *
         * Untranslated, and exactly matches the
         * [
 * untranslated string in C git](https://git.kernel.org/pub/scm/git/git.git/tree/builtin/fetch.c?id=f3da2b79be9565779e4f76dc5812c68e156afdf0#n680).
         *
         * @since 4.9
         */
        const val PREFIX_FAST_FORWARD: String = "fast-forward" //$NON-NLS-1$

        /**
         * Prefix used in reflog messages when the ref was force updated.
         *
         *
         * Untranslated, and exactly matches the
         * [
 * untranslated string in C git](https://git.kernel.org/pub/scm/git/git.git/tree/builtin/fetch.c?id=f3da2b79be9565779e4f76dc5812c68e156afdf0#n695).
         *
         * @since 4.9
         */
        const val PREFIX_FORCED_UPDATE: String = "forced-update" //$NON-NLS-1$
    }
}
