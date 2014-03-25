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
package org.codice.ddf.spatial.ogc.csw.catalog.converter.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordMetacardType;
import org.xml.sax.helpers.NamespaceSupport;

import ddf.catalog.data.Metacard;

/**
 * DefaultCswRecordMap defines the system map between CSW Records and a framework Metacard. It
 * provides functions to retrieve the mapped values in either direction, and to retrieve metacard
 * fields for a CSW field as a {@link QName} or just a local name as a {@link String}. If a mapped
 * value isn't found, then the input value is used as the mapped value.
 * 
 */
public class DefaultCswRecordMap {
    private static final Map<QName, String> cswRecordQNameMapping;
    private static final Map<String, String> cswRecordLocalNameMapping;
    private static final Map<String, List<QName>> metacardMapping;

    private static final Map<String, String> prefixToUriMapping;
    private static final DefaultCswRecordMap mapping;
    
    static {
        Map<String, String> localNameMap = new CaseInsensitiveMap();
        
        localNameMap.put(CswConstants.ANY_TEXT, Metacard.ANY_TEXT);
        localNameMap.put(CswConstants.CSW_TITLE, Metacard.TITLE);
        localNameMap.put(CswRecordMetacardType.CSW_TITLE, Metacard.TITLE);
        localNameMap.put(CswRecordMetacardType.CSW_ALTERNATIVE, Metacard.TITLE);
        localNameMap.put(CswRecordMetacardType.CSW_TYPE, Metacard.CONTENT_TYPE);
        localNameMap.put(CswRecordMetacardType.CSW_IDENTIFIER, Metacard.ID);
        localNameMap.put(CswRecordMetacardType.CSW_BIBLIOGRAPHIC_CITATION, Metacard.ID);
        localNameMap.put(CswConstants.CSW_CREATED, Metacard.CREATED);
        localNameMap.put(CswConstants.CSW_MODIFIED, Metacard.MODIFIED);
        localNameMap.put(CswRecordMetacardType.CSW_CREATED, Metacard.CREATED);
        localNameMap.put(CswRecordMetacardType.CSW_MODIFIED, Metacard.MODIFIED);
        localNameMap.put(CswRecordMetacardType.CSW_DATE, Metacard.MODIFIED);
        localNameMap.put(CswRecordMetacardType.CSW_DATE_SUBMITTED, Metacard.MODIFIED);
        localNameMap.put(CswRecordMetacardType.CSW_ISSUED, Metacard.MODIFIED);
        localNameMap.put(CswRecordMetacardType.CSW_DATE_ACCEPTED, Metacard.EFFECTIVE);
        localNameMap.put(CswRecordMetacardType.CSW_DATE_COPYRIGHTED, Metacard.EFFECTIVE);
        localNameMap.put(CswRecordMetacardType.CSW_VALID, Metacard.EXPIRATION);

        cswRecordLocalNameMapping = Collections.unmodifiableMap(localNameMap);
        
        Map<QName, String> qNameMap = new HashMap<QName, String>();
        
        qNameMap.put(CswRecordMetacardType.CSW_IDENTIFIER_QNAME, Metacard.ID);
        qNameMap.put(CswRecordMetacardType.CSW_BIBLIOGRAPHIC_CITATION_QNAME, Metacard.ID);
        qNameMap.put(CswRecordMetacardType.CSW_TITLE_QNAME, Metacard.TITLE);
        qNameMap.put(CswRecordMetacardType.CSW_ALTERNATIVE_QNAME, Metacard.TITLE);
        qNameMap.put(CswRecordMetacardType.CSW_TYPE_QNAME, Metacard.CONTENT_TYPE);
        qNameMap.put(CswRecordMetacardType.CSW_DATE_QNAME, Metacard.MODIFIED);
        qNameMap.put(CswRecordMetacardType.CSW_MODIFIED_QNAME, Metacard.MODIFIED);
        qNameMap.put(CswRecordMetacardType.CSW_CREATED_QNAME, Metacard.CREATED);
        qNameMap.put(CswRecordMetacardType.CSW_DATE_ACCEPTED_QNAME, Metacard.EFFECTIVE);
        qNameMap.put(CswRecordMetacardType.CSW_DATE_COPYRIGHTED_QNAME, Metacard.EFFECTIVE);
        qNameMap.put(CswRecordMetacardType.CSW_DATE_SUBMITTED_QNAME, Metacard.MODIFIED);
        qNameMap.put(CswRecordMetacardType.CSW_ISSUED_QNAME, Metacard.MODIFIED);
        qNameMap.put(CswRecordMetacardType.CSW_VALID_QNAME, Metacard.EXPIRATION);

        cswRecordQNameMapping = Collections.unmodifiableMap(qNameMap);

        Map<String, List<QName>> metacardMap = new HashMap<String, List<QName>>();

        metacardMap.put(Metacard.ID, Arrays.asList(CswRecordMetacardType.CSW_IDENTIFIER_QNAME, CswRecordMetacardType.CSW_BIBLIOGRAPHIC_CITATION_QNAME));
        metacardMap.put(Metacard.TITLE, Arrays.asList(CswRecordMetacardType.CSW_TITLE_QNAME, CswRecordMetacardType.CSW_ALTERNATIVE_QNAME));
        metacardMap.put(Metacard.CONTENT_TYPE, Arrays.asList(CswRecordMetacardType.CSW_TYPE_QNAME));
        metacardMap.put(Metacard.MODIFIED, Arrays.asList(CswRecordMetacardType.CSW_DATE_QNAME, CswRecordMetacardType.CSW_MODIFIED_QNAME, CswRecordMetacardType.CSW_DATE_SUBMITTED_QNAME, CswRecordMetacardType.CSW_ISSUED_QNAME));

        metacardMap.put(Metacard.CREATED,  Arrays.asList(CswRecordMetacardType.CSW_CREATED_QNAME));
        metacardMap.put(Metacard.EFFECTIVE, Arrays.asList(CswRecordMetacardType.CSW_DATE_ACCEPTED_QNAME, CswRecordMetacardType.CSW_DATE_COPYRIGHTED_QNAME));
        metacardMap.put(Metacard.EXPIRATION, Arrays.asList(CswRecordMetacardType.CSW_VALID_QNAME));

        metacardMapping = Collections.unmodifiableMap(metacardMap);

        Map<String, String> prefixMapping = new HashMap<String, String>();

        prefixMapping.put(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
        prefixMapping.put(CswConstants.XML_SCHEMA_INSTANCE_NAMESPACE_PREFIX,
                XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
        prefixMapping.put(CswConstants.XML_SCHEMA_NAMESPACE_PREFIX,
                XMLConstants.W3C_XML_SCHEMA_NS_URI);
        prefixMapping.put(CswConstants.OWS_NAMESPACE_PREFIX, CswConstants.OWS_NAMESPACE);
        prefixMapping.put(CswConstants.CSW_NAMESPACE_PREFIX, CswConstants.CSW_OUTPUT_SCHEMA);
        prefixMapping.put(CswConstants.DUBLIN_CORE_NAMESPACE_PREFIX,
                CswConstants.DUBLIN_CORE_SCHEMA);
        prefixMapping.put(CswConstants.DUBLIN_CORE_TERMS_NAMESPACE_PREFIX,
                CswConstants.DUBLIN_CORE_TERMS_SCHEMA);

        prefixToUriMapping = Collections.unmodifiableMap(prefixMapping);

        mapping = new DefaultCswRecordMap();
    }
    
    public static DefaultCswRecordMap getDefaultCswRecordMap() {
        return mapping;
    }

    public Map<String, String> getCswToMetacardAttributeNames() {
        return cswRecordLocalNameMapping;
    }
    
    public String getDefaultMetacardFieldFor(QName cswField) {
        if (cswRecordQNameMapping.containsKey(cswField)) {
            return cswRecordQNameMapping.get(cswField);
        }
        
        return getDefaultMetacardFieldFor(cswField.getLocalPart());
    }

    public boolean hasDefaultMetacardFieldFor(QName cswField) {
        return cswRecordQNameMapping.containsKey(cswField);
    }

    public boolean hasDefaultMetacardFieldFor(String cswField) {
        return cswRecordLocalNameMapping.containsKey(cswField);
    }

    public String getDefaultMetacardFieldFor(String cswField) {
        if (cswRecordLocalNameMapping.containsKey(cswField)) {
            return cswRecordLocalNameMapping.get(cswField);
        }
        
        return cswField;
    }

    public boolean hasDefaultMetacardFieldForPrefixedString(String name) {
        return hasDefaultMetacardFieldForPrefixedString(name, null);
    }

    public boolean hasDefaultMetacardFieldForPrefixedString(String propertyName,
            NamespaceSupport namespaceSupport) {
        if (propertyName.contains(":")) {
            String prefix = propertyName.substring(0, propertyName.indexOf(":"));
            String localName = propertyName.substring(propertyName.indexOf(":") + 1);
            if (namespaceSupport != null && namespaceSupport.getURI(prefix) != null) {
                String uri = namespaceSupport.getURI(prefix);
                QName qname = new QName(uri, localName, prefix);
                return hasDefaultMetacardFieldFor(qname);
            } else {
                return hasDefaultMetacardFieldFor(localName);
            }
        } else {
            return hasDefaultMetacardFieldFor(propertyName);
        }
    }

    public String getDefaultMetacardFieldForPrefixedString(String name) {
        return getDefaultMetacardFieldForPrefixedString(name, null);
    }

    public String getDefaultMetacardFieldForPrefixedString(String propertyName,
            NamespaceSupport namespaceSupport) {
        String name;
        if (propertyName.contains(":")) {
            String prefix = propertyName.substring(0, propertyName.indexOf(":"));
            String localName = propertyName.substring(propertyName.indexOf(":") + 1);
            if (namespaceSupport != null && namespaceSupport.getURI(prefix) != null) {
                String uri = namespaceSupport.getURI(prefix);
                QName qname = new QName(uri, localName, prefix);
                name = getDefaultMetacardFieldFor(qname);
            } else {
                name = getDefaultMetacardFieldFor(localName);
            }
        } else {
            name = getDefaultMetacardFieldFor(propertyName);
        }
        return name;
    }
    
    public List<QName> getCswFieldsFor(String metacardField) {
        if (metacardMapping.containsKey(metacardField)) {
            return metacardMapping.get(metacardField);
        }
        return Arrays.asList(new QName(metacardField));
    }

    public Map<String, String> getPrefixToUriMapping() {
        return prefixToUriMapping;
    }
}
