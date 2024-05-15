/*
 * Copyright (C) 2012, Research In Motion Limited
 * Copyright (C) 2012, Christian Halstrick <christian.halstrick@sap.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
/*
 * Contributors:
 *    George Young - initial API and implementation
 *    Christian Halstrick - initial API and implementation
 */
package org.eclipse.jgit.merge

import org.eclipse.jgit.dircache.DirCache
import org.eclipse.jgit.errors.IncorrectObjectTypeException
import org.eclipse.jgit.errors.NoMergeBaseException
import org.eclipse.jgit.internal.JGitText
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.merge.RecursiveMerger
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.filter.RevFilter
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import java.io.IOException
import java.text.MessageFormat
import java.util.*
import kotlin.math.max

/**
 * A three-way merger performing a content-merge if necessary across multiple
 * bases using recursion
 *
 * This merger extends the resolve merger and does several things differently:
 *
 * - allow more than one merge base, up to a maximum
 *
 * - uses "Lists" instead of Arrays for chained types
 *
 * - recursively merges the merge bases together to compute a usable base
 *
 * @since 3.0
 */
class RecursiveMerger : ResolveMerger {

    /**
     * Normal recursive merge when you want a choice of DirCache placement
     * inCore
     *
     * @param local
     * a [org.eclipse.jgit.lib.Repository] object.
     * @param inCore
     * a boolean.
     */
    /**
     * Normal recursive merge, implies not inCore
     *
     * @param local a [org.eclipse.jgit.lib.Repository] object.
     */
    constructor(local: Repository, inCore: Boolean = false) : super(local, inCore)

    /**
     * Normal recursive merge, implies inCore.
     *
     * @param inserter
     * an [org.eclipse.jgit.lib.ObjectInserter] object.
     * @param config
     * the repository configuration
     * @since 4.8
     */
    constructor(inserter: ObjectInserter?, config: Config) : super(inserter, config)

    /**
     * {@inheritDoc}
     *
     *
     * Get a single base commit for two given commits. If the two source commits
     * have more than one base commit recursively merge the base commits
     * together until you end up with a single base commit.
     */
    @Throws(IncorrectObjectTypeException::class, IOException::class)
    override fun getBaseCommit(a: RevCommit, b: RevCommit): RevCommit? {
        return getBaseCommit(a, b, 0)
    }

    /**
     * Get a single base commit for two given commits. If the two source commits
     * have more than one base commit recursively merge the base commits
     * together until a virtual common base commit has been found.
     *
     * @param a
     * the first commit to be merged
     * @param b
     * the second commit to be merged
     * @param callDepth
     * the callDepth when this method is called recursively
     * @return the merge base of two commits. If a criss-cross merge required a
     * synthetic merge base this commit is visible only the merger's
     * RevWalk and will not be in the repository.
     * @throws java.io.IOException
     * if an IO error occurred
     * @throws IncorrectObjectTypeException
     * one of the input objects is not a commit.
     * @throws NoMergeBaseException
     * too many merge bases are found or the computation of a common
     * merge base failed (e.g. because of a conflict).
     */
    @Throws(IOException::class)
    protected fun getBaseCommit(a: RevCommit, b: RevCommit, callDepth: Int): RevCommit? {
        val baseCommits = ArrayList<RevCommit>()
        walk.reset()
        walk.revFilter = RevFilter.MERGE_BASE
        walk.markStart(a)
        walk.markStart(b)
        var c: RevCommit?
        while ((walk.next().also { c = it }) != null) baseCommits.add(c!!)

        if (baseCommits.isEmpty()) return null
        if (baseCommits.size == 1) return baseCommits[0]
        if (baseCommits.size >= Companion.MAX_BASES) throw NoMergeBaseException(
            NoMergeBaseException.MergeBaseFailureReason.TOO_MANY_MERGE_BASES, MessageFormat.format(
                JGitText.get().mergeRecursiveTooManyMergeBasesFor,
                Companion.MAX_BASES, a.name(), b.name(),
                baseCommits.size
            )
        )

        // We know we have more than one base commit. We have to do merges now
        // to determine a single base commit. We don't want to spoil the current
        // dircache and working tree with the results of this intermediate
        // merges. Therefore set the dircache to a new in-memory dircache and
        // disable that we update the working-tree. We set this back to the
        // original values once a single base commit is created.
        var currentBase = baseCommits[0]
        val oldDircache = dircache
        val oldIncore = inCore
        val oldWTreeIt = workingTreeIterator
        workingTreeIterator = null
        try {
            dircache = DirCache.read(reader, currentBase.tree)
            inCore = true

            val parents: MutableList<RevCommit> = ArrayList()
            parents.add(currentBase)
            for (commitIdx in 1 until baseCommits.size) {
                val nextBase = baseCommits[commitIdx]
                if (commitIdx >= Companion.MAX_BASES) throw NoMergeBaseException(
                    NoMergeBaseException.MergeBaseFailureReason.TOO_MANY_MERGE_BASES,
                    MessageFormat.format(
                        JGitText.get().mergeRecursiveTooManyMergeBasesFor,
                        Companion.MAX_BASES, a.name(), b.name(),
                        baseCommits.size
                    )
                )
                parents.add(nextBase)
                val bc = getBaseCommit(
                    currentBase, nextBase,
                    callDepth + 1
                )
                val bcTree = if ((bc == null)) EmptyTreeIterator()
                else openTree(bc.tree)
                if (mergeTrees(
                        bcTree, currentBase.tree,
                        nextBase.tree, true
                    )
                ) currentBase = createCommitForTree(resultTree!!, parents)
                else throw NoMergeBaseException(
                    NoMergeBaseException.MergeBaseFailureReason.CONFLICTS_DURING_MERGE_BASE_CALCULATION,
                    MessageFormat.format(
                        JGitText.get().mergeRecursiveConflictsWhenMergingCommonAncestors,
                        currentBase.name, nextBase.name
                    )
                )
            }
        } finally {
            inCore = oldIncore
            dircache = oldDircache
            workingTreeIterator = oldWTreeIt
            unmergedPaths.clear()
            mergeResults.clear()
            failingPaths.clear()
        }
        return currentBase
    }

    /**
     * Create a new commit by explicitly specifying the content tree and the
     * parents. The commit message is not set and author/committer are set to
     * the current user.
     *
     * @param tree
     * the tree this commit should capture
     * @param parents
     * the list of parent commits
     * @return a new commit visible only within this merger's RevWalk.
     * @throws IOException
     * if an IO error occurred
     */
    @Throws(IOException::class)
    private fun createCommitForTree(tree: ObjectId, parents: List<RevCommit>): RevCommit {
        val c = CommitBuilder()
        c.setTreeId(tree)
        c.setParentIds(parents)
        c.author = mockAuthor(parents)
        c.committer = c.author
        return RevCommit.parse(walk, c.build())
    }

    companion object {
        private fun mockAuthor(parents: List<RevCommit>): PersonIdent {
            val name = RecursiveMerger::class.java.simpleName
            var time = 0
            for (p in parents) time = max(time.toDouble(), p.commitTime.toDouble()).toInt()
            return PersonIdent(
                name, "$name@JGit",  //$NON-NLS-1$
                Date((time + 1) * 1000L),
                TimeZone.getTimeZone("GMT+0000")
            ) //$NON-NLS-1$
        }

        /**
         * The maximum number of merge bases. This merge will stop when the number
         * of merge bases exceeds this value
         */
        const val MAX_BASES: Int = 200
    }
}
