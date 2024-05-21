/*
 * Copyright (C) 2008-2009, Google Inc.
 * Copyright (C) 2006-2008, Shawn O. Pearce <spearce@spearce.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.util

//import org.eclipse.jgit.annotations.Nullable
import org.eclipse.jgit.diff.RawText.Companion.isBinary
//import org.eclipse.jgit.errors.BinaryBlobException
import org.eclipse.jgit.lib.Constants.encode
//import org.eclipse.jgit.lib.ObjectChecker
//import org.eclipse.jgit.lib.PersonIdent
import java.nio.ByteBuffer
import java.nio.charset.*
import java.util.*
import kotlin.math.max

/**
 * Handy utility functions to parse raw object contents.
 */
object RawParseUtils {
    /**
     * UTF-8 charset constant.
     *
     * @since 2.2
     */
    @Deprecated("use {@link java.nio.charset.StandardCharsets#UTF_8} instead")
    val UTF8_CHARSET: Charset = StandardCharsets.UTF_8

    private val digits10: ByteArray

    private val digits16: ByteArray

    private val footerLineKeyChars: ByteArray

    private val encodingAliases: MutableMap<String, Charset> = HashMap()

    init {
        encodingAliases["latin-1"] = StandardCharsets.ISO_8859_1 //$NON-NLS-1$
        encodingAliases["iso-latin-1"] = StandardCharsets.ISO_8859_1 //$NON-NLS-1$

        digits10 = ByteArray('9'.code + 1)
        Arrays.fill(digits10, (-1).toByte())
        run {
            var i = '0'
            while (i <= '9') {
                digits10[i.code] = (i.code - '0'.code).toByte()
                i++
            }
        }

        digits16 = ByteArray('f'.code + 1)
        Arrays.fill(digits16, (-1).toByte())
        run {
            var i = '0'
            while (i <= '9') {
                digits16[i.code] = (i.code - '0'.code).toByte()
                i++
            }
        }
        run {
            var i = 'a'
            while (i <= 'f') {
                digits16[i.code] = ((i.code - 'a'.code) + 10).toByte()
                i++
            }
        }
        run {
            var i = 'A'
            while (i <= 'F') {
                digits16[i.code] = ((i.code - 'A'.code) + 10).toByte()
                i++
            }
        }

        footerLineKeyChars = ByteArray('z'.code + 1)
        footerLineKeyChars['-'.code] = 1
        run {
            var i = '0'
            while (i <= '9') {
                footerLineKeyChars[i.code] = 1
                i++
            }
        }
        run {
            var i = 'A'
            while (i <= 'Z') {
                footerLineKeyChars[i.code] = 1
                i++
            }
        }
        var i = 'a'
        while (i <= 'z') {
            footerLineKeyChars[i.code] = 1
            i++
        }
    }

    /**
     * Determine if b[ptr] matches src.
     *
     * @param b
     * the buffer to scan.
     * @param ptr
     * first position within b, this should match src[0].
     * @param src
     * the buffer to test for equality with b.
     * @return ptr + src.length if b[ptr.src.length] == src; else -1.
     */
    @JvmStatic
    fun match(b: ByteArray, ptr: Int, src: ByteArray): Int {
        var ptr = ptr
        if (ptr + src.size > b.size) return -1
        var i = 0
        while (i < src.size) {
            if (b[ptr] != src[i]) return -1
            i++
            ptr++
        }
        return ptr
    }

    private val base10byte = byteArrayOf(
        '0'.code.toByte(),
        '1'.code.toByte(),
        '2'.code.toByte(),
        '3'.code.toByte(),
        '4'.code.toByte(),
        '5'.code.toByte(),
        '6'.code.toByte(),
        '7'.code.toByte(),
        '8'.code.toByte(),
        '9'.code.toByte()
    )

    /**
     * Format a base 10 numeric into a temporary buffer.
     *
     *
     * Formatting is performed backwards. The method starts at offset
     * `o-1` and ends at `o-1-digits`, where
     * `digits` is the number of positions necessary to store the
     * base 10 value.
     *
     *
     * The argument and return values from this method make it easy to chain
     * writing, for example:
     *
     *
     * <pre>
     * final byte[] tmp = new byte[64];
     * int ptr = tmp.length;
     * tmp[--ptr] = '\n';
     * ptr = RawParseUtils.formatBase10(tmp, ptr, 32);
     * tmp[--ptr] = ' ';
     * ptr = RawParseUtils.formatBase10(tmp, ptr, 18);
     * tmp[--ptr] = 0;
     * final String str = new String(tmp, ptr, tmp.length - ptr);
    </pre> *
     *
     * @param b
     * buffer to write into.
     * @param o
     * one offset past the location where writing will begin; writing
     * proceeds towards lower index values.
     * @param value
     * the value to store.
     * @return the new offset value `o`. This is the position of
     * the last byte written. Additional writing should start at one
     * position earlier.
     */
    @JvmStatic
    fun formatBase10(b: ByteArray, o: Int, value: Int): Int {
        var o = o
        var value = value
        if (value == 0) {
            b[--o] = '0'.code.toByte()
            return o
        }
        val isneg = value < 0
        if (isneg) value = -value
        while (value != 0) {
            b[--o] = base10byte[value % 10]
            value /= 10
        }
        if (isneg) b[--o] = '-'.code.toByte()
        return o
    }

