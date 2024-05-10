/*
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2010, Chris Aniszczyk <caniszczyk@gmail.com>
 * Copyright (C) 2012-2013, Robin Rosenberg
 * Copyright (C) 2018-2022, Andre Bossert <andre.bossert@siemens.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.lib

/**
 * Constants for use with the Configuration classes: section names,
 * configuration keys
 */
object ConfigConstants {
    /** The "core" section  */
    const val CONFIG_CORE_SECTION: String = "core"

    /** The "branch" section  */
    const val CONFIG_BRANCH_SECTION: String = "branch"

    /** The "remote" section  */
    const val CONFIG_REMOTE_SECTION: String = "remote"

    /** The "diff" section  */
    const val CONFIG_DIFF_SECTION: String = "diff"

    /**
     * The "tool" key within "diff" or "merge" section
     *
     * @since 6.1
     */
    const val CONFIG_KEY_TOOL: String = "tool"

    /**
     * The "guitool" key within "diff" or "merge" section
     *
     * @since 6.1
     */
    const val CONFIG_KEY_GUITOOL: String = "guitool"

    /**
     * The "difftool" section
     *
     * @since 6.1
     */
    const val CONFIG_DIFFTOOL_SECTION: String = "difftool"

    /**
     * The "prompt" key within "difftool" or "mergetool" section
     *
     * @since 6.1
     */
    const val CONFIG_KEY_PROMPT: String = "prompt"

    /**
     * The "trustExitCode" key within "difftool" or "mergetool.&lt;name&gt;."
     * section
     *
     * @since 6.1
     */
    const val CONFIG_KEY_TRUST_EXIT_CODE: String = "trustExitCode"

    /**
     * The "cmd" key within "difftool.*." or "mergetool.*." section
     *
     * @since 6.1
     */
    const val CONFIG_KEY_CMD: String = "cmd"

    /** The "dfs" section  */
    const val CONFIG_DFS_SECTION: String = "dfs"

    /**
     * The "receive" section
     * @since 4.6
     */
    const val CONFIG_RECEIVE_SECTION: String = "receive"

    /** The "user" section  */
    const val CONFIG_USER_SECTION: String = "user"

    /** The "gerrit" section  */
    const val CONFIG_GERRIT_SECTION: String = "gerrit"

    /** The "workflow" section  */
    const val CONFIG_WORKFLOW_SECTION: String = "workflow"

    /** The "submodule" section  */
    const val CONFIG_SUBMODULE_SECTION: String = "submodule"

    /**
     * The "rebase" section
     * @since 3.2
     */
    const val CONFIG_REBASE_SECTION: String = "rebase"

    /** The "gc" section  */
    const val CONFIG_GC_SECTION: String = "gc"

    /**
     * The "repack" section
     * @since 5.13.3
     */
    const val CONFIG_REPACK_SECTION: String = "repack"

    /** The "pack" section  */
    const val CONFIG_PACK_SECTION: String = "pack"

    /**
     * The "fetch" section
     *
     * @since 3.3
     */
    const val CONFIG_FETCH_SECTION: String = "fetch"

    /**
     * The "pull" section
     * @since 3.5
     */
    const val CONFIG_PULL_SECTION: String = "pull"

    /**
     * The "merge" section
     * @since 4.9
     */
    const val CONFIG_MERGE_SECTION: String = "merge"

    /**
     * The "mergetool" section
     *
     * @since 6.2
     */
    const val CONFIG_MERGETOOL_SECTION: String = "mergetool"

    /**
     * The "keepBackup" key within "mergetool" section
     *
     * @since 6.2
     */
    const val CONFIG_KEY_KEEP_BACKUP: String = "keepBackup"

    /**
     * The "keepTemporaries" key within "mergetool" section
     *
     * @since 6.2
     */
    const val CONFIG_KEY_KEEP_TEMPORARIES: String = "keepTemporaries"

    /**
     * The "writeToTemp" key within "mergetool" section
     *
     * @since 6.2
     */
    const val CONFIG_KEY_WRITE_TO_TEMP: String = "writeToTemp"

