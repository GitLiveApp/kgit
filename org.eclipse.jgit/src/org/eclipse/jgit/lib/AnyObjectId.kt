/*
 * Copyright (C) 2006-2008, Shawn O. Pearce <spearce@spearce.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.lib

import org.eclipse.jgit.lib.ObjectId
//import org.eclipse.jgit.util.NB
//import org.eclipse.jgit.util.References
//import java.io.IOException
//import java.io.OutputStream
//import java.io.Writer
//import java.nio.ByteBuffer

/**
 * A (possibly mutable) SHA-1 abstraction.
 *
 *
 * If this is an instance of [org.eclipse.jgit.lib.MutableObjectId] the
 * concept of equality with this instance can alter at any time, if this
 * instance is modified to represent a different object name.
 */
abstract class AnyObjectId /*: Comparable<AnyObjectId> */{
	var w1: Int = 0

	var w2: Int = 0

	var w3: Int = 0

	var w4: Int = 0

	var w5: Int = 0

    val firstByte: Int
        /**
         * Get the first 8 bits of the ObjectId.
         *
         * This is a faster version of `getByte(0)`.
         *
         * @return a discriminator usable for a fan-out style map. Returned values
         * are unsigned and thus are in the range [0,255] rather than the
         * signed byte range of [-128, 127].
         */
        get() = w1 ushr 24

    /**
     * Get any byte from the ObjectId.
     *
     * Callers hard-coding `getByte(0)` should instead use the much faster
     * special case variant [.getFirstByte].
     *
     * @param index
     * index of the byte to obtain from the raw form of the ObjectId.
     * Must be in range [0,
     * [org.eclipse.jgit.lib.Constants.OBJECT_ID_LENGTH]).
     * @return the value of the requested byte at `index`. Returned values
     * are unsigned and thus are in the range [0,255] rather than the
     * signed byte range of [-128, 127].
     * @throws java.lang.IndexOutOfBoundsException
     * `index` is less than 0, equal to
     * [org.eclipse.jgit.lib.Constants.OBJECT_ID_LENGTH], or
     * greater than
     * [org.eclipse.jgit.lib.Constants.OBJECT_ID_LENGTH].
     */
    fun getByte(index: Int): Int {
        val w = when (index shr 2) {
            0 -> w1
            1 -> w2
            2 -> w3
            3 -> w4
            4 -> w5
            else -> throw IndexOutOfBoundsException(index.toString())
        }
        return (w ushr (8 * (3 - (index and 3)))) and 0xff
    }

    /**
     * {@inheritDoc}
     *
     *
     * Compare this ObjectId to another and obtain a sort ordering.
     */
//    override fun compareTo(other: AnyObjectId): Int {
//        if (this === other) return 0
//
//        var cmp = NB.compareUInt32(w1, other.w1)
//        if (cmp != 0) return cmp
//
//        cmp = NB.compareUInt32(w2, other.w2)
//        if (cmp != 0) return cmp
//
//        cmp = NB.compareUInt32(w3, other.w3)
//        if (cmp != 0) return cmp
//
//        cmp = NB.compareUInt32(w4, other.w4)
//        if (cmp != 0) return cmp
//
//        return NB.compareUInt32(w5, other.w5)
//    }

    /**
     * Compare this ObjectId to a network-byte-order ObjectId.
     *
     * @param bs
     * array containing the other ObjectId in network byte order.
     * @param p
     * position within `bs` to start the compare at. At least
     * 20 bytes, starting at this position are required.
     * @return a negative integer, zero, or a positive integer as this object is
     * less than, equal to, or greater than the specified object.
     */
//    fun compareTo(bs: ByteArray?, p: Int): Int {
//        var cmp = NB.compareUInt32(w1, NB.decodeInt32(bs, p))
//        if (cmp != 0) return cmp
//
//        cmp = NB.compareUInt32(w2, NB.decodeInt32(bs, p + 4))
//        if (cmp != 0) return cmp
//
//        cmp = NB.compareUInt32(w3, NB.decodeInt32(bs, p + 8))
//        if (cmp != 0) return cmp
//
//        cmp = NB.compareUInt32(w4, NB.decodeInt32(bs, p + 12))
//        if (cmp != 0) return cmp
//
//        return NB.compareUInt32(w5, NB.decodeInt32(bs, p + 16))
//    }