    /**
     * Parse a base 10 numeric from a sequence of ASCII digits into an int.
     *
     *
     * Digit sequences can begin with an optional run of spaces before the
     * sequence, and may start with a '+' or a '-' to indicate sign position.
     * Any other characters will cause the method to stop and return the current
     * result to the caller.
     *
     * @param b
     * buffer to scan.
     * @param ptr
     * position within buffer to start parsing digits at.
     * @param ptrResult
     * optional location to return the new ptr value through. If null
     * the ptr value will be discarded.
     * @return the value at this location; 0 if the location is not a valid
     * numeric.
     */
//    @JvmStatic
//    fun parseBase10(
//        b: ByteArray, ptr: Int,
//        ptrResult: MutableInteger?
//    ): Int {
//        var ptr = ptr
//        var r = 0
//        var sign = 0
//        try {
//            val sz = b.size
//            while (ptr < sz && b[ptr] == ' '.code.toByte()) ptr++
//            if (ptr >= sz) return 0
//
//            when (b[ptr]) {
//                '-'.code.toByte() -> {
//                    sign = -1
//                    ptr++
//                }
//
//                '+'.code.toByte() -> ptr++
//            }
//            while (ptr < sz) {
//                val v = digits10[b[ptr].toInt()]
//                if (v < 0) break
//                r = (r * 10) + v
//                ptr++
//            }
//        } catch (e: ArrayIndexOutOfBoundsException) {
//            // Not a valid digit.
//        }
//        if (ptrResult != null) ptrResult.value = ptr
//        return if (sign < 0) -r else r
//    }

    /**
     * Parse a base 10 numeric from a sequence of ASCII digits into a long.
     *
     *
     * Digit sequences can begin with an optional run of spaces before the
     * sequence, and may start with a '+' or a '-' to indicate sign position.
     * Any other characters will cause the method to stop and return the current
     * result to the caller.
     *
     * @param b
     * buffer to scan.
     * @param ptr
     * position within buffer to start parsing digits at.
     * @param ptrResult
     * optional location to return the new ptr value through. If null
     * the ptr value will be discarded.
     * @return the value at this location; 0 if the location is not a valid
     * numeric.
     */
//    @JvmStatic
//    fun parseLongBase10(
//        b: ByteArray, ptr: Int,
//        ptrResult: MutableInteger?
//    ): Long {
//        var ptr = ptr
//        var r: Long = 0
//        var sign = 0
//        try {
//            val sz = b.size
//            while (ptr < sz && b[ptr] == ' '.code.toByte()) ptr++
//            if (ptr >= sz) return 0
//
//            when (b[ptr]) {
//                '-'.code.toByte() -> {
//                    sign = -1
//                    ptr++
//                }
//
//                '+'.code.toByte() -> ptr++
//            }
//            while (ptr < sz) {
//                val v = digits10[b[ptr].toInt()]
//                if (v < 0) break
//                r = (r * 10) + v
//                ptr++
//            }
//        } catch (e: ArrayIndexOutOfBoundsException) {
//            // Not a valid digit.
//        }
//        if (ptrResult != null) ptrResult.value = ptr
//        return if (sign < 0) -r else r
//    }

    /**
     * Parse 4 character base 16 (hex) formatted string to unsigned integer.
     *
     *
     * The number is read in network byte order, that is, most significant
     * nybble first.
     *
     * @param bs
     * buffer to parse digits from; positions `[p, p+4)` will
     * be parsed.
     * @param p
     * first position within the buffer to parse.
     * @return the integer value.
     * @throws java.lang.ArrayIndexOutOfBoundsException
     * if the string is not hex formatted.
     */
    @JvmStatic
    fun parseHexInt16(bs: ByteArray, p: Int): Int {
        var r = digits16[bs[p].toInt()].toInt() shl 4

        r = r or digits16[bs[p + 1].toInt()].toInt()
        r = r shl 4

        r = r or digits16[bs[p + 2].toInt()].toInt()
        r = r shl 4

        r = r or digits16[bs[p + 3].toInt()].toInt()
        if (r < 0) throw ArrayIndexOutOfBoundsException()
        return r
    }

    /**
     * Parse 8 character base 16 (hex) formatted string to unsigned integer.
     *
     *
     * The number is read in network byte order, that is, most significant
     * nybble first.
     *
     * @param bs
     * buffer to parse digits from; positions `[p, p+8)` will
     * be parsed.
     * @param p
     * first position within the buffer to parse.
     * @return the integer value.
     * @throws java.lang.ArrayIndexOutOfBoundsException
     * if the string is not hex formatted.
     */
    @JvmStatic
    fun parseHexInt32(bs: ByteArray, p: Int): Int {
        var r = digits16[bs[p].toInt()].toInt() shl 4

        r = r or digits16[bs[p + 1].toInt()].toInt()
        r = r shl 4

        r = r or digits16[bs[p + 2].toInt()].toInt()
        r = r shl 4

        r = r or digits16[bs[p + 3].toInt()].toInt()
        r = r shl 4

        r = r or digits16[bs[p + 4].toInt()].toInt()
        r = r shl 4

        r = r or digits16[bs[p + 5].toInt()].toInt()
        r = r shl 4

        r = r or digits16[bs[p + 6].toInt()].toInt()

        val last = digits16[bs[p + 7].toInt()].toInt()
        if (r < 0 || last < 0) throw ArrayIndexOutOfBoundsException()
        return (r shl 4) or last
    }