    /**
     * The "filter" section
     * @since 4.6
     */
    const val CONFIG_FILTER_SECTION: String = "filter"

    /**
     * The "gpg" section
     * @since 5.2
     */
    const val CONFIG_GPG_SECTION: String = "gpg"

    /**
     * The "protocol" section
     * @since 5.9
     */
    const val CONFIG_PROTOCOL_SECTION: String = "protocol"

    /**
     * The "format" key
     * @since 5.2
     */
    const val CONFIG_KEY_FORMAT: String = "format"

    /**
     * The "program" key
     *
     * @since 5.11
     */
    const val CONFIG_KEY_PROGRAM: String = "program"

    /**
     * The "signingKey" key
     *
     * @since 5.2
     */
    const val CONFIG_KEY_SIGNINGKEY: String = "signingKey"

    /**
     * The "commit" section
     * @since 5.2
     */
    const val CONFIG_COMMIT_SECTION: String = "commit"

    /**
     * The "template" key
     *
     * @since 5.13
     */
    const val CONFIG_KEY_COMMIT_TEMPLATE: String = "template"

    /**
     * The "tag" section
     *
     * @since 5.11
     */
    const val CONFIG_TAG_SECTION: String = "tag"

    /**
     * The "cleanup" key
     *
     * @since 6.1
     */
    const val CONFIG_KEY_CLEANUP: String = "cleanup"

    /**
     * The "gpgSign" key
     *
     * @since 5.2
     */
    const val CONFIG_KEY_GPGSIGN: String = "gpgSign"

    /**
     * The "forceSignAnnotated" key
     *
     * @since 5.11
     */
    const val CONFIG_KEY_FORCE_SIGN_ANNOTATED: String = "forceSignAnnotated"

    /**
     * The "commentChar" key.
     *
     * @since 6.2
     */
    const val CONFIG_KEY_COMMENT_CHAR: String = "commentChar"

    /**
     * The "hooksPath" key.
     *
     * @since 5.6
     */
    const val CONFIG_KEY_HOOKS_PATH: String = "hooksPath"

    /**
     * The "quotePath" key.
     * @since 5.6
     */
    const val CONFIG_KEY_QUOTE_PATH: String = "quotePath"

    /** The "algorithm" key  */
    const val CONFIG_KEY_ALGORITHM: String = "algorithm"

    /** The "autocrlf" key  */
    const val CONFIG_KEY_AUTOCRLF: String = "autocrlf"

    /**
     * The "auto" key
     * @since 4.6
     */
    const val CONFIG_KEY_AUTO: String = "auto"

    /**
     * The "autogc" key
     * @since 4.6
     */
    const val CONFIG_KEY_AUTOGC: String = "autogc"

    /**
     * The "autopacklimit" key
     * @since 4.6
     */
    const val CONFIG_KEY_AUTOPACKLIMIT: String = "autopacklimit"

    /**
     * The "eol" key
     *
     * @since 4.3
     */
    const val CONFIG_KEY_EOL: String = "eol"

    /** The "bare" key  */
    const val CONFIG_KEY_BARE: String = "bare"

    /** The "excludesfile" key  */
    const val CONFIG_KEY_EXCLUDESFILE: String = "excludesfile"

    /**
     * The "attributesfile" key
     *
     * @since 3.7
     */
    const val CONFIG_KEY_ATTRIBUTESFILE: String = "attributesfile"

    /** The "filemode" key  */
    const val CONFIG_KEY_FILEMODE: String = "filemode"

    /** The "logallrefupdates" key  */
    const val CONFIG_KEY_LOGALLREFUPDATES: String = "logallrefupdates"

    /** The "repositoryformatversion" key  */
    const val CONFIG_KEY_REPO_FORMAT_VERSION: String = "repositoryformatversion"

    /** The "worktree" key  */
    const val CONFIG_KEY_WORKTREE: String = "worktree"

    /** The "blockLimit" key  */
    const val CONFIG_KEY_BLOCK_LIMIT: String = "blockLimit"

    /** The "blockSize" key  */
    const val CONFIG_KEY_BLOCK_SIZE: String = "blockSize"

