/*
 * Copyright (C) 2009, Christian Halstrick <christian.halstrick@sap.com>
 * Copyright (C) 2014, André de Oliveira <andre.oliveira@liferay.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.merge

import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.merge.MergeChunk.ConflictState
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.Charset

internal class MergeFormatterPass @JvmOverloads constructor(
    out: OutputStream,
    private val res: MergeResult<RawText>,
    private val seqName: List<String>, private val charset: Charset, // diff3-style requested
    private val writeBase: Boolean = false
) {
    private val out = EolAwareOutputStream(out)

    private val threeWayMerge = res.sequences.size == 3

    private var lastConflictingName: String? = null // is set to non-null whenever we are in

    /**
     * @param out
     * the [java.io.OutputStream] where to write the textual
     * presentation
     * @param res
     * the merge result which should be presented
     * @param seqName
     * When a conflict is reported each conflicting range will get a
     * name. This name is following the "&lt;&lt;&lt;&lt;&lt;&lt;&lt;
     * ", "|||||||" or "&gt;&gt;&gt;&gt;&gt;&gt;&gt; " conflict
     * markers. The names for the sequences are given in this list
     * @param charset
     * the character set used when writing conflict metadata
     * @param writeBase
     * base's contribution should be written in conflicts
     */
    // a conflict

    @Throws(IOException::class)
    fun formatMerge() {
        var missingNewlineAtEnd = false
        for (chunk in res) {
            if (!isBase(chunk) || writeBase) {
                val seq = res.sequences[chunk.sequenceIndex]
                writeConflictMetadata(chunk)
                // the lines with conflict-metadata are written. Now write the
                // chunk
                for (i in chunk.begin until chunk.end) writeLine(seq, i)
                missingNewlineAtEnd = seq.isMissingNewlineAtEnd
            }
        }
        // one possible leftover: if the merge result ended with a conflict we
        // have to close the last conflict here
        if (lastConflictingName != null) writeConflictEnd()
        if (!missingNewlineAtEnd) out.beginln()
    }

    @Throws(IOException::class)
    private fun writeConflictMetadata(chunk: MergeChunk) {
        if (lastConflictingName != null && !isTheirs(chunk) && !isBase(chunk)) {
            // found the end of a conflict
            writeConflictEnd()
        }
        if (isOurs(chunk)) {
            // found the start of a conflict
            writeConflictStart(chunk)
        } else if (isTheirs(chunk)) {
            // found the theirs conflicting chunk
            writeConflictChange(chunk)
        } else if (isBase(chunk)) {
            // found the base conflicting chunk
            writeConflictBase(chunk)
        }
    }

    @Throws(IOException::class)
    private fun writeConflictEnd() {
        writeln(">>>>>>> $lastConflictingName") //$NON-NLS-1$
        lastConflictingName = null
    }

    @Throws(IOException::class)
    private fun writeConflictStart(chunk: MergeChunk) {
        lastConflictingName = seqName[chunk.sequenceIndex]
        writeln("<<<<<<< $lastConflictingName") //$NON-NLS-1$
    }

    @Throws(IOException::class)
    private fun writeConflictChange(chunk: MergeChunk) {
        /*
		 * In case of a non-three-way merge I'll add the name of the conflicting
		 * chunk behind the equal signs. I also append the name of the last
		 * conflicting chunk after the ending greater-than signs. If somebody
		 * knows a better notation to present non-three-way merges - feel free
		 * to correct here.
		 */
        lastConflictingName = seqName[chunk.sequenceIndex]
        writeln(
            if (threeWayMerge) "=======" else ("======= " //$NON-NLS-1$ //$NON-NLS-2$
                + lastConflictingName)
        )
    }

    @Throws(IOException::class)
    private fun writeConflictBase(chunk: MergeChunk) {
        lastConflictingName = seqName[chunk.sequenceIndex]
        writeln("||||||| $lastConflictingName") //$NON-NLS-1$
    }

    @Throws(IOException::class)
    private fun writeln(s: String) {
        out.beginln()
        out.write((s + "\n").toByteArray(charset)) //$NON-NLS-1$
    }

    @Throws(IOException::class)
    private fun writeLine(seq: RawText, i: Int) {
        out.beginln()
        seq.writeLine(out, i)
        // still BOL? It was a blank line. But writeLine won't lf, so we do.
        if (out.isBeginln) out.write('\n'.code)
    }

    private fun isBase(chunk: MergeChunk): Boolean {
        return chunk.conflictState == ConflictState.BASE_CONFLICTING_RANGE
    }

    private fun isOurs(chunk: MergeChunk): Boolean {
        return chunk.conflictState == ConflictState.FIRST_CONFLICTING_RANGE
    }

    private fun isTheirs(chunk: MergeChunk): Boolean {
        return chunk.conflictState == ConflictState.NEXT_CONFLICTING_RANGE
    }
}
