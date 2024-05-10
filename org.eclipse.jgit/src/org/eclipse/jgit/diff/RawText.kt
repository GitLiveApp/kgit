/*
 * Copyright (C) 2009, Google Inc.
 * Copyright (C) 2008-2021, Johannes E. Schindelin <johannes.schindelin@gmx.de> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.diff

import org.eclipse.jgit.errors.BinaryBlobException
import org.eclipse.jgit.errors.LargeObjectException.OutOfMemory
import org.eclipse.jgit.lib.ObjectLoader
import org.eclipse.jgit.util.IO
import org.eclipse.jgit.util.IntList
import org.eclipse.jgit.util.RawParseUtils
import java.io.*
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

/**
 * A Sequence supporting UNIX formatted text in byte[] format.
 *
 *
 * Elements of the sequence are the lines of the file, as delimited by the UNIX
 * newline character ('\n'). The file content is treated as 8 bit binary text,
 * with no assumptions or requirements on character encoding.
 *
 *
 * Note that the first line of the file is element 0, as defined by the Sequence
 * interface API. Traditionally in a text editor a patch file the first line is
 * line number 1. Callers may need to subtract 1 prior to invoking methods if
 * they are converting from "line number" to "element index".
 */
class RawText
/**
 * Create a new sequence from an existing content byte array.
 *
 *
 * The entire array (indexes 0 through length-1) is used as the content.
 *
 * @param input
 * the content array. The object retains a reference to this
 * array, so it should be immutable.
 */ @JvmOverloads constructor(
    /** The file content for this sequence.  */
    val rawContent: ByteArray,
    /** Map of line number to starting position within [.content].  */
    internal val lines: IntList = RawParseUtils.lineMap(rawContent, 0, rawContent.size)
) : Sequence() {
    /**
     * Get the raw content
     *
     * @return the raw, unprocessed content read.
     * @since 4.11
     */

    /**
     * Create a new sequence from the existing content byte array and the line
     * map indicating line boundaries.
     *
     * @param rawContent
     * the content array. The object retains a reference to this
     * array, so it should be immutable.
     * @param lines
     * an array with 1-based offsets for the start of each line.
     * The first and last entries should be [Integer.MIN_VALUE]
     * and an offset one past the end of the last line, respectively.
     * @since 5.0
     */

    /**
     * Create a new sequence from a file.
     *
     *
     * The entire file contents are used.
     *
     * @param file
     * the text file.
     * @throws java.io.IOException
     * if Exceptions occur while reading the file
     */
    constructor(file: File?) : this(IO.readFully(file))

    /** @return total number of items in the sequence.
     */
    override fun size(): Int {
        // The line map is always 2 entries larger than the number of lines in
        // the file. Index 0 is padded out/unused. The last index is the total
        // length of the buffer, and acts as a sentinel.
        //
        return lines.size() - 2
    }

    /**
     * Write a specific line to the output stream, without its trailing LF.
     *
     *
     * The specified line is copied as-is, with no character encoding
     * translation performed.
     *
     *
     * If the specified line ends with an LF ('\n'), the LF is **not**
     * copied. It is up to the caller to write the LF, if desired, between
     * output lines.
     *
     * @param out
     * stream to copy the line data onto.
     * @param i
     * index of the line to extract. Note this is 0-based, so line
     * number 1 is actually index 0.
     * @throws java.io.IOException
     * the stream write operation failed.
     */
    @Throws(IOException::class)
    fun writeLine(out: OutputStream, i: Int) {
        val start = getStart(i)
        var end = getEnd(i)
        if (rawContent[end - 1] == '\n'.code.toByte()) end--
        out.write(rawContent, start, end - start)
    }

    val isMissingNewlineAtEnd: Boolean
        /**
         * Determine if the file ends with a LF ('\n').
         *
         * @return true if the last line has an LF; false otherwise.
         */
        get() {
            val end = lines[lines.size() - 1]
            if (end == 0) return true
            return rawContent[end - 1] != '\n'.code.toByte()
        }

    /**
     * Get the text for a single line.
     *
     * @param i
     * index of the line to extract. Note this is 0-based, so line
     * number 1 is actually index 0.
     * @return the text for the line, without a trailing LF.
     */
    fun getString(i: Int): String {
        return getString(i, i + 1, true)
    }

    /**
     * Get the raw text for a single line.
     *
     * @param i
     * index of the line to extract. Note this is 0-based, so line
     * number 1 is actually index 0.
     * @return the text for the line, without a trailing LF, as a
     * [ByteBuffer] that is backed by a slice of the
     * [raw content][.getRawContent], with the buffer's position
     * on the start of the line and the limit at the end.
     * @since 5.12
     */
    fun getRawString(i: Int): ByteBuffer {
        val s = getStart(i)
        var e = getEnd(i)
        if (e > 0 && rawContent[e - 1] == '\n'.code.toByte()) {
            e--
        }
        return ByteBuffer.wrap(rawContent, s, e - s)
    }

    /**
     * Get the text for a region of lines.
     *
     * @param begin
     * index of the first line to extract. Note this is 0-based, so
     * line number 1 is actually index 0.
     * @param end
     * index of one past the last line to extract.
     * @param dropLF
     * if true the trailing LF ('\n') of the last returned line is
     * dropped, if present.
     * @return the text for lines `[begin, end)`.
     */
    fun getString(begin: Int, end: Int, dropLF: Boolean): String {
        if (begin == end) return "" //$NON-NLS-1$


        val s = getStart(begin)
        var e = getEnd(end - 1)
        if (dropLF && rawContent[e - 1] == '\n'.code.toByte()) e--
        return decode(s, e)
    }

    /**
     * Decode a region of the text into a String.
     *
     * The default implementation of this method tries to guess the character
     * set by considering UTF-8, the platform default, and falling back on
     * ISO-8859-1 if neither of those can correctly decode the region given.
     *
     * @param start
     * first byte of the content to decode.
     * @param end
     * one past the last byte of the content to decode.
     * @return the region `[start, end)` decoded as a String.
     */
    protected fun decode(start: Int, end: Int): String {
        return RawParseUtils.decode(rawContent, start, end)
    }

    private fun getStart(i: Int): Int {
        return lines[i + 1]
    }

    private fun getEnd(i: Int): Int {
        return lines[i + 2]
    }

    val lineDelimiter: String?
        /**
         * Get the line delimiter for the first line.
         *
         * @since 2.0
         * @return the line delimiter or `null`
         */
        get() {
            if (size() == 0) {
                return null
            }
            val e = getEnd(0)
            if (rawContent[e - 1] != '\n'.code.toByte()) {
                return null
            }
            if (rawContent.size > 1 && e > 1 && rawContent[e - 2] == '\r'.code.toByte()) {
                return "\r\n" //$NON-NLS-1$
            }
            return "\n" //$NON-NLS-1$
        }

    companion object {
        /** A RawText of length 0  */
		@JvmField
		val EMPTY_TEXT: RawText = RawText(ByteArray(0))

        /**
         * Default and minimum for [.BUFFER_SIZE].
         */
        private const val FIRST_FEW_BYTES = 8 * 1024

        /**
         * Number of bytes to check for heuristics in [.isBinary].
         */
        private val BUFFER_SIZE = AtomicInteger(
            FIRST_FEW_BYTES
        )

        @JvmStatic
		val bufferSize: Int
            /**
             * Obtains the buffer size to use for analyzing whether certain content is
             * text or binary, or what line endings are used if it's text.
             *
             * @return the buffer size, by default [.FIRST_FEW_BYTES] bytes
             * @since 6.0
             */
            get() = BUFFER_SIZE.get()

        /**
         * Sets the buffer size to use for analyzing whether certain content is text
         * or binary, or what line endings are used if it's text. If the given
         * `bufferSize` is smaller than [.FIRST_FEW_BYTES] set the
         * buffer size to [.FIRST_FEW_BYTES].
         *
         * @param bufferSize
         * Size to set
         * @return the size actually set
         * @since 6.0
         */
		@JvmStatic
		fun setBufferSize(bufferSize: Int): Int {
            val newSize = max(FIRST_FEW_BYTES.toDouble(), bufferSize.toDouble()).toInt()
            return BUFFER_SIZE.updateAndGet { curr: Int -> newSize }
        }

        /**
         * Determine heuristically whether the bytes contained in a stream
         * represents binary (as opposed to text) content.
         *
         * Note: Do not further use this stream after having called this method! The
         * stream may not be fully read and will be left at an unknown position
         * after consuming an unknown number of bytes. The caller is responsible for
         * closing the stream.
         *
         * @param raw
         * input stream containing the raw file content.
         * @return true if raw is likely to be a binary file, false otherwise
         * @throws java.io.IOException
         * if input stream could not be read
         */
        @JvmStatic
		@Throws(IOException::class)
        fun isBinary(raw: InputStream): Boolean {
            val buffer = ByteArray(bufferSize + 1)
            var cnt = 0
            while (cnt < buffer.size) {
                val n = raw.read(buffer, cnt, buffer.size - cnt)
                if (n == -1) {
                    break
                }
                cnt += n
            }
            return isBinary(buffer, cnt, cnt < buffer.size)
        }

        /**
         * Determine heuristically whether a byte array represents binary (as
         * opposed to text) content.
         *
         * @param raw
         * the raw file content.
         * @return true if raw is likely to be a binary file, false otherwise
         */
        fun isBinary(raw: ByteArray): Boolean {
            return isBinary(raw, raw.size)
        }

        /**
         * Determine heuristically whether a byte array represents binary (as
         * opposed to text) content.
         *
         * @param raw
         * the raw file content.
         * @param length
         * number of bytes in `raw` to evaluate. This should be
         * `raw.length` unless `raw` was over-allocated by
         * the caller.
         * @return true if raw is likely to be a binary file, false otherwise
         */
        fun isBinary(raw: ByteArray, length: Int): Boolean {
            return isBinary(raw, length, false)
        }

        /**
         * Determine heuristically whether a byte array represents binary (as
         * opposed to text) content.
         *
         * @param raw
         * the raw file content.
         * @param length
         * number of bytes in `raw` to evaluate. This should be
         * `raw.length` unless `raw` was over-allocated by
         * the caller.
         * @param complete
         * whether `raw` contains the whole data
         * @return true if raw is likely to be a binary file, false otherwise
         * @since 6.0
         */
		@JvmStatic
		fun isBinary(raw: ByteArray, length: Int, complete: Boolean): Boolean {
            // Similar heuristic as C Git. Differences:
            // - limited buffer size; may be only the beginning of a large blob
            // - no counting of printable vs. non-printable bytes < 0x20 and 0x7F
            var length = length
            val maxLength = bufferSize
            var isComplete = complete
            if (length > maxLength) {
                // We restrict the length in all cases to getBufferSize() to get
                // predictable behavior. Sometimes we load streams, and sometimes we
                // have the full data in memory. With streams, we never look at more
                // than the first getBufferSize() bytes. If we looked at more when
                // we have the full data, different code paths in JGit might come to
                // different conclusions.
                length = maxLength
                isComplete = false
            }
            var last = 'x'.code.toByte() // Just something inconspicuous.
            for (ptr in 0 until length) {
                val curr = raw[ptr]
                if (isBinary(curr, last)) {
                    return true
                }
                last = curr
            }
            if (isComplete) {
                // Buffer contains everything...
                return last == '\r'.code.toByte() // ... so this must be a lone CR
            }
            return false
        }

        /**
         * Determines from the last two bytes read from a source if it looks like
         * binary content.
         *
         * @param curr
         * the last byte, read after `prev`
         * @param prev
         * the previous byte, read before `last`
         * @return `true` if either byte is NUL, or if prev is CR and curr is
         * not LF, `false` otherwise
         * @since 6.0
         */
		@JvmStatic
		fun isBinary(curr: Byte, prev: Byte): Boolean {
            return curr == '\u0000'.code.toByte() || (curr != '\n'.code.toByte() && prev == '\r'.code.toByte()) || prev == '\u0000'.code.toByte()
        }

        /**
         * Determine heuristically whether a byte array represents text content
         * using CR-LF as line separator.
         *
         * @param raw
         * the raw file content.
         * @return `true` if raw is likely to be CR-LF delimited text,
         * `false` otherwise
         * @since 5.3
         */
		@JvmStatic
		fun isCrLfText(raw: ByteArray): Boolean {
            return isCrLfText(raw, raw.size)
        }

        /**
         * Determine heuristically whether the bytes contained in a stream represent
         * text content using CR-LF as line separator.
         *
         * Note: Do not further use this stream after having called this method! The
         * stream may not be fully read and will be left at an unknown position
         * after consuming an unknown number of bytes. The caller is responsible for
         * closing the stream.
         *
         * @param raw
         * input stream containing the raw file content.
         * @return `true` if raw is likely to be CR-LF delimited text,
         * `false` otherwise
         * @throws java.io.IOException
         * if input stream could not be read
         * @since 5.3
         */
        @JvmStatic
		@Throws(IOException::class)
        fun isCrLfText(raw: InputStream): Boolean {
            val buffer = ByteArray(bufferSize)
            var cnt = 0
            while (cnt < buffer.size) {
                val n = raw.read(buffer, cnt, buffer.size - cnt)
                if (n == -1) {
                    break
                }
                cnt += n
            }
            return isCrLfText(buffer, cnt)
        }

        /**
         * Determine heuristically whether a byte array represents text content
         * using CR-LF as line separator.
         *
         * @param raw
         * the raw file content.
         * @param length
         * number of bytes in `raw` to evaluate.
         * @return `true` if raw is likely to be CR-LF delimited text,
         * `false` otherwise
         * @since 5.3
         */
        fun isCrLfText(raw: ByteArray, length: Int): Boolean {
            return isCrLfText(raw, length, false)
        }

        /**
         * Determine heuristically whether a byte array represents text content
         * using CR-LF as line separator.
         *
         * @param raw
         * the raw file content.
         * @param length
         * number of bytes in `raw` to evaluate.
         * @return `true` if raw is likely to be CR-LF delimited text,
         * `false` otherwise
         * @param complete
         * whether `raw` contains the whole data
         * @since 6.0
         */
		@JvmStatic
		fun isCrLfText(raw: ByteArray, length: Int, complete: Boolean): Boolean {
            var has_crlf = false
            var last = 'x'.code.toByte() // Just something inconspicuous
            for (ptr in 0 until length) {
                val curr = raw[ptr]
                if (isBinary(curr, last)) {
                    return false
                }
                if (curr == '\n'.code.toByte() && last == '\r'.code.toByte()) {
                    has_crlf = true
                }
                last = curr
            }
            if (last == '\r'.code.toByte()) {
                if (complete) {
                    // Lone CR: it's binary after all.
                    return false
                }
                // Tough call. If the next byte, which we don't have, would be a
                // '\n', it'd be a CR-LF text, otherwise it'd be binary. Just decide
                // based on what we already scanned; it wasn't binary until now.
            }
            return has_crlf
        }

        /**
         * Read a blob object into RawText, or throw BinaryBlobException if the blob
         * is binary.
         *
         * @param ldr
         * the ObjectLoader for the blob
         * @param threshold
         * if the blob is larger than this size, it is always assumed to
         * be binary.
         * @since 4.10
         * @return the RawText representing the blob.
         * @throws org.eclipse.jgit.errors.BinaryBlobException
         * if the blob contains binary data.
         * @throws java.io.IOException
         * if the input could not be read.
         */
        @JvmStatic
		@Throws(IOException::class, BinaryBlobException::class)
        fun load(ldr: ObjectLoader, threshold: Int): RawText {
            val sz = ldr.size

            if (sz > threshold) {
                throw BinaryBlobException()
            }

            val bufferSize = bufferSize
            if (sz <= bufferSize) {
                val data = ldr.getCachedBytes(bufferSize)
                if (isBinary(data, data.size, true)) {
                    throw BinaryBlobException()
                }
                return RawText(data)
            }

            val head = ByteArray(bufferSize)
            ldr.openStream().use { stream ->
                var off = 0
                var left = head.size
                var last = 'x'.code.toByte() // Just something inconspicuous
                while (left > 0) {
                    var n = stream.read(head, off, left)
                    if (n < 0) {
                        throw EOFException()
                    }
                    left -= n

                    while (n > 0) {
                        val curr = head[off]
                        if (isBinary(curr, last)) {
                            throw BinaryBlobException()
                        }
                        last = curr
                        off++
                        n--
                    }
                }

                val data: ByteArray
                try {
                    data = ByteArray(sz.toInt())
                } catch (e: OutOfMemoryError) {
                    throw OutOfMemory(e)
                }

                System.arraycopy(head, 0, data, 0, head.size)
                IO.readFully(stream, data, off, (sz - off).toInt())
                return RawText(data, RawParseUtils.lineMapOrBinary(data, 0, sz.toInt()))
            }
        }
    }
}
