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
package org.codice.ddf.spatial.ogc.csw.catalog.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS Parameter Bean Class for a CSW request. The member variables will be
 * automatically injected by the JAX-RS annotations.  This serves as a parent
 * class to all other CSW requests.
 * 
 */

public class CswRequest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CswRequest.class);

    private String request;

    private String service;

    public CswRequest() {
    }

    public CswRequest(String request) {
        this.request = request;
    }

    public CswRequest(String service, String request) {
        this.service = service;
        this.request = request;
    }
    
    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    protected List<QName> typeStringToQNames(String typeNames, Map<String, String> namespaces) throws CswException {
        List<QName> qNames = new ArrayList<QName>();
        if (typeNames == null) {
            return qNames;
        }

        String[] types = typeNames.split(CswConstants.COMMA);
        for (String typeName : types) {
            if(typeName.indexOf(CswConstants.NAMESPACE_DELIMITER) != -1) {
                int index = typeName.indexOf(CswConstants.NAMESPACE_DELIMITER);
                String prefix = typeName.substring(0, index);
                String localPart = typeName.substring(index + 1);
                if(namespaces != null && namespaces.containsKey(prefix)) {
                    QName qname = new QName(namespaces.get(prefix), localPart, prefix);
                    qNames.add(qname);
                } else {
                    throw createUnknownNamespacePrefixException(prefix);
                }
            } else {
                QName qname = new QName(typeName);
                qNames.add(qname);                
            }
        }
        return qNames;
    }
    
    public Map<String, String> parseNamespaces(String namespaces) throws CswException {
        Map<String, String> namespaceMap = new HashMap<String, String>();
        if(namespaces == null) {
            LOGGER.warn("Namespaces list is null");
            return namespaceMap;
        }
        
        String[] namespaceArray = namespaces.split(CswConstants.COMMA);
        
        for(String namespace : namespaceArray) {
            if(namespace.startsWith(CswConstants.XMLNS_DEFINITION_PREFIX) &&
                    namespace.endsWith(CswConstants.XMLNS_DEFINITION_POSTFIX)) {
                
                String nsAssignment = namespace.substring(CswConstants.XMLNS_DEFINITION_PREFIX.length(), 
                        namespace.length() - CswConstants.XMLNS_DEFINITION_POSTFIX.length());
                
                String[] split = nsAssignment.split(CswConstants.EQUALS);
                
                if(split.length == 2) {
                    namespaceMap.put(split[0], split[1]);
                } else if(split.length == 1) {  // default namespace
                    namespaceMap.put("", split[0]);
                } else {
                    throw createInvalidNamespaceFormatException(namespaces);
                }
            } else {
                throw createInvalidNamespaceFormatException(namespaces);                
            }
        }
        
        return namespaceMap;
    }

    private CswException createInvalidNamespaceFormatException(final String namespaces) {
        return new CswException("The NAMESPACE value '" + namespaces + "' is not properly formatted.");
    }

    private CswException createUnknownNamespacePrefixException(final String prefix) {
        return new CswException("The namespace '" + prefix + "' is not specified as a namespace.");
    }

}
