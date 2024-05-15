/*
 * Copyright (C) 2008, Florian Köberle <florianskarten@web.de>
 * Copyright (C) 2009, Vasyl' Vavrychuk <vvavrychuk@gmail.com>
 * Copyright (C) 2009, Yann Simon <yann.simon.fr@gmail.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.errors

import org.eclipse.jgit.internal.JGitText
import java.text.MessageFormat

/**
 * Thrown when a pattern contains a character group which is open to the right
 * side or a character class which is open to the right side.
 */
class NoClosingBracketException
/**
 * Constructor for NoClosingBracketException
 *
 * @param indexOfOpeningBracket
 * the position of the [ character which has no ] character.
 * @param openingBracket
 * the unclosed bracket.
 * @param closingBracket
 * the missing closing bracket.
 * @param pattern
 * the invalid pattern.
 */
    (
    indexOfOpeningBracket: Int,
    openingBracket: String, closingBracket: String,
    pattern: String
) : InvalidPatternException(
    createMessage(
        indexOfOpeningBracket, openingBracket,
        closingBracket
    ), pattern
) {
    companion object {
        private const val serialVersionUID = 1L

        private fun createMessage(
            indexOfOpeningBracket: Int,
            openingBracket: String, closingBracket: String
        ): String {
            return MessageFormat.format(
                JGitText.get().noClosingBracket,
                closingBracket, openingBracket,
                indexOfOpeningBracket
            )
        }
    }
}
