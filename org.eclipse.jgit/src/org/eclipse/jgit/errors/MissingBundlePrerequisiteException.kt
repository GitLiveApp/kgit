/*
 * Copyright (C) 2008, Google Inc.
 * Copyright (C) 2009, Sasa Zivkov <sasa.zivkov@sap.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.errors

import org.eclipse.jgit.internal.JGitText
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.transport.URIish

/**
 * Indicates a base/common object was required, but is not found.
 */
class MissingBundlePrerequisiteException
/**
 * Constructs a MissingBundlePrerequisiteException for a set of objects.
 *
 * @param uri
 * URI used for transport
 * @param missingCommits
 * the Map of the base/common object(s) we don't have. Keys are
 * ids of the missing objects and values are short descriptions.
 */
    (
    uri: URIish,
    missingCommits: Map<ObjectId, String?>
) : TransportException(uri, format(missingCommits)) {
    companion object {
        private const val serialVersionUID = 1L

        private fun format(missingCommits: Map<ObjectId, String?>): String {
            val r = StringBuilder()
            r.append(JGitText.get().missingPrerequisiteCommits)
            for ((key, value) in missingCommits) {
                r.append("\n  ") //$NON-NLS-1$
                r.append(key.name())
                if (value != null) r.append(" ").append(value) //$NON-NLS-1$
            }
            return r.toString()
        }
    }
}