    /**
     * Parse 16 character base 16 (hex) formatted string to unsigned long.
     *
     *
     * The number is read in network byte order, that is, most significant
     * nibble first.
     *
     * @param bs
     * buffer to parse digits from; positions `[p, p+16)` will
     * be parsed.
     * @param p
     * first position within the buffer to parse.
     * @return the integer value.
     * @throws java.lang.ArrayIndexOutOfBoundsException
     * if the string is not hex formatted.
     * @since 4.3
     */
    @JvmStatic
    fun parseHexInt64(bs: ByteArray, p: Int): Long {
        var r = (digits16[bs[p].toInt()].toInt() shl 4).toLong()

        r = r or digits16[bs[p + 1].toInt()].toLong()
        r = r shl 4

        r = r or digits16[bs[p + 2].toInt()].toLong()
        r = r shl 4

        r = r or digits16[bs[p + 3].toInt()].toLong()
        r = r shl 4

        r = r or digits16[bs[p + 4].toInt()].toLong()
        r = r shl 4

        r = r or digits16[bs[p + 5].toInt()].toLong()
        r = r shl 4

        r = r or digits16[bs[p + 6].toInt()].toLong()
        r = r shl 4

        r = r or digits16[bs[p + 7].toInt()].toLong()
        r = r shl 4

        r = r or digits16[bs[p + 8].toInt()].toLong()
        r = r shl 4

        r = r or digits16[bs[p + 9].toInt()].toLong()
        r = r shl 4

        r = r or digits16[bs[p + 10].toInt()].toLong()
        r = r shl 4

        r = r or digits16[bs[p + 11].toInt()].toLong()
        r = r shl 4

        r = r or digits16[bs[p + 12].toInt()].toLong()
        r = r shl 4

        r = r or digits16[bs[p + 13].toInt()].toLong()
        r = r shl 4

        r = r or digits16[bs[p + 14].toInt()].toLong()

        val last = digits16[bs[p + 15].toInt()].toInt()
        if (r < 0 || last < 0) throw ArrayIndexOutOfBoundsException()
        return (r shl 4) or last.toLong()
    }

    /**
     * Parse a single hex digit to its numeric value (0-15).
     *
     * @param digit
     * hex character to parse.
     * @return numeric value, in the range 0-15.
     * @throws java.lang.ArrayIndexOutOfBoundsException
     * if the input digit is not a valid hex digit.
     */
    @JvmStatic
    fun parseHexInt4(digit: Byte): Int {
        val r = digits16[digit.toInt()]
        if (r < 0) throw ArrayIndexOutOfBoundsException()
        return r.toInt()
    }

    /**
     * Parse a Git style timezone string.
     *
     *
     * The sequence "-0315" will be parsed as the numeric value -195, as the
     * lower two positions count minutes, not 100ths of an hour.
     *
     * @param b
     * buffer to scan.
     * @param ptr
     * position within buffer to start parsing digits at.
     * @param ptrResult
     * optional location to return the new ptr value through. If null
     * the ptr value will be discarded.
     * @return the timezone at this location, expressed in minutes.
     * @since 4.1
     */
    /**
     * Parse a Git style timezone string.
     *
     *
     * The sequence "-0315" will be parsed as the numeric value -195, as the
     * lower two positions count minutes, not 100ths of an hour.
     *
     * @param b
     * buffer to scan.
     * @param ptr
     * position within buffer to start parsing digits at.
     * @return the timezone at this location, expressed in minutes.
     */
//    @JvmOverloads
//    @JvmStatic
//    fun parseTimeZoneOffset(
//        b: ByteArray, ptr: Int,
//        ptrResult: MutableInteger? = null
//    ): Int {
//        val v = parseBase10(b, ptr, ptrResult)
//        val tzMins = v % 100
//        val tzHours = v / 100
//        return tzHours * 60 + tzMins
//    }

    /**
     * Locate the first position after a given character.
     *
     * @param b
     * buffer to scan.
     * @param ptr
     * position within buffer to start looking for chrA at.
     * @param chrA
     * character to find.
     * @return new position just after chrA.
     */
    @JvmStatic
    fun next(b: ByteArray, ptr: Int, chrA: Char): Int {
        var ptr = ptr
        val sz = b.size
        while (ptr < sz) {
            if (b[ptr++] == chrA.code.toByte()) return ptr
        }
        return ptr
    }

    /**
     * Locate the first position after the next LF.
     *
     *
     * This method stops on the first '\n' it finds.
     *
     * @param b
     * buffer to scan.
     * @param ptr
     * position within buffer to start looking for LF at.
     * @return new position just after the first LF found.
     */
    @JvmStatic
    fun nextLF(b: ByteArray, ptr: Int): Int {
        return next(b, ptr, '\n')
    }

    /**
     * Locate the first position after either the given character or LF.
     *
     *
     * This method stops on the first match it finds from either chrA or '\n'.
     *
     * @param b
     * buffer to scan.
     * @param ptr
     * position within buffer to start looking for chrA or LF at.
     * @param chrA
     * character to find.
     * @return new position just after the first chrA or LF to be found.
     */
    @JvmStatic
    fun nextLF(b: ByteArray, ptr: Int, chrA: Char): Int {
        var ptr = ptr
        val sz = b.size
        while (ptr < sz) {
            val c = b[ptr++]
            if (c == chrA.code.toByte() || c == '\n'.code.toByte()) return ptr
        }
        return ptr
    }

    /**
     * Locate the first end of line after the given position, while treating
     * following lines which are starting with spaces as part of the current
     * line.
     *
     *
     * For example, `nextLfSkippingSplitLines(
     * "row \n with space at beginning of a following line\nThe actual next line",
     * 0)` will return the position of `"\nThe actual next line"`.
     *
     * @param b
     * buffer to scan.
     * @param ptr
     * position within buffer to start looking for the next line.
     * @return new position just after the line end of the last line-split. This
     * is either b.length, or the index of the current split-line's
     * terminating newline.
     * @since 6.9
     */
    @JvmStatic
    fun nextLfSkippingSplitLines(b: ByteArray, ptr: Int): Int {
        var ptr = ptr
        val sz = b.size
        while (ptr < sz) {
            val c = b[ptr++]
            if (c == '\n'.code.toByte() && (ptr == sz || b[ptr] != ' '.code.toByte())) {
                return ptr - 1
            }
        }
        return ptr
    }

