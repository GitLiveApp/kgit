/*
 * Copyright (C) 2010, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.errors

import org.eclipse.jgit.internal.JGitText
import org.eclipse.jgit.lib.AnyObjectId
import org.eclipse.jgit.lib.ObjectId
import java.text.MessageFormat

/**
 * An object is too big to load into memory as a single byte array.
 */
open class LargeObjectException : RuntimeException {
    /**
     * Get identity of the object that is too large; may be null
     *
     * @return identity of the object that is too large; may be null
     */
    var objectId: ObjectId? = null

    /**
     * Set the identity of the object, if its not already set.
     *
     * @param id
     * the id of the object that is too large to process.
     */
    fun setObjectId(id: AnyObjectId) {
        if (objectId == null) objectId = id.copy()
    }

    /**
     * Create a large object exception, where the object isn't known.
     */
    constructor()

    /**
     * Create a large object exception, where the object isn't known.
     *
     * @param cause
     * the cause
     * @since 4.10
     */
    constructor(cause: Throwable?) {
        initCause(cause)
    }

    /**
     * Create a large object exception, naming the object that is too big.
     *
     * @param id
     * identity of the object that is too big to be loaded as a byte
     * array in this JVM.
     */
    constructor(id: AnyObjectId) {
        objectId = id.copy()
    }

    protected val objectName: String
        /**
         * Get the hex encoded name of the object, or 'unknown object'
         *
         * @return either the hex encoded name of the object, or 'unknown object'
         */
        get() {
            if (objectId != null) return objectId!!.name()
            return JGitText.get().unknownObject
        }

    override val message: String
        get() = MessageFormat.format(
            JGitText.get().largeObjectException,
            objectName
        )

    /** An error caused by the JVM being out of heap space.  */
    class OutOfMemory(cause: OutOfMemoryError?) : LargeObjectException() {
        /**
         * Construct a wrapper around the original OutOfMemoryError.
         *
         * @param cause
         * the original root cause.
         */
        init {
            initCause(cause)
        }

        override val message: String
            get() = MessageFormat.format(
                JGitText.get().largeObjectOutOfMemory,
                objectName
            )

        companion object {
            private const val serialVersionUID = 1L
        }
    }

    /** Object size exceeds JVM limit of 2 GiB per byte array.  */
    class ExceedsByteArrayLimit : LargeObjectException() {
        override val message: String
            get() = MessageFormat
                .format(
                    JGitText.get().largeObjectExceedsByteArray,
                    objectName
                )

        companion object {
            private const val serialVersionUID = 1L
        }
    }

    /** Object size exceeds the caller's upper limit.  */
    class ExceedsLimit
    /**
     * Construct an exception for a particular size being exceeded.
     *
     * @param limit
     * the limit the caller imposed on the object.
     * @param size
     * the actual size of the object.
     */(private val limit: Long, private val size: Long) : LargeObjectException() {
        override val message: String
            get() = MessageFormat.format(
                JGitText.get().largeObjectExceedsLimit,
                objectName, limit, size
            )


        companion object {
            private const val serialVersionUID = 1L
        }
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
