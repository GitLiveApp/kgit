/*
 * Copyright (C) 2010, Christian Halstrick <christian.halstrick@sap.com>,
 * Copyright (C) 2010-2012, Matthias Sohn <matthias.sohn@sap.com>
 * Copyright (C) 2012, Research In Motion Limited
 * Copyright (C) 2017, Obeo (mathieu.cartaud@obeo.fr)
 * Copyright (C) 2018, 2023 Thomas Wolf <twolf@apache.org>
 * Copyright (C) 2023, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.merge

import org.eclipse.jgit.annotations.NonNull
import org.eclipse.jgit.annotations.Nullable
import org.eclipse.jgit.attributes.Attribute
import org.eclipse.jgit.attributes.Attributes
import org.eclipse.jgit.diff.DiffAlgorithm.Companion.getAlgorithm
import org.eclipse.jgit.diff.DiffAlgorithm.SupportedAlgorithm
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.diff.RawText.Companion.load
import org.eclipse.jgit.diff.RawTextComparator
import org.eclipse.jgit.diff.Sequence
import org.eclipse.jgit.dircache.*
import org.eclipse.jgit.dircache.DirCacheCheckout.CheckoutMetadata
import org.eclipse.jgit.dircache.DirCacheCheckout.StreamSupplier
import org.eclipse.jgit.errors.BinaryBlobException
import org.eclipse.jgit.errors.IndexWriteException
import org.eclipse.jgit.errors.NoWorkTreeException
import org.eclipse.jgit.internal.JGitText
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.lib.CoreConfig.EolStreamType
import org.eclipse.jgit.lib.ObjectId.Companion.zeroId
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.storage.pack.PackConfig
import org.eclipse.jgit.submodule.SubmoduleConflict
import org.eclipse.jgit.treewalk.*
import org.eclipse.jgit.treewalk.TreeWalk.OperationType
import org.eclipse.jgit.treewalk.filter.TreeFilter
import org.eclipse.jgit.util.LfsFactory
import org.eclipse.jgit.util.TemporaryBuffer
import org.eclipse.jgit.util.TemporaryBuffer.LocalFile
import org.eclipse.jgit.util.io.EolStreamTypeUtil
import java.io.*
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*

/**
 * A three-way merger performing a content-merge if necessary
 */
open class ResolveMerger : ThreeWayMerger {
    /**
     * Handles work tree updates on both the checkout and the index.
     *
     *
     * You should use a single instance for all of your file changes. In case of
     * an error, make sure your instance is released, and initiate a new one if
     * necessary.
     *
     * @since 6.3.1
     */
    protected class WorkTreeUpdater : Closeable {
        /**
         * The result of writing the index changes.
         */
        class Result {
            /**
             * Get path of modified files
             *
             * @return Files modified during this operation.
             */
            val modifiedFiles: MutableList<String> = ArrayList()

            /**
             * Get path of files that couldn't be deleted
             *
             * @return Files that couldn't be deleted.
             */
            val failedToDelete: MutableList<String> = ArrayList()

            /**
             * Get modified tree id if any
             *
             * @return Modified tree ID if any, or null otherwise.
             */
            var treeId: ObjectId? = null
        }

        var result: Result = Result()

        /**
         * The repository this handler operates on.
         */
        @Nullable
        private val repo: Repository?

        /**
         * Set to true if this operation should work in-memory. The repo's
         * dircache and workingtree are not touched by this method. Eventually
         * needed files are created as temporary files and a new empty,
         * in-memory dircache will be used instead the repo's one. Often used
         * for bare repos where the repo doesn't even have a workingtree and
         * dircache.
         */
        private val inCore: Boolean

        private val inserter: ObjectInserter

        private val reader: ObjectReader

        private var dirCache: DirCache?

        private var implicitDirCache = false

        /**
         * Builder to update the dir cache during this operation.
         */
        private var builder: DirCacheBuilder? = null

        /**
         * The [WorkingTreeOptions] are needed to determine line endings
         * for affected files.
         */
        private var workingTreeOptions: WorkingTreeOptions? = null

        /**
         * Gets the size limit for in-core files in this config.
         *
         * @return the size
         */
        /**
         * The size limit (bytes) which controls a file to be stored in
         * `Heap` or `LocalFile` during the operation.
         */
        var inCoreFileSizeLimit: Int = 0
            private set

        /**
         * If the operation has nothing to do for a file but check it out at the
         * end of the operation, it can be added here.
         */
        private val toBeCheckedOut: MutableMap<String, DirCacheEntry> = HashMap()

        /**
         * Files in this list will be deleted from the local copy at the end of
         * the operation.
         */
        private val toBeDeleted = TreeMap<String, File?>()

        /**
         * Keeps [CheckoutMetadata] for [.checkout].
         */
        private var checkoutMetadataByPath: MutableMap<String, CheckoutMetadata>? = null

        /**
         * Keeps [CheckoutMetadata] for [.revertModifiedFiles].
         */
        private var cleanupMetadataByPath: MutableMap<String, CheckoutMetadata>? = null

        /**
         * Whether the changes were successfully written.
         */
        private var indexChangesWritten = false

        /**
         * [Checkout] to use for actually checking out files if
         * [.inCore] is `false`.
         */
        private var checkout: Checkout? = null

        /**
         * @param repo
         * the [Repository].
         * @param dirCache
         * if set, use the provided dir cache. Otherwise, use the
         * default repository one
         */
        private constructor(repo: Repository, dirCache: DirCache?) {
            this.repo = repo
            this.dirCache = dirCache

            this.inCore = false
            this.inserter = repo.newObjectInserter()
            this.reader = inserter.newReader()
            val config: Config = repo.config
            this.workingTreeOptions = config.get(WorkingTreeOptions.KEY)
            this.inCoreFileSizeLimit = getInCoreFileSizeLimit(config)
            this.checkoutMetadataByPath = HashMap()
            this.cleanupMetadataByPath = HashMap()
            this.checkout = Checkout(nonNullRepo(), workingTreeOptions)
        }

        /**
         * @param repo
         * the [Repository].
         * @param dirCache
         * if set, use the provided dir cache. Otherwise, creates a
         * new one
         * @param oi
         * to use for writing the modified objects with.
         */
        private constructor(
            repo: Repository?, dirCache: DirCache?,
            oi: ObjectInserter
        ) {
            this.repo = repo
            this.dirCache = dirCache
            this.inserter = oi

            this.inCore = true
            this.reader = oi.newReader()
            if (repo != null) {
                this.inCoreFileSizeLimit = getInCoreFileSizeLimit(
                    repo.config
                )
            }
        }

        @get:Throws(IOException::class)
        val lockedDirCache: DirCache?
            /**
             * Gets dir cache for the repo. Locked if not inCore.
             *
             * @return the result dir cache
             * @throws IOException
             * is case the dir cache cannot be read
             */
            get() {
                if (dirCache == null) {
                    implicitDirCache = true
                    dirCache = if (inCore) {
                        DirCache.newInCore()
                    } else {
                        nonNullRepo()!!.lockDirCache()
                    }
                }
                if (builder == null) {
                    builder = dirCache!!.builder()
                }
                return dirCache
            }

        /**
         * Creates a [DirCacheBuildIterator] for the builder of this
         * [WorkTreeUpdater].
         *
         * @return the [DirCacheBuildIterator]
         */
        fun createDirCacheBuildIterator(): DirCacheBuildIterator {
            return DirCacheBuildIterator(builder)
        }