    /**
     * Extract a part of a buffer as a header value, removing the single blanks
     * at the front of continuation lines.
     *
     * @param b
     * buffer to extract the header from
     * @param start
     * of the header value, see
     * [.headerStart]
     * @param end
     * of the header; see
     * [.nextLfSkippingSplitLines]
     * @return the header value, with blanks indicating continuation lines
     * stripped
     * @since 6.9
     */
    @JvmStatic
    fun headerValue(b: ByteArray, start: Int, end: Int): ByteArray {
        val data = ByteArray(end - start)
        var out = 0
        var last = '\u0000'.code.toByte()
        for (`in` in start until end) {
            val ch = b[`in`]
            if (ch != ' '.code.toByte() || last != '\n'.code.toByte()) {
                data[out++] = ch
            }
            last = ch
        }
        if (out == data.size) {
            return data
        }
        return data.copyOf(out)
    }

    /**
     * Locate the first end of header after the given position. Note that
     * headers may be more than one line long.
     *
     *
     * Also note that there might be multiple headers. If you wish to find the
     * last header's end - call this in a loop.
     *
     * @param b
     * buffer to scan.
     * @param ptr
     * position within buffer to start looking for the header
     * (normally a new-line).
     * @return new position just after the line end. This is either b.length, or
     * the index of the header's terminating newline.
     * @since 5.1
     */
    @Deprecated("use {{@link #nextLfSkippingSplitLines}} directly instead")
    @JvmStatic
    fun headerEnd(b: ByteArray, ptr: Int): Int {
        return nextLfSkippingSplitLines(b, ptr)
    }

    /**
     * Find the start of the contents of a given header.
     *
     * @param b
     * buffer to scan.
     * @param headerName
     * header to search for
     * @param ptr
     * position within buffer to start looking for header at.
     * @return new position at the start of the header's contents, -1 for
     * not found
     * @since 5.1
     */
    @JvmStatic
    fun headerStart(headerName: ByteArray, b: ByteArray, ptr: Int): Int {
        // Start by advancing to just past a LF or buffer start
        var ptr = ptr
        if (ptr != 0) {
            ptr = nextLF(b, ptr - 1)
        }
        while (ptr < b.size - (headerName.size + 1)) {
            var found = true
            for (element in headerName) {
                if (element != b[ptr++]) {
                    found = false
                    break
                }
            }
            if (found && b[ptr++] == ' '.code.toByte()) {
                return ptr
            }
            ptr = nextLF(b, ptr)
        }
        return -1
    }

    /**
     * Returns whether the message starts with any known headers.
     *
     * @param b
     * buffer to scan.
     * @return whether the message starts with any known headers
     * @since 6.9
     */
//    @JvmStatic
//    fun hasAnyKnownHeaders(b: ByteArray): Boolean {
//        return match(b, 0, ObjectChecker.tree) != -1 || match(
//            b,
//            0,
//            ObjectChecker.parent
//        ) != -1 || match(
//            b,
//            0,
//            ObjectChecker.author
//        ) != -1 || match(b, 0, ObjectChecker.committer) != -1 || match(
//            b,
//            0,
//            ObjectChecker.encoding
//        ) != -1 || match(b, 0, ObjectChecker.`object`) != -1 || match(
//            b,
//            0,
//            ObjectChecker.type
//        ) != -1 || match(b, 0, ObjectChecker.tag) != -1 || match(b, 0, ObjectChecker.tagger) != -1
//    }

    /**
     * Locate the first position before a given character.
     *
     * @param b
     * buffer to scan.
     * @param ptr
     * position within buffer to start looking for chrA at.
     * @param chrA
     * character to find.
     * @return new position just before chrA, -1 for not found
     */
    @JvmStatic
    fun prev(b: ByteArray, ptr: Int, chrA: Char): Int {
        var ptr = ptr
        if (ptr == b.size) --ptr
        while (ptr >= 0) {
            if (b[ptr--] == chrA.code.toByte()) return ptr
        }
        return ptr
    }

    /**
     * Locate the first position before the previous LF.
     *
     *
     * This method stops on the first '\n' it finds.
     *
     * @param b
     * buffer to scan.
     * @param ptr
     * position within buffer to start looking for LF at.
     * @return new position just before the first LF found, -1 for not found
     */
    @JvmStatic
    fun prevLF(b: ByteArray, ptr: Int): Int {
        return prev(b, ptr, '\n')
    }

    /**
     * Locate the previous position before either the given character or LF.
     *
     *
     * This method stops on the first match it finds from either chrA or '\n'.
     *
     * @param b
     * buffer to scan.
     * @param ptr
     * position within buffer to start looking for chrA or LF at.
     * @param chrA
     * character to find.
     * @return new position just before the first chrA or LF to be found, -1 for
     * not found
     */
    @JvmStatic
    fun prevLF(b: ByteArray, ptr: Int, chrA: Char): Int {
        var ptr = ptr
        if (ptr == b.size) --ptr
        while (ptr >= 0) {
            val c = b[ptr--]
            if (c == chrA.code.toByte() || c == '\n'.code.toByte()) return ptr
        }
        return ptr
    }

    /**
     * Index the region between `[ptr, end)` to find line starts.
     *
     *
     * The returned list is 1 indexed. Index 0 contains
     * [java.lang.Integer.MIN_VALUE] to pad the list out.
     *
     *
     * Using a 1 indexed list means that line numbers can be directly accessed
     * from the list, so `list.get(1)` (aka get line 1) returns
     * `ptr`.
     *
     *
     * The last element (index `map.size()-1`) always contains
     * `end`.
     *
     * @param buf
     * buffer to scan.
     * @param ptr
     * position within the buffer corresponding to the first byte of
     * line 1.
     * @param end
     * 1 past the end of the content within `buf`.
     * @return a line map indicating the starting position of each line.
     */
    @JvmStatic
    fun lineMap(buf: ByteArray, ptr: Int, end: Int): IntList {
        var ptr = ptr
        val map = IntList((end - ptr) / 36)
        map.fillTo(1, Int.MIN_VALUE)
        while (ptr < end) {
            map.add(ptr)
            ptr = nextLF(buf, ptr)
        }
        map.add(end)
        return map
    }

