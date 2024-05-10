/*
 * Copyright (C) 2015, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.lib

/**
 * Simple set of ObjectIds.
 *
 *
 * Usually backed by a read-only data structure such as
 * [org.eclipse.jgit.internal.storage.file.PackIndex]. Mutable types like
 * [org.eclipse.jgit.lib.ObjectIdOwnerMap] also implement the interface by
 * checking keys.
 *
 * @since 4.2
 */
interface ObjectIdSet {
    /**
     * Returns true if the objectId is contained within the collection.
     *
     * @param objectId
     * the objectId to find
     * @return whether the collection contains the objectId.
     */
    fun contains(objectId: AnyObjectId?): Boolean
}
