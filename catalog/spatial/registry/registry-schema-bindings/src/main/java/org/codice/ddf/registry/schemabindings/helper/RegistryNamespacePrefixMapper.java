/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

package org.codice.ddf.registry.schemabindings.helper;

import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;

import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

public class RegistryNamespacePrefixMapper extends NamespacePrefixMapper {

    private static final String WRS_SCHEMA = "http://www.opengis.net/cat/wrs/1.0";

    private static final String WRS_NAMESPACE = "wrs";

    private Map<String, String> namespaceMap = new HashMap<>();

    public RegistryNamespacePrefixMapper() {
        namespaceMap.put(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI,
                CswConstants.XML_SCHEMA_NAMESPACE_PREFIX);
        namespaceMap.put(WRS_SCHEMA, WRS_NAMESPACE);
        namespaceMap.put(CswConstants.EBRIM_SCHEMA, CswConstants.EBRIM_NAMESPACE_PREFIX);
        namespaceMap.put(CswConstants.OGC_SCHEMA, CswConstants.OGC_NAMESPACE_PREFIX);
        namespaceMap.put(CswConstants.GML_SCHEMA, CswConstants.GML_NAMESPACE_PREFIX);
    }

    @Override
    public String getPreferredPrefix(String namespaceUri, String suggestion,
            boolean requirePrefix) {
        return namespaceMap.getOrDefault(namespaceUri, suggestion);
    }
}
