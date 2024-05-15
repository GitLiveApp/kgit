/*
 * Copyright (C) 2008-2009, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.merge

import org.eclipse.jgit.dircache.DirCache
import org.eclipse.jgit.dircache.DirCacheBuilder
import org.eclipse.jgit.dircache.DirCacheEntry
import org.eclipse.jgit.errors.UnmergedPathException
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.NameConflictTreeWalk
import java.io.IOException

/**
 * Merges two commits together in-memory, ignoring any working directory.
 *
 *
 * The strategy chooses a path from one of the two input trees if the path is
 * unchanged in the other relative to their common merge base tree. This is a
 * trivial 3-way merge (at the file path level only).
 *
 *
 * Modifications of the same file path (content and/or file mode) by both input
 * trees will cause a merge conflict, as this strategy does not attempt to merge
 * file contents.
 */
class StrategySimpleTwoWayInCore
/**
 * Create a new instance of the strategy.
 */
constructor() : ThreeWayMergeStrategy() {
    override val name: String
        get() = "simple-two-way-in-core" //$NON-NLS-1$

    override fun newMerger(db: Repository): ThreeWayMerger {
        return InCoreMerger(db)
    }

    override fun newMerger(db: Repository, inCore: Boolean): ThreeWayMerger {
        // This class is always inCore, so ignore the parameter
        return newMerger(db)
    }

    override fun newMerger(inserter: ObjectInserter, config: Config): ThreeWayMerger? {
        return InCoreMerger(inserter)
    }

    private class InCoreMerger : ThreeWayMerger {
        private val tw: NameConflictTreeWalk

        private val cache: DirCache

        private var builder: DirCacheBuilder? = null

        private var resultTree: ObjectId? = null

        constructor(local: Repository?) : super(local) {
            tw = NameConflictTreeWalk(local, reader)
            cache = DirCache.newInCore()
        }

        constructor(inserter: ObjectInserter) : super(inserter) {
            tw = NameConflictTreeWalk(null, reader)
            cache = DirCache.newInCore()
        }

        @Throws(IOException::class)
        override fun mergeImpl(): Boolean {
            tw.addTree(mergeBase())
            tw.addTree(sourceTrees!![0])
            tw.addTree(sourceTrees!![1])

            var hasConflict = false
            builder = cache.builder()
            while (tw.next()) {
                val modeO = tw.getRawMode(T_OURS)
                val modeT = tw.getRawMode(T_THEIRS)
                if (modeO == modeT && tw.idEqual(T_OURS, T_THEIRS)) {
                    add(T_OURS, DirCacheEntry.STAGE_0)
                    continue
                }

                val modeB = tw.getRawMode(T_BASE)
                if (modeB == modeO && tw.idEqual(T_BASE, T_OURS)) add(T_THEIRS, DirCacheEntry.STAGE_0)
                else if (modeB == modeT && tw.idEqual(T_BASE, T_THEIRS)) add(T_OURS, DirCacheEntry.STAGE_0)
                else {
                    if (nonTree(modeB)) {
                        add(T_BASE, DirCacheEntry.STAGE_1)
                        hasConflict = true
                    }
                    if (nonTree(modeO)) {
                        add(T_OURS, DirCacheEntry.STAGE_2)
                        hasConflict = true
                    }
                    if (nonTree(modeT)) {
                        add(T_THEIRS, DirCacheEntry.STAGE_3)
                        hasConflict = true
                    }
                    if (tw.isSubtree) tw.enterSubtree()
                }
            }
            builder!!.finish()
            builder = null

            if (hasConflict) return false
            try {
                val odi = objectInserter
                resultTree = cache.writeTree(odi)
                odi.flush()
                return true
            } catch (upe: UnmergedPathException) {
                resultTree = null
                return false
            }
        }

        @Throws(IOException::class)
        private fun add(tree: Int, stage: Int) {
            val i = getTree(tree)
            if (i != null) {
                if (FileMode.TREE.equals(tw.getRawMode(tree))) {
                    builder!!.addTree(
                        tw.rawPath, stage, reader, tw
                            .getObjectId(tree)
                    )
                } else {
                    val e = DirCacheEntry(tw.rawPath, stage)
                    e.setObjectIdFromRaw(i.idBuffer(), i.idOffset())
                    e.fileMode = tw.getFileMode(tree)
                    builder!!.add(e)
                }
            }
        }

        private fun getTree(tree: Int): AbstractTreeIterator? {
            return tw.getTree(tree, AbstractTreeIterator::class.java)
        }

        override val resultTreeId: ObjectId?
            get() = resultTree

        companion object {
            private const val T_BASE = 0

            private const val T_OURS = 1

            private const val T_THEIRS = 2

            private fun nonTree(mode: Int): Boolean {
                return mode != 0 && !FileMode.TREE.equals(mode)
            }
        }
    }
}
