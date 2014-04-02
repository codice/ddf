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

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.RecordConverter;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.RecordConverterFactory;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.impl.CswRecordConverterFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.io.xml.WstxDriver;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;

public class CswRecordInputTransformer implements InputTransformer {

    private XStream xstream;

    public CswRecordInputTransformer() {

        RecordConverterFactory recordConverterFactory = new CswRecordConverterFactory();

        RecordConverter recordConverter = recordConverterFactory.createConverter(null, null, null,
                null, true);

        xstream = new XStream(new WstxDriver());
        xstream.setClassLoader(this.getClass().getClassLoader());
        xstream.registerConverter(recordConverter);
        xstream.alias(CswConstants.CSW_RECORD_LOCAL_NAME, Metacard.class);
    }

    @Override
    public Metacard transform(InputStream inputStream) throws IOException,
        CatalogTransformerException {
        return transform(inputStream, null);
    }

    @Override
    public Metacard transform(InputStream inputStream, String id) throws IOException,
        CatalogTransformerException {
        Metacard metacard = null;
        try {
            metacard = (Metacard) xstream.fromXML(inputStream);
            if (StringUtils.isNotEmpty(id)) {
                metacard.setAttribute(new AttributeImpl(Metacard.ID, id));
            }
        } catch (XStreamException e) {
            throw new CatalogTransformerException(
                    "Unable to transform from CSW Record to Metacard.", e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }

        if (metacard == null) {
            throw new CatalogTransformerException(
                    "Unable to transform from CSW Record to Metacard.");
        }

        return metacard;
    }

}
