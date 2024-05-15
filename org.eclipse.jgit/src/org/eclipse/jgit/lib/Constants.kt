/*
 * Copyright (C) 2008, Google Inc.
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006, 2022, Shawn O. Pearce <spearce@spearce.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.lib

import org.eclipse.jgit.errors.CorruptObjectException
import org.eclipse.jgit.internal.JGitText
import org.eclipse.jgit.util.MutableInteger
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.MessageFormat

/**
 * Misc. constants and helpers used throughout JGit.
 */
object Constants {
    /** Hash function used natively by Git for all objects.  */
    private const val HASH_FUNCTION = "SHA-1"

    /**
     * A Git object hash is 160 bits, i.e. 20 bytes.
     *
     *
     * Changing this assumption is not going to be as easy as changing this
     * declaration.
     */
    const val OBJECT_ID_LENGTH: Int = 20

    /**
     * A Git object can be expressed as a 40 character string of hexadecimal
     * digits.
     *
     * @see .OBJECT_ID_LENGTH
     */
    const val OBJECT_ID_STRING_LENGTH: Int = OBJECT_ID_LENGTH * 2

    /**
     * The historic length of an abbreviated Git object hash string. Git 2.11
     * changed this static number to a dynamically calculated one that scales
     * as the repository grows.
     *
     * @since 6.1
     */
    const val OBJECT_ID_ABBREV_STRING_LENGTH: Int = 7

    /** Special name for the "HEAD" symbolic-ref.  */
    const val HEAD: String = "HEAD"

    /** Special name for the "FETCH_HEAD" symbolic-ref.  */
    const val FETCH_HEAD: String = "FETCH_HEAD"

    /**
     * Text string that identifies an object as a commit.
     *
     *
     * Commits connect trees into a string of project histories, where each
     * commit is an assertion that the best way to continue is to use this other
     * tree (set of files).
     */
    const val TYPE_COMMIT: String = "commit"

    /**
     * Text string that identifies an object as a blob.
     *
     *
     * Blobs store whole file revisions. They are used for any user file, as
     * well as for symlinks. Blobs form the bulk of any project's storage space.
     */
    const val TYPE_BLOB: String = "blob"

    /**
     * Text string that identifies an object as a tree.
     *
     *
     * Trees attach object ids (hashes) to names and file modes. The normal use
     * for a tree is to store a version of a directory and its contents.
     */
    const val TYPE_TREE: String = "tree"

    /**
     * Text string that identifies an object as an annotated tag.
     *
     *
     * Annotated tags store a pointer to any other object, and an additional
     * message. It is most commonly used to record a stable release of the
     * project.
     */
    const val TYPE_TAG: String = "tag"

    private val ENCODED_TYPE_COMMIT = encodeASCII(TYPE_COMMIT)

    private val ENCODED_TYPE_BLOB = encodeASCII(TYPE_BLOB)

    private val ENCODED_TYPE_TREE = encodeASCII(TYPE_TREE)

    private val ENCODED_TYPE_TAG = encodeASCII(TYPE_TAG)

    /** An unknown or invalid object type code.  */
    const val OBJ_BAD: Int = -1

    /**
     * In-pack object type: extended types.
     *
     *
     * This header code is reserved for future expansion. It is currently
     * undefined/unsupported.
     */
    const val OBJ_EXT: Int = 0

    /**
     * In-pack object type: commit.
     *
     *
     * Indicates the associated object is a commit.
     *
     *
     * **This constant is fixed and is defined by the Git packfile format.**
     *
     * @see .TYPE_COMMIT
     */
    const val OBJ_COMMIT: Int = 1

    /**
     * In-pack object type: tree.
     *
     *
     * Indicates the associated object is a tree.
     *
     *
     * **This constant is fixed and is defined by the Git packfile format.**
     *
     * @see .TYPE_BLOB
     */
    const val OBJ_TREE: Int = 2