        /**
         * Writes the changes to the working tree (but not to the index).
         *
         * @param shouldCheckoutTheirs
         * before committing the changes
         * @throws IOException
         * if any of the writes fail
         */
        @Throws(IOException::class)
        fun writeWorkTreeChanges(shouldCheckoutTheirs: Boolean) {
            handleDeletedFiles()

            if (inCore) {
                builder!!.finish()
                return
            }
            if (shouldCheckoutTheirs) {
                // No problem found. The only thing left to be done is to
                // check out all files from "theirs" which have been selected to
                // go into the new index.
                checkout()
            }

            // All content operations are successfully done. If we can now write
            // the new index we are on quite safe ground. Even if the checkout
            // of files coming from "theirs" fails the user can work around such
            // failures by checking out the index again.
            if (!builder!!.commit()) {
                revertModifiedFiles()
                throw IndexWriteException()
            }
        }

        /**
         * Writes the changes to the index.
         *
         * @return the [Result] of the operation.
         * @throws IOException
         * if any of the writes fail
         */
        @Throws(IOException::class)
        fun writeIndexChanges(): Result {
            result.treeId = lockedDirCache!!.writeTree(inserter)
            indexChangesWritten = true
            return result
        }

        /**
         * Adds a [DirCacheEntry] for direct checkout and remembers its
         * [CheckoutMetadata].
         *
         * @param path
         * of the entry
         * @param entry
         * to add
         * @param cleanupStreamType
         * to use for the cleanup metadata
         * @param cleanupSmudgeCommand
         * to use for the cleanup metadata
         * @param checkoutStreamType
         * to use for the checkout metadata
         * @param checkoutSmudgeCommand
         * to use for the checkout metadata
         */
        fun addToCheckout(
            path: String, entry: DirCacheEntry?,
            cleanupStreamType: EolStreamType?, cleanupSmudgeCommand: String?,
            checkoutStreamType: EolStreamType?,
            checkoutSmudgeCommand: String?
        ) {
            if (entry != null) {
                // In some cases, we just want to add the metadata.
                toBeCheckedOut[path] = entry
            }
            addCheckoutMetadata(
                cleanupMetadataByPath, path, cleanupStreamType,
                cleanupSmudgeCommand
            )
            addCheckoutMetadata(
                checkoutMetadataByPath, path,
                checkoutStreamType, checkoutSmudgeCommand
            )
        }

        /**
         * Gets a map which maps the paths of files which have to be checked out
         * because the operation created new fully-merged content for this file
         * into the index.
         *
         *
         * This means: the operation wrote a new stage 0 entry for this path.
         *
         *
         * @return the map
         */
        fun getToBeCheckedOut(): Map<String, DirCacheEntry> {
            return toBeCheckedOut
        }

        /**
         * Remembers the given file to be deleted.
         *
         *
         * Note the actual deletion is only done in
         * [.writeWorkTreeChanges].
         *
         * @param path
         * of the file to be deleted
         * @param file
         * to be deleted
         * @param streamType
         * to use for cleanup metadata
         * @param smudgeCommand
         * to use for cleanup metadata
         */
        fun deleteFile(
            path: String, file: File?, streamType: EolStreamType?,
            smudgeCommand: String?
        ) {
            toBeDeleted[path] = file
            if (file != null && file.isFile) {
                addCheckoutMetadata(
                    cleanupMetadataByPath, path, streamType,
                    smudgeCommand
                )
            }
        }

        /**
         * Remembers the [CheckoutMetadata] for the given path; it may be
         * needed in [.checkout] or in [.revertModifiedFiles].
         *
         * @param map
         * to add the metadata to
         * @param path
         * of the current node
         * @param streamType
         * to use for the metadata
         * @param smudgeCommand
         * to use for the metadata
         */
        private fun addCheckoutMetadata(
            map: MutableMap<String, CheckoutMetadata>?,
            path: String, streamType: EolStreamType?, smudgeCommand: String?
        ) {
            if (inCore || map == null) {
                return
            }
            map[path] = CheckoutMetadata(streamType, smudgeCommand)
        }

        /**
         * Detects if CRLF conversion has been configured.
         *
         *
         * See [EolStreamTypeUtil.detectStreamType] for more info.
         *
         * @param attributes
         * of the file for which the type is to be detected
         * @return the detected type
         */
        fun detectCheckoutStreamType(attributes: Attributes?): EolStreamType? {
            if (inCore) {
                return null
            }
            return EolStreamTypeUtil.detectStreamType(
                OperationType.CHECKOUT_OP,
                workingTreeOptions, attributes
            )
        }

        private fun handleDeletedFiles() {
            // Iterate in reverse so that "folder/file" is deleted before
            // "folder". Otherwise, this could result in a failing path because
            // of a non-empty directory, for which delete() would fail.
            for (path in toBeDeleted.descendingKeySet()) {
                val file = if (inCore) null else toBeDeleted[path]
                if (file != null && !file.delete()) {
                    if (!file.isDirectory) {
                        result.failedToDelete.add(path)
                    }
                }
            }
        }

        /**
         * Marks the given path as modified in the operation.
         *
         * @param path
         * to mark as modified
         */
        fun markAsModified(path: String) {
            result.modifiedFiles.add(path)
        }

        /**
         * Gets the list of files which were modified in this operation.
         *
         * @return the list
         */
        fun getModifiedFiles(): List<String> {
            return result.modifiedFiles
        }

        @Throws(NoWorkTreeException::class, IOException::class)
        private fun checkout() {
            for ((gitPath, dirCacheEntry) in toBeCheckedOut) {
                if (dirCacheEntry.fileMode === FileMode.GITLINK) {
                    checkout!!.checkoutGitlink(dirCacheEntry, gitPath)
                } else {
                    checkout!!.checkout(
                        dirCacheEntry,
                        checkoutMetadataByPath!![gitPath], reader,
                        gitPath
                    )
                    result.modifiedFiles.add(gitPath)
                }
            }
        }

        /**
         * Reverts any uncommitted changes in the worktree. We know that for all
         * modified files the old content was in the old index and the index
         * contained only stage 0. In case of inCore operation just clear the
         * history of modified files.
         *
         * @throws IOException
         * in case the cleaning up failed
         */
        @Throws(IOException::class)
        fun revertModifiedFiles() {
            if (inCore) {
                result.modifiedFiles.clear()
                return
            }
            if (indexChangesWritten) {
                return
            }
            for (path in result.modifiedFiles) {
                val entry = dirCache!!.getEntry(path)
                if (entry != null) {
                    checkout!!.checkout(
                        entry, cleanupMetadataByPath!![path],
                        reader, path
                    )
                }
            }
        }

        @Throws(IOException::class)
        override fun close() {
            if (implicitDirCache) {
                dirCache!!.unlock()
            }
        }

        /**
         * Updates the file in the checkout with the given content.
         *
         * @param inputStream
         * the content to be updated
         * @param streamType
         * for parsing the content
         * @param smudgeCommand
         * for formatting the content
         * @param path
         * of the file to be updated
         * @param file
         * to be updated
         * @throws IOException
         * if the file cannot be updated
         */
        @Throws(IOException::class)
        fun updateFileWithContent(
            inputStream: StreamSupplier?,
            streamType: EolStreamType?, smudgeCommand: String?, path: String?,
            file: File
        ) {
            if (inCore) {
                return
            }
            checkout!!.safeCreateParentDirectory(
                path, file.parentFile,
                false
            )
            val metadata = CheckoutMetadata(
                streamType,
                smudgeCommand
            )

            FileOutputStream(file).use { outputStream ->
                DirCacheCheckout.getContent(
                    repo, path, metadata, inputStream,
                    workingTreeOptions, outputStream
                )
            }
        }

        /**
         * Creates a path with the given content, and adds it to the specified
         * stage to the index builder.
         *
         * @param input
         * the content to be updated
         * @param path
         * of the file to be updated
         * @param fileMode
         * of the modified file
         * @param entryStage
         * of the new entry
         * @param lastModified
         * instant of the modified file
         * @param len
         * of the content
         * @param lfsAttribute
         * for checking for LFS enablement
         * @return the entry which was added to the index
         * @throws IOException
         * if inserting the content fails
         */
        @Throws(IOException::class)
        fun insertToIndex(
            input: InputStream, path: ByteArray?,
            fileMode: FileMode?, entryStage: Int, lastModified: Instant?,
            len: Int, lfsAttribute: Attribute?
        ): DirCacheEntry {
            return addExistingToIndex(
                insertResult(input, lfsAttribute, len.toLong()),
                path, fileMode, entryStage, lastModified, len
            )
        }

