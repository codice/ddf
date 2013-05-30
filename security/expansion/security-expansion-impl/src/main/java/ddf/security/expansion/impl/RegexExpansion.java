/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.security.expansion.impl;

public class RegexExpansion extends AbstractExpansion
{


    public RegexExpansion()
    {
    }

    /**
     * Performs an expansion using regular expressions for both parts of the rule. The first element is a regular
     * expression identifying what to find in the original string, the second element is a regular expression indicating
     * what is to be replaced (may contain substitution groups, etc.)
     * @param original  The original string to be expanded
     * @param rule  Two values specifying the expression to search for and the expression to replace it with
     * @return  the (potentially) expanded string as a result of applying the expansion rule
     */
    @Override
    protected String doExpansion(String original, String[] rule)
    {
        String expandedValue;
        expandedValue = original.replaceAll(rule[0], rule[1]);
        return expandedValue;
    }

}