    /**
     * The "concurrencyLevel" key
     *
     * @since 4.6
     */
    const val CONFIG_KEY_CONCURRENCY_LEVEL: String = "concurrencyLevel"

    /** The "deltaBaseCacheLimit" key  */
    const val CONFIG_KEY_DELTA_BASE_CACHE_LIMIT: String = "deltaBaseCacheLimit"

    /**
     * The "symlinks" key
     * @since 3.3
     */
    const val CONFIG_KEY_SYMLINKS: String = "symlinks"

    /**
     * The "streamFileThreshold" key
     *
     * @since 6.8
     */
    const val CONFIG_KEY_STREAM_FILE_THRESHOLD: String = "streamFileThreshold"


    @Deprecated("typo, use CONFIG_KEY_STREAM_FILE_THRESHOLD instead")
    val CONFIG_KEY_STREAM_FILE_TRESHOLD: String = CONFIG_KEY_STREAM_FILE_THRESHOLD

    /**
     * The "packedGitMmap" key
     * @since 5.1.13
     */
    const val CONFIG_KEY_PACKED_GIT_MMAP: String = "packedgitmmap"

    /**
     * The "packedGitWindowSize" key
     * @since 5.1.13
     */
    const val CONFIG_KEY_PACKED_GIT_WINDOWSIZE: String = "packedgitwindowsize"

    /**
     * The "packedGitLimit" key
     * @since 5.1.13
     */
    const val CONFIG_KEY_PACKED_GIT_LIMIT: String = "packedgitlimit"

    /**
     * The "packedGitOpenFiles" key
     * @since 5.1.13
     */
    const val CONFIG_KEY_PACKED_GIT_OPENFILES: String = "packedgitopenfiles"

    /**
     * The "packedGitUseStrongRefs" key
     * @since 5.1.13
     */
    const val CONFIG_KEY_PACKED_GIT_USE_STRONGREFS: String = "packedgitusestrongrefs"

    /**
     * The "packedIndexGitUseStrongRefs" key
     * @since 6.7
     */
    const val CONFIG_KEY_PACKED_INDEX_GIT_USE_STRONGREFS: String = "packedindexgitusestrongrefs"

    /** The "remote" key  */
    const val CONFIG_KEY_REMOTE: String = "remote"

    /**
     * The "pushRemote" key.
     *
     * @since 6.1
     */
    const val CONFIG_KEY_PUSH_REMOTE: String = "pushRemote"

    /**
     * The "pushDefault" key.
     *
     * @since 6.1
     */
    const val CONFIG_KEY_PUSH_DEFAULT: String = "pushDefault"

    /** The "merge" key  */
    const val CONFIG_KEY_MERGE: String = "merge"

    /** The "rebase" key  */
    const val CONFIG_KEY_REBASE: String = "rebase"

    /** The "url" key  */
    const val CONFIG_KEY_URL: String = "url"

    /** The "autosetupmerge" key  */
    const val CONFIG_KEY_AUTOSETUPMERGE: String = "autosetupmerge"

    /** The "autosetuprebase" key  */
    const val CONFIG_KEY_AUTOSETUPREBASE: String = "autosetuprebase"

    /**
     * The "autostash" key
     * @since 3.2
     */
    const val CONFIG_KEY_AUTOSTASH: String = "autostash"

    /** The "name" key  */
    const val CONFIG_KEY_NAME: String = "name"

    /** The "email" key  */
    const val CONFIG_KEY_EMAIL: String = "email"

    /** The "false" key (used to configure [.CONFIG_KEY_AUTOSETUPMERGE]  */
    const val CONFIG_KEY_FALSE: String = "false"

    /** The "true" key (used to configure [.CONFIG_KEY_AUTOSETUPMERGE]  */
    const val CONFIG_KEY_TRUE: String = "true"

    /**
     * The "always" key (used to configure [.CONFIG_KEY_AUTOSETUPREBASE]
     * and [.CONFIG_KEY_AUTOSETUPMERGE]
     */
    const val CONFIG_KEY_ALWAYS: String = "always"

