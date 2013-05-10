/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.BinaryContentImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.QueryResponseTransformer;
import ddf.catalog.transformer.xml.adapter.AdaptedSourceResponse;

/**
 * Transforms a {@link SourceResponse} object into Metacard Element XML text,
 * which is GML 3.1.1. compliant XML.
 * 
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 */
public class XmlResponseQueueTransformer extends AbstractXmlTransformer
        implements QueryResponseTransformer {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(XmlResponseQueueTransformer.class);

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

    @Override
    public BinaryContent transform(SourceResponse response,
            Map<String, Serializable> args) throws CatalogTransformerException {

        if (response == null) {
            LOGGER.debug("Attempted to transform null response");
            throw new CatalogTransformerException(
                    "Unable to transform null response");
        }

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(
                    XmlMetacardTransformer.class.getClassLoader());

            ByteArrayOutputStream os = new ByteArrayOutputStream();

            try {
                Marshaller marshaller = CONTEXT.createMarshaller();

                // TODO configure this option via Metatype DDF-2158
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
                        Boolean.TRUE);

                marshaller.setEventHandler(new DefaultValidationEventHandler());

                try {
                    marshaller.marshal(new AdaptedSourceResponse(response), os);
                } catch (RuntimeException e) {
                    LOGGER.info("Failed transformation", e);
                    throw new CatalogTransformerException(
                            "Failed Transformation", e);
                }
            } catch (JAXBException e) {
                LOGGER.info("Failed JAXB transformation", e);
                throw new CatalogTransformerException(
                        "Failed JAXB Transformation", e);
            }

            ByteArrayInputStream bais = new ByteArrayInputStream(
                    os.toByteArray());

            return new BinaryContentImpl(bais, MIME_TYPE);

        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }

    }

}
