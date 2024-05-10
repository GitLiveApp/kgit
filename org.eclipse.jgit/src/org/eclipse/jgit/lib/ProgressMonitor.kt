/*
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.lib

/**
 * A progress reporting interface.
 */
interface ProgressMonitor {
    /**
     * Advise the monitor of the total number of subtasks.
     *
     *
     * This should be invoked at most once per progress monitor interface.
     *
     * @param totalTasks
     * the total number of tasks the caller will need to complete
     * their processing.
     */
    fun start(totalTasks: Int)

    /**
     * Begin processing a single task.
     *
     * @param title
     * title to describe the task. Callers should publish these as
     * stable string constants that implementations could match
     * against for translation support.
     * @param totalWork
     * total number of work units the application will perform;
     * [.UNKNOWN] if it cannot be predicted in advance.
     */
    fun beginTask(title: String?, totalWork: Int)

    /**
     * Denote that some work units have been completed.
     *
     *
     * This is an incremental update; if invoked once per work unit the correct
     * value for our argument is `1`, to indicate a single unit of
     * work has been finished by the caller.
     *
     * @param completed
     * the number of work units completed since the last call.
     */
    fun update(completed: Int)

    /**
     * Finish the current task, so the next can begin.
     */
    fun endTask()

    /**
     * Check for user task cancellation.
     *
     * @return true if the user asked the process to stop working.
     */
	val isCancelled: Boolean

    /**
     * Set whether the monitor should show elapsed time per task
     *
     * @param enabled
     * whether to show elapsed time per task
     * @since 6.5
     */
    fun showDuration(enabled: Boolean)

    companion object {
        /** Constant indicating the total work units cannot be predicted.  */
        const val UNKNOWN: Int = 0
    }
}
