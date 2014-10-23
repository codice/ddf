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
package org.codice.ddf.spatial.ogc.csw.catalog.transformer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.DataHolder;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.WstxDriver;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.impl.CswRecordConverter;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.impl.DefaultCswRecordMap;

import javax.activation.MimeType;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Map;

/**
 * MetacardTransformer implementation for CSW Record Schema.
 */
public class CswRecordMetacardTransformer implements MetacardTransformer {

    private XStream xstream;

    private CswRecordConverter recordConverter;

    public CswRecordMetacardTransformer() {
        recordConverter = new CswRecordConverter();

        xstream = new XStream(new WstxDriver());
        xstream.setClassLoader(this.getClass().getClassLoader());
        xstream.registerConverter(recordConverter);
        xstream.alias(CswConstants.CSW_RECORD, Metacard.class);
    }

    @Override public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
            throws CatalogTransformerException {
        StringWriter stringWriter = new StringWriter();
        stringWriter.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        PrettyPrintWriter writer = new PrettyPrintWriter(stringWriter);
        DataHolder holder = xstream.newDataHolder();
        holder.put(CswConstants.WRITE_NAMESPACES, true);

        xstream.marshal(metacard, writer, holder);

        BinaryContent transformedContent = null;

        ByteArrayInputStream bais = new ByteArrayInputStream(stringWriter.toString().getBytes());
        transformedContent = new BinaryContentImpl(bais, new MimeType());
        return transformedContent;
    }
}
