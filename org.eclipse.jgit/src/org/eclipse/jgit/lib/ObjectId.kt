/*
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006-2008, Shawn O. Pearce <spearce@spearce.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.lib

import org.eclipse.jgit.annotations.Nullable
import org.eclipse.jgit.errors.InvalidObjectIdException
import org.eclipse.jgit.lib.Constants.encodeASCII
import org.eclipse.jgit.util.NB
import org.eclipse.jgit.util.RawParseUtils
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

/**
 * A SHA-1 abstraction.
 */
open class ObjectId : AnyObjectId, Serializable {
    /**
     * Construct an ObjectId from 160 bits provided in 5 words.
     *
     * @param new_1
     * an int
     * @param new_2
     * an int
     * @param new_3
     * an int
     * @param new_4
     * an int
     * @param new_5
     * an int
     * @since 4.7
     */
    constructor(new_1: Int, new_2: Int, new_3: Int, new_4: Int, new_5: Int) {
        w1 = new_1
        w2 = new_2
        w3 = new_3
        w4 = new_4
        w5 = new_5
    }

    /**
     * Initialize this instance by copying another existing ObjectId.
     *
     *
     * This constructor is mostly useful for subclasses who want to extend an
     * ObjectId with more properties, but initialize from an existing ObjectId
     * instance acquired by other means.
     *
     * @param src
     * another already parsed ObjectId to copy the value out of.
     */
    constructor(src: AnyObjectId) {
        w1 = src.w1
        w2 = src.w2
        w3 = src.w3
        w4 = src.w4
        w5 = src.w5
    }

    override fun toObjectId(): ObjectId {
        return this
    }

    @Throws(IOException::class)
    private fun writeObject(os: ObjectOutputStream) {
        os.writeInt(w1)
        os.writeInt(w2)
        os.writeInt(w3)
        os.writeInt(w4)
        os.writeInt(w5)
    }

    @Throws(IOException::class)
    private fun readObject(ois: ObjectInputStream) {
        w1 = ois.readInt()
        w2 = ois.readInt()
        w3 = ois.readInt()
        w4 = ois.readInt()
        w5 = ois.readInt()
    }

