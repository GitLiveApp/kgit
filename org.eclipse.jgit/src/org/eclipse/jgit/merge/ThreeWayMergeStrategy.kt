/*
 * Copyright (C) 2009, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.merge

import org.eclipse.jgit.lib.Repository

/**
 * A merge strategy to merge 2 trees, using a common base ancestor tree.
 */
abstract class ThreeWayMergeStrategy : MergeStrategy() {
    abstract override fun newMerger(db: Repository): ThreeWayMerger

    abstract override fun newMerger(db: Repository, inCore: Boolean): ThreeWayMerger
}
