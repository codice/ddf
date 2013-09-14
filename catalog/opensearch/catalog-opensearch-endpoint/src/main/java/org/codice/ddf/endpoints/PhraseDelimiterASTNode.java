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
 * ASTNode that denotes the beginning of a phrase in the stack.
 */
public class PhraseDelimiterASTNode extends ASTNode {
    public PhraseDelimiterASTNode() {
        super(null, null);
    }

    @Override
    public String getKeyword() {
        return null;
    }

    @Override
    public ASTNode.Operator getOperator() {
        return null;
    }

    @Override
    public boolean isKeyword() {
        return false;
    }

    @Override
    public boolean isOperator() {
        return false;
    }

    @Override
    public boolean isPhraseStartDelimiter() {
        return true;
    }

    @Override
    public String toString() {
        return "Phrase Start Delimiter";
    }
}