        /**
         * Adds a path with the specified stage to the index builder.
         *
         * @param objectId
         * of the existing object to add
         * @param path
         * of the modified file
         * @param fileMode
         * of the modified file
         * @param entryStage
         * of the new entry
         * @param lastModified
         * instant of the modified file
         * @param len
         * of the modified file content
         * @return the entry which was added to the index
         */
        fun addExistingToIndex(
            objectId: ObjectId?, path: ByteArray?,
            fileMode: FileMode?, entryStage: Int, lastModified: Instant?,
            len: Int
        ): DirCacheEntry {
            val dce = DirCacheEntry(path, entryStage)
            dce.fileMode = fileMode
            if (lastModified != null) {
                dce.setLastModified(lastModified)
            }
            dce.length = if (inCore) 0 else len
            dce.setObjectId(objectId)
            builder!!.add(dce)
            return dce
        }

        @Throws(IOException::class)
        private fun insertResult(
            input: InputStream, lfsAttribute: Attribute?,
            length: Long
        ): ObjectId {
            LfsFactory.getInstance()
                .applyCleanFilter(repo, input, length, lfsAttribute).use { `is` ->
                    return inserter.insert(Constants.OBJ_BLOB, `is`.length, `is`)
                }
        }

        /**
         * Gets the non-null repository instance of this
         * [WorkTreeUpdater].
         *
         * @return non-null repository instance
         * @throws NullPointerException
         * if the handler was constructed without a repository.
         */
        @NonNull
        @Throws(NullPointerException::class)
        private fun nonNullRepo(): Repository? {
            return Objects.requireNonNull(
                repo
            ) { JGitText.get().repositoryIsRequired }
        }

        companion object {
            /**
             * Creates a new [WorkTreeUpdater] for the given repository.
             *
             * @param repo
             * the [Repository].
             * @param dirCache
             * if set, use the provided dir cache. Otherwise, use the
             * default repository one
             * @return the [WorkTreeUpdater].
             */
            fun createWorkTreeUpdater(
                repo: Repository,
                dirCache: DirCache?
            ): WorkTreeUpdater {
                return WorkTreeUpdater(repo, dirCache)
            }

            /**
             * Creates a new [WorkTreeUpdater] that works in memory only.
             *
             * @param repo
             * the [Repository].
             * @param dirCache
             * if set, use the provided dir cache. Otherwise, creates a
             * new one
             * @param oi
             * to use for writing the modified objects with.
             * @return the [WorkTreeUpdater]
             */
            fun createInCoreWorkTreeUpdater(
                repo: Repository?, dirCache: DirCache?, oi: ObjectInserter
            ): WorkTreeUpdater {
                return WorkTreeUpdater(repo, dirCache, oi)
            }

            private fun getInCoreFileSizeLimit(config: Config): Int {
                return config.getInt(
                    ConfigConstants.CONFIG_MERGE_SECTION,
                    ConfigConstants.CONFIG_KEY_IN_CORE_LIMIT, 10 shl 20
                )
            }
        }
    }

    /**
     * If the merge fails (means: not stopped because of unresolved conflicts)
     * this enum is used to explain why it failed
     */
    enum class MergeFailureReason {
        /** the merge failed because of a dirty index  */
        DIRTY_INDEX,

        /** the merge failed because of a dirty workingtree  */
        DIRTY_WORKTREE,

        /** the merge failed because of a file could not be deleted  */
        COULD_NOT_DELETE
    }

    /**
     * The tree walk which we'll iterate over to merge entries.
     *
     * @since 3.4
     */
    protected var tw: NameConflictTreeWalk? = null

    /**
     * Get the names of the commits as they would appear in conflict markers.
     *
     * @return the names of the commits as they would appear in conflict
     * markers.
     */
    /**
     * Set the names of the commits as they would appear in conflict markers
     *
     * @param commitNames
     * the names of the commits as they would appear in conflict
     * markers
     */
    /**
     * string versions of a list of commit SHA1s
     *
     * @since 3.0
     */
    var commitNames: Array<String>

    /**
     * Handler for repository I/O actions.
     *
     * @since 6.3
     */
    protected var workTreeUpdater: WorkTreeUpdater? = null

    /**
     * merge result as tree
     *
     * @since 3.0
     */
    protected var resultTree: ObjectId? = null

    /**
     * Files modified during this operation. Note this list is only updated after a successful write.
     */
    @JvmField
    protected var modifiedFiles: List<String> = ArrayList()

    /**
     * Paths that could not be merged by this merger because of an unsolvable
     * conflict.
     *
     * @since 3.4
     */
    @JvmField
    protected var unmergedPaths: MutableList<String> = ArrayList()

    /**
     * Low-level textual merge results. Will be passed on to the callers in case
     * of conflicts.
     *
     * @since 3.4
     */
    @JvmField
    protected var mergeResults: MutableMap<String, MergeResult<out Sequence>?> = HashMap()

    /**
     * Paths for which the merge failed altogether.
     *
     * @since 3.4
     */
    @JvmField
    protected var failingPaths: MutableMap<String, MergeFailureReason> = HashMap()

    /**
     * Updated as we merge entries of the tree walk. Tells us whether we should
     * recurse into the entry if it is a subtree.
     *
     * @since 3.4
     */
    protected var enterSubtree: Boolean = false

    /**
     * Set to true if this merge should work in-memory. The repos dircache and
     * workingtree are not touched by this method. Eventually needed files are
     * created as temporary files and a new empty, in-memory dircache will be
     * used instead the repo's one. Often used for bare repos where the repo
     * doesn't even have a workingtree and dircache.
     * @since 3.0
     */
    protected var inCore: Boolean

    /**
     * Directory cache
     * @since 3.0
     */
    protected var dircache: DirCache? = null

    /**
     * The iterator to access the working tree. If set to `null` this
     * merger will not touch the working tree.
     * @since 3.0
     */
    @JvmField
    protected var workingTreeIterator: WorkingTreeIterator? = null

    /**
     * our merge algorithm
     * @since 3.0
     */
    protected var mergeAlgorithm: MergeAlgorithm

    /**
     * The [ContentMergeStrategy] to use for "resolve" and "recursive"
     * merges.
     */
    @NonNull
    private var contentStrategy = ContentMergeStrategy.CONFLICT

    /**
     * Constructor for ResolveMerger.
     *
     * @param local
     * the [org.eclipse.jgit.lib.Repository].
     * @param inCore
     * a boolean.
     */
    /**
     * Constructor for ResolveMerger.
     *
     * @param local
     * the [org.eclipse.jgit.lib.Repository].
     */
    constructor(local: Repository, inCore: Boolean = false) : super(local) {
        val config: Config = local.config
        mergeAlgorithm = getMergeAlgorithm(config)
        commitNames = defaultCommitNames()
        this.inCore = inCore
    }

    /**
     * Constructor for ResolveMerger.
     *
     * @param inserter
     * an [org.eclipse.jgit.lib.ObjectInserter] object.
     * @param config
     * the repository configuration
     * @since 4.8
     */
    constructor(inserter: ObjectInserter?, config: Config) : super(inserter!!) {
        mergeAlgorithm = getMergeAlgorithm(config)
        commitNames = defaultCommitNames()
        inCore = true
    }

