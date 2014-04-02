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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.utils;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.common.util.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;

/**
 * Registry of type information used by metadata of one or more registered resource types.
 *
 */
public class RecordTypeRegistry {
    List<RecordTypeEntry> recordTypes;

    public RecordTypeRegistry() {
        recordTypes = new ArrayList<RecordTypeEntry>();
        initializeRegistry();
    }

    private void initializeRegistry() {
        // TODO possibly make this dynamic.
        recordTypes.add(new RecordTypeEntry(new QName(CswConstants.CSW_OUTPUT_SCHEMA, "Record",
                "csw"), CswConstants.VERSION_2_0_2, "csw/2.0.2/record.xsd",
                CswConstants.XML_SCHEMA_LANGUAGE));
    }

    public List<RecordTypeEntry> getRecordTypes() {
        return recordTypes;
    }

    public void setRecordTypes(List<RecordTypeEntry> recordTypes) {
        this.recordTypes = recordTypes;
    }

    public RecordTypeEntry getEntry(QName type, String version) {
        RecordTypeEntry other = new RecordTypeEntry(type, version, null, null);
        for (RecordTypeEntry entry : recordTypes) {
            // Handling logic for no namespace passed.
            if (StringUtils.isEmpty(type.getNamespaceURI()) && StringUtils.isEmpty(type.getPrefix())) {
                // Not using namespaces.
                if (entry.getType().getLocalPart() != null && type.getLocalPart() != null) {
                    return entry;
                }
            }
            if (entry.equals(other)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Returns true if there is any matching value in typeName
     *
     * @param typeName single type name
     * @param version version specified in the request
     * @return determination on whether the given type exists in the list
     */
    public boolean containsType(QName typeName, String version) {
        if (typeName == null || version == null) {
            return false;
        }
        RecordTypeEntry other = new RecordTypeEntry(typeName, version, null, null);
        for (RecordTypeEntry entry : recordTypes) {
            if (StringUtils.isEmpty(typeName.getNamespaceURI()) && StringUtils.isEmpty(typeName.getPrefix())) {
                // Not using namespaces.
                if (entry.getType().getLocalPart() != null && typeName.getLocalPart() != null) {
                    if (entry.getType().getLocalPart().equals(typeName.getLocalPart())) {
                        return true;
                    }
                }
            }
            if (entry.equals(other)) {
                return true;
            }
        }
        return false;
    }

}
