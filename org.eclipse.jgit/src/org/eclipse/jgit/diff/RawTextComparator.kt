/*
 * Copyright (C) 2009-2010, Google Inc.
 * Copyright (C) 2008-2009, Johannes E. Schindelin <johannes.schindelin@gmx.de> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.diff

import org.eclipse.jgit.util.IntList
//import org.eclipse.jgit.util.RawCharUtil

/**
 * Equivalence function for [org.eclipse.jgit.diff.RawText].
 */
abstract class RawTextComparator : SequenceComparator<RawText>() {
    override fun hash(seq: RawText, ptr: Int): Int {
        val begin = seq.lines[ptr + 1]
        val end = seq.lines[ptr + 2]
        return hashRegion(seq.rawContent, begin, end)
    }

    override fun reduceCommonStartEnd(a: RawText, b: RawText, e: Edit): Edit? {
        // This is a faster exact match based form that tries to improve
        // performance for the common case of the header and trailer of
        // a text file not changing at all. After this fast path we use
        // the slower path based on the super class' using equals() to
        // allow for whitespace ignore modes to still work.

        if (e.beginA == e.endA || e.beginB == e.endB) return e

        val aRaw = a.rawContent
        val bRaw = b.rawContent

        var aPtr = a.lines[e.beginA + 1]
        var bPtr = a.lines[e.beginB + 1]

        var aEnd = a.lines[e.endA + 1]
        var bEnd = b.lines[e.endB + 1]

        // This can never happen, but the JIT doesn't know that. If we
        // define this assertion before the tight while loops below it
        // should be able to skip the array bound checks on access.
        //
        if (aPtr < 0 || bPtr < 0 || aEnd > aRaw.size || bEnd > bRaw.size) throw IndexOutOfBoundsException()

        while (aPtr < aEnd && bPtr < bEnd && aRaw[aPtr] == bRaw[bPtr]) {
            aPtr++
            bPtr++
        }

        while (aPtr < aEnd && bPtr < bEnd && aRaw[aEnd - 1] == bRaw[bEnd - 1]) {
            aEnd--
            bEnd--
        }

        e.beginA = findForwardLine(a.lines, e.beginA, aPtr)
        e.beginB = findForwardLine(b.lines, e.beginB, bPtr)

        e.endA = findReverseLine(a.lines, e.endA, aEnd)

        val partialA = aEnd < a.lines[e.endA + 1]
        if (partialA) bEnd += a.lines[e.endA + 1] - aEnd

        e.endB = findReverseLine(b.lines, e.endB, bEnd)

        if (!partialA && bEnd < b.lines[e.endB + 1]) e.endA++

        return super.reduceCommonStartEnd(a, b, e)
    }

    /**
     * Compute a hash code for a region.
     *
     * @param raw
     * the raw file content.
     * @param ptr
     * first byte of the region to hash.
     * @param end
     * 1 past the last byte of the region.
     * @return hash code for the region `[ptr, end)` of raw.
     */
    protected abstract fun hashRegion(raw: ByteArray, ptr: Int, end: Int): Int