    @get:NonNull
    var contentMergeStrategy: ContentMergeStrategy?
        /**
         * Retrieves the content merge strategy for content conflicts.
         *
         * @return the [ContentMergeStrategy] in effect
         * @since 5.12
         */
        get() = contentStrategy
        /**
         * Sets the content merge strategy for content conflicts.
         *
         * @param strategy
         * [ContentMergeStrategy] to use
         * @since 5.12
         */
        set(strategy) {
            contentStrategy = strategy ?: ContentMergeStrategy.CONFLICT
        }

    @Throws(IOException::class)
    override fun mergeImpl(): Boolean {
        return mergeTrees(
            mergeBase(), sourceTrees!![0], sourceTrees!![1],
            false
        )
    }

    /**
     * adds a new path with the specified stage to the index builder
     *
     * @param path
     * the new path
     * @param p
     * canonical tree parser
     * @param stage
     * the stage
     * @param lastModified
     * lastModified attribute of the file
     * @param len
     * file length
     * @return the entry which was added to the index
     */
    private fun add(
        path: ByteArray, p: CanonicalTreeParser?, stage: Int,
        lastModified: Instant, len: Long
    ): DirCacheEntry? {
        if (p != null && p.entryFileMode != FileMode.TREE) {
            return workTreeUpdater!!.addExistingToIndex(
                p.entryObjectId, path,
                p.entryFileMode, stage,
                lastModified, len.toInt()
            )
        }
        return null
    }

    /**
     * Adds the conflict stages for the current path of [.tw] to the index
     * builder and returns the "theirs" stage; if present.
     *
     * @param base
     * of the conflict
     * @param ours
     * of the conflict
     * @param theirs
     * of the conflict
     * @return the [DirCacheEntry] for the "theirs" stage, or `null`
     */
    private fun addConflict(
        base: CanonicalTreeParser?,
        ours: CanonicalTreeParser?, theirs: CanonicalTreeParser?
    ): DirCacheEntry? {
        add(tw!!.rawPath, base, DirCacheEntry.STAGE_1, Instant.EPOCH, 0)
        add(tw!!.rawPath, ours, DirCacheEntry.STAGE_2, Instant.EPOCH, 0)
        return add(tw!!.rawPath, theirs, DirCacheEntry.STAGE_3, Instant.EPOCH, 0)
    }

    /**
     * adds a entry to the index builder which is a copy of the specified
     * DirCacheEntry
     *
     * @param e
     * the entry which should be copied
     *
     * @return the entry which was added to the index
     */
    private fun keep(e: DirCacheEntry?): DirCacheEntry {
        return workTreeUpdater!!.addExistingToIndex(
            e!!.objectId, e.rawPath, e.fileMode,
            e.stage, e.lastModifiedInstant, e.length
        )
    }

    /**
     * Adds a [DirCacheEntry] for direct checkout and remembers its
     * [CheckoutMetadata].
     *
     * @param path
     * of the entry
     * @param entry
     * to add
     * @param attributes
     * the [Attributes] of the trees
     * @throws IOException
     * if the [CheckoutMetadata] cannot be determined
     * @since 6.1
     */
    @Throws(IOException::class)
    protected fun addToCheckout(
        path: String, entry: DirCacheEntry?,
        attributes: Array<Attributes>
    ) {
        val cleanupStreamType = workTreeUpdater!!.detectCheckoutStreamType(attributes[T_OURS])
        val cleanupSmudgeCommand = tw!!.getSmudgeCommand(attributes[T_OURS])
        val checkoutStreamType = workTreeUpdater!!.detectCheckoutStreamType(attributes[T_THEIRS])
        val checkoutSmudgeCommand = tw!!.getSmudgeCommand(attributes[T_THEIRS])
        workTreeUpdater!!.addToCheckout(
            path, entry, cleanupStreamType, cleanupSmudgeCommand,
            checkoutStreamType, checkoutSmudgeCommand
        )
    }

    /**
     * Remember a path for deletion, and remember its [CheckoutMetadata]
     * in case it has to be restored in the cleanUp.
     *
     * @param path
     * of the entry
     * @param isFile
     * whether it is a file
     * @param attributes
     * to use for determining the [CheckoutMetadata]
     * @throws IOException
     * if the [CheckoutMetadata] cannot be determined
     * @since 5.1
     */
    @Throws(IOException::class)
    protected fun addDeletion(
        path: String, isFile: Boolean,
        attributes: Attributes?
    ) {
        if (repository == null || nonNullRepo().isBare || !isFile) return

        val file = File(nonNullRepo().workTree, path)
        val streamType = workTreeUpdater!!.detectCheckoutStreamType(attributes)
        val smudgeCommand = tw!!.getSmudgeCommand(attributes)
        workTreeUpdater!!.deleteFile(path, file, streamType, smudgeCommand)
    }

