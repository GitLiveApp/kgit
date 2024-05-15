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
 * This exception will be thrown when a translation bundle loading
 * fails.
 */
class TranslationBundleLoadingException
/**
 * Construct a
 * [org.eclipse.jgit.errors.TranslationBundleLoadingException] for the
 * specified bundle class and locale.
 *
 * @param bundleClass
 * the bundle class for which the loading failed
 * @param locale
 * the locale for which the loading failed
 * @param cause
 * the original exception thrown from the
 * [java.util.ResourceBundle.getBundle]
 * method.
 */
    (bundleClass: Class<*>, locale: Locale, cause: Exception?) : TranslationBundleException(
    "Loading of translation bundle failed for [" //$NON-NLS-1$
        + bundleClass.name + ", " + locale.toString() + "]",  //$NON-NLS-1$ //$NON-NLS-2$
    bundleClass, locale, cause
) {
    companion object {
        private const val serialVersionUID = 1L
    }
}
