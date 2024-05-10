/*
 * Copyright (C) 2012, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.lib

/**
 * Base object type accessed during bitmap expansion.
 *
 * @since 3.0
 */
abstract class BitmapObject {
    /**
     * Get Git object type. See [org.eclipse.jgit.lib.Constants].
     *
     * @return object type
     */
    abstract val type: Int

    /**
     * Get the name of this object.
     *
     * @return unique hash of this object.
     */
    abstract val objectId: ObjectId?
}
