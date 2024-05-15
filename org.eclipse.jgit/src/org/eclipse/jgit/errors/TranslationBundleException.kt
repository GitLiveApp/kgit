/*
 * Copyright (C) 2010, Sasa Zivkov <sasa.zivkov@sap.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.errors

import java.util.*

/**
 * Common base class for all translation bundle related exceptions.
 */
abstract class TranslationBundleException
/**
 * Construct an instance of
 * [org.eclipse.jgit.errors.TranslationBundleException]
 *
 * @param message
 * exception message
 * @param bundleClass
 * bundle class for which the exception occurred
 * @param locale
 * locale for which the exception occurred
 * @param cause
 * original exception that caused this exception. Usually thrown
 * from the [java.util.ResourceBundle] class.
 */ protected constructor(
    message: String?,
    /**
     * Get bundle class
     *
     * @return bundle class for which the exception occurred
     */
    val bundleClass: Class<*>,
    /**
     * Get locale for which the exception occurred
     *
     * @return locale for which the exception occurred
     */
    val locale: Locale, cause: Exception?
) : RuntimeException(message, cause) {
    companion object {
        private const val serialVersionUID = 1L
    }
}
