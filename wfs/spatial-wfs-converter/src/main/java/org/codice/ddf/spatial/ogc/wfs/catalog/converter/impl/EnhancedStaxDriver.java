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

package org.codice.ddf.spatial.ogc.wfs.catalog.converter.impl;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.thoughtworks.xstream.io.naming.NameCoder;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.StaxDriver;

public class EnhancedStaxDriver extends StaxDriver {

    private QNameMap qnameMap;

    public EnhancedStaxDriver() {
        this(new QNameMap());
    }

    public EnhancedStaxDriver(QNameMap qnameMap) {
        super(qnameMap);
        this.qnameMap = qnameMap;
    }

    public EnhancedStaxDriver(NameCoder nameCoder) {
        this(new QNameMap(), nameCoder);
    }

    public EnhancedStaxDriver(QNameMap qnameMap, NameCoder nameCoder) {
        super(qnameMap, nameCoder);
        this.qnameMap = qnameMap;
    }

    @Override
    public EnhancedStaxWriter createStaxWriter(XMLStreamWriter out, boolean writeStartEndDocument)
        throws XMLStreamException {
        return new EnhancedStaxWriter(qnameMap, out, writeStartEndDocument, isRepairingNamespace(),
                getNameCoder());
    }

}