    /**
     * Like [.lineMap] but throw
     * [BinaryBlobException] if a NUL byte is encountered.
     *
     * @param buf
     * buffer to scan.
     * @param ptr
     * position within the buffer corresponding to the first byte of
     * line 1.
     * @param end
     * 1 past the end of the content within `buf`.
     * @return a line map indicating the starting position of each line.
     * @throws BinaryBlobException
     * if a NUL byte or a lone CR is found.
     * @since 5.0
     */
//    @Throws(BinaryBlobException::class)
//    @JvmStatic
//    fun lineMapOrBinary(buf: ByteArray, ptr: Int, end: Int): IntList {
//        // Experimentally derived from multiple source repositories
//        // the average number of bytes/line is 36. Its a rough guess
//        // to initially size our map close to the target.
//        var ptr = ptr
//        val map = IntList((end - ptr) / 36)
//        map.add(Int.MIN_VALUE)
//        var last = '\n'.code.toByte() // Must be \n to add the initial ptr
//        while (ptr < end) {
//            if (last == '\n'.code.toByte()) {
//                map.add(ptr)
//            }
//            val curr = buf[ptr]
//            if (isBinary(curr, last)) {
//                throw BinaryBlobException()
//            }
//            last = curr
//            ptr++
//        }
//        if (last == '\r'.code.toByte()) {
//            // Counts as binary
//            throw BinaryBlobException()
//        }
//        map.add(end)
//        return map
//    }

    /**
     * Locate the "author " header line data.
     *
     * @param b
     * buffer to scan.
     * @param ptr
     * position in buffer to start the scan at. Most callers should
     * pass 0 to ensure the scan starts from the beginning of the
     * commit buffer and does not accidentally look at message body.
     * @return position just after the space in "author ", so the first
     * character of the author's name. If no author header can be
     * located -1 is returned.
     */
//    @JvmStatic
//    fun author(b: ByteArray, ptr: Int): Int {
//        var ptr = ptr
//        val sz = b.size
//        if (ptr == 0) ptr += 46 // skip the "tree ..." line.
//
//        while (ptr < sz && b[ptr] == 'p'.code.toByte()) ptr += 48 // skip this parent.
//
//        return match(b, ptr, ObjectChecker.author)
//    }

    /**
     * Locate the "committer " header line data.
     *
     * @param b
     * buffer to scan.
     * @param ptr
     * position in buffer to start the scan at. Most callers should
     * pass 0 to ensure the scan starts from the beginning of the
     * commit buffer and does not accidentally look at message body.
     * @return position just after the space in "committer ", so the first
     * character of the committer's name. If no committer header can be
     * located -1 is returned.
     */
//    @JvmStatic
//    fun committer(b: ByteArray, ptr: Int): Int {
//        var ptr = ptr
//        val sz = b.size
//        if (ptr == 0) ptr += 46 // skip the "tree ..." line.
//
//        while (ptr < sz && b[ptr] == 'p'.code.toByte()) ptr += 48 // skip this parent.
//
//        if (ptr < sz && b[ptr] == 'a'.code.toByte()) ptr = nextLF(b, ptr)
//        return match(b, ptr, ObjectChecker.committer)
//    }

    /**
     * Locate the "tagger " header line data.
     *
     * @param b
     * buffer to scan.
     * @param ptr
     * position in buffer to start the scan at. Most callers should
     * pass 0 to ensure the scan starts from the beginning of the tag
     * buffer and does not accidentally look at message body.
     * @return position just after the space in "tagger ", so the first
     * character of the tagger's name. If no tagger header can be
     * located -1 is returned.
     */
//    @JvmStatic
//    fun tagger(b: ByteArray, ptr: Int): Int {
//        var ptr = ptr
//        val sz = b.size
//        if (ptr == 0) ptr += 48 // skip the "object ..." line.
//
//        while (ptr < sz) {
//            if (b[ptr] == '\n'.code.toByte()) return -1
//            val m = match(b, ptr, ObjectChecker.tagger)
//            if (m >= 0) return m
//            ptr = nextLF(b, ptr)
//        }
//        return -1
//    }

    /**
     * Locate the "encoding " header line.
     *
     * @param b
     * buffer to scan.
     * @param ptr
     * position in buffer to start the scan at. Most callers should
     * pass 0 to ensure the scan starts from the beginning of the
     * buffer and does not accidentally look at the message body.
     * @return position just after the space in "encoding ", so the first
     * character of the encoding's name. If no encoding header can be
     * located -1 is returned (and UTF-8 should be assumed).
     */
//    @JvmStatic
//    fun encoding(b: ByteArray, ptr: Int): Int {
//        var ptr = ptr
//        val sz = b.size
//        while (ptr < sz) {
//            if (b[ptr] == '\n'.code.toByte()) return -1
//            if (b[ptr] == 'e'.code.toByte()) break
//            ptr = nextLF(b, ptr)
//        }
//        return match(b, ptr, ObjectChecker.encoding)
//    }

