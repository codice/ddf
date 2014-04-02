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

import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.naming.NameCoder;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.StaxWriter;

public class EnhancedStaxWriter extends StaxWriter {
    private XMLStreamWriter out;

    public EnhancedStaxWriter(QNameMap qnameMap, XMLStreamWriter out,
            boolean writeEnclosingDocument, boolean namespaceRepairingMode, NameCoder nameCoder)
        throws XMLStreamException {
        super(qnameMap, out, writeEnclosingDocument, namespaceRepairingMode, nameCoder);
        this.out = out;
    }

    public EnhancedStaxWriter(QNameMap qnameMap, XMLStreamWriter out,
            boolean writeEnclosingDocument, boolean namespaceRepairingMode)
        throws XMLStreamException {
        super(qnameMap, out, writeEnclosingDocument, namespaceRepairingMode);
        this.out = out;
    }

    public EnhancedStaxWriter(QNameMap qnameMap, XMLStreamWriter out, NameCoder nameCoder)
        throws XMLStreamException {
        super(qnameMap, out, nameCoder);
        this.out = out;
    }

    public EnhancedStaxWriter(QNameMap qnameMap, XMLStreamWriter out) throws XMLStreamException {
        super(qnameMap, out);
        this.out = out;
    }

    public void writeCdata(String cdata) {
        try {
            out.writeCData(cdata);
        } catch (XMLStreamException e) {
            throw new StreamException(e);
        }
    }

}