    /**
     * Processes one path and tries to merge taking git attributes in account.
     * This method will do all trivial (not content) merges and will also detect
     * if a merge will fail. The merge will fail when one of the following is
     * true
     *
     *  * the index entry does not match the entry in ours. When merging one
     * branch into the current HEAD, ours will point to HEAD and theirs will
     * point to the other branch. It is assumed that the index matches the HEAD
     * because it will only not match HEAD if it was populated before the merge
     * operation. But the merge commit should not accidentally contain
     * modifications done before the merge. Check the [git read-tree](http://www.kernel.org/pub/software/scm/git/docs/git-read-tree.html#_3_way_merge) documentation for further explanations.
     *  * A conflict was detected and the working-tree file is dirty. When a
     * conflict is detected the content-merge algorithm will try to write a
     * merged version into the working-tree. If the file is dirty we would
     * override unsaved data.
     *
     *
     * @param base
     * the common base for ours and theirs
     * @param ours
     * the ours side of the merge. When merging a branch into the
     * HEAD ours will point to HEAD
     * @param theirs
     * the theirs side of the merge. When merging a branch into the
     * current HEAD theirs will point to the branch which is merged
     * into HEAD.
     * @param index
     * the index entry
     * @param work
     * the file in the working tree
     * @param ignoreConflicts
     * see
     * [org.eclipse.jgit.merge.ResolveMerger.mergeTrees]
     * @param attributes
     * the [Attributes] for the three trees
     * @return `false` if the merge will fail because the index entry
     * didn't match ours or the working-dir file was dirty and a
     * conflict occurred
     * @throws java.io.IOException
     * if an IO error occurred
     * @since 6.1
     */
    @Throws(IOException::class)
    protected fun processEntry(
        base: CanonicalTreeParser?,
        ours: CanonicalTreeParser?, theirs: CanonicalTreeParser?,
        index: DirCacheBuildIterator?, work: WorkingTreeIterator?,
        ignoreConflicts: Boolean, attributes: Array<Attributes>
    ): Boolean {
        enterSubtree = true
        val modeO = tw!!.getRawMode(T_OURS)
        val modeT = tw!!.getRawMode(T_THEIRS)
        val modeB = tw!!.getRawMode(T_BASE)
        val gitLinkMerging = (isGitLink(modeO) || isGitLink(modeT)
            || isGitLink(modeB))
        if (modeO == 0 && modeT == 0 && modeB == 0) {
            // File is either untracked or new, staged but uncommitted
            return true
        }

        if (isIndexDirty) {
            return false
        }

        var ourDce: DirCacheEntry? = null

        if (index == null || index.dirCacheEntry == null) {
            // create a fake DCE, but only if ours is valid. ours is kept only
            // in case it is valid, so a null ourDce is ok in all other cases.
            if (nonTree(modeO)) {
                ourDce = DirCacheEntry(tw!!.rawPath)
                ourDce.setObjectId(tw!!.getObjectId(T_OURS))
                ourDce.fileMode = tw!!.getFileMode(T_OURS)
            }
        } else {
            ourDce = index.dirCacheEntry
        }

        if (nonTree(modeO) && nonTree(modeT) && tw!!.idEqual(T_OURS, T_THEIRS)) {
            // OURS and THEIRS have equal content. Check the file mode
            if (modeO == modeT) {
                // content and mode of OURS and THEIRS are equal: it doesn't
                // matter which one we choose. OURS is chosen. Since the index
                // is clean (the index matches already OURS) we can keep the existing one
                keep(ourDce)
                // no checkout needed!
                return true
            }
            // same content but different mode on OURS and THEIRS.
            // Try to merge the mode and report an error if this is
            // not possible.
            val newMode = mergeFileModes(modeB, modeO, modeT)
            if (newMode != FileMode.MISSING.bits) {
                if (newMode == modeO) {
                    // ours version is preferred
                    keep(ourDce)
                } else {
                    // the preferred version THEIRS has a different mode
                    // than ours. Check it out!
                    if (isWorktreeDirty(work, ourDce)) {
                        return false
                    }
                    // we know about length and lastMod only after we have
                    // written the new content.
                    // This will happen later. Set these values to 0 for know.
                    val e = add(
                        tw!!.rawPath, theirs,
                        DirCacheEntry.STAGE_0, Instant.EPOCH, 0
                    )
                    addToCheckout(tw!!.pathString, e, attributes)
                }
                return true
            }
            if (!ignoreConflicts) {
                // FileModes are not mergeable. We found a conflict on modes.
                // For conflicting entries we don't know lastModified and
                // length.
                // This path can be skipped on ignoreConflicts, so the caller
                // could use virtual commit.
                addConflict(base, ours, theirs)
                unmergedPaths.add(tw!!.pathString)
                mergeResults[tw!!.pathString] = MergeResult(emptyList())
            }
            return true
        }

        if (modeB == modeT && tw!!.idEqual(T_BASE, T_THEIRS)) {
            // THEIRS was not changed compared to BASE. All changes must be in
            // OURS. OURS is chosen. We can keep the existing entry.
            if (ourDce != null) {
                keep(ourDce)
            }
            // no checkout needed!
            return true
        }

        if (modeB == modeO && tw!!.idEqual(T_BASE, T_OURS)) {
            // OURS was not changed compared to BASE. All changes must be in
            // THEIRS. THEIRS is chosen.

            // Check worktree before checking out THEIRS

            if (isWorktreeDirty(work, ourDce)) {
                return false
            }
            if (nonTree(modeT)) {
                // we know about length and lastMod only after we have written
                // the new content.
                // This will happen later. Set these values to 0 for know.
                val e = add(
                    tw!!.rawPath, theirs,
                    DirCacheEntry.STAGE_0, Instant.EPOCH, 0
                )
                if (e != null) {
                    addToCheckout(tw!!.pathString, e, attributes)
                }
                return true
            }
            // we want THEIRS ... but THEIRS contains a folder or the
            // deletion of the path. Delete what's in the working tree,
            // which we know to be clean.
            if (tw!!.treeCount > T_FILE && tw!!.getRawMode(T_FILE) == 0) {
                // Not present in working tree, so nothing to delete
                return true
            }
            if (modeT != 0 && modeT == modeB) {
                // Base, ours, and theirs all contain a folder: don't delete
                return true
            }
            addDeletion(tw!!.pathString, nonTree(modeO), attributes[T_OURS])
            return true
        }

        if (tw!!.isSubtree) {
            // file/folder conflicts: here I want to detect only file/folder
            // conflict between ours and theirs. file/folder conflicts between
            // base/index/workingTree and something else are not relevant or
            // detected later
            if (nonTree(modeO) != nonTree(modeT)) {
                if (ignoreConflicts) {
                    // In case of merge failures, ignore this path instead of reporting unmerged, so
                    // a caller can use virtual commit. This will not result in files with conflict
                    // markers in the index/working tree. The actual diff on the path will be
                    // computed directly on children.
                    enterSubtree = false
                    return true
                }
                if (nonTree(modeB)) {
                    add(tw!!.rawPath, base, DirCacheEntry.STAGE_1, Instant.EPOCH, 0)
                }
                if (nonTree(modeO)) {
                    add(tw!!.rawPath, ours, DirCacheEntry.STAGE_2, Instant.EPOCH, 0)
                }
                if (nonTree(modeT)) {
                    add(tw!!.rawPath, theirs, DirCacheEntry.STAGE_3, Instant.EPOCH, 0)
                }
                unmergedPaths.add(tw!!.pathString)
                enterSubtree = false
                return true
            }

            // ours and theirs are both folders or both files (and treewalk
            // tells us we are in a subtree because of index or working-dir).
            // If they are both folders no content-merge is required - we can
            // return here.
            if (!nonTree(modeO)) {
                return true
            }

            // ours and theirs are both files, just fall out of the if block
            // and do the content merge
        }

        if (nonTree(modeO) && nonTree(modeT)) {
            // Check worktree before modifying files
            val worktreeDirty = isWorktreeDirty(work, ourDce)
            if (!attributes[T_OURS].canBeContentMerged() && worktreeDirty) {
                return false
            }

            if (gitLinkMerging && ignoreConflicts) {
                // Always select 'ours' in case of GITLINK merge failures so
                // a caller can use virtual commit.
                add(tw!!.rawPath, ours, DirCacheEntry.STAGE_0, Instant.EPOCH, 0)
                return true
            } else if (gitLinkMerging) {
                addConflict(base, ours, theirs)
                val result = createGitLinksMergeResult(
                    base, ours, theirs
                )
                result.setContainsConflicts(true)
                mergeResults[tw!!.pathString] = result
                unmergedPaths.add(tw!!.pathString)
                return true
            } else if (!attributes[T_OURS].canBeContentMerged()) {
                // File marked as binary
                when (contentMergeStrategy) {
                    ContentMergeStrategy.OURS -> {
                        keep(ourDce)
                        return true
                    }

                    ContentMergeStrategy.THEIRS -> {
                        val theirEntry = add(
                            tw!!.rawPath, theirs,
                            DirCacheEntry.STAGE_0, Instant.EPOCH, 0
                        )
                        addToCheckout(tw!!.pathString, theirEntry, attributes)
                        return true
                    }

                    else -> {}
                }
                // add the conflicting path to merge result
                val currentPath = tw!!.pathString
                val result = MergeResult(
                    emptyList<RawText>()
                )
                result.setContainsConflicts(true)
                mergeResults[currentPath] = result
                addConflict(base, ours, theirs)
                // attribute merge issues are conflicts but not failures
                unmergedPaths.add(currentPath)
                return true
            }

            // Check worktree before modifying files
            if (worktreeDirty) {
                return false
            }

            var result: MergeResult<RawText>?
            val hasSymlink = (FileMode.SYMLINK.equals(modeO)
                || FileMode.SYMLINK.equals(modeT))

            val currentPath = tw!!.pathString
            // if the path is not a symlink in ours and theirs
            if (!hasSymlink) {
                try {
                    result = contentMerge(
                        base, ours, theirs, attributes,
                        contentMergeStrategy!!
                    )
                    if (result.containsConflicts() && !ignoreConflicts) {
                        result.setContainsConflicts(true)
                        unmergedPaths.add(currentPath)
                    } else if (ignoreConflicts) {
                        result.setContainsConflicts(false)
                    }
                    updateIndex(base, ours, theirs, result, attributes[T_OURS])
                    workTreeUpdater!!.markAsModified(currentPath)
                    // Entry is null - only add the metadata
                    addToCheckout(currentPath, null, attributes)
                    return true
                } catch (e: BinaryBlobException) {
                    // if the file is binary in either OURS, THEIRS or BASE
                    // here, we don't have an option to ignore conflicts
                }
            }
            when (contentMergeStrategy) {
                ContentMergeStrategy.OURS -> {
                    keep(ourDce)
                    return true
                }

                ContentMergeStrategy.THEIRS -> {
                    val e = add(
                        tw!!.rawPath, theirs,
                        DirCacheEntry.STAGE_0, Instant.EPOCH, 0
                    )
                    if (e != null) {
                        addToCheckout(currentPath, e, attributes)
                    }
                    return true
                }

                else -> {
                    result = MergeResult(emptyList())
                    result.setContainsConflicts(true)
                }
            }
            if (hasSymlink) {
                if (ignoreConflicts) {
                    result.setContainsConflicts(false)
                    if (((modeT and FileMode.TYPE_MASK) == FileMode.TYPE_FILE)) {
                        val e = add(
                            tw!!.rawPath, theirs,
                            DirCacheEntry.STAGE_0, Instant.EPOCH, 0
                        )
                        addToCheckout(currentPath, e, attributes)
                    } else {
                        keep(ourDce)
                    }
                } else {
                    val e = addConflict(base, ours, theirs)
                    mergeResults[currentPath] = result
                    unmergedPaths.add(currentPath)
                    // If theirs is a file, check it out. In link/file
                    // conflicts, C git prefers the file.
                    if (((modeT and FileMode.TYPE_MASK) == FileMode.TYPE_FILE)
                        && e != null
                    ) {
                        addToCheckout(currentPath, e, attributes)
                    }
                }
            } else {
                result.setContainsConflicts(true)
                addConflict(base, ours, theirs)
                unmergedPaths.add(currentPath)
                mergeResults[currentPath] = result
            }
            return true
        } else if (modeO != modeT) {
            // OURS or THEIRS has been deleted
            if (((modeO != 0 && !tw!!.idEqual(T_BASE, T_OURS)) || (modeT != 0 && !tw!!
                    .idEqual(T_BASE, T_THEIRS)))
            ) {
                if (gitLinkMerging && ignoreConflicts) {
                    add(tw!!.rawPath, ours, DirCacheEntry.STAGE_0, Instant.EPOCH, 0)
                } else if (gitLinkMerging) {
                    addConflict(base, ours, theirs)
                    val result = createGitLinksMergeResult(
                        base, ours, theirs
                    )
                    result.setContainsConflicts(true)
                    mergeResults[tw!!.pathString] = result
                    unmergedPaths.add(tw!!.pathString)
                } else {
                    val isSymLink = ((modeO or modeT)
                        and FileMode.TYPE_MASK) == FileMode.TYPE_SYMLINK
                    // Content merge strategy does not apply to delete-modify
                    // conflicts!
                    var result: MergeResult<RawText>
                    if (isSymLink) {
                        // No need to do a content merge
                        result = MergeResult(emptyList())
                        result.setContainsConflicts(true)
                    } else {
                        try {
                            result = contentMerge(
                                base, ours, theirs,
                                attributes, ContentMergeStrategy.CONFLICT
                            )
                        } catch (e: BinaryBlobException) {
                            result = MergeResult(emptyList())
                            result.setContainsConflicts(true)
                        }
                    }
                    if (ignoreConflicts) {
                        result.setContainsConflicts(false)
                        if (isSymLink) {
                            if (modeO != 0) {
                                keep(ourDce)
                            } else {
                                // Check out theirs
                                if (isWorktreeDirty(work, ourDce)) {
                                    return false
                                }
                                val e = add(
                                    tw!!.rawPath, theirs,
                                    DirCacheEntry.STAGE_0, Instant.EPOCH, 0
                                )
                                if (e != null) {
                                    addToCheckout(
                                        tw!!.pathString, e,
                                        attributes
                                    )
                                }
                            }
                        } else {
                            // In case a conflict is detected the working tree
                            // file is again filled with new content (containing
                            // conflict markers). But also stage 0 of the index
                            // is filled with that content.
                            updateIndex(
                                base, ours, theirs, result,
                                attributes[T_OURS]
                            )
                        }
                    } else {
                        val e = addConflict(base, ours, theirs)

                        // OURS was deleted checkout THEIRS
                        if (modeO == 0) {
                            // Check worktree before checking out THEIRS
                            if (isWorktreeDirty(work, ourDce)) {
                                return false
                            }
                            if (nonTree(modeT) && e != null) {
                                addToCheckout(
                                    tw!!.pathString, e,
                                    attributes
                                )
                            }
                        }

                        unmergedPaths.add(tw!!.pathString)

                        // generate a MergeResult for the deleted file
                        mergeResults[tw!!.pathString] = result
                    }
                }
            }
        }
        return true
    }

