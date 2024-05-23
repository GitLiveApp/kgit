/*
 * Copyright (C) 2008-2009, Johannes E. Schindelin <johannes.schindelin@gmx.de> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.diff

/**
 * A modified region detected between two versions of roughly the same content.
 *
 *
 * An edit covers the modified region only. It does not cover a common region.
 *
 *
 * Regions should be specified using 0 based notation, so add 1 to the start and
 * end marks for line numbers in a file.
 *
 *
 * An edit where `beginA == endA && beginB < endB` is an insert edit, that
 * is sequence B inserted the elements in region `[beginB, endB)` at
 * `beginA`.
 *
 *
 * An edit where `beginA < endA && beginB == endB` is a delete edit, that
 * is sequence B has removed the elements between `[beginA, endA)`.
 *
 *
 * An edit where `beginA < endA && beginB < endB` is a replace edit, that
 * is sequence B has replaced the range of elements between
 * `[beginA, endA)` with those found in `[beginB, endB)`.
 */
class Edit
/**
 * Create a new edit.
 *
 * @param as
 * beginA: start of region in sequence A; 0 based.
 * @param ae
 * endA: end of region in sequence A; must be &gt;= as.
 * @param bs
 * beginB: start of region in sequence B; 0 based.
 * @param be
 * endB: end of region in sequence B; must be &gt; = bs.
 */(
	/**
     * Get start point in sequence A
     *
     * @return start point in sequence A
     */
	var beginA: Int,
	/**
     * Get end point in sequence A
     *
     * @return end point in sequence A
     */
	var endA: Int,
	/**
     * Get start point in sequence B
     *
     * @return start point in sequence B
     */
	var beginB: Int,
	/**
     * Get end point in sequence B
     *
     * @return end point in sequence B
     */
	var endB: Int
) {
    /** Type of edit  */
    enum class Type {
        /** Sequence B has inserted the region.  */
        INSERT,

        /** Sequence B has removed the region.  */
        DELETE,

        /** Sequence B has replaced the region with different content.  */
        REPLACE,

        /** Sequence A and B have zero length, describing nothing.  */
        EMPTY
    }

    /**
     * Create a new empty edit.
     *
     * @param as
     * beginA: start and end of region in sequence A; 0 based.
     * @param bs
     * beginB: start and end of region in sequence B; 0 based.
     */
    constructor(`as`: Int, bs: Int) : this(`as`, `as`, bs, bs)

    val type: Type
        /**
         * Get type
         *
         * @return the type of this region
         */
        get() {
            if (beginA < endA) {
                if (beginB < endB) {
                    return Type.REPLACE
                }
                return Type.DELETE
            }
            if (beginB < endB) {
                return Type.INSERT
            }
            // beginB == endB)
            return Type.EMPTY
        }

    val isEmpty: Boolean
        /**
         * Whether edit is empty
         *
         * @return `true` if the edit is empty (lengths of both a and b is
         * zero)
         */
        get() = beginA == endA && beginB == endB

    val lengthA: Int
        /**
         * Get length of the region in A
         *
         * @return length of the region in A
         */
        get() = endA - beginA

    val lengthB: Int
        /**
         * Get length of the region in B
         *
         * @return return length of the region in B
         */
        get() = endB - beginB

    /**
     * Move the edit region by the specified amount.
     *
     * @param amount
     * the region is shifted by this amount, and can be positive or
     * negative.
     * @since 4.8
     */
    fun shift(amount: Int) {
        beginA += amount
        endA += amount
        beginB += amount
        endB += amount
    }

    /**
     * Construct a new edit representing the region before cut.
     *
     * @param cut
     * the cut point. The beginning A and B points are used as the
     * end points of the returned edit.
     * @return an edit representing the slice of `this` edit that occurs
     * before `cut` starts.
     */
    fun before(cut: Edit): Edit {
        return Edit(beginA, cut.beginA, beginB, cut.beginB)
    }

    /**
     * Construct a new edit representing the region after cut.
     *
     * @param cut
     * the cut point. The ending A and B points are used as the
     * starting points of the returned edit.
     * @return an edit representing the slice of `this` edit that occurs
     * after `cut` ends.
     */
    fun after(cut: Edit): Edit {
        return Edit(cut.endA, endA, cut.endB, endB)
    }

    /**
     * Increase [.getEndA] by 1.
     */
    fun extendA() {
        endA++
    }

    /**
     * Increase [.getEndB] by 1.
     */
    fun extendB() {
        endB++
    }

    /**
     * Swap A and B, so the edit goes the other direction.
     */
    fun swap() {
        val sBegin = beginA
        val sEnd = endA

        beginA = beginB
        endA = endB

        beginB = sBegin
        endB = sEnd
    }

    override fun hashCode(): Int {
        return beginA xor endA
    }

    override fun equals(other: Any?): Boolean {
        if (other is Edit) {
            val e = other
            return this.beginA == e.beginA && (this.endA == e.endA
                ) && (this.beginB == e.beginB) && (this.endB == e.endB)
        }
        return false
    }

    override fun toString(): String {
        val t = type
        return "$t($beginA-$endA,$beginB-$endB)"
    }
}
