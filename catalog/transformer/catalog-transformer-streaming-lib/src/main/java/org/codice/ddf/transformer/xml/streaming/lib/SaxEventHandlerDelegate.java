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
package org.codice.ddf.transformer.xml.streaming.lib;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.input.TeeInputStream;
import org.codice.ddf.transformer.xml.streaming.SaxEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.transform.CatalogTransformerException;

/**
 * This class is used by the {@link XmlInputTransformer} to delegate SAX Parse events to the relevant handlers.
 * Its primary method is {@link SaxEventHandlerDelegate#read} which takes in an {@link InputStream} returns a {@link Metacard},
 * populated with all the {@link Attribute}s parsed by the {@link SaxEventHandlerDelegate#eventHandlers}
 */
public class SaxEventHandlerDelegate extends DefaultHandler {

    private XMLReader parser;

    private List<SaxEventHandler> eventHandlers = new ArrayList<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(SaxEventHandlerDelegate.class);

    private MetacardType metacardType = BasicTypes.BASIC_METACARD;

    private InputTransformerErrorHandler inputTransformerErrorHandler;

    public SaxEventHandlerDelegate() {
        try {
            parser = XMLReaderFactory.createXMLReader();
        } catch (Exception e) {
            LOGGER.debug(
                    "Exception thrown during creation of SaxEventHandlerDelegate. Probably caused by one of the setFeature calls",
                    e);
        }
    }

    public SaxEventHandlerDelegate(List<SaxEventHandler> eventHandlers) {
        this();
        this.eventHandlers = eventHandlers;
    }

    /**
     * Takes in an {@link InputStream} returns a {@link Metacard},
     * populated with all the {@link Attribute}s parsed by the {@link SaxEventHandlerDelegate#eventHandlers}
     *
     * @param inputStream an XML document that can be parsed into a Metacard
     * @return a {@link Metacard},
     * populated with all the {@link Attribute}s parsed by the {@link SaxEventHandlerDelegate#eventHandlers}
     * @throws CatalogTransformerException
     */
    public Metacard read(InputStream inputStream) throws CatalogTransformerException {

        /*
         * Create a new MetacardImpl with the proper MetacardType
         */
        Metacard metacard = new MetacardImpl(metacardType);

        try {
            InputSource newStream = new InputSource(new BufferedInputStream(inputStream));

            /*
             * Set the parser's ContentHandler to this delegate, which ensures the delegate receives all
             * parse events that should be handled by a SaxEventHandler (startElement, endElement, characters, startPrefixMapping, etc)
             * Set the parser's ErrorHandler to be a new InputTransformerHandler
             */
            parser.setContentHandler(this);
            InputTransformerErrorHandler inputTransformerErrorHandler =
                    getInputTransformerErrorHandler().configure(new StringBuilder());
            parser.setErrorHandler(inputTransformerErrorHandler);
            parser.parse(newStream);

            /*
             * If any major errors occur during parsing, print them out and add them to the metacard as validation errors
             */
            List<Serializable> errorsAndWarnings =
                    Arrays.asList(inputTransformerErrorHandler.getParseWarningsErrors());
            if (!((String) errorsAndWarnings.get(0)).isEmpty()) {
                LOGGER.warn((String) errorsAndWarnings.get(0));
                Attribute attr;
                List<Serializable> values;
                if ((attr = metacard.getAttribute(BasicTypes.VALIDATION_ERRORS)) != null
                        && (values = attr.getValues()) != null) {
                    errorsAndWarnings.addAll(values);
                }
                metacard.setAttribute(new AttributeImpl(BasicTypes.VALIDATION_ERRORS,
                        errorsAndWarnings));
            }

        } catch (IOException | SAXException e) {
            LOGGER.debug("Exception thrown during parsing of inputStream", e);
            throw new CatalogTransformerException("Could not properly parse metacard", e);
        }

        /*
         * Populate metacard with all attributes constructed in SaxEventHandlers during parsing
         */
        for (SaxEventHandler eventHandler : eventHandlers) {
            List<Attribute> attributes = eventHandler.getAttributes();
            for (Attribute attribute : attributes) {
                Attribute tmpAttr;

                /*
                 * If metacard already has values in the attribute, put them together into a multivalued list,
                 * instead of simply overwriting the existing values.
                 */
                if ((tmpAttr = metacard.getAttribute(attribute.getName())) != null) {
                    List<Serializable> tmpAttrValues = tmpAttr.getValues();

                    tmpAttrValues.addAll(attribute.getValues());
                    tmpAttr = new AttributeImpl(attribute.getName(), tmpAttrValues);
                    metacard.setAttribute(tmpAttr);

                } else {
                    metacard.setAttribute(attribute);
                }
            }
        }
        return metacard;
    }

    /**
     * Takes in a sax event from {@link SaxEventHandlerDelegate#parser}
     * and passes it to the {@link SaxEventHandlerDelegate#eventHandlers}
     */
    @Override
    public void startDocument() throws SAXException {
        for (SaxEventHandler transformer : eventHandlers) {
            transformer.startDocument();
        }
    }

