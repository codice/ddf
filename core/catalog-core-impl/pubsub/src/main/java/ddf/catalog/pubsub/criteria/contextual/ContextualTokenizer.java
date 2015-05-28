/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package ddf.catalog.pubsub.criteria.contextual;

import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.CharTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a workaround for https://issues.apache.org/jira/browse/LUCENE-588
 */
public class ContextualTokenizer extends CharTokenizer {

    public static final Set<Character> SPECIAL_CHARACTERS_SET;

    private static final Logger LOGGER = LoggerFactory.getLogger(ContextualTokenizer.class);

    static {
        final List<Character> specialChars = Arrays
                .asList('-', '_', '+', '&', '|', '!', '(', ')', '{', '}', '[', ']', '~', '?', ':',
                        '*', '\\', '^');
        final HashSet<Character> specialCharsSet = new HashSet<>(specialChars);
        SPECIAL_CHARACTERS_SET = Collections.unmodifiableSet(specialCharsSet);
    }

    public ContextualTokenizer(Reader input) {
        super(input);
    }

    @Override
    protected boolean isTokenChar(char c) {
        return Character.isLetterOrDigit(c) || SPECIAL_CHARACTERS_SET.contains(c);
    }

}
