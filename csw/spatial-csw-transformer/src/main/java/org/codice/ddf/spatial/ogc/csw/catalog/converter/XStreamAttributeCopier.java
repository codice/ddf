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
package org.codice.ddf.spatial.ogc.csw.catalog.converter;

import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.Map.Entry;

/**
 * XStream tool to copy contents of a HierarichalStreamReader to a HierarichalStreamWriter.
 * <p/>
 * If an attributeMap is provided, the attributes will be added to the root element.
 * This class was dervied from the implementation of HierarchicalStreamCopier.
 */
public class XStreamAttributeCopier extends HierarchicalStreamCopier {

    public XStreamAttributeCopier() {
        super();
    }

    public void copyAttributes(HierarchicalStreamReader source,
            HierarchicalStreamWriter destination, Map<String, String> namespaceMap) {
        destination.startNode(source.getNodeName());
        int attributeCount = source.getAttributeCount();
        for (int i = 0; i < attributeCount; i++) {
            destination.addAttribute(source.getAttributeName(i), source.getAttribute(i));
        }
        if (namespaceMap != null && !namespaceMap.isEmpty()) {
            for (Entry<String, String> entry : namespaceMap.entrySet()) {
                if (StringUtils.isBlank(source.getAttribute(entry.getKey()))) {
                    destination.addAttribute(entry.getKey(), entry.getValue());
                }
            }
        }
        String value = source.getValue();
        if (value != null && value.length() > 0) {
            destination.setValue(value);
        }
        while (source.hasMoreChildren()) {
            source.moveDown();
            super.copy(source, destination);
            source.moveUp();
        }
        destination.endNode();
    }
}
