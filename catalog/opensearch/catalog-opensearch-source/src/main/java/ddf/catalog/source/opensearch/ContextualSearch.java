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
package ddf.catalog.source.opensearch;


public class ContextualSearch
{
    private String searchPhrase;
    
    private String selectors;
    
    private boolean isCaseSensitive;
    
    
    public ContextualSearch( String selectors, String searchPhrase, boolean isCaseSensitive )
    {
        this.selectors = selectors;
        this.searchPhrase = searchPhrase;
        this.isCaseSensitive = isCaseSensitive;
    }


    public String getSearchPhrase()
    {
        return searchPhrase;
    }


    public void setSearchPhrase( String searchPhrase )
    {
        this.searchPhrase = searchPhrase;
    }


    public String getSelectors()
    {
        return selectors;
    }


    public void setSelectors( String selectors )
    {
        this.selectors = selectors;
    }


    public boolean isCaseSensitive()
    {
        return isCaseSensitive;
    }


    public void setCaseSensitive( boolean isCaseSensitive )
    {
        this.isCaseSensitive = isCaseSensitive;
    }
    
}