    /**
     * Takes in a sax event from {@link SaxEventHandlerDelegate#parser}
     * and passes it to the {@link SaxEventHandlerDelegate#eventHandlers}
     *
     * @param uri        the URI that is passed in by {@link SaxEventHandlerDelegate}
     * @param localName  the localName that is passed in by {@link SaxEventHandlerDelegate}
     * @param qName      the qName that is passed in by {@link SaxEventHandlerDelegate}
     * @param attributes the attributes that are passed in by {@link SaxEventHandlerDelegate}
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        for (SaxEventHandler transformer : eventHandlers) {
            transformer.startElement(uri, localName, qName, attributes);
        }

    }

    /**
     * Takes in a sax event from {@link SaxEventHandlerDelegate#parser}
     * and passes it to the {@link SaxEventHandlerDelegate#eventHandlers}
     *
     * @param ch     the ch that is passed in by {@link SaxEventHandlerDelegate}
     * @param start  the start that is passed in by {@link SaxEventHandlerDelegate}
     * @param length the length that is passed in by {@link SaxEventHandlerDelegate}
     * @throws SAXException
     */
    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
        for (SaxEventHandler transformer : eventHandlers) {
            transformer.characters(ch, start, length);
        }
    }

    /**
     * Takes in a sax event from {@link SaxEventHandlerDelegate#parser}
     * and passes it to the {@link SaxEventHandlerDelegate#eventHandlers}
     *
     * @param namespaceURI the namespaceURI that is passed in by {@link SaxEventHandlerDelegate}
     * @param localName    the localName that is passed in by {@link SaxEventHandlerDelegate}
     * @param qName        the qName that is passed in by {@link SaxEventHandlerDelegate}
     * @throws SAXException
     */
    @Override
    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException {

        for (SaxEventHandler transformer : eventHandlers) {
            transformer.endElement(namespaceURI, localName, qName);
        }

    }

    /**
     * Takes in a sax event from {@link SaxEventHandlerDelegate#parser}
     * and passes it to the {@link SaxEventHandlerDelegate#eventHandlers}
     *
     * @param prefix the prefix that is passed in by {@link SaxEventHandlerDelegate}
     * @param uri    the uri that is passed in by {@link SaxEventHandlerDelegate}
     * @throws SAXException
     */
    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        for (SaxEventHandler transformer : eventHandlers) {
            transformer.startPrefixMapping(prefix, uri);
        }
    }

    public SaxEventHandlerDelegate setMetacardType(MetacardType metacardType) {
        this.metacardType = metacardType;
        return this;
    }

    public TeeInputStream getMetadataStream(InputStream inputStream, OutputStream outputStream) {
        return new TeeInputStream(inputStream, outputStream);
    }

    InputTransformerErrorHandler getInputTransformerErrorHandler() {
        if (this.inputTransformerErrorHandler == null) {
            this.inputTransformerErrorHandler = new InputTransformerErrorHandler();
        }
        return this.inputTransformerErrorHandler;
    }
}

/**
 * A private class used to handle errors that occur during SAX Parsing. It allows all the errors
 * that occur during the parsing of a single document to be easily and succinctly logged at the end
 * of parsing.
 */
class InputTransformerErrorHandler implements ErrorHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(InputTransformerErrorHandler.class);

    private StringBuilder outWriter;

    /**
     * A helper method to help parse relevant information from the exception
     *
     * @param exception An exception that was passed into the {@link InputTransformerErrorHandler} by the parser
     * @return a string of relevant information about the exception
     */
    private String getParseExceptionInfo(SAXParseException exception) {
        String systemId = exception.getSystemId();

        if (systemId == null) {
            systemId = "null";
        }

        return "URI=" + systemId + " Line=" + exception.getLineNumber() + ": "
                + exception.getMessage();
    }

    /**
     * Takes in an warning exception thrown by the parser and writes relevant information about it to BufferedWriter
     *
     * @param exception an exception thrown by the parser
     * @throws SAXException
     */
    @Override
    public void warning(SAXParseException exception) throws SAXException {
        outWriter.append("Warning: " + getParseExceptionInfo(exception) + '\n');
    }

    /**
     * Takes in an error exception thrown by the parser and writes relevant information about it to BufferedWriter
     *
     * @param exception an exception thrown by the parser
     * @throws SAXException
     */
    @Override
    public void error(SAXParseException exception) throws SAXException {
        outWriter.append("Error: " + getParseExceptionInfo(exception) + '\n');
    }

    /**
     * Takes in a fatalError exception thrown by the parser and writes relevant information about it to BufferedWriter
     * Also, throws a new exception, because SAX parsing can not continue after a Fatal Error.
     *
     * @param exception an exception thrown by the parser
     * @throws SAXException
     */
    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
        String message = "Fatal Error: " + getParseExceptionInfo(exception);
        outWriter.append(message + '\n');
        throw new SAXException(message);
    }

    /**
     * Gets the String value of the outWriter and resets the writer.
     *
     * @return a String containing a log of relevant information about warnings and errors that occured
     * during parsing
     */
    public String getParseWarningsErrors() {
        String returnString = outWriter.toString()
                .trim();
        outWriter.setLength(0);
        return returnString;
    }

    public InputTransformerErrorHandler configure(StringBuilder outWriter) {
        this.outWriter = outWriter;
        return this;
    }
}