    /**
     * In-pack object type: blob.
     *
     *
     * Indicates the associated object is a blob.
     *
     *
     * **This constant is fixed and is defined by the Git packfile format.**
     *
     * @see .TYPE_BLOB
     */
    const val OBJ_BLOB: Int = 3

    /**
     * In-pack object type: annotated tag.
     *
     *
     * Indicates the associated object is an annotated tag.
     *
     *
     * **This constant is fixed and is defined by the Git packfile format.**
     *
     * @see .TYPE_TAG
     */
    const val OBJ_TAG: Int = 4

    /** In-pack object type: reserved for future use.  */
    const val OBJ_TYPE_5: Int = 5

    /**
     * In-pack object type: offset delta
     *
     *
     * Objects stored with this type actually have a different type which must
     * be obtained from their delta base object. Delta objects store only the
     * changes needed to apply to the base object in order to recover the
     * original object.
     *
     *
     * An offset delta uses a negative offset from the start of this object to
     * refer to its delta base. The base object must exist in this packfile
     * (even in the case of a thin pack).
     *
     *
     * **This constant is fixed and is defined by the Git packfile format.**
     */
    const val OBJ_OFS_DELTA: Int = 6

    /**
     * In-pack object type: reference delta
     *
     *
     * Objects stored with this type actually have a different type which must
     * be obtained from their delta base object. Delta objects store only the
     * changes needed to apply to the base object in order to recover the
     * original object.
     *
     *
     * A reference delta uses a full object id (hash) to reference the delta
     * base. The base object is allowed to be omitted from the packfile, but
     * only in the case of a thin pack being transferred over the network.
     *
     *
     * **This constant is fixed and is defined by the Git packfile format.**
     */
    const val OBJ_REF_DELTA: Int = 7

    /**
     * Pack file signature that occurs at file header - identifies file as Git
     * packfile formatted.
     *
     *
     * **This constant is fixed and is defined by the Git packfile format.**
     */
	@JvmField
	val PACK_SIGNATURE: ByteArray =
        byteArrayOf('P'.code.toByte(), 'A'.code.toByte(), 'C'.code.toByte(), 'K'.code.toByte())

    /**
     * Native character encoding for commit messages, file names...
     *
     */
    @Deprecated(
        """Use {@link java.nio.charset.StandardCharsets#UTF_8} directly
	              instead."""
    )
    val CHARSET: Charset

    /**
     * Native character encoding for commit messages, file names...
     *
     */
    @Deprecated(
        """Use {@link java.nio.charset.StandardCharsets#UTF_8} directly
	              instead."""
    )
    val CHARACTER_ENCODING: String

    /** Default main branch name  */
    const val MASTER: String = "master"

    /** Default stash branch name  */
    const val STASH: String = "stash"

    /** Prefix for branch refs  */
    const val R_HEADS: String = "refs/heads/"

    /** Prefix for remotes refs  */
    const val R_REMOTES: String = "refs/remotes/"

    /** Prefix for tag refs  */
    const val R_TAGS: String = "refs/tags/"

    /** Prefix for notes refs  */
    const val R_NOTES: String = "refs/notes/"

    /** Standard notes ref  */
    const val R_NOTES_COMMITS: String = R_NOTES + "commits"

    /** Prefix for any ref  */
    const val R_REFS: String = "refs/"

    /** Standard stash ref  */
    const val R_STASH: String = R_REFS + STASH

    /** Logs folder name  */
    const val LOGS: String = "logs"

    /**
     * Objects folder name
     * @since 5.5
     */
    const val OBJECTS: String = "objects"

    /**
     * Reftable folder name
     * @since 5.6
     */
    const val REFTABLE: String = "reftable"

    /**
     * Reftable table list name.
     * @since 5.6.2
     */
    const val TABLES_LIST: String = "tables.list"

    /** Info refs folder  */
    const val INFO_REFS: String = "info/refs"

    /**
     * Info alternates file (goes under OBJECTS)
     * @since 5.5
     */
    const val INFO_ALTERNATES: String = "info/alternates"