    /**
     * Parse the "encoding " header as a string.
     *
     *
     * Locates the "encoding " header (if present) and returns its value.
     *
     * @param b
     * buffer to scan.
     * @return the encoding header as specified in the commit; null if the
     * header was not present and should be assumed.
     * @since 4.2
     */
//    @Nullable
//    @JvmStatic
//    fun parseEncodingName(b: ByteArray): String? {
//        val enc = encoding(b, 0)
//        if (enc < 0) {
//            return null
//        }
//        val lf = nextLF(b, enc)
//        return decode(StandardCharsets.UTF_8, b, enc, lf - 1)
//    }

    /**
     * Parse the "encoding " header into a character set reference.
     *
     *
     * Locates the "encoding " header (if present) by first calling
     * [.encoding] and then returns the proper character set
     * to apply to this buffer to evaluate its contents as character data.
     *
     *
     * If no encoding header is present `UTF-8` is assumed.
     *
     * @param b
     * buffer to scan.
     * @return the Java character set representation. Never null.
     * @throws IllegalCharsetNameException
     * if the character set requested by the encoding header is
     * malformed and unsupportable.
     * @throws UnsupportedCharsetException
     * if the JRE does not support the character set requested by
     * the encoding header.
     */
//    @JvmStatic
//    fun parseEncoding(b: ByteArray): Charset {
//        val enc = parseEncodingName(b) ?: return StandardCharsets.UTF_8
//
//        val name = enc.trim { it <= ' ' }
//        try {
//            return Charset.forName(name)
//        } catch (badName: IllegalCharsetNameException) {
//            val aliased = charsetForAlias(name)
//            if (aliased != null) {
//                return aliased
//            }
//            throw badName
//        } catch (badName: UnsupportedCharsetException) {
//            val aliased = charsetForAlias(name)
//            if (aliased != null) {
//                return aliased
//            }
//            throw badName
//        }
//    }

    /**
     * Parse the "encoding " header into a character set reference.
     *
     *
     * If unsuccessful, return UTF-8.
     *
     * @param buffer
     * buffer to scan.
     * @return the Java character set representation. Never null. Default to
     * UTF-8.
     * @see .parseEncoding
     * @since 6.7
     */
//    @JvmStatic
//    fun guessEncoding(buffer: ByteArray): Charset {
//        return try {
//            parseEncoding(buffer)
//        } catch (e: IllegalCharsetNameException) {
//            StandardCharsets.UTF_8
//        } catch (e: UnsupportedCharsetException) {
//            StandardCharsets.UTF_8
//        }
//    }

    /**
     * Parse a name string (e.g. author, committer, tagger) into a PersonIdent.
     *
     *
     * Leading spaces won't be trimmed from the string, i.e. will show up in the
     * parsed name afterwards.
     *
     * @param in
     * the string to parse a name from.
     * @return the parsed identity or null in case the identity could not be
     * parsed.
     */
//    @JvmStatic
//    fun parsePersonIdent(`in`: String?): PersonIdent? {
//        return parsePersonIdent(encode(`in`), 0)
//    }

    /**
     * Parse a name line (e.g. author, committer, tagger) into a PersonIdent.
     *
     *
     * When passing in a value for `nameB` callers should use the
     * return value of [.author] or
     * [.committer], as these methods provide the proper
     * position within the buffer.
     *
     * @param raw
     * the buffer to parse character data from.
     * @param nameB
     * first position of the identity information. This should be the
     * first position after the space which delimits the header field
     * name (e.g. "author" or "committer") from the rest of the
     * identity line.
     * @return the parsed identity or null in case the identity could not be
     * parsed.
     */
//    @JvmStatic
//    fun parsePersonIdent(raw: ByteArray, nameB: Int): PersonIdent? {
//        var cs = try {
//            parseEncoding(raw)
//        } catch (e: IllegalCharsetNameException) {
//            // Assume UTF-8 for person identities, usually this is correct.
//            // If not decode() will fall back to the ISO-8859-1 encoding.
//            StandardCharsets.UTF_8
//        } catch (e: UnsupportedCharsetException) {
//            StandardCharsets.UTF_8
//        }
//
//        val emailB = nextLF(raw, nameB, '<')
//        val emailE = nextLF(raw, emailB, '>')
//        if (emailB >= raw.size || raw[emailB] == '\n'.code.toByte() ||
//            (emailE >= raw.size - 1 && raw[emailE - 1] != '>'.code.toByte())
//        ) return null
//
//        val nameEnd = if (emailB - 2 >= nameB && raw[emailB - 2] == ' '.code.toByte()) emailB - 2 else emailB - 1
//        val name = decode(cs, raw, nameB, nameEnd)
//        val email = decode(cs, raw, emailB, emailE - 1)
//
//        // Start searching from end of line, as after first name-email pair,
//        // another name-email pair may occur. We will ignore all kinds of
//        // "junk" following the first email.
//        //
//        // We've to use (emailE - 1) for the case that raw[email] is LF,
//        // otherwise we would run too far. "-2" is necessary to position
//        // before the LF in case of LF termination resp. the penultimate
//        // character if there is no trailing LF.
//        val tzBegin = lastIndexOfTrim(
//            raw, ' ',
//            nextLF(raw, emailE - 1) - 2
//        ) + 1
//        if (tzBegin <= emailE) // No time/zone, still valid
//            return PersonIdent(name, email, 0, 0)
//
//        val whenBegin = max(
//            emailE.toDouble(),
//            (lastIndexOfTrim(raw, ' ', tzBegin - 1) + 1).toDouble()
//        ).toInt()
//        if (whenBegin >= tzBegin - 1) // No time/zone, still valid
//            return PersonIdent(name, email, 0, 0)
//
//        val `when` = parseLongBase10(raw, whenBegin, null)
//        val tz = parseTimeZoneOffset(raw, tzBegin)
//        return PersonIdent(name, email, `when` * 1000L, tz)
//    }

