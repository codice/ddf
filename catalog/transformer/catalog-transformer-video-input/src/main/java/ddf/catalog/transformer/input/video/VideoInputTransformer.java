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
package ddf.catalog.transformer.input.video;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.ToXMLContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transformer.common.tika.MetacardCreator;
import ddf.catalog.transformer.common.tika.TikaMetadataExtractor;
import net.sf.saxon.TransformerFactoryImpl;

public class VideoInputTransformer implements InputTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(VideoInputTransformer.class);

    private Templates templates = null;

    public VideoInputTransformer() {
        ClassLoader tccl = Thread.currentThread()
                .getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            templates = TransformerFactory.newInstance(TransformerFactoryImpl.class.getName(),
                    this.getClass()
                            .getClassLoader())
                    .newTemplates(new StreamSource(TikaMetadataExtractor.class.getResourceAsStream(
                            "/metadata.xslt")));
        } catch (TransformerConfigurationException e) {
            LOGGER.debug("Couldn't create XML transformer", e);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    @Override
    public Metacard transform(InputStream input) throws IOException, CatalogTransformerException {
        return transform(input, null);
    }

    @Override
    public Metacard transform(InputStream input, String id)
            throws IOException, CatalogTransformerException {
        Parser parser = new AutoDetectParser();
        ToXMLContentHandler handler = new ToXMLContentHandler();
        TikaMetadataExtractor tikaMetadataExtractor = new TikaMetadataExtractor(parser, handler);

        Metadata metadata = tikaMetadataExtractor.parseMetadata(input, new ParseContext());

        String metadataText = handler.toString();
        if (templates != null) {
            metadataText = transformToXml(metadataText);
        }

        return MetacardCreator.createBasicMetacard(metadata, id, metadataText);
    }

    private String transformToXml(String xhtml) {
        LOGGER.debug("Transforming xhtml to xml.");
        XMLReader xmlReader = null;
        try {
            XMLReader xmlParser = XMLReaderFactory.createXMLReader();
            xmlParser.setFeature("http://xml.org/sax/features/external-general-entities", false);
            xmlParser.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            xmlParser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",
                    false);
            xmlReader = new XMLFilterImpl(xmlParser);
        } catch (SAXException e) {
            LOGGER.debug(e.getMessage(), e);
        }
        if (xmlReader != null) {
            try {
                Writer xml = new StringWriter();
                Transformer transformer = templates.newTransformer();
                transformer.transform(new SAXSource(xmlReader, new InputSource(new StringReader(xhtml))),
                        new StreamResult(xml));
                return xml.toString();
            } catch (TransformerException e) {
                LOGGER.debug("Unable to transform metadata from XHTML to XML.", e);
            }
        }
        return xhtml;
    }
}