    /**
     * Compare this ObjectId to a network-byte-order ObjectId.
     *
     * @param bs
     * array containing the other ObjectId in network byte order.
     * @param p
     * position within `bs` to start the compare at. At least 5
     * integers, starting at this position are required.
     * @return a negative integer, zero, or a positive integer as this object is
     * less than, equal to, or greater than the specified object.
     */
//    fun compareTo(bs: IntArray, p: Int): Int {
//        var cmp = NB.compareUInt32(w1, bs[p])
//        if (cmp != 0) return cmp
//
//        cmp = NB.compareUInt32(w2, bs[p + 1])
//        if (cmp != 0) return cmp
//
//        cmp = NB.compareUInt32(w3, bs[p + 2])
//        if (cmp != 0) return cmp
//
//        cmp = NB.compareUInt32(w4, bs[p + 3])
//        if (cmp != 0) return cmp
//
//        return NB.compareUInt32(w5, bs[p + 4])
//    }

    /**
     * Tests if this ObjectId starts with the given abbreviation.
     *
     * @param abbr
     * the abbreviation.
     * @return true if this ObjectId begins with the abbreviation; else false.
     */
//    fun startsWith(abbr: AbbreviatedObjectId): Boolean {
//        return abbr.prefixCompare(this) == 0
//    }

    override fun hashCode(): Int {
        return w2
    }

    /**
     * Determine if this ObjectId has exactly the same value as another.
     *
     * @param other
     * the other id to compare to. May be null.
     * @return true only if both ObjectIds have identical bits.
     */
//    fun equals(other: AnyObjectId?): Boolean {
//        return if (other != null) isEqual(this, other) else false
//    }

    override fun equals(o: Any?): Boolean {
        if (o is AnyObjectId) {
            return equals(o as AnyObjectId?)
        }
        return false
    }

    /**
     * Copy this ObjectId to an output writer in raw binary.
     *
     * @param w
     * the buffer to copy to. Must be in big endian order.
     */
//    fun copyRawTo(w: ByteBuffer) {
//        w.putInt(w1)
//        w.putInt(w2)
//        w.putInt(w3)
//        w.putInt(w4)
//        w.putInt(w5)
//    }

    /**
     * Copy this ObjectId to a byte array.
     *
     * @param b
     * the buffer to copy to.
     * @param o
     * the offset within b to write at.
     */
//    fun copyRawTo(b: ByteArray?, o: Int) {
//        NB.encodeInt32(b, o, w1)
//        NB.encodeInt32(b, o + 4, w2)
//        NB.encodeInt32(b, o + 8, w3)
//        NB.encodeInt32(b, o + 12, w4)
//        NB.encodeInt32(b, o + 16, w5)
//    }

    /**
     * Copy this ObjectId to an int array.
     *
     * @param b
     * the buffer to copy to.
     * @param o
     * the offset within b to write at.
     */
    fun copyRawTo(b: IntArray, o: Int) {
        b[o] = w1
        b[o + 1] = w2
        b[o + 2] = w3
        b[o + 3] = w4
        b[o + 4] = w5
    }

    /**
     * Copy this ObjectId to an output writer in raw binary.
     *
     * @param w
     * the stream to write to.
     * @throws java.io.IOException
     * the stream writing failed.
     */
//    @Throws(IOException::class)
//    fun copyRawTo(w: OutputStream) {
//        writeRawInt(w, w1)
//        writeRawInt(w, w2)
//        writeRawInt(w, w3)
//        writeRawInt(w, w4)
//        writeRawInt(w, w5)
//    }

    /**
     * Copy this ObjectId to an output writer in hex format.
     *
     * @param w
     * the stream to copy to.
     * @throws java.io.IOException
     * the stream writing failed.
     */
//    @Throws(IOException::class)
//    fun copyTo(w: OutputStream) {
//        w.write(toHexByteArray())
//    }

