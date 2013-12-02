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
package ddf.catalog.transformer.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.Map;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;

import org.apache.log4j.Logger;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transformer.xml.adapter.AdaptedMetacard;
import ddf.catalog.transformer.xml.adapter.AttributeAdapter;

public class XmlMetacardTransformer extends AbstractXmlTransformer implements MetacardTransformer {

    private static final Logger LOGGER = Logger.getLogger(XmlMetacardTransformer.class);

    @Override
    public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
        throws CatalogTransformerException {

        BinaryContent transformedContent = null;

        if (metacard == null) {
            LOGGER.debug("Attempted to transform null metacard");
            throw new CatalogTransformerException("Unable to transform null metacard");
        }

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(
                    XmlMetacardTransformer.class.getClassLoader());
            Marshaller marshaller = CONTEXT.createMarshaller();
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            // TODO configure this option via Metatype DDF-2158
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setAdapter(new AttributeAdapter(metacard.getMetacardType()));
            marshaller.setEventHandler(new DefaultValidationEventHandler());

            try {
                marshaller.marshal(new AdaptedMetacard(metacard), os);
            } catch (RuntimeException e) {
                // catch runtime exception of JAXB or writing to stream.
                throw new CatalogTransformerException("Failed Transformation", e);
            }
            ByteArrayInputStream bais = new ByteArrayInputStream(os.toByteArray());
            transformedContent = new BinaryContentImpl(bais, new MimeType(TEXT_XML));
        } catch (JAXBException e) {
            throw new CatalogTransformerException("Failed JAXB Transformation", e);
        } catch (MimeTypeParseException e) {
            throw new CatalogTransformerException(
                    "Failed Transformation with MimeType Parsing error", e);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }

        return transformedContent;
    }
}