    /**
     * HTTP alternates file (goes under OBJECTS)
     * @since 5.5
     */
    const val INFO_HTTP_ALTERNATES: String = "info/http-alternates"

    /**
     * info commit-graph file (goes under OBJECTS)
     * @since 6.5
     */
    const val INFO_COMMIT_GRAPH: String = "info/commit-graph"

    /** Packed refs file  */
    const val PACKED_REFS: String = "packed-refs"

    /**
     * Excludes-file
     *
     * @since 3.0
     */
    const val INFO_EXCLUDE: String = "info/exclude"

    /**
     * Attributes-override-file
     *
     * @since 4.2
     */
    const val INFO_ATTRIBUTES: String = "info/attributes"

    /**
     * The system property that contains the system user name
     *
     * @since 3.6
     */
    const val OS_USER_DIR: String = "user.dir"

    /** The system property that contains the system user name  */
    const val OS_USER_NAME_KEY: String = "user.name"

    /** The environment variable that contains the author's name  */
    const val GIT_AUTHOR_NAME_KEY: String = "GIT_AUTHOR_NAME"

    /** The environment variable that contains the author's email  */
    const val GIT_AUTHOR_EMAIL_KEY: String = "GIT_AUTHOR_EMAIL"

    /** The environment variable that contains the commiter's name  */
    const val GIT_COMMITTER_NAME_KEY: String = "GIT_COMMITTER_NAME"

    /** The environment variable that contains the commiter's email  */
    const val GIT_COMMITTER_EMAIL_KEY: String = "GIT_COMMITTER_EMAIL"

    /**
     * The environment variable that blocks use of the system config file
     *
     * @since 3.3
     */
    const val GIT_CONFIG_NOSYSTEM_KEY: String = "GIT_CONFIG_NOSYSTEM"

    /**
     * The key of the XDG_CONFIG_HOME directory defined in the
     * [
 * XDG Base Directory specification](https://wiki.archlinux.org/index.php/XDG_Base_Directory).
     *
     * @since 5.5.2
     */
    const val XDG_CONFIG_HOME: String = "XDG_CONFIG_HOME"

    /**
     * The environment variable that limits how close to the root of the file
     * systems JGit will traverse when looking for a repository root.
     */
    const val GIT_CEILING_DIRECTORIES_KEY: String = "GIT_CEILING_DIRECTORIES"

    /**
     * The environment variable that tells us which directory is the ".git"
     * directory
     */
    const val GIT_DIR_KEY: String = "GIT_DIR"

    /**
     * The environment variable that tells us which directory is the working
     * directory.
     */
    const val GIT_WORK_TREE_KEY: String = "GIT_WORK_TREE"

    /**
     * The environment variable that tells us which file holds the Git index.
     */
    const val GIT_INDEX_FILE_KEY: String = "GIT_INDEX_FILE"

    /**
     * The environment variable that tells us where objects are stored
     */
    const val GIT_OBJECT_DIRECTORY_KEY: String = "GIT_OBJECT_DIRECTORY"

    /**
     * The environment variable that tells us where to look for objects, besides
     * the default objects directory.
     */
    const val GIT_ALTERNATE_OBJECT_DIRECTORIES_KEY: String = "GIT_ALTERNATE_OBJECT_DIRECTORIES"

    /** Default value for the user name if no other information is available  */
    const val UNKNOWN_USER_DEFAULT: String = "unknown-user"

    /** Beginning of the common "Signed-off-by: " commit message line  */
    const val SIGNED_OFF_BY_TAG: String = "Signed-off-by: "

    /** A gitignore file name  */
    const val GITIGNORE_FILENAME: String = ".gitignore"

    /** Default remote name used by clone, push and fetch operations  */
    const val DEFAULT_REMOTE_NAME: String = "origin"

    /** Default name for the Git repository directory  */
    const val DOT_GIT: String = ".git"

    /** Default name for the Git repository configuration  */
    const val CONFIG: String = "config"

    /** A bare repository typically ends with this string  */
    const val DOT_GIT_EXT: String = ".git"

