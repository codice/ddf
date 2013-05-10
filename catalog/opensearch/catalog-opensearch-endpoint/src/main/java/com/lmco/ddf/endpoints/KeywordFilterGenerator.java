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
package com.lmco.ddf.endpoints;

import ddf.catalog.data.Metacard;
import ddf.catalog.filter.FilterBuilder;
import org.opengis.filter.Filter;

public class KeywordFilterGenerator
{
    //TODO refactor this from helper class
    private final FilterBuilder filterBuilder;

    public KeywordFilterGenerator(FilterBuilder filterBuilder)
    {
        this.filterBuilder = filterBuilder;
    }

    public Filter getFilterFromASTNode(ASTNode astNode, String xpathSelector) throws IllegalStateException
    {
        if (astNode == null)
        {
            throw new IllegalStateException("Unable to generate Filter from null ASTNode.");
        }

        if (filterBuilder == null)
        {
            throw new IllegalStateException("Unable to generate Filter using null FilterBuilder.");
        }

        if (astNode.isKeyword())
        {
            if (xpathSelector != null && !xpathSelector.isEmpty())
            {
                return filterBuilder.xpath(xpathSelector).is().like().text(astNode.getKeyword());
            }
            else
            {
                return filterBuilder.attribute(Metacard.ANY_TEXT).is().like().text(astNode.getKeyword());
            }
        }

        //it's an operator
        if (astNode.isOperator())
        {
            switch(astNode.getOperator())
            {
                case AND:
                    return filterBuilder.allOf(getFilterFromASTNode(astNode.left()), getFilterFromASTNode(astNode.right()));
                case OR:
                    return filterBuilder.anyOf(getFilterFromASTNode(astNode.left()), getFilterFromASTNode(astNode.right()));
                case NOT: //since NOT really means AND NOT
                    return filterBuilder.allOf(getFilterFromASTNode(astNode.left()), filterBuilder.not(getFilterFromASTNode(astNode.right())));
                default:
                    throw new IllegalStateException("Unable to generate Filter from invalid OperatorASTNode.");
            }
        }

        throw new IllegalStateException("Unable to generate Filter from ASTNode. Found invalid ASTNode in the tree!");
    }

    public Filter getFilterFromASTNode(ASTNode astNode) throws IllegalStateException
    {
        return getFilterFromASTNode(astNode, null);
    }
}