    companion object {
        private const val serialVersionUID = 1L

        private val ZEROID = ObjectId(0, 0, 0, 0, 0)

        private val ZEROID_STR: String = ZEROID.name()

        /**
         * Get the special all-null ObjectId.
         *
         * @return the all-null ObjectId, often used to stand-in for no object.
         */
		@JvmStatic
		fun zeroId(): ObjectId {
            return ZEROID
        }

        /**
         * Test a string of characters to verify it is a hex format.
         *
         *
         * If true the string can be parsed with [.fromString].
         *
         * @param id
         * the string to test.
         * @return true if the string can converted into an ObjectId.
         */
		@JvmStatic
		fun isId(@Nullable id: String?): Boolean {
            if (id == null) {
                return false
            }
            if (id.length != Constants.OBJECT_ID_STRING_LENGTH) return false
            try {
                for (i in 0 until Constants.OBJECT_ID_STRING_LENGTH) {
                    RawParseUtils.parseHexInt4(id[i].code.toByte())
                }
                return true
            } catch (e: ArrayIndexOutOfBoundsException) {
                return false
            }
        }

        /**
         * Convert an ObjectId into a hex string representation.
         *
         * @param i
         * the id to convert. May be null.
         * @return the hex string conversion of this id's content.
         */
		@JvmStatic
		fun toString(i: ObjectId?): String {
            return if (i != null) i.name() else ZEROID_STR
        }

        /**
         * Compare two object identifier byte sequences for equality.
         *
         * @param firstBuffer
         * the first buffer to compare against. Must have at least 20
         * bytes from position fi through the end of the buffer.
         * @param fi
         * first offset within firstBuffer to begin testing.
         * @param secondBuffer
         * the second buffer to compare against. Must have at least 20
         * bytes from position si through the end of the buffer.
         * @param si
         * first offset within secondBuffer to begin testing.
         * @return true if the two identifiers are the same.
         */
		@JvmStatic
		fun equals(
            firstBuffer: ByteArray, fi: Int,
            secondBuffer: ByteArray, si: Int
        ): Boolean {
            return firstBuffer[fi] == secondBuffer[si] && firstBuffer[fi + 1] == secondBuffer[si + 1] && firstBuffer[fi + 2] == secondBuffer[si + 2] && firstBuffer[fi + 3] == secondBuffer[si + 3] && firstBuffer[fi + 4] == secondBuffer[si + 4] && firstBuffer[fi + 5] == secondBuffer[si + 5] && firstBuffer[fi + 6] == secondBuffer[si + 6] && firstBuffer[fi + 7] == secondBuffer[si + 7] && firstBuffer[fi + 8] == secondBuffer[si + 8] && firstBuffer[fi + 9] == secondBuffer[si + 9] && firstBuffer[fi + 10] == secondBuffer[si + 10] && firstBuffer[fi + 11] == secondBuffer[si + 11] && firstBuffer[fi + 12] == secondBuffer[si + 12] && firstBuffer[fi + 13] == secondBuffer[si + 13] && firstBuffer[fi + 14] == secondBuffer[si + 14] && firstBuffer[fi + 15] == secondBuffer[si + 15] && firstBuffer[fi + 16] == secondBuffer[si + 16] && firstBuffer[fi + 17] == secondBuffer[si + 17] && firstBuffer[fi + 18] == secondBuffer[si + 18] && firstBuffer[fi + 19] == secondBuffer[si + 19]
        }

        /**
         * Convert an ObjectId from raw binary representation.
         *
         * @param bs
         * the raw byte buffer to read from. At least 20 bytes after p
         * must be available within this byte array.
         * @param p
         * position to read the first byte of data from.
         * @return the converted object id.
         */
        /**
         * Convert an ObjectId from raw binary representation.
         *
         * @param bs
         * the raw byte buffer to read from. At least 20 bytes must be
         * available within this byte array.
         * @return the converted object id.
         */
        @JvmStatic
		@JvmOverloads
        fun fromRaw(bs: ByteArray?, p: Int = 0): ObjectId {
            val a = NB.decodeInt32(bs, p)
            val b = NB.decodeInt32(bs, p + 4)
            val c = NB.decodeInt32(bs, p + 8)
            val d = NB.decodeInt32(bs, p + 12)
            val e = NB.decodeInt32(bs, p + 16)
            return ObjectId(a, b, c, d, e)
        }

        /**
         * Convert an ObjectId from raw binary representation.
         *
         * @param is
         * the raw integers buffer to read from. At least 5 integers
         * after p must be available within this int array.
         * @param p
         * position to read the first integer of data from.
         * @return the converted object id.
         */
        /**
         * Convert an ObjectId from raw binary representation.
         *
         * @param is
         * the raw integers buffer to read from. At least 5 integers must
         * be available within this int array.
         * @return the converted object id.
         */
        @JvmStatic
		@JvmOverloads
        fun fromRaw(`is`: IntArray, p: Int = 0): ObjectId {
            return ObjectId(`is`[p], `is`[p + 1], `is`[p + 2], `is`[p + 3], `is`[p + 4])
        }

        /**
         * Convert an ObjectId from hex characters (US-ASCII).
         *
         * @param buf
         * the US-ASCII buffer to read from. At least 40 bytes after
         * offset must be available within this byte array.
         * @param offset
         * position to read the first character from.
         * @return the converted object id.
         */
		@JvmStatic
		fun fromString(buf: ByteArray, offset: Int): ObjectId {
            return fromHexString(buf, offset)
        }

        /**
         * Convert an ObjectId from hex characters.
         *
         * @param str
         * the string to read from. Must be 40 characters long.
         * @return the converted object id.
         */
		@JvmStatic
		fun fromString(str: String): ObjectId {
            if (str.length != Constants.OBJECT_ID_STRING_LENGTH) {
                throw InvalidObjectIdException(str)
            }
            return fromHexString(encodeASCII(str), 0)
        }

        private fun fromHexString(bs: ByteArray, p: Int): ObjectId {
            try {
                val a = RawParseUtils.parseHexInt32(bs, p)
                val b = RawParseUtils.parseHexInt32(bs, p + 8)
                val c = RawParseUtils.parseHexInt32(bs, p + 16)
                val d = RawParseUtils.parseHexInt32(bs, p + 24)
                val e = RawParseUtils.parseHexInt32(bs, p + 32)
                return ObjectId(a, b, c, d, e)
            } catch (e: ArrayIndexOutOfBoundsException) {
                val e1 = InvalidObjectIdException(
                    bs, p,
                    Constants.OBJECT_ID_STRING_LENGTH
                )
                e1.initCause(e)
                throw e1
            }
        }
    }
}