    /** The "never" key (used to configure [.CONFIG_KEY_AUTOSETUPREBASE]  */
    const val CONFIG_KEY_NEVER: String = "never"

    /** The "local" key (used to configure [.CONFIG_KEY_AUTOSETUPREBASE]  */
    const val CONFIG_KEY_LOCAL: String = "local"

    /** The "createchangeid" key  */
    const val CONFIG_KEY_CREATECHANGEID: String = "createchangeid"

    /** The "defaultsourceref" key  */
    const val CONFIG_KEY_DEFBRANCHSTARTPOINT: String = "defbranchstartpoint"

    /** The "path" key  */
    const val CONFIG_KEY_PATH: String = "path"

    /** The "update" key  */
    const val CONFIG_KEY_UPDATE: String = "update"

    /**
     * The "ignore" key
     * @since 3.6
     */
    const val CONFIG_KEY_IGNORE: String = "ignore"

    /** The "compression" key  */
    const val CONFIG_KEY_COMPRESSION: String = "compression"

    /** The "indexversion" key  */
    const val CONFIG_KEY_INDEXVERSION: String = "indexversion"

    /**
     * The "skiphash" key
     * @since 5.13.2
     */
    const val CONFIG_KEY_SKIPHASH: String = "skiphash"

    /**
     * The "hidedotfiles" key
     * @since 3.5
     */
    const val CONFIG_KEY_HIDEDOTFILES: String = "hidedotfiles"

    /**
     * The "dirnogitlinks" key
     * @since 4.3
     */
    const val CONFIG_KEY_DIRNOGITLINKS: String = "dirNoGitLinks"

    /** The "precomposeunicode" key  */
    const val CONFIG_KEY_PRECOMPOSEUNICODE: String = "precomposeunicode"

    /** The "pruneexpire" key  */
    const val CONFIG_KEY_PRUNEEXPIRE: String = "pruneexpire"

    /**
     * The "prunepackexpire" key
     * @since 4.3
     */
    const val CONFIG_KEY_PRUNEPACKEXPIRE: String = "prunepackexpire"

    /**
     * The "logexpiry" key
     *
     * @since 4.7
     */
    const val CONFIG_KEY_LOGEXPIRY: String = "logExpiry"

    /**
     * The "autodetach" key
     *
     * @since 4.7
     */
    const val CONFIG_KEY_AUTODETACH: String = "autoDetach"

    /**
     * The "aggressiveDepth" key
     * @since 3.6
     */
    const val CONFIG_KEY_AGGRESSIVE_DEPTH: String = "aggressiveDepth"

    /**
     * The "aggressiveWindow" key
     * @since 3.6
     */
    const val CONFIG_KEY_AGGRESSIVE_WINDOW: String = "aggressiveWindow"

    /** The "mergeoptions" key  */
    const val CONFIG_KEY_MERGEOPTIONS: String = "mergeoptions"

    /** The "ff" key  */
    const val CONFIG_KEY_FF: String = "ff"

    /**
     * The "conflictStyle" key.
     *
     * @since 5.12
     */
    const val CONFIG_KEY_CONFLICTSTYLE: String = "conflictStyle"

    /**
     * The "checkstat" key
     *
     * @since 3.0
     */
    const val CONFIG_KEY_CHECKSTAT: String = "checkstat"

    /**
     * The "renamelimit" key in the "diff" section
     * @since 3.0
     */
    const val CONFIG_KEY_RENAMELIMIT: String = "renamelimit"

    /**
     * The "trustfolderstat" key in the "core" section
     * @since 3.6
     */
    const val CONFIG_KEY_TRUSTFOLDERSTAT: String = "trustfolderstat"

    /**
     * The "supportsAtomicFileCreation" key in the "core" section
     *
     * @since 4.5
     */
    const val CONFIG_KEY_SUPPORTSATOMICFILECREATION: String = "supportsatomicfilecreation"

    /**
     * The "sha1Implementation" key in the "core" section
     *
     * @since 5.13.2
     */
    const val SHA1_IMPLEMENTATION: String = "sha1implementation"