    /**
     * Copy this ObjectId to a byte array in hex format.
     *
     * @param b
     * the buffer to copy to.
     * @param o
     * the offset within b to write at.
     */
    fun copyTo(b: ByteArray, o: Int) {
        formatHexByte(b, o + 0, w1)
        formatHexByte(b, o + 8, w2)
        formatHexByte(b, o + 16, w3)
        formatHexByte(b, o + 24, w4)
        formatHexByte(b, o + 32, w5)
    }

    /**
     * Copy this ObjectId to a ByteBuffer in hex format.
     *
     * @param b
     * the buffer to copy to.
     */
//    fun copyTo(b: ByteBuffer) {
//        b.put(toHexByteArray())
//    }

    private fun toHexByteArray(): ByteArray {
        val dst = ByteArray(Constants.OBJECT_ID_STRING_LENGTH)
        formatHexByte(dst, 0, w1)
        formatHexByte(dst, 8, w2)
        formatHexByte(dst, 16, w3)
        formatHexByte(dst, 24, w4)
        formatHexByte(dst, 32, w5)
        return dst
    }

    /**
     * Copy this ObjectId to an output writer in hex format.
     *
     * @param w
     * the stream to copy to.
     * @throws java.io.IOException
     * the stream writing failed.
     */
//    @Throws(IOException::class)
//    fun copyTo(w: Writer) {
//        w.write(toHexCharArray())
//    }

    /**
     * Copy this ObjectId to an output writer in hex format.
     *
     * @param tmp
     * temporary char array to buffer construct into before writing.
     * Must be at least large enough to hold 2 digits for each byte
     * of object id (40 characters or larger).
     * @param w
     * the stream to copy to.
     * @throws java.io.IOException
     * the stream writing failed.
     */
//    @Throws(IOException::class)
//    fun copyTo(tmp: CharArray, w: Writer) {
//        toHexCharArray(tmp)
//        w.write(tmp, 0, Constants.OBJECT_ID_STRING_LENGTH)
//    }

    /**
     * Copy this ObjectId to a StringBuilder in hex format.
     *
     * @param tmp
     * temporary char array to buffer construct into before writing.
     * Must be at least large enough to hold 2 digits for each byte
     * of object id (40 characters or larger).
     * @param w
     * the string to append onto.
     */
    fun copyTo(tmp: CharArray, w: StringBuilder) {
        toHexCharArray(tmp)
        w.appendRange(tmp, 0, 0 + Constants.OBJECT_ID_STRING_LENGTH)
    }

    private fun toHexCharArray(): CharArray {
        val dst = CharArray(Constants.OBJECT_ID_STRING_LENGTH)
        toHexCharArray(dst)
        return dst
    }

    private fun toHexCharArray(dst: CharArray) {
        formatHexChar(dst, 0, w1)
        formatHexChar(dst, 8, w2)
        formatHexChar(dst, 16, w3)
        formatHexChar(dst, 24, w4)
        formatHexChar(dst, 32, w5)
    }

    override fun toString(): String {
        return "AnyObjectId[" + name() + "]"
    }

    /**
     *
     * name.
     *
     * @return string form of the SHA-1, in lower case hexadecimal.
     */
    fun name(): String {
        return toHexCharArray().concatToString()
    }

    private val name: String
        /**
         * Get string form of the SHA-1, in lower case hexadecimal.
         *
         * @return string form of the SHA-1, in lower case hexadecimal.
         */
        get() = name()

    /**
     * Return an abbreviation (prefix) of this object SHA-1.
     *
     *
     * This implementation does not guarantee uniqueness. Callers should instead
     * use
     * [org.eclipse.jgit.lib.ObjectReader.abbreviate] to
     * obtain a unique abbreviation within the scope of a particular object
     * database.
     *
     * @param len
     * length of the abbreviated string.
     * @return SHA-1 abbreviation.
     */
//    fun abbreviate(len: Int): AbbreviatedObjectId {
//        val a = AbbreviatedObjectId.mask(len, 1, w1)
//        val b = AbbreviatedObjectId.mask(len, 2, w2)
//        val c = AbbreviatedObjectId.mask(len, 3, w3)
//        val d = AbbreviatedObjectId.mask(len, 4, w4)
//        val e = AbbreviatedObjectId.mask(len, 5, w5)
//        return AbbreviatedObjectId(len, a, b, c, d, e)
//    }

