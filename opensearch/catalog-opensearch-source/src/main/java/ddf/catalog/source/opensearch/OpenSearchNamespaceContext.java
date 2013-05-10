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
public class OpenSearchNamespaceContext implements NamespaceContext
{

    private Map<String, String> namespaces;


    /**
     * Sets the default namespaces: ddms, atom, opensearch, georss, ICISM, fs,
     * gml.
     */
    public OpenSearchNamespaceContext()
    {
        namespaces = new HashMap<String, String>();
        namespaces.put( "ddms", "http://metadata.dod.mil/mdr/ns/DDMS/2.0/" );
        namespaces.put( "atom", "http://www.w3.org/2005/Atom" );
        namespaces.put( "opensearch", "http://a9.com/-/spec/opensearch/1.1/" );
        namespaces.put( "georss", "http://www.georss.org/georss" );
        namespaces.put( "fs", "http://a9.com/-/opensearch/extensions/federation/1.0/" );
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
