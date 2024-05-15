/*
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
import java.util.*

/**
 * An exception detailing multiple reasons for failure.
 */
class CompoundException(why: Collection<Throwable>) : Exception(format(why)) {
    /**
     * Get the complete list of reasons why this failure happened.
     *
     * @return unmodifiable collection of all possible reasons.
     */
    val allCauses: List<Throwable> = Collections.unmodifiableList(ArrayList(why))

    companion object {
        private const val serialVersionUID = 1L

        private fun format(causes: Collection<Throwable>): String {
            val msg = StringBuilder()
            msg.append(JGitText.get().failureDueToOneOfTheFollowing)
            for (c in causes) {
                msg.append("  ") //$NON-NLS-1$
                msg.append(c.message)
                msg.append("\n") //$NON-NLS-1$
            }
            return msg.toString()
        }
    }
}