    /**
     * The "noprefix" key in the "diff" section
     * @since 3.0
     */
    const val CONFIG_KEY_NOPREFIX: String = "noprefix"

    /**
     * A "renamelimit" value in the "diff" section
     * @since 3.0
     */
    const val CONFIG_RENAMELIMIT_COPY: String = "copy"

    /**
     * A "renamelimit" value in the "diff" section
     * @since 3.0
     */
    const val CONFIG_RENAMELIMIT_COPIES: String = "copies"

    /**
     * The "renames" key in the "diff" section
     * @since 3.0
     */
    const val CONFIG_KEY_RENAMES: String = "renames"

    /**
     * The "inCoreLimit" key in the "merge" section. It's a size limit (bytes) used to
     * control a file to be stored in `Heap` or `LocalFile` during the merge.
     * @since 4.9
     */
    const val CONFIG_KEY_IN_CORE_LIMIT: String = "inCoreLimit"

    /**
     * The "prune" key
     * @since 3.3
     */
    const val CONFIG_KEY_PRUNE: String = "prune"

    /**
     * The "streamBuffer" key
     * @since 4.0
     */
    const val CONFIG_KEY_STREAM_BUFFER: String = "streamBuffer"

    /**
     * The "streamRatio" key
     * @since 4.0
     */
    const val CONFIG_KEY_STREAM_RATIO: String = "streamRatio"

    /**
     * Flag in the filter section whether to use JGit's implementations of
     * filters and hooks
     * @since 4.6
     */
    const val CONFIG_KEY_USEJGITBUILTIN: String = "useJGitBuiltin"

    /**
     * The "fetchRecurseSubmodules" key
     * @since 4.7
     */
    const val CONFIG_KEY_FETCH_RECURSE_SUBMODULES: String = "fetchRecurseSubmodules"

    /**
     * The "recurseSubmodules" key
     * @since 4.7
     */
    const val CONFIG_KEY_RECURSE_SUBMODULES: String = "recurseSubmodules"

    /**
     * The "required" key
     * @since 4.11
     */
    const val CONFIG_KEY_REQUIRED: String = "required"

    /**
     * The "lfs" section
     * @since 4.11
     */
    const val CONFIG_SECTION_LFS: String = "lfs"

    /**
     * The "i18n" section
     *
     * @since 5.2
     */
    const val CONFIG_SECTION_I18N: String = "i18n"

    /**
     * The "commitEncoding" key
     *
     * @since 5.13
     */
    const val CONFIG_KEY_COMMIT_ENCODING: String = "commitEncoding"

    /**
     * The "logOutputEncoding" key
     *
     * @since 5.2
     */
    const val CONFIG_KEY_LOG_OUTPUT_ENCODING: String = "logOutputEncoding"

    /**
     * The "filesystem" section
     * @since 5.1.9
     */
    const val CONFIG_FILESYSTEM_SECTION: String = "filesystem"

    /**
     * The "timestampResolution" key
     * @since 5.1.9
     */
    const val CONFIG_KEY_TIMESTAMP_RESOLUTION: String = "timestampResolution"

    /**
     * The "minRacyThreshold" key
     * @since 5.1.9
     */
    const val CONFIG_KEY_MIN_RACY_THRESHOLD: String = "minRacyThreshold"


    /**
     * The "refStorage" key
     *
     * @since 5.6.2
     */
    const val CONFIG_KEY_REF_STORAGE: String = "refStorage"

    /**
     * The "extensions" section
     *
     * @since 5.6.2
     */
    const val CONFIG_EXTENSIONS_SECTION: String = "extensions"

    /**
     * The extensions.refStorage key
     * @since 5.7
     */
    const val CONFIG_KEY_REFSTORAGE: String = "refStorage"

    /**
     * The "reftable" refStorage format
     * @since 5.7
     */
    const val CONFIG_REF_STORAGE_REFTABLE: String = "reftable"

    /**
     * The "jmx" section
     * @since 5.1.13
     */
    const val CONFIG_JMX_SECTION: String = "jmx"

