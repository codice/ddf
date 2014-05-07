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
package ddf.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

public class NamespaceMapImpl implements NamespaceContext {
    private static final XLogger logger = new XLogger(LoggerFactory.getLogger(NamespaceMapImpl.class));

    private Map<String, String> allNamespaceUris;

    private Map<String, String> allPrefixes;

    public NamespaceMapImpl(Map<String, String> namespaces) {
        logger.debug("MockNamespaceResolver constructor");

        allNamespaceUris = new HashMap<String, String>();
        allPrefixes = new HashMap<String, String>();

        allNamespaceUris.putAll(namespaces);

        for (String key : namespaces.keySet()) {
            allPrefixes.put(namespaces.get(key), key);
        }
    }

    @Override
    public String getNamespaceURI(String prefix) {
        return allNamespaceUris.get(prefix);
    }

    @Override
    public String getPrefix(String namespaceURI) {
        return allPrefixes.get(namespaceURI);
    }

    @Override
    public Iterator getPrefixes(String namespaceURI) {
        return null;
    }
}