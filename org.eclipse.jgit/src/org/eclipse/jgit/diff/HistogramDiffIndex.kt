/*
 * Copyright (C) 2010, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.diff

//import org.eclipse.jgit.internal.JGitText
import kotlin.math.max
import kotlin.math.min

/**
 * Support [HistogramDiff] by computing occurrence counts of elements.
 *
 *
 * Each element in the range being considered is put into a hash table, tracking
 * the number of times that distinct element appears in the sequence. Once all
 * elements have been inserted from sequence A, each element of sequence B is
 * probed in the hash table and the longest common subsequence with the lowest
 * occurrence count in A is used as the result.
 *
 * @param <S>
 * type of the base sequence.
</S> */
internal class HistogramDiffIndex<S : Sequence>(
    private val maxChainLength: Int, private val cmp: HashedSequenceComparator<S>,
    private val a: HashedSequence<S>, private val b: HashedSequence<S>, private val region: Edit
) {
    /** Keyed by [.hash] for [.recs] index.  */
    private val table: IntArray

    /** Number of low bits to discard from a key to index [.table].  */
    private val keyShift: Int

    /**
     * Describes a unique element in sequence A.
     *
     * The records in this table are actually 3-tuples of:
     *
     *  * index of next record in this table that has same hash code
     *  * index of first element in this occurrence chain
     *  * occurrence count for this element (length of locs list)
     *
     *
     * The occurrence count is capped at [.MAX_CNT], as the field is only
     * a few bits wide. Elements that occur more frequently will have their
     * count capped.
     */
    private var recs: LongArray

    /** Number of elements in [.recs]; also is the unique element count.  */
    private var recCnt = 0

    /**
     * For `ptr`, `next[ptr - ptrShift]` has subsequent index.
     *
     * For the sequence element `ptr`, the value stored at location
     * `next[ptr - ptrShift]` is the next occurrence of the exact same
     * element in the sequence.
     *
     * Chains always run from the lowest index to the largest index. Therefore
     * the array will store `next[1] = 2`, but never `next[2] = 1`.
     * This allows a chain to terminate with `0`, as `0` would never
     * be a valid next element.
     *
     * The array is sized to be `region.getLengthA()` and element indexes
     * are converted to array indexes by subtracting [.ptrShift], which is
     * just a cached version of `region.beginA`.
     */
    private val next: IntArray

    /**
     * For element `ptr` in A, index of the record in [.recs] array.
     *
     * The record at `recs[recIdx[ptr - ptrShift]]` is the record
     * describing all occurrences of the element appearing in sequence A at
     * position `ptr`. The record is needed to get the occurrence count of
     * the element, or to locate all other occurrences of that element within
     * sequence A. This index provides constant-time access to the record, and
     * avoids needing to scan the hash chain.
     */
    private val recIdx: IntArray

    /** Value to subtract from element indexes to key [.next] array.  */
    private val ptrShift: Int

    private var lcs: Edit? = null

    private var cnt = 0

    private var hasCommon = false

    init {
//        require(region.endA < MAX_PTR) { JGitText.get().sequenceTooLargeForDiffAlgorithm }

        val sz = region.lengthA
        val tableBits = tableBits(sz)
        table = IntArray(1 shl tableBits)
        keyShift = 32 - tableBits
        ptrShift = region.beginA

        recs = LongArray(max(4.0, (sz ushr 3).toDouble()).toInt())
        next = IntArray(sz)
        recIdx = IntArray(sz)
    }

    fun findLongestCommonSequence(): Edit? {
        if (!scanA()) return null

        lcs = Edit(0, 0)
        cnt = maxChainLength + 1

        var bPtr = region.beginB
        while (bPtr < region.endB) {
            bPtr = tryLongestCommonSequence(bPtr)
        }

        return if (hasCommon && maxChainLength < cnt) null else lcs
    }

    private fun scanA(): Boolean {
        // Scan the elements backwards, inserting them into the hash table
        // as we go. Going in reverse places the earliest occurrence of any
        // element at the start of the chain, so we consider earlier matches
        // before later matches.
        //
        var ptr = region.endA - 1
        SCAN@ while (region.beginA <= ptr) {
            val tIdx = hash(a, ptr)

            var chainLen = 0
                var rIdx = table[tIdx]
                while (rIdx != 0) {
                    val rec = recs[rIdx]
                    if (cmp.equals(a, recPtr(rec), a, ptr)) {
                        // ptr is identical to another element. Insert it onto
                        // the front of the existing element chain.
                        //
                        var newCnt = recCnt(rec) + 1
                        if (MAX_CNT < newCnt) newCnt = MAX_CNT
                        recs[rIdx] = recCreate(recNext(rec), ptr, newCnt)
                        next[ptr - ptrShift] = recPtr(rec)
                        recIdx[ptr - ptrShift] = rIdx
                        ptr--
                        continue@SCAN
                    }

                    rIdx = recNext(rec)
                    chainLen++
                }

            if (chainLen == maxChainLength) return false

            // This is the first time we have ever seen this particular
            // element in the sequence. Construct a new chain for it.
            //
            rIdx = ++recCnt
            if (rIdx == recs.size) {
                val sz = min((recs.size shl 1).toDouble(), (1 + region.lengthA).toDouble()).toInt()
                val n = LongArray(sz)
                System.arraycopy(recs, 0, n, 0, recs.size)
                recs = n
            }

            recs[rIdx] = recCreate(table[tIdx], ptr, 1)
            recIdx[ptr - ptrShift] = rIdx
            table[tIdx] = rIdx
            ptr--
        }
        return true
    }

    private fun tryLongestCommonSequence(bPtr: Int): Int {
        var bNext = bPtr + 1
        var rIdx = table[hash(b, bPtr)]
        var rec: Long
        while (rIdx != 0) {
            rec = recs[rIdx]

            // If there are more occurrences in A, don't use this chain.
            if (recCnt(rec) > cnt) {
                if (!hasCommon) hasCommon = cmp.equals(a, recPtr(rec), b, bPtr)
                rIdx = recNext(rec)
                continue
            }

            var `as` = recPtr(rec)
            if (!cmp.equals(a, `as`, b, bPtr)) {
                rIdx = recNext(rec)
                continue
            }

            hasCommon = true
            TRY_LOCATIONS@ while (true) {
                var np = next[`as` - ptrShift]
                var bs = bPtr
                var ae = `as` + 1
                var be = bs + 1
                var rc = recCnt(rec)

                while (region.beginA < `as` && region.beginB < bs && cmp.equals(a, `as` - 1, b, bs - 1)) {
                    `as`--
                    bs--
                    if (1 < rc) rc = min(
                        rc.toDouble(),
                        recCnt(recs[recIdx[`as` - ptrShift]]).toDouble()
                    ).toInt()
                }
                while (ae < region.endA && be < region.endB && cmp.equals(a, ae, b, be)) {
                    if (1 < rc) rc = min(rc.toDouble(), recCnt(recs[recIdx[ae - ptrShift]]).toDouble())
                        .toInt()
                    ae++
                    be++
                }

                if (bNext < be) bNext = be
                if (lcs!!.lengthA < ae - `as` || rc < cnt) {
                    // If this region is the longest, or there are less
                    // occurrences of it in A, its now our LCS.
                    //
                    lcs!!.beginA = `as`
                    lcs!!.beginB = bs
                    lcs!!.endA = ae
                    lcs!!.endB = be
                    cnt = rc
                }

                // Because we added elements in reverse order index 0
                // cannot possibly be the next position. Its the first
                // element of the sequence and thus would have been the
                // value of as at the start of the TRY_LOCATIONS loop.
                //
                if (np == 0) break@TRY_LOCATIONS

                while (np < ae) {
                    // The next location to consider was actually within
                    // the LCS we examined above. Don't reconsider it.
                    //
                    np = next[np - ptrShift]
                    if (np == 0) break@TRY_LOCATIONS
                }

                `as` = np
            }
            rIdx = recNext(rec)
        }
        return bNext
    }

    private fun hash(s: HashedSequence<S>, idx: Int): Int {
        return (cmp.hash(s, idx) * -0x61c8ffff /* mix bits */) ushr keyShift
    }

    companion object {
        private const val REC_NEXT_SHIFT = 28 + 8

        private const val REC_PTR_SHIFT = 8

        private const val REC_PTR_MASK = (1 shl 28) - 1

        private const val REC_CNT_MASK = (1 shl 8) - 1

        private const val MAX_PTR = REC_PTR_MASK

        private const val MAX_CNT = (1 shl 8) - 1

        private fun recCreate(next: Int, ptr: Int, cnt: Int): Long {
            return ((next.toLong() shl REC_NEXT_SHIFT) //
                or (ptr.toLong() shl REC_PTR_SHIFT) //
                or cnt.toLong())
        }

        private fun recNext(rec: Long): Int {
            return (rec ushr REC_NEXT_SHIFT).toInt()
        }

        private fun recPtr(rec: Long): Int {
            return ((rec ushr REC_PTR_SHIFT).toInt()) and REC_PTR_MASK
        }

        private fun recCnt(rec: Long): Int {
            return (rec.toInt()) and REC_CNT_MASK
        }

        private fun tableBits(sz: Int): Int {
            var bits = 31 - Integer.numberOfLeadingZeros(sz)
            if (bits == 0) bits = 1
            if (1 shl bits < sz) bits++
            return bits
        }
    }
}