    /**
     * The "pack.bigfilethreshold" key
     * @since 5.8
     */
    const val CONFIG_KEY_BIGFILE_THRESHOLD: String = "bigfilethreshold"

    /**
     * The "pack.bitmapContiguousCommitCount" key
     * @since 5.8
     */
    const val CONFIG_KEY_BITMAP_CONTIGUOUS_COMMIT_COUNT: String = "bitmapcontiguouscommitcount"

    /**
     * The "pack.bitmapDistantCommitSpan" key
     * @since 5.8
     */
    const val CONFIG_KEY_BITMAP_DISTANT_COMMIT_SPAN: String = "bitmapdistantcommitspan"

    /**
     * The "pack.bitmapExcessiveBranchCount" key
     * @since 5.8
     */
    const val CONFIG_KEY_BITMAP_EXCESSIVE_BRANCH_COUNT: String = "bitmapexcessivebranchcount"

    /**
     * The "pack.bitmapExcessiveBranchTipCount" key
     *
     * @since 6.9
     */
    const val CONFIG_KEY_BITMAP_EXCESSIVE_BRANCH_TIP_COUNT: String = "bitmapexcessivebranchtipcount"

    /**
     * The "pack.bitmapExcludedRefsPrefixes" key
     * @since 5.13.2
     */
    const val CONFIG_KEY_BITMAP_EXCLUDED_REFS_PREFIXES: String = "bitmapexcludedrefsprefixes"

    /**
     * The "pack.bitmapInactiveBranchAgeInDays" key
     * @since 5.8
     */
    const val CONFIG_KEY_BITMAP_INACTIVE_BRANCH_AGE_INDAYS: String = "bitmapinactivebranchageindays"

    /**
     * The "pack.bitmapRecentCommitSpan" key
     * @since 5.8
     */
    const val CONFIG_KEY_BITMAP_RECENT_COMMIT_COUNT: String = "bitmaprecentcommitspan"

    /**
     * The "pack.writeReverseIndex" key
     *
     * @since 6.6
     */
    const val CONFIG_KEY_WRITE_REVERSE_INDEX: String = "writeReverseIndex"

    /**
     * The "pack.buildBitmaps" key
     * @since 5.8
     */
    const val CONFIG_KEY_BUILD_BITMAPS: String = "buildbitmaps"

    /**
     * The "pack.cutDeltaChains" key
     * @since 5.8
     */
    const val CONFIG_KEY_CUT_DELTACHAINS: String = "cutdeltachains"

    /**
     * The "pack.deltaCacheLimit" key
     * @since 5.8
     */
    const val CONFIG_KEY_DELTA_CACHE_LIMIT: String = "deltacachelimit"

    /**
     * The "pack.deltaCacheSize" key
     * @since 5.8
     */
    const val CONFIG_KEY_DELTA_CACHE_SIZE: String = "deltacachesize"

    /**
     * The "pack.deltaCompression" key
     * @since 5.8
     */
    const val CONFIG_KEY_DELTA_COMPRESSION: String = "deltacompression"

    /**
     * The "pack.depth" key
     * @since 5.8
     */
    const val CONFIG_KEY_DEPTH: String = "depth"

    /**
     * The "pack.minSizePreventRacyPack" key
     * @since 5.8
     */
    const val CONFIG_KEY_MIN_SIZE_PREVENT_RACYPACK: String = "minsizepreventracypack"

    /**
     * The "pack.reuseDeltas" key
     * @since 5.8
     */
    const val CONFIG_KEY_REUSE_DELTAS: String = "reusedeltas"

    /**
     * The "pack.reuseObjects" key
     * @since 5.8
     */
    const val CONFIG_KEY_REUSE_OBJECTS: String = "reuseobjects"

    /**
     * The "pack.singlePack" key
     * @since 5.8
     */
    const val CONFIG_KEY_SINGLE_PACK: String = "singlepack"

    /**
     * The "pack.threads" key
     * @since 5.8
     */
    const val CONFIG_KEY_THREADS: String = "threads"