    /**
     * The default extension for local bundle files
     *
     * @since 5.8
     */
    const val DOT_BUNDLE_EXT: String = ".bundle"

    /**
     * Name of the attributes file
     *
     * @since 3.7
     */
    const val DOT_GIT_ATTRIBUTES: String = ".gitattributes"

    /**
     * Key for filters in .gitattributes
     *
     * @since 4.2
     */
    const val ATTR_FILTER: String = "filter"

    /**
     * clean command name, used to call filter driver
     *
     * @since 4.2
     */
    const val ATTR_FILTER_TYPE_CLEAN: String = "clean"

    /**
     * smudge command name, used to call filter driver
     *
     * @since 4.2
     */
    const val ATTR_FILTER_TYPE_SMUDGE: String = "smudge"

    /**
     * Builtin filter commands start with this prefix
     *
     * @since 4.6
     */
    const val BUILTIN_FILTER_PREFIX: String = "jgit://builtin/"

    /** Name of the ignore file  */
    const val DOT_GIT_IGNORE: String = ".gitignore"

    /** Name of the submodules file  */
    const val DOT_GIT_MODULES: String = ".gitmodules"

    /** Name of the .git/shallow file  */
    const val SHALLOW: String = "shallow"

    /**
     * Prefix of the first line in a ".git" file
     *
     * @since 3.6
     */
    const val GITDIR: String = "gitdir: "

    /**
     * Name of the folder (inside gitDir) where submodules are stored
     *
     * @since 3.6
     */
    const val MODULES: String = "modules"

    /**
     * Name of the folder (inside gitDir) where the hooks are stored.
     *
     * @since 3.7
     */
    const val HOOKS: String = "hooks"

    /**
     * Merge attribute.
     *
     * @since 4.9
     */
    const val ATTR_MERGE: String = "merge" //$NON-NLS-1$

    /**
     * Diff attribute.
     *
     * @since 4.11
     */
    const val ATTR_DIFF: String = "diff" //$NON-NLS-1$

    /**
     * Binary value for custom merger.
     *
     * @since 4.9
     */
    const val ATTR_BUILTIN_BINARY_MERGER: String = "binary" //$NON-NLS-1$

    /**
     * Create a new digest function for objects.
     *
     * @return a new digest object.
     * @throws java.lang.RuntimeException
     * this Java virtual machine does not support the required hash
     * function. Very unlikely given that JGit uses a hash function
     * that is in the Java reference specification.
     */
	@JvmStatic
	fun newMessageDigest(): MessageDigest {
        try {
            return MessageDigest.getInstance(HASH_FUNCTION)
        } catch (nsae: NoSuchAlgorithmException) {
            throw RuntimeException(
                MessageFormat.format(
                    JGitText.get().requiredHashFunctionNotAvailable, HASH_FUNCTION
                ), nsae
            )
        }
    }

    /**
     * Convert an OBJ_* type constant to a TYPE_* type constant.
     *
     * @param typeCode the type code, from a pack representation.
     * @return the canonical string name of this type.
     */
	@JvmStatic
	fun typeString(typeCode: Int): String {
        return when (typeCode) {
            OBJ_COMMIT -> TYPE_COMMIT
            OBJ_TREE -> TYPE_TREE
            OBJ_BLOB -> TYPE_BLOB
            OBJ_TAG -> TYPE_TAG
            else -> throw IllegalArgumentException(
                MessageFormat.format(
                    JGitText.get().badObjectType, typeCode
                )
            )
        }
    }

    /**
     * Convert an OBJ_* type constant to an ASCII encoded string constant.
     *
     *
     * The ASCII encoded string is often the canonical representation of
     * the type within a loose object header, or within a tag header.
     *
     * @param typeCode the type code, from a pack representation.
     * @return the canonical ASCII encoded name of this type.
     */
	@JvmStatic
	fun encodedTypeString(typeCode: Int): ByteArray {
        return when (typeCode) {
            OBJ_COMMIT -> ENCODED_TYPE_COMMIT
            OBJ_TREE -> ENCODED_TYPE_TREE
            OBJ_BLOB -> ENCODED_TYPE_BLOB
            OBJ_TAG -> ENCODED_TYPE_TAG
            else -> throw IllegalArgumentException(
                MessageFormat.format(
                    JGitText.get().badObjectType, typeCode
                )
            )
        }
    }

