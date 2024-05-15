/*
 * Copyright (C) 2009, Google Inc.
 * Copyright (C) 2012, Research In Motion Limited and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.merge

import org.eclipse.jgit.errors.IncorrectObjectTypeException
import org.eclipse.jgit.errors.MissingObjectException
import org.eclipse.jgit.lib.AnyObjectId
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectInserter
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import java.io.IOException

/**
 * A merge of 2 trees, using a common base ancestor tree.
 */
abstract class ThreeWayMerger : Merger {
    private var baseTree: RevTree? = null

    override var baseCommitId: ObjectId? = null

    /**
     * Create a new merge instance for a repository.
     *
     * @param local
     * the repository this merger will read and write data on.
     */
    protected constructor(local: Repository?) : super(local)

    /**
     * Create a new merge instance for a repository.
     *
     * @param local
     * the repository this merger will read and write data on.
     * @param inCore
     * perform the merge in core with no working folder involved
     */
    protected constructor(local: Repository?, inCore: Boolean) : this(local)

    /**
     * Create a new in-core merge instance from an inserter.
     *
     * @param inserter
     * the inserter to write objects to.
     * @since 4.8
     */
    protected constructor(inserter: ObjectInserter) : super(inserter)

    /**
     * Set the common ancestor tree.
     *
     * @param id
     * common base treeish; null to automatically compute the common
     * base from the input commits during
     * [.merge].
     * @throws org.eclipse.jgit.errors.IncorrectObjectTypeException
     * the object is not a treeish.
     * @throws org.eclipse.jgit.errors.MissingObjectException
     * the object does not exist.
     * @throws java.io.IOException
     * the object could not be read.
     */
    @Throws(MissingObjectException::class, IncorrectObjectTypeException::class, IOException::class)
    fun setBase(id: AnyObjectId?) {
        baseTree = if (id != null) {
            walk.parseTree(id)
        } else {
            null
        }
    }

    @Throws(IOException::class)
    override fun merge(vararg tips: AnyObjectId): Boolean {
        if (tips.size != 2) return false
        return super.merge(*tips)
    }

    /**
     * Create an iterator to walk the merge base.
     *
     * @return an iterator over the caller-specified merge base, or the natural
     * merge base of the two input commits.
     * @throws java.io.IOException
     * if an IO error occurred
     */
    @Throws(IOException::class)
    protected fun mergeBase(): AbstractTreeIterator {
        if (baseTree != null) {
            return openTree(baseTree)
        }
        val baseCommit = if ((baseCommitId != null)) walk
            .parseCommit(baseCommitId) else getBaseCommit(
            sourceCommits!![0]!!,
            sourceCommits!![1]!!
        )
        if (baseCommit == null) {
            baseCommitId = null
            return EmptyTreeIterator()
        }
        baseCommitId = baseCommit.toObjectId()
        return openTree(baseCommit.tree)
    }
}
