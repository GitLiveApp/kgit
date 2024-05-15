/*
 * Copyright (C) 2012, Research In Motion Limited and others
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
 *
 * @since 3.0
 */
class StrategyRecursive : StrategyResolve() {
    override fun newMerger(db: Repository): ThreeWayMerger {
        return RecursiveMerger(db, false)
    }

    override fun newMerger(db: Repository, inCore: Boolean): ThreeWayMerger {
        return RecursiveMerger(db, inCore)
    }

    override fun newMerger(inserter: ObjectInserter, config: Config): ThreeWayMerger? {
        return RecursiveMerger(inserter, config)
    }

    override val name: String
        get() = "recursive" //$NON-NLS-1$
}