    /**
     * Parse an encoded type string into a type constant.
     *
     * @param id
     * object id this type string came from; may be null if that is
     * not known at the time the parse is occurring.
     * @param typeString
     * string version of the type code.
     * @param endMark
     * character immediately following the type string. Usually ' '
     * (space) or '\n' (line feed).
     * @param offset
     * position within `typeString` where the parse
     * should start. Updated with the new position (just past
     * `endMark` when the parse is successful.
     * @return a type code constant (one of [.OBJ_BLOB],
     * [.OBJ_COMMIT], [.OBJ_TAG], [.OBJ_TREE].
     * @throws org.eclipse.jgit.errors.CorruptObjectException
     * there is no valid type identified by `typeString`.
     */
    @JvmStatic
	@Throws(CorruptObjectException::class)
    fun decodeTypeString(
        id: AnyObjectId,
        typeString: ByteArray, endMark: Byte,
        offset: MutableInteger
    ): Int {
        try {
            val position = offset.value
            when (typeString[position].toChar()) {
                'b' -> {
                    if (typeString[position + 1] != 'l'.code.toByte() || typeString[position + 2] != 'o'.code.toByte() || typeString[position + 3] != 'b'.code.toByte() || typeString[position + 4] != endMark) throw CorruptObjectException(
                        id,
                        JGitText.get().corruptObjectInvalidType
                    )
                    offset.value = position + 5
                    return OBJ_BLOB
                }

                'c' -> {
                    if (typeString[position + 1] != 'o'.code.toByte() || typeString[position + 2] != 'm'.code.toByte() || typeString[position + 3] != 'm'.code.toByte() || typeString[position + 4] != 'i'.code.toByte() || typeString[position + 5] != 't'.code.toByte() || typeString[position + 6] != endMark) throw CorruptObjectException(
                        id,
                        JGitText.get().corruptObjectInvalidType
                    )
                    offset.value = position + 7
                    return OBJ_COMMIT
                }

                't' -> when (typeString[position + 1].toChar()) {
                    'a' -> {
                        if (typeString[position + 2] != 'g'.code.toByte()
                            || typeString[position + 3] != endMark
                        ) throw CorruptObjectException(id, JGitText.get().corruptObjectInvalidType)
                        offset.value = position + 4
                        return OBJ_TAG
                    }

                    'r' -> {
                        if (typeString[position + 2] != 'e'.code.toByte() || typeString[position + 3] != 'e'.code.toByte() || typeString[position + 4] != endMark) throw CorruptObjectException(
                            id,
                            JGitText.get().corruptObjectInvalidType
                        )
                        offset.value = position + 5
                        return OBJ_TREE
                    }

                    else -> throw CorruptObjectException(id, JGitText.get().corruptObjectInvalidType)
                }

                else -> throw CorruptObjectException(id, JGitText.get().corruptObjectInvalidType)
            }
        } catch (bad: ArrayIndexOutOfBoundsException) {
            val coe = CorruptObjectException(
                id,
                JGitText.get().corruptObjectInvalidType
            )
            coe.initCause(bad)
            throw coe
        }
    }

    /**
     * Convert an integer into its decimal representation.
     *
     * @param s
     * the integer to convert.
     * @return a decimal representation of the input integer. The returned array
     * is the smallest array that will hold the value.
     */
	@JvmStatic
	fun encodeASCII(s: Long): ByteArray {
        return encodeASCII(s.toString())
    }

