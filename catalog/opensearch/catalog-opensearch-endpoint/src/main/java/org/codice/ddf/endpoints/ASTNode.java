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

import org.parboiled.trees.ImmutableBinaryTreeNode;

/**
 * Abstract Syntax Tree Node </br> Abstract class that extends built-in Parboiled tree node class.
 * </br> </br> During the parsing of the keyword string, the parser creates and stores instances of
 * this class's subclasses on the processing stack. This allows the parser to build an Abstract
 * Syntax Tree from the keyword string.
 */

public abstract class ASTNode extends ImmutableBinaryTreeNode<ASTNode> {
    // Do not use this directly!
    protected ASTNode(ASTNode left, ASTNode right) {
        super(left, right);
    }

    public enum Operator {
        AND, OR, NOT;

        public static Operator getOperatorFromString(String operatorString) {
            if (operatorString.trim().equals(KeywordTextParser.AND_STRING)) {
                return Operator.AND;
            } else if (operatorString.trim().equals(KeywordTextParser.OR_STRING)) {
                return Operator.OR;
            } else if (operatorString.trim().equals(KeywordTextParser.NOT_STRING)) {
                return Operator.NOT;
            } else if (operatorString.contains(KeywordTextParser.SPACE_STRING) // if the string is
                                                                               // all spaces, it's
                                                                               // an AND
                    && operatorString.trim().isEmpty()) {
                return Operator.AND;
            } else {
                return null;
            }
        }
    }

    public abstract String getKeyword();

    public abstract Operator getOperator();

    public abstract boolean isKeyword();

    public abstract boolean isOperator();

    public abstract boolean isPhraseStartDelimiter();

    @Override
    public abstract String toString();
}
