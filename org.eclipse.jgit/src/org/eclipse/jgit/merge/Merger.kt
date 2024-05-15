/*
 * Copyright (C) 2008-2013, Google Inc.
 * Copyright (C) 2016, Laurent Delaigue <laurent.delaigue@obeo.fr> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.merge

import org.eclipse.jgit.annotations.Nullable
import org.eclipse.jgit.errors.IncorrectObjectTypeException
import org.eclipse.jgit.errors.NoMergeBaseException
import org.eclipse.jgit.errors.NoMergeBaseException.MergeBaseFailureReason
import org.eclipse.jgit.internal.JGitText
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevObject
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.filter.RevFilter
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import java.io.IOException
import java.text.MessageFormat

/**
 * Instance of a specific [org.eclipse.jgit.merge.MergeStrategy] for a
 * single [org.eclipse.jgit.lib.Repository].
 */
abstract class Merger {
    /**
     * Get the repository this merger operates on.
     *
     * @return the repository this merger operates on.
     */
    /**
     * The repository this merger operates on.
     *
     *
     * Null if and only if the merger was constructed with [ ][.Merger]. Callers that want to assume the repo is not null
     * (e.g. because of a previous check that the merger is not in-core) may use
     * [.nonNullRepo].
     */
    @get:Nullable
    @Nullable
    val repository: Repository?

    /** Reader to support [.walk] and other object loading.  */
    protected var reader: ObjectReader

    /** A RevWalk for computing merge bases, or listing incoming commits.  */
    protected var walk: RevWalk

    private var inserter: ObjectInserter

    /** The original objects supplied in the merge; this can be any tree-ish.  */
    protected var sourceObjects: Array<RevObject?>? = null

    /** If [.sourceObjects][i] is a commit, this is the commit.  */
    protected var sourceCommits: Array<RevCommit?>? = null

    /** The trees matching every entry in [.sourceObjects].  */
    protected var sourceTrees: Array<RevTree?>? = null

    /**
     * A progress monitor.
     *
     * @since 4.2
     */
    protected var monitor: ProgressMonitor = NullProgressMonitor.INSTANCE

    /**
     * Create a new merge instance for a repository.
     *
     * @param local
     * the repository this merger will read and write data on.
     */
    protected constructor(local: Repository?) {
        if (local == null) {
            throw NullPointerException(JGitText.get().repositoryIsRequired)
        }
        repository = local
        inserter = local.newObjectInserter()
        reader = inserter.newReader()
        walk = RevWalk(reader)
    }

    /**
     * Create a new in-core merge instance from an inserter.
     *
     * @param oi
     * the inserter to write objects to. Will be closed at the
     * conclusion of `merge`, unless `flush` is false.
     * @since 4.8
     */
    protected constructor(oi: ObjectInserter) {
        repository = null
        inserter = oi
        reader = oi.newReader()
        walk = RevWalk(reader)
    }

    /**
     * Get non-null repository instance
     *
     * @return non-null repository instance
     * @throws java.lang.NullPointerException
     * if the merger was constructed without a repository.
     * @since 4.8
     */
    protected fun nonNullRepo(): Repository {
        if (repository == null) {
            throw NullPointerException(JGitText.get().repositoryIsRequired)
        }
        return repository!!
    }

    var objectInserter: ObjectInserter
        /**
         * Get an object writer to create objects, writing objects to
         * [.getRepository]
         *
         * @return an object writer to create objects, writing objects to
         * [.getRepository] (if a repository was provided).
         */
        get() = inserter
        /**
         * Set the inserter this merger will use to create objects.
         *
         *
         * If an inserter was already set on this instance (such as by a prior set,
         * or a prior call to [.getObjectInserter]), the prior inserter as
         * well as the in-progress walk will be released.
         *
         * @param oi
         * the inserter instance to use. Must be associated with the
         * repository instance returned by [.getRepository] (if a
         * repository was provided). Will be closed at the conclusion of
         * `merge`, unless `flush` is false.
         */
        set(oi) {
            walk.close()
            reader.close()
            inserter.close()
            inserter = oi
            reader = oi.newReader()
            walk = RevWalk(reader)
        }

    /**
     * Merge together two or more tree-ish objects.
     *
     *
     * Any tree-ish may be supplied as inputs. Commits and/or tags pointing at
     * trees or commits may be passed as input objects.
     *
     * @param tips
     * source trees to be combined together. The merge base is not
     * included in this set.
     * @return true if the merge was completed without conflicts; false if the
     * merge strategy cannot handle this merge or there were conflicts
     * preventing it from automatically resolving all paths.
     * @throws IncorrectObjectTypeException
     * one of the input objects is not a commit, but the strategy
     * requires it to be a commit.
     * @throws java.io.IOException
     * one or more sources could not be read, or outputs could not
     * be written to the Repository.
     */
    @Throws(IOException::class)
    open fun merge(vararg tips: AnyObjectId): Boolean {
        return merge(true, *tips)
    }

