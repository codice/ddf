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

import javax.xml.namespace.QName;

import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Represents one record type entry.  This provides type information for each type
 * of Record used by metadata of one or more registered resource types.  This is
 * used in the discovery process.
 *
 */
public class RecordTypeEntry {
    private QName type;

    private String version;

    private String resourcePath;
    
    private String schemaLanguage;

    public RecordTypeEntry(QName type, String version, String resourcePath, String schemaLanguage) {
        this.type = type;
        this.version = version;
        this.resourcePath = resourcePath;
        this.schemaLanguage = schemaLanguage;
    }

    public QName getType() {
        return type;
    }

    public void setType(QName type) {
        this.type = type;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String getSchemaLanguage() {
        return schemaLanguage;
    }

    public void setSchemaLanguage(String schemaLanguage) {
        this.schemaLanguage = schemaLanguage;
    }

    @Override
    public boolean equals(Object obj) {
        // if the two objects are equal in reference, they are equal
        if (this == obj) {
            return true;
        } else if (obj instanceof RecordTypeEntry) {
            RecordTypeEntry otherEntry = (RecordTypeEntry) obj;
            return ((((otherEntry.getType() == null) && (this.getType() == null))
                    || otherEntry.getType().equals(this.getType()))
                    && ((otherEntry.getVersion() == null) && (this.getVersion() == null)
                    || otherEntry.getVersion().equals(this.getVersion())));

        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(type).append(version).toHashCode();
    }
}