    /**
     * Does the content merge. The three texts base, ours and theirs are
     * specified with [CanonicalTreeParser]. If any of the parsers is
     * specified as `null` then an empty text will be used instead.
     *
     * @param base
     * used to parse base tree
     * @param ours
     * used to parse ours tree
     * @param theirs
     * used to parse theirs tree
     * @param attributes
     * attributes for the different stages
     * @param strategy
     * merge strategy
     *
     * @return the result of the content merge
     * @throws BinaryBlobException
     * if any of the blobs looks like a binary blob
     * @throws IOException
     * if an IO error occurred
     */
    @Throws(BinaryBlobException::class, IOException::class)
    private fun contentMerge(
        base: CanonicalTreeParser?,
        ours: CanonicalTreeParser?, theirs: CanonicalTreeParser?,
        attributes: Array<Attributes>, strategy: ContentMergeStrategy
    ): MergeResult<RawText> {
        // TW: The attributes here are used to determine the LFS smudge filter.
        // Is doing a content merge on LFS items really a good idea??
        val baseText = if (base == null) RawText.EMPTY_TEXT
        else getRawText(base.entryObjectId, attributes[T_BASE])
        val ourText = if (ours == null) RawText.EMPTY_TEXT
        else getRawText(ours.entryObjectId, attributes[T_OURS])
        val theirsText = if (theirs == null) RawText.EMPTY_TEXT
        else getRawText(theirs.entryObjectId, attributes[T_THEIRS])
        mergeAlgorithm.contentMergeStrategy = strategy
        return mergeAlgorithm.merge(
            RawTextComparator.DEFAULT, baseText,
            ourText, theirsText
        )
    }

    private val isIndexDirty: Boolean
        get() {
            if (inCore) {
                return false
            }

            val modeI = tw!!.getRawMode(T_INDEX)
            val modeO = tw!!.getRawMode(T_OURS)

            // Index entry has to match ours to be considered clean
            val isDirty = (nonTree(modeI)
                && !(modeO == modeI && tw!!.idEqual(T_INDEX, T_OURS)))
            if (isDirty) {
                failingPaths[tw!!.pathString] = MergeFailureReason.DIRTY_INDEX
            }
            return isDirty
        }

    @Throws(IOException::class)
    private fun isWorktreeDirty(
        work: WorkingTreeIterator?,
        ourDce: DirCacheEntry?
    ): Boolean {
        if (work == null) {
            return false
        }

        val modeF = tw!!.getRawMode(T_FILE)
        val modeO = tw!!.getRawMode(T_OURS)

        // Worktree entry has to match ours to be considered clean
        var isDirty: Boolean
        if (ourDce != null) {
            isDirty = work.isModified(ourDce, true, reader)
        } else {
            isDirty = work.isModeDifferent(modeO)
            if (!isDirty && nonTree(modeF)) {
                isDirty = !tw!!.idEqual(T_FILE, T_OURS)
            }
        }

        // Ignore existing empty directories
        if (isDirty && modeF == FileMode.TYPE_TREE && modeO == FileMode.TYPE_MISSING) {
            isDirty = false
        }
        if (isDirty) {
            failingPaths[tw!!.pathString] = MergeFailureReason.DIRTY_WORKTREE
        }
        return isDirty
    }

