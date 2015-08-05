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
import java.io.Serializable;
import java.util.Map;

import javax.xml.bind.Marshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;

import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transformer.xml.adapter.AdaptedMetacard;
import ddf.catalog.transformer.xml.adapter.AttributeAdapter;

public class XmlMetacardTransformer extends AbstractXmlTransformer implements MetacardTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(XmlMetacardTransformer.class);

    public XmlMetacardTransformer(Parser parser) {
        super(parser);
    }

    @Override
    public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
            throws CatalogTransformerException {
        if (metacard == null) {
            LOGGER.debug("Attempted to transform null metacard");
            throw new CatalogTransformerException("Unable to transform null metacard");
        }

        ParserConfigurator parserConfigurator = getParserConfigurator()
                .setAdapter(new AttributeAdapter(metacard.getMetacardType()))
                .setHandler(new DefaultValidationEventHandler())
                .addProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            getParser().marshal(parserConfigurator, new AdaptedMetacard(metacard), os);

            ByteArrayInputStream bais = new ByteArrayInputStream(os.toByteArray());
            return new BinaryContentImpl(bais, MIME_TYPE);
        } catch (ParserException e) {
            throw new CatalogTransformerException("Failed XML Transformation", e);
        }
    }
}
