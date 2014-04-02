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

import java.util.List;

import javax.xml.namespace.QName;

import com.thoughtworks.xstream.converters.Converter;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;

/**
 * Extension of the XStream <a href=
 * "http://xstream.codehaus.org/javadoc/com/thoughtworks/xstream/converters/Converter.html"
 * >Converter</a> interface
 * 
 * 
 * A RecordConverter implementation converts csw records to {@link Metacard}s. 
 * 
 */
public interface RecordConverter extends Converter {
    
    /**
     * Set the MetacardType on the converter
     * 
     * @param metacardType
     */
    public void setMetacardType(MetacardType metacardType);
    
    /**
     * Get this converter's MetacardType
     * 
     * @return metacardType
     */
    public MetacardType getMetacardType();
    
    /**
     * Sets the fields to write to the {@link Metacard}
     * 
     * @param fieldsToWrite
     */
    public void setFieldsToWrite(List<QName> fieldsToWrite);

    /**
     * Gets the fields to write to the {@link Metacard}
     * 
     * @return fields to write
     */
    public List<QName> getFieldsToWrite();

    /**
     * Gets the root element name to write for this Element Set.
     * 
     * @param elementSetType
     *            - the element set name
     * @return the root element name
     */
    public String getRootElementName(String elementSetType);

}