    companion object {
        /** No special treatment.  */
		val DEFAULT: RawTextComparator = object : RawTextComparator() {
            override fun equals(a: RawText, ai: Int, b: RawText, bi: Int): Boolean {
                var ai = ai
                var bi = bi
                ai++
                bi++

                var `as` = a.lines[ai]
                var bs = b.lines[bi]
                val ae = a.lines[ai + 1]
                val be = b.lines[bi + 1]

                if (ae - `as` != be - bs) return false

                while (`as` < ae) {
                    if (a.rawContent[`as`++] != b.rawContent[bs++]) return false
                }
                return true
            }

            override fun hashRegion(raw: ByteArray, ptr: Int, end: Int): Int {
                var ptr = ptr
                var hash = 5381
                while (ptr < end) {
                    hash = ((hash shl 5) + hash) + (raw[ptr].toInt() and 0xff)
                    ptr++
                }
                return hash
            }
        }

        /** Ignores all whitespace.  */
////		val WS_IGNORE_ALL: RawTextComparator = object : RawTextComparator() {
//            override fun equals(a: RawText, ai: Int, b: RawText, bi: Int): Boolean {
//                var ai = ai
//                var bi = bi
//                ai++
//                bi++
//
//                var `as` = a.lines[ai]
//                var bs = b.lines[bi]
//                var ae = a.lines[ai + 1]
//                var be = b.lines[bi + 1]
//
//                ae = RawCharUtil.trimTrailingWhitespace(a.rawContent, `as`, ae)
//                be = RawCharUtil.trimTrailingWhitespace(b.rawContent, bs, be)
//
//                while (`as` < ae && bs < be) {
//                    var ac = a.rawContent[`as`]
//                    var bc = b.rawContent[bs]
//
//                    while (`as` < ae - 1 && RawCharUtil.isWhitespace(ac)) {
//                        `as`++
//                        ac = a.rawContent[`as`]
//                    }
//
//                    while (bs < be - 1 && RawCharUtil.isWhitespace(bc)) {
//                        bs++
//                        bc = b.rawContent[bs]
//                    }
//
//                    if (ac != bc) return false
//
//                    `as`++
//                    bs++
//                }
//
//                return `as` == ae && bs == be
//            }
//
//            override fun hashRegion(raw: ByteArray, ptr: Int, end: Int): Int {
//                var ptr = ptr
//                var hash = 5381
//                while (ptr < end) {
//                    val c = raw[ptr]
//                    if (!RawCharUtil.isWhitespace(c)) hash = ((hash shl 5) + hash) + (c.toInt() and 0xff)
//                    ptr++
//                }
//                return hash
//            }
//        }
//
//        /**
//         * Ignore leading whitespace.
//         */
////		val WS_IGNORE_LEADING: RawTextComparator = object : RawTextComparator() {
//            override fun equals(a: RawText, ai: Int, b: RawText, bi: Int): Boolean {
//                var ai = ai
//                var bi = bi
//                ai++
//                bi++
//
//                var `as` = a.lines[ai]
//                var bs = b.lines[bi]
//                val ae = a.lines[ai + 1]
//                val be = b.lines[bi + 1]
//
//                `as` = RawCharUtil.trimLeadingWhitespace(a.rawContent, `as`, ae)
//                bs = RawCharUtil.trimLeadingWhitespace(b.rawContent, bs, be)
//
//                if (ae - `as` != be - bs) return false
//
//                while (`as` < ae) {
//                    if (a.rawContent[`as`++] != b.rawContent[bs++]) return false
//                }
//                return true
//            }
//
//            override fun hashRegion(raw: ByteArray, ptr: Int, end: Int): Int {
//                var ptr = ptr
//                var hash = 5381
//                ptr = RawCharUtil.trimLeadingWhitespace(raw, ptr, end)
//                while (ptr < end) {
//                    hash = ((hash shl 5) + hash) + (raw[ptr].toInt() and 0xff)
//                    ptr++
//                }
//                return hash
//            }
//        }
//
//        /** Ignores trailing whitespace.  */
////		val WS_IGNORE_TRAILING: RawTextComparator = object : RawTextComparator() {
//            override fun equals(a: RawText, ai: Int, b: RawText, bi: Int): Boolean {
//                var ai = ai
//                var bi = bi
//                ai++
//                bi++
//
//                var `as` = a.lines[ai]
//                var bs = b.lines[bi]
//                var ae = a.lines[ai + 1]
//                var be = b.lines[bi + 1]
//
//                ae = RawCharUtil.trimTrailingWhitespace(a.rawContent, `as`, ae)
//                be = RawCharUtil.trimTrailingWhitespace(b.rawContent, bs, be)
//
//                if (ae - `as` != be - bs) return false
//
//                while (`as` < ae) {
//                    if (a.rawContent[`as`++] != b.rawContent[bs++]) return false
//                }
//                return true
//            }
//
//            override fun hashRegion(raw: ByteArray, ptr: Int, end: Int): Int {
//                var ptr = ptr
//                var end = end
//                var hash = 5381
//                end = RawCharUtil.trimTrailingWhitespace(raw, ptr, end)
//                while (ptr < end) {
//                    hash = ((hash shl 5) + hash) + (raw[ptr].toInt() and 0xff)
//                    ptr++
//                }
//                return hash
//            }
//        }
//
//        /** Ignores whitespace occurring between non-whitespace characters.  */
////		val WS_IGNORE_CHANGE: RawTextComparator = object : RawTextComparator() {
//            override fun equals(a: RawText, ai: Int, b: RawText, bi: Int): Boolean {
//                var ai = ai
//                var bi = bi
//                ai++
//                bi++
//
//                var `as` = a.lines[ai]
//                var bs = b.lines[bi]
//                var ae = a.lines[ai + 1]
//                var be = b.lines[bi + 1]
//
//                ae = RawCharUtil.trimTrailingWhitespace(a.rawContent, `as`, ae)
//                be = RawCharUtil.trimTrailingWhitespace(b.rawContent, bs, be)
//
//                while (`as` < ae && bs < be) {
//                    val ac = a.rawContent[`as`++]
//                    val bc = b.rawContent[bs++]
//
//                    if (RawCharUtil.isWhitespace(ac) && RawCharUtil.isWhitespace(bc)) {
//                        `as` = RawCharUtil.trimLeadingWhitespace(a.rawContent, `as`, ae)
//                        bs = RawCharUtil.trimLeadingWhitespace(b.rawContent, bs, be)
//                    } else if (ac != bc) {
//                        return false
//                    }
//                }
//                return `as` == ae && bs == be
//            }
//
//            override fun hashRegion(raw: ByteArray, ptr: Int, end: Int): Int {
//                var ptr = ptr
//                var end = end
//                var hash = 5381
//                end = RawCharUtil.trimTrailingWhitespace(raw, ptr, end)
//                while (ptr < end) {
//                    var c = raw[ptr++]
//                    if (RawCharUtil.isWhitespace(c)) {
//                        ptr = RawCharUtil.trimLeadingWhitespace(raw, ptr, end)
//                        c = ' '.code.toByte()
//                    }
//                    hash = ((hash shl 5) + hash) + (c.toInt() and 0xff)
//                }
//                return hash
//            }
//        }

        private fun findForwardLine(lines: IntList, idx: Int, ptr: Int): Int {
            var idx = idx
            val end = lines.size() - 2
            while (idx < end && lines[idx + 2] < ptr) idx++
            return idx
        }

        private fun findReverseLine(lines: IntList, idx: Int, ptr: Int): Int {
            var idx = idx
            while (0 < idx && ptr <= lines[idx]) idx--
            return idx
        }
    }
}
