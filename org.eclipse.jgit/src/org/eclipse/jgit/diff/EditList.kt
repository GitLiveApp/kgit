/*
 * Copyright (C) 2009, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.diff

/**
 * Specialized list of [org.eclipse.jgit.diff.Edit]s in a document.
 */
class EditList : ArrayList<Edit> {
    /**
     * Create a new, empty edit list.
     */
    constructor() : super(16)

    /**
     * Create an empty edit list with the specified capacity.
     *
     * @param capacity
     * the initial capacity of the edit list. If additional edits are
     * added to the list, it will be grown to support them.
     */
    constructor(capacity: Int) : super(capacity)

    override fun toString(): String {
        return "EditList" + super.toString() //$NON-NLS-1$
    }

    companion object {
        private const val serialVersionUID = 1L

        /**
         * Construct an edit list containing a single edit.
         *
         * @param edit
         * the edit to return in the list.
         * @return list containing only `edit`.
         */
		@JvmStatic
		fun singleton(edit: Edit): EditList {
            val res = EditList(1)
            res.add(edit)
            return res
        }
    }
}
