/*
 * Copyright (C) 2010, Christian Halstrick <christian.halstrick@sap.com>,
 * Copyright (C) 2010, Matthias Sohn <matthias.sohn@sap.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.merge

import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.lib.ObjectInserter
import org.eclipse.jgit.lib.Repository

/**
 * A three-way merge strategy performing a content-merge if necessary
 */
open class StrategyResolve : ThreeWayMergeStrategy() {
    override fun newMerger(db: Repository): ThreeWayMerger {
        return ResolveMerger(db, false)
    }

    override fun newMerger(db: Repository, inCore: Boolean): ThreeWayMerger {
        return ResolveMerger(db, inCore)
    }

    override fun newMerger(inserter: ObjectInserter, config: Config): ThreeWayMerger? {
        return ResolveMerger(inserter, config)
    }

    override val name: String
        get() = "resolve" //$NON-NLS-1$
}