    /**
     * Parse a name data (e.g. as within a reflog) into a PersonIdent.
     *
     *
     * When passing in a value for `nameB` callers should use the
     * return value of [.author] or
     * [.committer], as these methods provide the proper
     * position within the buffer.
     *
     * @param raw
     * the buffer to parse character data from.
     * @param nameB
     * first position of the identity information. This should be the
     * first position after the space which delimits the header field
     * name (e.g. "author" or "committer") from the rest of the
     * identity line.
     * @return the parsed identity. Never null.
     */
//    @JvmStatic
//    fun parsePersonIdentOnly(
//        raw: ByteArray,
//        nameB: Int
//    ): PersonIdent {
//        val stop = nextLF(raw, nameB)
//        val emailB = nextLF(raw, nameB, '<')
//        val emailE = nextLF(raw, emailB, '>')
//        val email = if (emailE < stop) {
//            decode(raw, emailB, emailE - 1)
//        } else {
//            "invalid" //$NON-NLS-1$
//        }
//        val name = if (emailB < stop) decode(raw, nameB, emailB - 2)
//        else decode(raw, nameB, stop)
//
//        val ptrout = MutableInteger()
//        val `when`: Long
//        val tz: Int
//        if (emailE < stop) {
//            `when` = parseLongBase10(raw, emailE + 1, ptrout)
//            tz = parseTimeZoneOffset(raw, ptrout.value)
//        } else {
//            `when` = 0
//            tz = 0
//        }
//        return PersonIdent(name, email, `when` * 1000L, tz)
//    }

    /**
     * Locate the end of a footer line key string.
     *
     *
     * If the region at `raw[ptr]` matches `^[A-Za-z0-9-]+:` (e.g.
     * "Signed-off-by: A. U. Thor\n") then this method returns the position of
     * the first ':'.
     *
     *
     * If the region at `raw[ptr]` does not match `^[A-Za-z0-9-]+:`
     * then this method returns -1.
     *
     * @param raw
     * buffer to scan.
     * @param ptr
     * first position within raw to consider as a footer line key.
     * @return position of the ':' which terminates the footer line key if this
     * is otherwise a valid footer line key; otherwise -1.
     */
    @JvmStatic
    fun endOfFooterLineKey(raw: ByteArray, ptr: Int): Int {
        var ptr = ptr
        try {
            while (true) {
                val c = raw[ptr]
                if (footerLineKeyChars[c.toInt()].toInt() == 0) {
                    if (c == ':'.code.toByte()) return ptr
                    return -1
                }
                ptr++
            }
        } catch (e: ArrayIndexOutOfBoundsException) {
            return -1
        }
    }

    /**
     * Decode a buffer under UTF-8, if possible.
     *
     * If the byte stream cannot be decoded that way, the platform default is
     * tried and if that too fails, the fail-safe ISO-8859-1 encoding is tried.
     *
     * @param buffer
     * buffer to pull raw bytes from.
     * @param start
     * start position in buffer
     * @param end
     * one position past the last location within the buffer to take
     * data from.
     * @return a string representation of the range `[start,end)`,
     * after decoding the region through the specified character set.
     */
    /**
     * Decode a buffer under UTF-8, if possible.
     *
     * If the byte stream cannot be decoded that way, the platform default is tried
     * and if that too fails, the fail-safe ISO-8859-1 encoding is tried.
     *
     * @param buffer
     * buffer to pull raw bytes from.
     * @return a string representation of the range `[start,end)`,
     * after decoding the region through the specified character set.
     */
//    @JvmOverloads
//    @JvmStatic
//    fun decode(
//        buffer: ByteArray, start: Int = 0,
//        end: Int = buffer.size
//    ): String {
//        return decode(StandardCharsets.UTF_8, buffer, start, end)
//    }

    /**
     * Decode a region of the buffer under the specified character set if possible.
     *
     * If the byte stream cannot be decoded that way, the platform default is tried
     * and if that too fails, the fail-safe ISO-8859-1 encoding is tried.
     *
     * @param cs
     * character set to use when decoding the buffer.
     * @param buffer
     * buffer to pull raw bytes from.
     * @param start
     * first position within the buffer to take data from.
     * @param end
     * one position past the last location within the buffer to take
     * data from.
     * @return a string representation of the range `[start,end)`,
     * after decoding the region through the specified character set.
     */
    /**
     * Decode a buffer under the specified character set if possible.
     *
     * If the byte stream cannot be decoded that way, the platform default is tried
     * and if that too fails, the fail-safe ISO-8859-1 encoding is tried.
     *
     * @param cs
     * character set to use when decoding the buffer.
     * @param buffer
     * buffer to pull raw bytes from.
     * @return a string representation of the range `[start,end)`,
     * after decoding the region through the specified character set.
     */
//    @JvmOverloads
//    @JvmStatic
//    fun decode(
//        cs: Charset?, buffer: ByteArray,
//        start: Int = 0, end: Int = buffer.size
//    ): String {
//        return try {
//            decodeNoFallback(cs, buffer, start, end)
//        } catch (e: CharacterCodingException) {
//            // Fall back to an ISO-8859-1 style encoding. At least all of
//            // the bytes will be present in the output.
//            //
//            extractBinaryString(buffer, start, end)
//        }
//    }

