/*
 * Copyright (C) 2008-2009, Google Inc.
 * Copyright (C) 2009, Robin Rosenberg <robin.rosenberg@dewire.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.merge

import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectInserter
import org.eclipse.jgit.lib.Repository
import java.io.IOException

/**
 * Trivial merge strategy to make the resulting tree exactly match an input.
 *
 *
 * This strategy can be used to cauterize an entire side branch of history, by
 * setting the output tree to one of the inputs, and ignoring any of the paths
 * of the other inputs.
 */
class StrategyOneSided
/**
 * Create a new merge strategy to select a specific input tree.
 *
 * @param name
 * name of this strategy.
 * @param index
 * the position of the input tree to accept as the result.
 */ constructor(override val name: String, private val treeIndex: Int) : MergeStrategy() {
    override fun newMerger(db: Repository): Merger? {
        return OneSide(db, treeIndex)
    }

    override fun newMerger(db: Repository, inCore: Boolean): Merger? {
        return OneSide(db, treeIndex)
    }

    override fun newMerger(inserter: ObjectInserter, config: Config): Merger? {
        return OneSide(inserter, treeIndex)
    }

    internal class OneSide : Merger {
        private val treeIndex: Int

        constructor(local: Repository?, index: Int) : super(local) {
            treeIndex = index
        }

        constructor(inserter: ObjectInserter, index: Int) : super(inserter) {
            treeIndex = index
        }

        @Throws(IOException::class)
        override fun mergeImpl(): Boolean {
            return treeIndex < sourceTrees!!.size
        }

        override val resultTreeId: ObjectId?
            get() = sourceTrees!![treeIndex]

        override val baseCommitId: ObjectId?
            get() = null
    }
}