    /**
     * Convert a string to US-ASCII encoding.
     *
     * @param s
     * the string to convert. Must not contain any characters over
     * 127 (outside of 7-bit ASCII).
     * @return a byte array of the same length as the input string, holding the
     * same characters, in the same order.
     * @throws java.lang.IllegalArgumentException
     * the input string contains one or more characters outside of
     * the 7-bit ASCII character space.
     */
	@JvmStatic
	fun encodeASCII(s: String): ByteArray {
        val r = ByteArray(s.length)
        for (k in r.indices.reversed()) {
            val c = s[k]
            require(c.code <= 127) { MessageFormat.format(JGitText.get().notASCIIString, s) }
            r[k] = c.code.toByte()
        }
        return r
    }

    /**
     * Convert a string to a byte array in the standard character encoding.
     *
     * @param str
     * the string to convert. May contain any Unicode characters.
     * @return a byte array representing the requested string, encoded using the
     * default character encoding (UTF-8).
     * @see .CHARACTER_ENCODING
     */
	@JvmStatic
	fun encode(str: String?): ByteArray {
        val bb = StandardCharsets.UTF_8.encode(str)
        val len = bb.limit()
        if (bb.hasArray() && bb.arrayOffset() == 0) {
            val arr = bb.array()
            if (arr.size == len) return arr
        }

        val arr = ByteArray(len)
        bb[arr]
        return arr
    }

    init {
        if (OBJECT_ID_LENGTH != newMessageDigest().digestLength) throw LinkageError(JGitText.get().incorrectOBJECT_ID_LENGTH)
        CHARSET = StandardCharsets.UTF_8
        CHARACTER_ENCODING = StandardCharsets.UTF_8.name()
    }

    /** name of the file containing the commit msg for a merge commit  */
    const val MERGE_MSG: String = "MERGE_MSG"

    /** name of the file containing the IDs of the parents of a merge commit  */
    const val MERGE_HEAD: String = "MERGE_HEAD"

    /** name of the file containing the ID of a cherry pick commit in case of conflicts  */
    const val CHERRY_PICK_HEAD: String = "CHERRY_PICK_HEAD"

    /** name of the file containing the commit msg for a squash commit  */
    const val SQUASH_MSG: String = "SQUASH_MSG"

    /** name of the file containing the ID of a revert commit in case of conflicts  */
    const val REVERT_HEAD: String = "REVERT_HEAD"

    /**
     * name of the ref ORIG_HEAD used by certain commands to store the original
     * value of HEAD
     */
    const val ORIG_HEAD: String = "ORIG_HEAD"

    /**
     * Name of the file in which git commands and hooks store and read the
     * message prepared for the upcoming commit.
     *
     * @since 4.0
     */
    const val COMMIT_EDITMSG: String = "COMMIT_EDITMSG"

    /**
     * Well-known object ID for the empty blob.
     *
     * @since 0.9.1
     */
	@JvmField
	val EMPTY_BLOB_ID: ObjectId = ObjectId
        .fromString("e69de29bb2d1d6434b8b29ae775ad8c2e48c5391")

    /**
     * Well-known object ID for the empty tree.
     *
     * @since 5.1
     */
    val EMPTY_TREE_ID: ObjectId = ObjectId
        .fromString("4b825dc642cb6eb9a060e54bf8d69288fbee4904")

    /**
     * Suffix of lock file name
     *
     * @since 4.7
     */
    const val LOCK_SUFFIX: String = ".lock" //$NON-NLS-1$

    /**
     * Depth used to unshallow a repository
     *
     * @since 6.3
     */
    const val INFINITE_DEPTH: Int = 0x7fffffff

    /**
     * We use ({@value}) as generation number for commits not in the
     * commit-graph file.
     *
     * @since 6.5
     */
    const val COMMIT_GENERATION_UNKNOWN: Int = Int.MAX_VALUE

    /**
     * If a commit-graph file was written by a version of Git that did not
     * compute generation numbers, then those commits will have generation
     * number represented by ({@value}).
     *
     * @since 6.5
     */
    const val COMMIT_GENERATION_NOT_COMPUTED: Int = 0
}
