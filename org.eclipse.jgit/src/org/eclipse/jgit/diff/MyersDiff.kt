/*
 * Copyright (C) 2008-2009, Johannes E. Schindelin <johannes.schindelin@gmx.de>
 * Copyright (C) 2009, Johannes Schindelin <johannes.schindelin@gmx.de> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.diff

import org.eclipse.jgit.errors.DiffInterruptedException
import org.eclipse.jgit.internal.JGitText
import org.eclipse.jgit.util.IntList
import org.eclipse.jgit.util.LongList
import java.io.File
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import java.text.MessageFormat

/**
 * Diff algorithm, based on "An O(ND) Difference Algorithm and its Variations",
 * by Eugene Myers.
 *
 *
 * The basic idea is to put the line numbers of text A as columns ("x") and the
 * lines of text B as rows ("y"). Now you try to find the shortest "edit path"
 * from the upper left corner to the lower right corner, where you can always go
 * horizontally or vertically, but diagonally from (x,y) to (x+1,y+1) only if
 * line x in text A is identical to line y in text B.
 *
 *
 * Myers' fundamental concept is the "furthest reaching D-path on diagonal k": a
 * D-path is an edit path starting at the upper left corner and containing
 * exactly D non-diagonal elements ("differences"). The furthest reaching D-path
 * on diagonal k is the one that contains the most (diagonal) elements which
 * ends on diagonal k (where k = y - x).
 *
 *
 * Example:
 *
 * <pre>
 * H E L L O   W O R L D
 * ____
 * L     \___
 * O         \___
 * W             \________
</pre> *
 *
 *
 * Since every D-path has exactly D horizontal or vertical elements, it can only
 * end on the diagonals -D, -D+2, ..., D-2, D.
 *
 *
 * Since every furthest reaching D-path contains at least one furthest reaching
 * (D-1)-path (except for D=0), we can construct them recursively.
 *
 *
 * Since we are really interested in the shortest edit path, we can start
 * looking for a 0-path, then a 1-path, and so on, until we find a path that
 * ends in the lower right corner.
 *
 *
 * To save space, we do not need to store all paths (which has quadratic space
 * requirements), but generate the D-paths simultaneously from both sides. When
 * the ends meet, we will have found "the middle" of the path. From the end
 * points of that diagonal part, we can generate the rest recursively.
 *
 *
 * This only requires linear space.
 *
 *
 * The overall (runtime) complexity is:
 *
 * <pre>
 * O(N * D^2 + 2 * N/2 * (D/2)^2 + 4 * N/4 * (D/4)^2 + ...)
 * = O(N * D^2 * 5 / 4) = O(N * D^2),
</pre> *
 *
 *
 * (With each step, we have to find the middle parts of twice as many regions as
 * before, but the regions (as well as the D) are halved.)
 *
 *
 * So the overall runtime complexity stays the same with linear space, albeit
 * with a larger constant factor.
 *
 * @param <S>
 * type of sequence.
</S> */
class MyersDiff<S : Sequence> private constructor(
    /**
     * The list of edits found during the last call to
     * [.calculateEdits]
     */
    protected var edits: EditList?,
    /** Comparison function for sequences.  */
    protected var cmp: HashedSequenceComparator<S>?,
    /**
     * The first text to be compared. Referred to as "Text A" in the comments
     */
    protected var a: HashedSequence<S>?,
    /**
     * The second text to be compared. Referred to as "Text B" in the comments
     */
    protected var b: HashedSequence<S>?, region: Edit?
) {
    // TODO: use ThreadLocal for future multi-threaded operations
    var middle: MiddleEdit = MiddleEdit()

    init {
        calculateEdits(region)
    }

    /**
     * Entrypoint into the algorithm this class is all about. This method triggers that the
     * differences between A and B are calculated in form of a list of edits.
     * @param r portion of the sequences to examine.
     */
    private fun calculateEdits(r: Edit?) {
        middle.initialize(r!!.beginA, r.endA, r.beginB, r.endB)
        if (middle.beginA >= middle.endA &&
            middle.beginB >= middle.endB
        ) return

        calculateEdits(
            middle.beginA, middle.endA,
            middle.beginB, middle.endB
        )
    }

    /**
     * Calculates the differences between a given part of A against another
     * given part of B
     *
     * @param beginA
     * start of the part of A which should be compared
     * (0&lt;=beginA&lt;sizeof(A))
     * @param endA
     * end of the part of A which should be compared
     * (beginA&lt;=endA&lt;sizeof(A))
     * @param beginB
     * start of the part of B which should be compared
     * (0&lt;=beginB&lt;sizeof(B))
     * @param endB
     * end of the part of B which should be compared
     * (beginB&lt;=endB&lt;sizeof(B))
     */
    protected fun calculateEdits(
        beginA: Int, endA: Int,
        beginB: Int, endB: Int
    ) {
        val edit = middle.calculate(beginA, endA, beginB, endB)

        if (beginA < edit!!.beginA || beginB < edit.beginB) {
            val k = edit.beginB - edit.beginA
            val x = middle.backward.snake(k, edit.beginA)
            calculateEdits(beginA, x, beginB, k + x)
        }

        if (edit.type != Edit.Type.EMPTY) edits!!.add(edits!!.size, edit)

        // after middle
        if (endA > edit.endA || endB > edit.endB) {
            val k = edit.endB - edit.endA
            val x = middle.forward.snake(k, edit.endA)
            calculateEdits(x, endA, k + x, endB)
        }
    }

    /**
     * A class to help bisecting the sequences a and b to find minimal
     * edit paths.
     *
     * As the arrays are reused for space efficiency, you will need one
     * instance per thread.
     *
     * The entry function is the calculate() method.
     */
    inner class MiddleEdit {
        fun initialize(beginA: Int, endA: Int, beginB: Int, endB: Int) {
            this.beginA = beginA
            this.endA = endA
            this.beginB = beginB
            this.endB = endB

            // strip common parts on either end
            var k = beginB - beginA
            this.beginA = forward.snake(k, beginA)
            this.beginB = k + this.beginA

            k = endB - endA
            this.endA = backward.snake(k, endA)
            this.endB = k + this.endA
        }

        /*
		 * This function calculates the "middle" Edit of the shortest
		 * edit path between the given subsequences of a and b.
		 *
		 * Once a forward path and a backward path meet, we found the
		 * middle part.  From the last snake end point on both of them,
		 * we construct the Edit.
		 *
		 * It is assumed that there is at least one edit in the range.
		 */
        // TODO: measure speed impact when this is synchronized
        fun calculate(beginA: Int, endA: Int, beginB: Int, endB: Int): Edit? {
            if (beginA == endA || beginB == endB) return Edit(beginA, endA, beginB, endB)
            this.beginA = beginA
            this.endA = endA
            this.beginB = beginB
            this.endB = endB

            /*
			 * Following the conventions in Myers' paper, "k" is
			 * the difference between the index into "b" and the
			 * index into "a".
			 */
            val minK = beginB - endA
            val maxK = endB - beginA

            forward.initialize(beginB - beginA, beginA, minK, maxK)
            backward.initialize(endB - endA, endA, minK, maxK)

            var d = 1
            while (true) {
                if (forward.calculate(d) ||
                    backward.calculate(d)
                ) return edit
                d++
            }
        }

        /*
		 * For each d, we need to hold the d-paths for the diagonals
		 * k = -d, -d + 2, ..., d - 2, d.  These are stored in the
		 * forward (and backward) array.
		 *
		 * As we allow subsequences, too, this needs some refinement:
		 * the forward paths start on the diagonal forwardK =
		 * beginB - beginA, and backward paths start on the diagonal
		 * backwardK = endB - endA.
		 *
		 * So, we need to hold the forward d-paths for the diagonals
		 * k = forwardK - d, forwardK - d + 2, ..., forwardK + d and
		 * the analogue for the backward d-paths.  This means that
		 * we can turn (k, d) into the forward array index using this
		 * formula:
		 *
		 *	i = (d + k - forwardK) / 2
		 *
		 * There is a further complication: the edit paths should not
		 * leave the specified subsequences, so k is bounded by
		 * minK = beginB - endA and maxK = endB - beginA.  However,
		 * (k - forwardK) _must_ be odd whenever d is odd, and it
		 * _must_ be even when d is even.
		 *
		 * The values in the "forward" and "backward" arrays are
		 * positions ("x") in the sequence a, to get the corresponding
		 * positions ("y") in the sequence b, you have to calculate
		 * the appropriate k and then y:
		 *
		 *	k = forwardK - d + i * 2
		 *	y = k + x
		 *
		 * (substitute backwardK for forwardK if you want to get the
		 * y position for an entry in the "backward" array.
		 */
        var forward: EditPaths = ForwardEditPaths()
        var backward: EditPaths = BackwardEditPaths()

        /* Some variables which are shared between methods */
        var beginA: Int = 0
        var endA: Int = 0
        var beginB: Int = 0
        var endB: Int = 0
        protected var edit: Edit? = null

        abstract inner class EditPaths {
            private val x = IntList()
            private val snake = LongList()
            var beginK: Int = 0
            var endK: Int = 0
            var middleK: Int = 0
            var prevBeginK: Int = 0
            var prevEndK: Int = 0

            /* if we hit one end early, no need to look further */
            var minK: Int = 0
            var maxK: Int = 0 // TODO: better explanation

            fun getIndex(d: Int, k: Int): Int {
// TODO: remove
                if (((d + k - middleK) % 2) != 0) throw RuntimeException(
                    MessageFormat.format(
                        JGitText.get().unexpectedOddResult,
                        d,
                        k,
                        middleK
                    )
                )
                return (d + k - middleK) / 2
            }

            fun getX(d: Int, k: Int): Int {
// TODO: remove
                if (k < beginK || k > endK) throw RuntimeException(
                    MessageFormat.format(
                        JGitText.get().kNotInRange,
                        k,
                        beginK,
                        endK
                    )
                )
                return x[getIndex(d, k)]
            }

            fun getSnake(d: Int, k: Int): Long {
// TODO: remove
                if (k < beginK || k > endK) throw RuntimeException(
                    MessageFormat.format(
                        JGitText.get().kNotInRange,
                        k,
                        beginK,
                        endK
                    )
                )
                return snake[getIndex(d, k)]
            }

            private fun forceKIntoRange(k: Int): Int {
                /* if k is odd, so must be the result */
                if (k < minK) return minK + ((k xor minK) and 1)
                else if (k > maxK) return maxK - ((k xor maxK) and 1)
                return k
            }

            fun initialize(k: Int, x: Int, minK: Int, maxK: Int) {
                this.minK = minK
                this.maxK = maxK
                middleK = k
                endK = middleK
                beginK = endK
                this.x.clear()
                this.x.add(x)
                snake.clear()
                snake.add(newSnake(k, x))
            }

            abstract fun snake(k: Int, x: Int): Int
            abstract fun getLeft(x: Int): Int
            abstract fun getRight(x: Int): Int
            abstract fun isBetter(left: Int, right: Int): Boolean
            abstract fun adjustMinMaxK(k: Int, x: Int)
            abstract fun meets(d: Int, k: Int, x: Int, snake: Long): Boolean

            fun newSnake(k: Int, x: Int): Long {
                val y = k.toLong() + x
                val ret = (x.toLong()) shl 32
                return ret or y
            }

            fun snake2x(snake: Long): Int {
                return (snake ushr 32).toInt()
            }

            fun snake2y(snake: Long): Int {
                return snake.toInt()
            }

            fun makeEdit(snake1: Long, snake2: Long): Boolean {
                var x1 = snake2x(snake1)
                val x2 = snake2x(snake2)
                var y1 = snake2y(snake1)
                val y2 = snake2y(snake2)
                /*
				 * Check for incompatible partial edit paths:
				 * when there are ambiguities, we might have
				 * hit incompatible (i.e. non-overlapping)
				 * forward/backward paths.
				 *
				 * In that case, just pretend that we have
				 * an empty edit at the end of one snake; this
				 * will force a decision which path to take
				 * in the next recursion step.
				 */
                if (x1 > x2 || y1 > y2) {
                    x1 = x2
                    y1 = y2
                }
                edit = Edit(x1, x2, y1, y2)
                return true
            }

            fun calculate(d: Int): Boolean {
                prevBeginK = beginK
                prevEndK = endK
                beginK = forceKIntoRange(middleK - d)
                endK = forceKIntoRange(middleK + d)
                // TODO: handle i more efficiently
                // TODO: walk snake(k, getX(d, k)) only once per (d, k)
                // TODO: move end points out of the loop to avoid conditionals inside the loop
                // go backwards so that we can avoid temp vars
                var k = endK
                while (k >= beginK) {
                    if (Thread.interrupted()) {
                        throw DiffInterruptedException()
                    }
                    var left = -1
                    var right = -1
                    var leftSnake = -1L
                    var rightSnake = -1L
                    // TODO: refactor into its own function
                    if (k > prevBeginK) {
                        val i = getIndex(d - 1, k - 1)
                        left = x[i]
                        val end = snake(k - 1, left)
                        leftSnake = if (left != end) newSnake(k - 1, end) else snake[i]
                        if (meets(d, k - 1, end, leftSnake)) return true
                        left = getLeft(end)
                    }
                    if (k < prevEndK) {
                        val i = getIndex(d - 1, k + 1)
                        right = x[i]
                        val end = snake(k + 1, right)
                        rightSnake = if (right != end) newSnake(k + 1, end) else snake[i]
                        if (meets(d, k + 1, end, rightSnake)) return true
                        right = getRight(end)
                    }
                    var newX: Int
                    var newSnake: Long
                    if (k >= prevEndK ||
                        (k > prevBeginK &&
                            isBetter(left, right))
                    ) {
                        newX = left
                        newSnake = leftSnake
                    } else {
                        newX = right
                        newSnake = rightSnake
                    }
                    if (meets(d, k, newX, newSnake)) return true
                    adjustMinMaxK(k, newX)
                    val i = getIndex(d, k)
                    x[i] = newX
                    snake[i] = newSnake
                    k -= 2
                }
                return false
            }
        }

        internal inner class ForwardEditPaths : EditPaths() {
            override fun snake(k: Int, x: Int): Int {
                var x = x
                while (x < endA && k + x < endB) {
                    if (!cmp!!.equals(a!!, x, b!!, k + x)) break
                    x++
                }
                return x
            }

            override fun getLeft(x: Int): Int {
                return x
            }

            override fun getRight(x: Int): Int {
                return x + 1
            }

            override fun isBetter(left: Int, right: Int): Boolean {
                return left > right
            }

            override fun adjustMinMaxK(k: Int, x: Int) {
                if (x >= endA || k + x >= endB) {
                    if (k > backward.middleK) maxK = k
                    else minK = k
                }
            }

            override fun meets(d: Int, k: Int, x: Int, snake: Long): Boolean {
                if (k < backward.beginK || k > backward.endK) return false
                // TODO: move out of loop
                if (((d - 1 + k - backward.middleK) % 2) != 0) return false
                if (x < backward.getX(d - 1, k)) return false
                makeEdit(snake, backward.getSnake(d - 1, k))
                return true
            }
        }

        internal inner class BackwardEditPaths : EditPaths() {
            override fun snake(k: Int, x: Int): Int {
                var x = x
                while (x > beginA && k + x > beginB) {
                    if (!cmp!!.equals(a!!, x - 1, b!!, k + x - 1)) break
                    x--
                }
                return x
            }

            override fun getLeft(x: Int): Int {
                return x - 1
            }

            override fun getRight(x: Int): Int {
                return x
            }

            override fun isBetter(left: Int, right: Int): Boolean {
                return left < right
            }

            override fun adjustMinMaxK(k: Int, x: Int) {
                if (x <= beginA || k + x <= beginB) {
                    if (k > forward.middleK) maxK = k
                    else minK = k
                }
            }

            override fun meets(d: Int, k: Int, x: Int, snake: Long): Boolean {
                if (k < forward.beginK || k > forward.endK) return false
                // TODO: move out of loop
                if (((d + k - forward.middleK) % 2) != 0) return false
                if (x > forward.getX(d, k)) return false
                makeEdit(forward.getSnake(d, k), snake)
                return true
            }
        }
    }

    companion object {
        /** Singleton instance of MyersDiff.  */
		@JvmField
		val INSTANCE: DiffAlgorithm = object : LowLevelDiffAlgorithm() {
            @Suppress("unused")
            override fun <S : Sequence> diffNonCommon(
                edits: EditList,
                cmp: HashedSequenceComparator<S>, a: HashedSequence<S>,
                b: HashedSequence<S>, region: Edit
            ) {
                MyersDiff(edits, cmp, a, b, region)
            }
        }

        /**
         * Main method
         *
         * @param args
         * two filenames specifying the contents to be diffed
         */
        @JvmStatic
        fun main(args: Array<String>) {
            if (args.size != 2) {
                err().println(JGitText.get().need2Arguments)
                System.exit(1)
            }
            try {
                val a = RawText(File(args[0]))
                val b = RawText(File(args[1]))
                val r = INSTANCE.diff(RawTextComparator.DEFAULT, a, b)
                println(r.toString())
            } catch (e: Exception) {
                val err = err()
                err.println(e.message)
                e.printStackTrace(err)
            }
        }

        private fun err(): PrintWriter {
            return PrintWriter(OutputStreamWriter(System.err, StandardCharsets.UTF_8))
        }
    }
}
