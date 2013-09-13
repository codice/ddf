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
package ddf.catalog.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.util.NamespaceMapImpl;
import ddf.util.NamespaceResolver;

public class MockNamespaceResolver extends NamespaceResolver {
    private static XLogger logger = new XLogger(
            LoggerFactory.getLogger(MockNamespaceResolver.class));

    private static final Map<String, String> DEFAULT_NAMESPACE_MAP = new HashMap<String, String>();
    static {
        DEFAULT_NAMESPACE_MAP.put("ns1", "http://metadata.abc.com/mdr/ns/ns1/2.0/");
        DEFAULT_NAMESPACE_MAP.put("ns2", "urn:abc:xyz:ic:ism:v2");
        DEFAULT_NAMESPACE_MAP.put("ns3", "http://www.opengis.net/gml");
        DEFAULT_NAMESPACE_MAP.put("abc", "http://abc.com/metadata");
    }

    public MockNamespaceResolver() {
        this(DEFAULT_NAMESPACE_MAP);
    }

    public MockNamespaceResolver(Map<String, String> namespaces) {
        logger.debug("MockNamespaceResolver constructor");

        namespaceContexts = new ArrayList<NamespaceContext>();
        namespaceContexts.add(new NamespaceMapImpl(namespaces));
    }

    public String getNamespaceURI(String prefix) {
        String methodName = "getNamespaceURI";
        logger.trace("ENTERING: " + methodName);

        String namespaceUri = null;

        for (NamespaceContext nc : namespaceContexts) {
            namespaceUri = nc.getNamespaceURI(prefix);
            if (namespaceUri != null)
                break;
        }

        logger.trace("EXITING: " + methodName + "    (namespaceUri = " + namespaceUri + ")");

        return namespaceUri;
    }

    public String getPrefix(String namespace) {
        String methodName = "getPrefix";
        logger.trace("ENTERING: " + methodName + ",   namespace = " + namespace);

        String prefix = null;

        for (NamespaceContext nc : namespaceContexts) {
            prefix = nc.getPrefix(namespace);
            if (prefix != null)
                break;
        }

        logger.trace("EXITING: " + methodName + "    (prefix = " + prefix + ")");

        return prefix;
    }

}
