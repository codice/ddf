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
package ddf.catalog.transformer.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transformer.api.MetacardMarshaller;

public class XmlMetacardTransformer implements MetacardTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(XmlMetacardTransformer.class);

    private MetacardMarshaller metacardMarshaller;

    public static final MimeType MIME_TYPE = new MimeType();

    static {
        try {
            MIME_TYPE.setPrimaryType("text");
            MIME_TYPE.setSubType("xml");
        } catch (MimeTypeParseException e) {
            LOGGER.info("Failure creating MIME type", e);
            throw new ExceptionInInitializerError(e);
        }
    }

    public XmlMetacardTransformer(MetacardMarshaller metacardMarshaller) {
        this.metacardMarshaller = metacardMarshaller;
    }

    @Override
    public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
            throws CatalogTransformerException {

        if (metacard == null) {
            LOGGER.debug("Attempted to transform null metacard");
            throw new CatalogTransformerException("Unable to transform null metacard");
        }

        try {
            String xmlString = metacardMarshaller.marshal(metacard, arguments);
            ByteArrayInputStream bais = new ByteArrayInputStream(xmlString.getBytes());
            return new BinaryContentImpl(bais, MIME_TYPE);
        } catch (XmlPullParserException | IOException e) {
            throw new CatalogTransformerException(e);
        }
    }

}
