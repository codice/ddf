/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.pubsub.criteria.contextual;

import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.CharTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextualTokenizer extends CharTokenizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContextualTokenizer.class);
    
    public static final Set<?> SPECIAL_CHARACTERS_SET;

    static {
        final List<String> specialChars = Arrays.asList("-", "_", "+", "|", "!", "(", ")", "{", "}", "[", "]", "~", "?", ":", "*");
        final CharArraySet specialCharsSet = new CharArraySet(specialChars.size(), false);
        specialCharsSet.addAll(specialChars);
        SPECIAL_CHARACTERS_SET = CharArraySet.unmodifiableSet(specialCharsSet);
    }
    
    public ContextualTokenizer(Reader input) {
        super(input);
    }
    
    @Override
    protected boolean isTokenChar(char c) {
        if (Character.isLetterOrDigit(c) || SPECIAL_CHARACTERS_SET.contains(c)) {
            return true;
        }
        return false;
    }

}