    /**
     * Updates the index after a content merge has happened. If no conflict has
     * occurred this includes persisting the merged content to the object
     * database. In case of conflicts this method takes care to write the
     * correct stages to the index.
     *
     * @param base
     * used to parse base tree
     * @param ours
     * used to parse ours tree
     * @param theirs
     * used to parse theirs tree
     * @param result
     * merge result
     * @param attributes
     * the file's attributes
     * @throws IOException
     * if an IO error occurred
     */
    @Throws(IOException::class)
    private fun updateIndex(
        base: CanonicalTreeParser?,
        ours: CanonicalTreeParser?, theirs: CanonicalTreeParser?,
        result: MergeResult<RawText>?, attributes: Attributes
    ) {
        var rawMerged: TemporaryBuffer? = null
        try {
            rawMerged = doMerge(result)
            val mergedFile = if (inCore) null
            else writeMergedFile(rawMerged, attributes)
            if (result!!.containsConflicts()) {
                // A conflict occurred, the file will contain conflict markers
                // the index will be populated with the three stages and the
                // workdir (if used) contains the halfway merged content.
                addConflict(base, ours, theirs)
                mergeResults[tw!!.pathString] = result
                return
            }

            // No conflict occurred, the file will contain fully merged content.
            // The index will be populated with the new merged version.
            val lastModified = if (mergedFile == null) null
            else nonNullRepo().fs.lastModifiedInstant(mergedFile)
            // Set the mode for the new content. Fall back to REGULAR_FILE if
            // we can't merge modes of OURS and THEIRS.
            val newMode = mergeFileModes(
                tw!!.getRawMode(0), tw!!.getRawMode(1),
                tw!!.getRawMode(2)
            )
            val mode = if (newMode == FileMode.MISSING.bits
            ) FileMode.REGULAR_FILE else FileMode.fromBits(newMode)
            workTreeUpdater!!.insertToIndex(
                rawMerged.openInputStream(),
                tw!!.pathString.toByteArray(StandardCharsets.UTF_8), mode,
                DirCacheEntry.STAGE_0, lastModified,
                rawMerged.length().toInt(),
                attributes[Constants.ATTR_MERGE]
            )
        } finally {
            rawMerged?.destroy()
        }
    }

    /**
     * Writes merged file content to the working tree.
     *
     * @param rawMerged
     * the raw merged content
     * @param attributes
     * the files .gitattributes entries
     * @return the working tree file to which the merged content was written.
     * @throws IOException
     * if an IO error occurred
     */
    @Throws(IOException::class)
    private fun writeMergedFile(
        rawMerged: TemporaryBuffer?,
        attributes: Attributes
    ): File {
        val workTree = nonNullRepo().workTree
        val gitPath = tw!!.pathString
        val of = File(workTree, gitPath)
        val eol = workTreeUpdater!!.detectCheckoutStreamType(attributes)
        workTreeUpdater!!.updateFileWithContent(
            { rawMerged!!.openInputStream() },
            eol, tw!!.getSmudgeCommand(attributes), gitPath, of
        )
        return of
    }

    @Throws(IOException::class)
    private fun doMerge(result: MergeResult<RawText>?): TemporaryBuffer {
        val buf = LocalFile(
            if (repository != null) nonNullRepo().directory else null, workTreeUpdater!!.inCoreFileSizeLimit
        )
        var success = false
        try {
            MergeFormatter().formatMerge(
                buf, result!!,
                Arrays.asList(*commitNames), StandardCharsets.UTF_8
            )
            buf.close()
            success = true
        } finally {
            if (!success) {
                buf.destroy()
            }
        }
        return buf
    }

    /**
     * Try to merge filemodes. If only ours or theirs have changed the mode
     * (compared to base) we choose that one. If ours and theirs have equal
     * modes return that one. If also that is not the case the modes are not
     * mergeable. Return [FileMode.MISSING] int that case.
     *
     * @param modeB
     * filemode found in BASE
     * @param modeO
     * filemode found in OURS
     * @param modeT
     * filemode found in THEIRS
     *
     * @return the merged filemode or [FileMode.MISSING] in case of a
     * conflict
     */
    private fun mergeFileModes(modeB: Int, modeO: Int, modeT: Int): Int {
        if (modeO == modeT) {
            return modeO
        }
        if (modeB == modeO) {
            // Base equal to Ours -> chooses Theirs if that is not missing
            return if ((modeT == FileMode.MISSING.bits)) modeO else modeT
        }
        if (modeB == modeT) {
            // Base equal to Theirs -> chooses Ours if that is not missing
            return if ((modeO == FileMode.MISSING.bits)) modeT else modeO
        }
        return FileMode.MISSING.bits
    }

    @Throws(IOException::class, BinaryBlobException::class)
    private fun getRawText(
        id: ObjectId,
        attributes: Attributes
    ): RawText {
        if (id.equals(zeroId())) {
            return RawText(byteArrayOf())
        }

        val loader = LfsFactory.getInstance().applySmudgeFilter(
            repository, reader.open(id, Constants.OBJ_BLOB),
            attributes[Constants.ATTR_MERGE]
        )
        val threshold = PackConfig.DEFAULT_BIG_FILE_THRESHOLD
        return load(loader, threshold)
    }

    override val resultTreeId: ObjectId?
        get() = if ((resultTree == null)) null else resultTree!!.toObjectId()

    /**
     * Get the paths with conflicts. This is a subset of the files listed by
     * [.getModifiedFiles]
     *
     * @return the paths with conflicts. This is a subset of the files listed by
     * [.getModifiedFiles]
     */
    fun getUnmergedPaths(): List<String> {
        return unmergedPaths
    }

    /**
     * Get the paths of files which have been modified by this merge.
     *
     * @return the paths of files which have been modified by this merge. A file
     * will be modified if a content-merge works on this path or if the
     * merge algorithm decides to take the theirs-version. This is a
     * superset of the files listed by [.getUnmergedPaths].
     */
    fun getModifiedFiles(): List<String> {
        return if (workTreeUpdater != null) workTreeUpdater!!.getModifiedFiles() else modifiedFiles
    }

    val toBeCheckedOut: Map<String, DirCacheEntry>
        /**
         * Get a map which maps the paths of files which have to be checked out
         * because the merge created new fully-merged content for this file into the
         * index.
         *
         * @return a map which maps the paths of files which have to be checked out
         * because the merge created new fully-merged content for this file
         * into the index. This means: the merge wrote a new stage 0 entry
         * for this path.
         */
        get() = workTreeUpdater!!.getToBeCheckedOut()

    /**
     * Get the mergeResults
     *
     * @return the mergeResults
     */
    fun getMergeResults(): Map<String, MergeResult<out Sequence>?> {
        return mergeResults
    }

    /**
     * Get list of paths causing this merge to fail (not stopped because of a
     * conflict).
     *
     * @return lists paths causing this merge to fail (not stopped because of a
     * conflict). `null` is returned if this merge didn't
     * fail.
     */
    fun getFailingPaths(): Map<String, MergeFailureReason>? {
        return if (failingPaths.isEmpty()) null else failingPaths
    }

    /**
     * Returns whether this merge failed (i.e. not stopped because of a
     * conflict)
     *
     * @return `true` if a failure occurred, `false`
     * otherwise
     */
    fun failed(): Boolean {
        return !failingPaths.isEmpty()
    }

    /**
     * Sets the DirCache which shall be used by this merger. If the DirCache is
     * not set explicitly and if this merger doesn't work in-core, this merger
     * will implicitly get and lock a default DirCache. If the DirCache is
     * explicitly set the caller is responsible to lock it in advance. Finally
     * the merger will call [org.eclipse.jgit.dircache.DirCache.commit]
     * which requires that the DirCache is locked. If the [.mergeImpl]
     * returns without throwing an exception the lock will be released. In case
     * of exceptions the caller is responsible to release the lock.
     *
     * @param dc
     * the DirCache to set
     */
    fun setDirCache(dc: DirCache?) {
        this.dircache = dc
    }

