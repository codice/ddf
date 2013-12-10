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
package ddf.catalog.transformer.input.tika;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;

public class TikaInputTransformer implements InputTransformer {
    private static final Logger LOGGER = Logger.getLogger(TikaInputTransformer.class);

    @Override
    public Metacard transform(InputStream input) throws IOException, CatalogTransformerException {
        return transform(input, null);
    }

    @Override
    public Metacard transform(InputStream input, String uri) throws IOException,
        CatalogTransformerException {
        if (input == null) {
            throw new CatalogTransformerException("Cannot transform null input.");
        }

        MetacardImpl metacard = new MetacardImpl(BasicTypes.BASIC_METACARD);

        Metadata metadata = new Metadata();
        ContentHandler handler = new BodyContentHandler();
        AutoDetectParser parser = new AutoDetectParser();

        try {
            parser.parse(input, handler, metadata);
            String title = metadata.get(TikaCoreProperties.TITLE);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Title: " + title);
                LOGGER.debug("Creator: " + metadata.get(TikaCoreProperties.CREATOR));
                LOGGER.debug("Author: " + metadata.get(Metadata.AUTHOR));
                LOGGER.debug("Creation Date: " + metadata.get(TikaCoreProperties.CREATED));
                LOGGER.debug("Modified Date: " + metadata.get(TikaCoreProperties.MODIFIED));
                LOGGER.debug("Content Type: " + metadata.get(Metadata.CONTENT_TYPE));
                // LOGGER.debug("content: " + handler.toString());
                // int count = 1;
                // for (String stringMetadata : metadata.names())
                // {
                // LOGGER.debug("Metadata " + count + " ----> name : "
                // + stringMetadata + "&nbsp; value : " + metadata.get(stringMetadata));
                // count++;
                // }
            }

            // mc.setMetadata(convertNodeToString(getDocument(jaxbDoc)));
            if (StringUtils.isEmpty(title)) {
                title = "<No title provided>";
            }
            metacard.setTitle(title);

            Date date = javax.xml.bind.DatatypeConverter.parseDateTime(
                    metadata.get(TikaCoreProperties.CREATED)).getTime();
            metacard.setCreatedDate(date);

            date = javax.xml.bind.DatatypeConverter.parseDateTime(
                    metadata.get(TikaCoreProperties.MODIFIED)).getTime();
            metacard.setModifiedDate(date);

            // metacard.setExpirationDate(getExpirationDate(resource));
            // metacard.setEffectiveDate(getEffectiveDate(resource));
            // metacard.setLocation(getLocation(resource));
            // metacard.setSourceId(getSourceId());
            // metacard.setResourceSize(getResourceSize(resource));
            if (uri != null) {
                metacard.setResourceURI(URI.create(uri));
            } else {
                metacard.setResourceURI(null);
            }
        } catch (SAXException e) {
            LOGGER.warn(e);
            throw new CatalogTransformerException(e);
        } catch (TikaException e) {
            LOGGER.warn(e);
            throw new CatalogTransformerException(e);
        }

        return metacard;
    }

}
