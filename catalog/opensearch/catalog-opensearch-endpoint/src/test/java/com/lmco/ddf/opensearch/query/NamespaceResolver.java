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
package com.lmco.ddf.opensearch.query;

import org.apache.xml.utils.PrefixResolver;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;
import org.w3c.dom.Node;

public class NamespaceResolver implements PrefixResolver {
    private static final XLogger LOGGER = new XLogger(
            LoggerFactory.getLogger(NamespaceResolver.class));

    private static final String DEFAULT_NAMESPACE = "http://www.opengis.net/ogc";

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.namespace.NamespaceContext#getNamespaceURI(java.lang.String)
     */
    public String getNamespaceForPrefix(String prefix) {
        String namespace = DEFAULT_NAMESPACE;
        if (prefix.equals("ogc")) {
            namespace = DEFAULT_NAMESPACE;
        } else if (prefix.equals("gml")) {
            namespace = "http://www.opengis.net/gml";
        }

        LOGGER.debug("namespace returned = " + namespace);

        return namespace;
    }

    public String getNamespaceForPrefix(String prefix, Node context) {
        String namespace = DEFAULT_NAMESPACE;
        if (prefix.equals("ogc")) {
            namespace = DEFAULT_NAMESPACE;
        } else if (prefix.equals("gml")) {
            namespace = "http://www.opengis.net/gml";
        }

        LOGGER.debug("namespace returned = " + namespace);

        return namespace;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.namespace.NamespaceContext#getPrefix(java.lang.String)
     */
    public String getBaseIdentifier() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.namespace.NamespaceContext#getPrefixes(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public boolean handlesNullPrefixes() {
        return true;
    }

}