    /**
     * Obtain an immutable copy of this current object name value.
     *
     *
     * Only returns `this` if this instance is an unsubclassed
     * instance of [org.eclipse.jgit.lib.ObjectId]; otherwise a new
     * instance is returned holding the same value.
     *
     *
     * This method is useful to shed any additional memory that may be tied to
     * the subclass, yet retain the unique identity of the object id for future
     * lookups within maps and repositories.
     *
     * @return an immutable copy, using the smallest memory footprint possible.
     */
    fun copy(): ObjectId {
        if (this::class == ObjectId::class) return this as ObjectId
        return ObjectId(this)
    }

    /**
     * Obtain an immutable copy of this current object name value.
     *
     *
     * See [.copy] if `this` is a possibly subclassed (but
     * immutable) identity and the application needs a lightweight identity
     * *only* reference.
     *
     * @return an immutable copy. May be `this` if this is already
     * an immutable instance.
     */
    abstract fun toObjectId(): ObjectId?

    companion object {
        /**
         * Compare two object identifier byte sequences for equality.
         *
         * @param firstObjectId
         * the first identifier to compare. Must not be null.
         * @param secondObjectId
         * the second identifier to compare. Must not be null.
         * @return true if the two identifiers are the same.
         */
//        @Deprecated("use {@link #isEqual(AnyObjectId, AnyObjectId)} instead")
//        fun equals(
//            firstObjectId: AnyObjectId,
//            secondObjectId: AnyObjectId
//        ): Boolean {
//            return isEqual(firstObjectId, secondObjectId)
//        }

        /**
         * Compare two object identifier byte sequences for equality.
         *
         * @param firstObjectId
         * the first identifier to compare. Must not be null.
         * @param secondObjectId
         * the second identifier to compare. Must not be null.
         * @return true if the two identifiers are the same.
         * @since 5.4
         */
//		@JvmStatic
//		fun isEqual(
//            firstObjectId: AnyObjectId,
//            secondObjectId: AnyObjectId
//        ): Boolean {
//            if (References.isSameObject(firstObjectId, secondObjectId)) {
//                return true
//            }
//            // We test word 3 first since the git file-based ODB
//            // uses the first byte of w1, and we use w2 as the
//            // hash code, one of those probably came up with these
//            // two instances which we are comparing for equality.
//            // Therefore the first two words are very likely to be
//            // identical. We want to break away from collisions as
//            // quickly as possible.
//            return firstObjectId.w3 == secondObjectId.w3 && firstObjectId.w4 == secondObjectId.w4 && firstObjectId.w5 == secondObjectId.w5 && firstObjectId.w1 == secondObjectId.w1 && firstObjectId.w2 == secondObjectId.w2
//        }

//        @Throws(IOException::class)
//        private fun writeRawInt(w: OutputStream, v: Int) {
//            w.write(v ushr 24)
//            w.write(v ushr 16)
//            w.write(v ushr 8)
//            w.write(v)
//        }

        private val hexbyte = byteArrayOf(
            '0'.code.toByte(),
            '1'.code.toByte(),
            '2'.code.toByte(),
            '3'.code.toByte(),
            '4'.code.toByte(),
            '5'.code.toByte(),
            '6'.code.toByte(),
            '7'.code.toByte(),
            '8'.code.toByte(),
            '9'.code.toByte(),
            'a'.code.toByte(),
            'b'.code.toByte(),
            'c'.code.toByte(),
            'd'.code.toByte(),
            'e'.code.toByte(),
            'f'.code.toByte()
        )

        private fun formatHexByte(dst: ByteArray, p: Int, w: Int) {
            var w = w
            var o = p + 7
            while (o >= p && w != 0) {
                dst[o--] = hexbyte[w and 0xf]
                w = w ushr 4
            }
            while (o >= p) dst[o--] = '0'.code.toByte()
        }

        private val hexchar = charArrayOf(
            '0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
        )

		fun formatHexChar(dst: CharArray, p: Int, w: Int) {
            var w = w
            var o = p + 7
            while (o >= p && w != 0) {
                dst[o--] = hexchar[w and 0xf]
                w = w ushr 4
            }
            while (o >= p) dst[o--] = '0'
        }
    }
}