    /**
     * Merge together two or more tree-ish objects.
     *
     *
     * Any tree-ish may be supplied as inputs. Commits and/or tags pointing at
     * trees or commits may be passed as input objects.
     *
     * @since 3.5
     * @param flush
     * whether to flush and close the underlying object inserter when
     * finished to store any content-merged blobs and virtual merged
     * bases; if false, callers are responsible for flushing.
     * @param tips
     * source trees to be combined together. The merge base is not
     * included in this set.
     * @return true if the merge was completed without conflicts; false if the
     * merge strategy cannot handle this merge or there were conflicts
     * preventing it from automatically resolving all paths.
     * @throws IncorrectObjectTypeException
     * one of the input objects is not a commit, but the strategy
     * requires it to be a commit.
     * @throws java.io.IOException
     * one or more sources could not be read, or outputs could not
     * be written to the Repository.
     */
    @Throws(IOException::class)
    fun merge(flush: Boolean, vararg tips: AnyObjectId?): Boolean {
        sourceObjects = arrayOfNulls(tips.size)
        val sourceObjects = sourceObjects!!
        for (i in tips.indices) sourceObjects[i] = walk.parseAny(tips[i])

        sourceCommits = arrayOfNulls(sourceObjects.size)
        val sourceCommits = sourceCommits!!
        for (i in sourceObjects.indices) {
            try {
                sourceCommits[i] = walk.parseCommit(sourceObjects[i])
            } catch (err: IncorrectObjectTypeException) {
                sourceCommits[i] = null
            }
        }

        sourceTrees = arrayOfNulls(sourceObjects.size)
        val sourceTrees = sourceTrees!!
        for (i in sourceObjects.indices) sourceTrees[i] = walk.parseTree(sourceObjects[i])

        try {
            val ok = mergeImpl()
            if (ok && flush) inserter.flush()
            return ok
        } finally {
            if (flush) inserter.close()
            reader.close()
        }
    }

    /**
     * Get the ID of the commit that was used as merge base for merging
     *
     * @return the ID of the commit that was used as merge base for merging, or
     * null if no merge base was used or it was set manually
     * @since 3.2
     */
    abstract val baseCommitId: ObjectId?

    /**
     * Return the merge base of two commits.
     *
     * @param a
     * the first commit in [.sourceObjects].
     * @param b
     * the second commit in [.sourceObjects].
     * @return the merge base of two commits
     * @throws org.eclipse.jgit.errors.IncorrectObjectTypeException
     * one of the input objects is not a commit.
     * @throws java.io.IOException
     * objects are missing or multiple merge bases were found.
     * @since 3.0
     */
    @Throws(IncorrectObjectTypeException::class, IOException::class)
    protected open fun getBaseCommit(a: RevCommit, b: RevCommit): RevCommit? {
        walk.reset()
        walk.revFilter = RevFilter.MERGE_BASE
        walk.markStart(a)
        walk.markStart(b)
        val base = walk.next() ?: return null
        val base2 = walk.next()
        if (base2 != null) {
            throw NoMergeBaseException(
                MergeBaseFailureReason.MULTIPLE_MERGE_BASES_NOT_SUPPORTED,
                MessageFormat.format(
                    JGitText.get().multipleMergeBasesFor, a.name(), b.name(),
                    base.name(), base2.name()
                )
            )
        }
        return base
    }

    /**
     * Open an iterator over a tree.
     *
     * @param treeId
     * the tree to scan; must be a tree (not a treeish).
     * @return an iterator for the tree.
     * @throws org.eclipse.jgit.errors.IncorrectObjectTypeException
     * the input object is not a tree.
     * @throws java.io.IOException
     * the tree object is not found or cannot be read.
     */
    @Throws(IncorrectObjectTypeException::class, IOException::class)
    protected fun openTree(treeId: AnyObjectId?): AbstractTreeIterator {
        return CanonicalTreeParser(null, reader, treeId)
    }

    /**
     * Execute the merge.
     *
     *
     * This method is called from [.merge] after the
     * [.sourceObjects], [.sourceCommits] and [.sourceTrees]
     * have been populated.
     *
     * @return true if the merge was completed without conflicts; false if the
     * merge strategy cannot handle this merge or there were conflicts
     * preventing it from automatically resolving all paths.
     * @throws IncorrectObjectTypeException
     * one of the input objects is not a commit, but the strategy
     * requires it to be a commit.
     * @throws java.io.IOException
     * one or more sources could not be read, or outputs could not
     * be written to the Repository.
     */
    @Throws(IOException::class)
    protected abstract fun mergeImpl(): Boolean

    /**
     * Get resulting tree.
     *
     * @return resulting tree, if [.merge] returned true.
     */
    abstract val resultTreeId: ObjectId?

    /**
     * Set a progress monitor.
     *
     * @param monitor
     * Monitor to use, can be null to indicate no progress reporting
     * is desired.
     * @since 4.2
     */
    fun setProgressMonitor(monitor: ProgressMonitor?) {
        if (monitor == null) {
            this.monitor = NullProgressMonitor.INSTANCE
        } else {
            this.monitor = monitor
        }
    }
}