    /**
     * The "pack.waitPreventRacyPack" key
     * @since 5.8
     */
    const val CONFIG_KEY_WAIT_PREVENT_RACYPACK: String = "waitpreventracypack"

    /**
     * The "pack.window" key
     * @since 5.8
     */
    const val CONFIG_KEY_WINDOW: String = "window"

    /**
     * The "pack.windowMemory" key
     * @since 5.8
     */
    const val CONFIG_KEY_WINDOW_MEMORY: String = "windowmemory"

    /**
     * the "pack.minBytesForObjSizeIndex" key
     *
     * @since 6.5
     */
    const val CONFIG_KEY_MIN_BYTES_OBJ_SIZE_INDEX: String = "minBytesForObjSizeIndex"

    /**
     * The "repack.packKeptObjects" key
     *
     * @since 5.13.3
     */
    const val CONFIG_KEY_PACK_KEPT_OBJECTS: String = "packkeptobjects"

    /**
     * The "feature" section
     *
     * @since 5.9
     */
    const val CONFIG_FEATURE_SECTION: String = "feature"

    /**
     * The "feature.manyFiles" key
     *
     * @since 5.9
     */
    const val CONFIG_KEY_MANYFILES: String = "manyFiles"

    /**
     * The "index" section
     *
     * @since 5.9
     */
    const val CONFIG_INDEX_SECTION: String = "index"

    /**
     * The "version" key
     *
     * @since 5.9
     */
    const val CONFIG_KEY_VERSION: String = "version"

    /**
     * The "init" section
     *
     * @since 5.11
     */
    const val CONFIG_INIT_SECTION: String = "init"

    /**
     * The "defaultBranch" key
     *
     * @since 5.11
     */
    const val CONFIG_KEY_DEFAULT_BRANCH: String = "defaultbranch"

    /**
     * The "pack.searchForReuseTimeout" key
     *
     * @since 5.13
     */
    const val CONFIG_KEY_SEARCH_FOR_REUSE_TIMEOUT: String = "searchforreusetimeout"

    /**
     * The "push" section.
     *
     * @since 6.1
     */
    const val CONFIG_PUSH_SECTION: String = "push"

    /**
     * The "default" key.
     *
     * @since 6.1
     */
    const val CONFIG_KEY_DEFAULT: String = "default"

    /**
     * The "abbrev" key
     *
     * @since 6.1
     */
    const val CONFIG_KEY_ABBREV: String = "abbrev"

    /**
     * The "writeCommitGraph" key
     *
     * @since 6.5
     */
    const val CONFIG_KEY_WRITE_COMMIT_GRAPH: String = "writeCommitGraph"

    /**
     * The "commitGraph" used by commit-graph feature
     *
     * @since 6.5
     */
    const val CONFIG_COMMIT_GRAPH: String = "commitGraph"

    /**
     * The "trustPackedRefsStat" key
     *
     * @since 6.1.1
     */
    const val CONFIG_KEY_TRUST_PACKED_REFS_STAT: String = "trustPackedRefsStat"

    /**
     * The "trustLooseRefStat" key
     *
     * @since 6.9
     */
    const val CONFIG_KEY_TRUST_LOOSE_REF_STAT: String = "trustLooseRefStat"

    /**
     * The "pack.preserveOldPacks" key
     *
     * @since 5.13.2
     */
    const val CONFIG_KEY_PRESERVE_OLD_PACKS: String = "preserveoldpacks"

    /**
     * The "pack.prunePreserved" key
     *
     * @since 5.13.2
     */
    const val CONFIG_KEY_PRUNE_PRESERVED: String = "prunepreserved"

    /**
     * The "commitGraph" section
     *
     * @since 6.7
     */
    const val CONFIG_COMMIT_GRAPH_SECTION: String = "commitGraph"

    /**
     * The "writeChangedPaths" key
     *
     * @since 6.7
     */
    const val CONFIG_KEY_WRITE_CHANGED_PATHS: String = "writeChangedPaths"

    /**
     * The "readChangedPaths" key
     *
     * @since 6.7
     */
    const val CONFIG_KEY_READ_CHANGED_PATHS: String = "readChangedPaths"
}