    /**
     * Sets the WorkingTreeIterator to be used by this merger. If no
     * WorkingTreeIterator is set this merger will ignore the working tree and
     * fail if a content merge is necessary.
     *
     *
     * TODO: enhance WorkingTreeIterator to support write operations. Then this
     * merger will be able to merge with a different working tree abstraction.
     *
     * @param workingTreeIterator
     * the workingTreeIt to set
     */
    fun setWorkingTreeIterator(workingTreeIterator: WorkingTreeIterator?) {
        this.workingTreeIterator = workingTreeIterator
    }


    /**
     * The resolve conflict way of three way merging
     *
     * @param baseTree
     * a [org.eclipse.jgit.treewalk.AbstractTreeIterator]
     * object.
     * @param headTree
     * a [org.eclipse.jgit.revwalk.RevTree] object.
     * @param mergeTree
     * a [org.eclipse.jgit.revwalk.RevTree] object.
     * @param ignoreConflicts
     * Controls what to do in case a content-merge is done and a
     * conflict is detected. The default setting for this should be
     * `false`. In this case the working tree file is
     * filled with new content (containing conflict markers) and the
     * index is filled with multiple stages containing BASE, OURS and
     * THEIRS content. Having such non-0 stages is the sign to git
     * tools that there are still conflicts for that path.
     *
     *
     * If `true` is specified the behavior is different.
     * In case a conflict is detected the working tree file is again
     * filled with new content (containing conflict markers). But
     * also stage 0 of the index is filled with that content. No
     * other stages are filled. Means: there is no conflict on that
     * path but the new content (including conflict markers) is
     * stored as successful merge result. This is needed in the
     * context of [org.eclipse.jgit.merge.RecursiveMerger]
     * where when determining merge bases we don't want to deal with
     * content-merge conflicts.
     * @return whether the trees merged cleanly
     * @throws java.io.IOException
     * if an IO error occurred
     * @since 3.5
     */
    @Throws(IOException::class)
    protected fun mergeTrees(
        baseTree: AbstractTreeIterator?,
        headTree: RevTree?, mergeTree: RevTree?, ignoreConflicts: Boolean
    ): Boolean {
        try {
            workTreeUpdater = if (inCore) WorkTreeUpdater.createInCoreWorkTreeUpdater(
                repository,
                dircache,
                objectInserter
            ) else WorkTreeUpdater.createWorkTreeUpdater(
                repository!!, dircache
            )
            dircache = workTreeUpdater!!.lockedDirCache
            tw = NameConflictTreeWalk(repository, reader)

            tw!!.addTree(baseTree)
            tw!!.setHead(tw!!.addTree(headTree))
            tw!!.addTree(mergeTree)
            val buildIt = workTreeUpdater!!.createDirCacheBuildIterator()
            val dciPos = tw!!.addTree(buildIt)
            if (workingTreeIterator != null) {
                tw!!.addTree(workingTreeIterator)
                workingTreeIterator!!.setDirCacheIterator(tw, dciPos)
            } else {
                tw!!.filter = TreeFilter.ANY_DIFF
            }

            if (!mergeTreeWalk(tw!!, ignoreConflicts)) {
                return false
            }

            workTreeUpdater!!.writeWorkTreeChanges(true)
            if (getUnmergedPaths().isEmpty() && !failed()) {
                val result = workTreeUpdater!!.writeIndexChanges()
                resultTree = result.treeId
                modifiedFiles = result.modifiedFiles
                for (f in result.failedToDelete) {
                    failingPaths[f] = MergeFailureReason.COULD_NOT_DELETE
                }
                return result.failedToDelete.isEmpty()
            }
            resultTree = null
            return false
        } finally {
            if (modifiedFiles.isEmpty()) {
                modifiedFiles = workTreeUpdater!!.getModifiedFiles()
            }
            workTreeUpdater!!.close()
            workTreeUpdater = null
        }
    }

    /**
     * Process the given TreeWalk's entries.
     *
     * @param treeWalk
     * The walk to iterate over.
     * @param ignoreConflicts
     * see
     * [org.eclipse.jgit.merge.ResolveMerger.mergeTrees]
     * @return Whether the trees merged cleanly.
     * @throws java.io.IOException
     * if an IO error occurred
     * @since 3.5
     */
    @Throws(IOException::class)
    protected fun mergeTreeWalk(treeWalk: TreeWalk, ignoreConflicts: Boolean): Boolean {
        val hasWorkingTreeIterator = tw!!.treeCount > T_FILE
        val hasAttributeNodeProvider = treeWalk
            .attributesNodeProvider != null
        while (treeWalk.next()) {
            val attributes = arrayOf(
                NO_ATTRIBUTES, NO_ATTRIBUTES,
                NO_ATTRIBUTES
            )
            if (hasAttributeNodeProvider) {
                attributes[T_BASE] = treeWalk.getAttributes(T_BASE)
                attributes[T_OURS] = treeWalk.getAttributes(T_OURS)
                attributes[T_THEIRS] = treeWalk.getAttributes(T_THEIRS)
            }
            if (!processEntry(
                    treeWalk.getTree(T_BASE, CanonicalTreeParser::class.java),
                    treeWalk.getTree(T_OURS, CanonicalTreeParser::class.java),
                    treeWalk.getTree(T_THEIRS, CanonicalTreeParser::class.java),
                    treeWalk.getTree(T_INDEX, DirCacheBuildIterator::class.java),
                    if (hasWorkingTreeIterator) treeWalk.getTree(
                        T_FILE,
                        WorkingTreeIterator::class.java
                    ) else null,
                    ignoreConflicts, attributes
                )
            ) {
                workTreeUpdater!!.revertModifiedFiles()
                return false
            }
            if (treeWalk.isSubtree && enterSubtree) {
                treeWalk.enterSubtree()
            }
        }
        return true
    }

    companion object {
        /**
         * Index of the base tree within the [tree walk][.tw].
         *
         * @since 3.4
         */
        protected const val T_BASE: Int = 0

        /**
         * Index of our tree in withthe [tree walk][.tw].
         *
         * @since 3.4
         */
        protected const val T_OURS: Int = 1

        /**
         * Index of their tree within the [tree walk][.tw].
         *
         * @since 3.4
         */
        protected const val T_THEIRS: Int = 2

        /**
         * Index of the index tree within the [tree walk][.tw].
         *
         * @since 3.4
         */
        protected const val T_INDEX: Int = 3

        /**
         * Index of the working directory tree within the [tree walk][.tw].
         *
         * @since 3.4
         */
        protected const val T_FILE: Int = 4

        private fun getMergeAlgorithm(config: Config): MergeAlgorithm {
            val diffAlg = config.getEnum<SupportedAlgorithm>(
                ConfigConstants.CONFIG_DIFF_SECTION, null, ConfigConstants.CONFIG_KEY_ALGORITHM,
                SupportedAlgorithm.HISTOGRAM
            )
            return MergeAlgorithm(getAlgorithm(diffAlg))
        }

        private fun defaultCommitNames(): Array<String> {
            return arrayOf("BASE", "OURS", "THEIRS") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }

        private val NO_ATTRIBUTES = Attributes()

        private fun createGitLinksMergeResult(
            base: CanonicalTreeParser?, ours: CanonicalTreeParser?,
            theirs: CanonicalTreeParser?
        ): MergeResult<SubmoduleConflict> {
            return MergeResult(
                Arrays.asList(
                    SubmoduleConflict(
                        base?.entryObjectId
                    ),
                    SubmoduleConflict(
                        ours?.entryObjectId
                    ),
                    SubmoduleConflict(
                        theirs?.entryObjectId
                    )
                )
            )
        }

        private fun nonTree(mode: Int): Boolean {
            return mode != 0 && !FileMode.TREE.equals(mode)
        }

        private fun isGitLink(mode: Int): Boolean {
            return FileMode.GITLINK.equals(mode)
        }
    }
}
