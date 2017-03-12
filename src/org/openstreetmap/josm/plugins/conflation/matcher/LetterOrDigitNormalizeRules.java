// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.conflation.matcher;

import org.openstreetmap.josm.data.validation.tests.SimilarNamedWays.NormalizeRule;

/**
 * Keep only letters or digits characters (remove spaces, punctuation, ...).
 *
 * REM: this will remove word separators.
 */
public class LetterOrDigitNormalizeRules implements NormalizeRule {

    public static final LetterOrDigitNormalizeRules INSTANCE = new LetterOrDigitNormalizeRules();

    @Override
    public String normalize(String value) {
        return value
                .codePoints()
                .filter(Character::isLetterOrDigit)
                .collect(StringBuilder::new,
                         StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

}
