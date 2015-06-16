/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

package ddf.catalog.transformer.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.xml.adapter.GeometryAdapter;

/**
 * Transforms Geometry object to BinaryContent
 */
class GeometryTransformer extends AbstractXmlTransformer {
    private static final int BUFFER_SIZE = 512;

    BinaryContent transform(Attribute attribute) throws CatalogTransformerException {
        BinaryContent transformedContent = null;

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread()
                    .setContextClassLoader(getClass().getClassLoader());

            ByteArrayOutputStream os = new ByteArrayOutputStream(BUFFER_SIZE);

            Marshaller marshaller = CONTEXT.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setEventHandler(new DefaultValidationEventHandler());

            try {
                marshaller.marshal(GeometryAdapter.marshalFrom(attribute), os);
            } catch (RuntimeException e) {
                throw new CatalogTransformerException("Failed to marshall geometry data", e);
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
