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
package org.codice.ddf.endpoints;

/**
 * ASTNode that holds a keyword.
 */
public class KeywordASTNode extends ASTNode {
    private final String keyword;

    public KeywordASTNode(String keyword) {
        super(null, null);
        this.keyword = keyword;
    }

    @Override
    public String getKeyword() {
        return keyword;
    }

    @Override
    public Operator getOperator() {
        return null;
    }

    @Override
    public boolean isKeyword() {
        return true;
    }

    @Override
    public boolean isOperator() {
        return false;
    }

    @Override
    public boolean isPhraseStartDelimiter() {
        return false;
    }

    @Override
    public String toString() {
        return "Keyword: " + keyword;
    }
}
