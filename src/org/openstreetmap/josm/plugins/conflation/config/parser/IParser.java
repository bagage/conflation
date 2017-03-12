// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.conflation.config.parser;

import java.util.List;

/**
 * Simple text parser.
 * @author osm
 *
 */
public interface IParser {
    /**
     * Parse the given text and return true if it is valid.
     */
    boolean parse(String text);

    /**
     * Return true if the parsed text is valid.
     */
    boolean isValid();

    /**
     * Return true if the text has been fully parsed.
     * (i.e. isValid() or errors occurred in the last token).
     */
    boolean isFullyParsed();

    /**
     * The character index where the parsing stopped.
     */
    int getParsedIndex();

    /**
     * The start charac)ter index of the last parsed token.
     */
    int getLastTokenIndex();

    /**
     * Get the description for the last parsed token.
     */
    String getLastTokenDescription();

    /**
     * Get the error message for the last parsed token.
     */
    String getErrorMessage();

    /**
     * Return the completion list, at the last parsed index.
     */
    List<String> getCompletionList();
}
