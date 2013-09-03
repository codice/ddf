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
package com.lmco.ddf.opensearch.query;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.NamespaceContext;


/**
 * Contains a set of all namespaces used during the processing of atom
 * messages.
 * 
 */
public class OsNamespaceContext implements NamespaceContext
{

    private Map<String, String> namespaces;


    /**
     * Sets the default namespaces.
     */
    public OsNamespaceContext()
    {
        namespaces = new HashMap<String, String>();
        namespaces.put( "ogc", "http://www.opengis.net/ogc" );
        namespaces.put( "gml", "http://www.opengis.net/gml" );
    }


    @Override
    public String getNamespaceURI( String prefix )
    {
        return namespaces.get( prefix );
    }


    @Override
    public String getPrefix( String namespaceURI )
    {
        String prefix = null;
        if ( namespaces.containsValue( namespaceURI ) )
        {
            Iterator<Entry<String, String>> curIter = namespaces.entrySet().iterator();
            while ( curIter.hasNext() )
            {
                Entry<String, String> curEntry = curIter.next();
                if ( curEntry.getValue().equals( namespaceURI ) )
                {
                    prefix = curEntry.getKey();
                }
            }
        }
        return prefix;
    }


    @Override
    public Iterator<String> getPrefixes( String namespaceURI )
    {
        ArrayList<String> prefixList = new ArrayList<String>( 1 );
        prefixList.add( getPrefix( namespaceURI ) );
        return prefixList.iterator();
    }

}
