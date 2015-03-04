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
package ddf.security.expansion.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StraightExpansionImpl extends AbstractExpansion {
    private static final Logger LOGGER = LoggerFactory.getLogger(StraightExpansionImpl.class);

    public StraightExpansionImpl() {
    }

    /**
     * Performs an expansion using straight string equality and substitution for both parts of the
     * rule. The first element is the string that must match the entire original string, the second
     * element is the complete replacement string to be substituted for the original.
     * 
     * @param original
     *            The original string to be expanded
     * @param rule
     *            Two values specifying the expression to search for and the string to replace it
     *            with
     * @return the (potentially) expanded string as a result of applying the provided rule
     */
    @Override
    protected String doExpansion(String original, String[] rule) {
        String expandedValue = original;
        if ((original != null) && (original.equals(rule[0]))) {
            expandedValue = rule[1];
        }
        return expandedValue;
    }
}