    /**
     * Decode a region of the buffer under the specified character set if
     * possible.
     *
     * If the byte stream cannot be decoded that way, the platform default is
     * tried and if that too fails, an exception is thrown.
     *
     * @param cs
     * character set to use when decoding the buffer.
     * @param buffer
     * buffer to pull raw bytes from.
     * @param start
     * first position within the buffer to take data from.
     * @param end
     * one position past the last location within the buffer to take
     * data from.
     * @return a string representation of the range `[start,end)`,
     * after decoding the region through the specified character set.
     * @throws java.nio.charset.CharacterCodingException
     * the input is not in any of the tested character sets.
     */
//    @JvmStatic
//    @Throws(CharacterCodingException::class)
//    fun decodeNoFallback(
//        cs: Charset?,
//        buffer: ByteArray?, start: Int, end: Int
//    ): String {
//        val b = ByteBuffer.wrap(buffer, start, end - start)
//        b.mark()
//
//        // Try our built-in favorite. The assumption here is that
//        // decoding will fail if the data is not actually encoded
//        // using that encoder.
//        try {
//            return decode(b, StandardCharsets.UTF_8)
//        } catch (e: CharacterCodingException) {
//            b.reset()
//        }
//
//        if (cs != StandardCharsets.UTF_8) {
//            // Try the suggested encoding, it might be right since it was
//            // provided by the caller.
//            try {
//                return decode(b, cs!!)
//            } catch (e: CharacterCodingException) {
//                b.reset()
//            }
//        }
//
//        // Try the default character set. A small group of people
//        // might actually use the same (or very similar) locale.
//        val defcs = SystemReader.getInstance().defaultCharset
//        if (defcs != cs && defcs != StandardCharsets.UTF_8) {
//            try {
//                return decode(b, defcs)
//            } catch (e: CharacterCodingException) {
//                b.reset()
//            }
//        }
//
//        throw CharacterCodingException()
//    }

    /**
     * Decode a region of the buffer under the ISO-8859-1 encoding.
     *
     * Each byte is treated as a single character in the 8859-1 character
     * encoding, performing a raw binary-&gt;char conversion.
     *
     * @param buffer
     * buffer to pull raw bytes from.
     * @param start
     * first position within the buffer to take data from.
     * @param end
     * one position past the last location within the buffer to take
     * data from.
     * @return a string representation of the range `[start,end)`.
     */
    @JvmStatic
    fun extractBinaryString(
        buffer: ByteArray,
        start: Int, end: Int
    ): String {
        val r = StringBuilder(end - start)
        for (i in start until end) r.append((buffer[i].toInt() and 0xff).toChar())
        return r.toString()
    }

    @JvmStatic
    @Throws(CharacterCodingException::class)
    private fun decode(b: ByteBuffer, charset: Charset): String {
        val d = charset.newDecoder()
        d.onMalformedInput(CodingErrorAction.REPORT)
        d.onUnmappableCharacter(CodingErrorAction.REPORT)
        return d.decode(b).toString()
    }

    /**
     * Locate the position of the commit message body.
     *
     * @param b
     * buffer to scan.
     * @param ptr
     * position in buffer to start the scan at. Most callers should
     * pass 0 to ensure the scan starts from the beginning of the
     * commit buffer.
     * @return position of the user's message buffer.
     */
    @JvmStatic
    fun commitMessage(b: ByteArray, ptr: Int): Int {
        var ptr = ptr
        val sz = b.size
        if (ptr == 0) ptr += 46 // skip the "tree ..." line.

        while (ptr < sz && b[ptr] == 'p'.code.toByte()) ptr += 48 // skip this parent.


        // Skip any remaining header lines, ignoring what their actual
        // header line type is. This is identical to the logic for a tag.
        //
        return tagMessage(b, ptr)
    }

    /**
     * Locate the position of the tag message body.
     *
     * @param b
     * buffer to scan.
     * @param ptr
     * position in buffer to start the scan at. Most callers should
     * pass 0 to ensure the scan starts from the beginning of the tag
     * buffer.
     * @return position of the user's message buffer.
     */
    @JvmStatic
    fun tagMessage(b: ByteArray, ptr: Int): Int {
        var ptr = ptr
        val sz = b.size
        if (ptr == 0) ptr += 48 // skip the "object ..." line.

        // Assume the rest of the current paragraph is all headers.
        while (ptr < sz && b[ptr] != '\n'.code.toByte()) ptr = nextLF(b, ptr)
        if (ptr < sz && b[ptr] == '\n'.code.toByte()) return ptr + 1
        return -1
    }

    /**
     * Locate the end of a paragraph.
     *
     *
     * A paragraph is ended by two consecutive LF bytes or CRLF pairs
     *
     * @param b
     * buffer to scan.
     * @param start
     * position in buffer to start the scan at. Most callers will
     * want to pass the first position of the commit message (as
     * found by [.commitMessage].
     * @return position of the LF at the end of the paragraph;
     * `b.length` if no paragraph end could be located.
     */
    @JvmStatic
    fun endOfParagraph(b: ByteArray, start: Int): Int {
        var ptr = start
        val sz = b.size
        while (ptr < sz && (b[ptr] != '\n'.code.toByte() && b[ptr] != '\r'.code.toByte())) ptr = nextLF(b, ptr)
        if (ptr > start && b[ptr - 1] == '\n'.code.toByte()) ptr--
        if (ptr > start && b[ptr - 1] == '\r'.code.toByte()) ptr--
        return ptr
    }

    /**
     * Get last index of `ch` in raw, trimming spaces.
     *
     * @param raw
     * buffer to scan.
     * @param ch
     * character to find.
     * @param pos
     * starting position.
     * @return last index of `ch` in raw, trimming spaces.
     * @since 4.1
     */
    @JvmStatic
    fun lastIndexOfTrim(raw: ByteArray, ch: Char, pos: Int): Int {
        var pos = pos
        while (pos >= 0 && raw[pos] == ' '.code.toByte()) pos--

        while (pos >= 0 && raw[pos] != ch.code.toByte()) pos--

        return pos
    }

//    private fun charsetForAlias(name: String): Charset? {
//        return encodingAliases[StringUtils.toLowerCase(name)]
//    }
}
