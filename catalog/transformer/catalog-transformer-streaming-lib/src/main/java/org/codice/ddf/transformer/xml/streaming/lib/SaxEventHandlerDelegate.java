/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.transformer.xml.streaming.lib;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Validation;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.input.TeeInputStream;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.platform.util.XMLUtils;
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

/**
 * This class is used by the {@link XmlInputTransformer} to delegate SAX Parse events to the
 * relevant handlers. Its primary method is {@link SaxEventHandlerDelegate#read} which takes in an
 * {@link InputStream} returns a {@link Metacard}, populated with all the {@link Attribute}s parsed
 * by the {@link SaxEventHandlerDelegate#eventHandlers}
 */
public class SaxEventHandlerDelegate extends DefaultHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(SaxEventHandlerDelegate.class);

  private static final XMLUtils XML_UTILS = XMLUtils.getInstance();

  private XMLReader parser;

  private List<SaxEventHandler> eventHandlers = new ArrayList<>();

  private InputTransformerErrorHandler inputTransformerErrorHandler;

  private SaxEventHandlerUtils saxEventHandlerUtils = new SaxEventHandlerUtils();

  public SaxEventHandlerDelegate() {
    try {
      parser = XML_UTILS.getSecureXmlParser();
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
   * Takes in an {@link InputStream} returns a {@link Metacard}, populated with all the {@link
   * Attribute}s parsed by the {@link SaxEventHandlerDelegate#eventHandlers}
   *
   * @param inputStream an XML document that can be parsed into a Metacard
   * @return a {@link Metacard}, populated with all the {@link Attribute}s parsed by the {@link
   *     SaxEventHandlerDelegate#eventHandlers}
   * @throws CatalogTransformerException
   */
  public SaxEventHandlerDelegate read(InputStream inputStream) throws CatalogTransformerException {

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
    } catch (IOException | SAXException e) {
      throw new CatalogTransformerException("Could not properly parse metacard", e);
    }

    return this;
  }

  public Metacard getMetacard(String id) {
    MetacardType metacardType = getMetacardType(id);

    if (metacardType == null) {
      metacardType = MetacardImpl.BASIC_METACARD;
      LOGGER.debug("No metacard type found. Defaulting to Basic Metacard.");
    }
    /*
     * Create a new MetacardImpl with the proper MetacardType
     */
    Metacard metacard = new MetacardImpl(metacardType);
    /*
     * If any major errors occur during parsing, print them out and add them to the metacard as validation errors
     */
    InputTransformerErrorHandler inputTransformerErrorHandler =
        (InputTransformerErrorHandler) parser.getErrorHandler();
    if (inputTransformerErrorHandler != null) {
      String parseWarningsErrors = inputTransformerErrorHandler.getParseWarningsErrors();
      if (StringUtils.isNotBlank(parseWarningsErrors)) {
        LOGGER.debug(parseWarningsErrors);

        List<Serializable> warningsAndErrors = new ArrayList<>();
        warningsAndErrors.add(parseWarningsErrors);
        if (metacard.getAttribute(Validation.VALIDATION_ERRORS) != null) {
          List<Serializable> values =
              metacard.getAttribute(Validation.VALIDATION_ERRORS).getValues();
          if (values != null) {
            warningsAndErrors.addAll(values);
          }
        }

        metacard.setAttribute(
            new AttributeImpl(
                Validation.VALIDATION_ERRORS, Collections.unmodifiableList(warningsAndErrors)));
      }
    }

    /*
     * Populate metacard with all attributes constructed in SaxEventHandlers during parsing
     */
    Map<String, Boolean> multiValuedMap =
        saxEventHandlerUtils.getMultiValuedNameMap(metacardType.getAttributeDescriptors());
    for (SaxEventHandler eventHandler : eventHandlers) {
      List<Attribute> attributes = eventHandler.getAttributes();
      for (Attribute attribute : attributes) {

        /*
         * If metacard already has values in the attribute, skip the attribute,
         * instead of simply overwriting the existing values.
         */
        if (metacard.getAttribute(attribute.getName()) == null) {
          metacard.setAttribute(attribute);
        } else if (multiValuedMap.getOrDefault(attribute.getName(), false)) {
          metacard.getAttribute(attribute.getName()).getValues().addAll(attribute.getValues());
        }
      }
    }
    return metacard;
  }

  /**
   * Takes in a sax event from {@link SaxEventHandlerDelegate#parser} and passes it to the {@link
   * SaxEventHandlerDelegate#eventHandlers}
   */
  @Override
  public void startDocument() throws SAXException {
    for (SaxEventHandler transformer : eventHandlers) {
      transformer.startDocument();
    }
  }

  /**
   * Takes in a sax event from {@link SaxEventHandlerDelegate#parser} and passes it to the {@link
   * SaxEventHandlerDelegate#eventHandlers}
   */
  @Override
  public void endDocument() throws SAXException {
    for (SaxEventHandler transformer : eventHandlers) {
      transformer.endDocument();
    }
  }

  /**
   * Takes in a sax event from {@link SaxEventHandlerDelegate#parser} and passes it to the {@link
   * SaxEventHandlerDelegate#eventHandlers}
   *
   * @param uri the URI that is passed in by {@link SaxEventHandlerDelegate}
   * @param localName the localName that is passed in by {@link SaxEventHandlerDelegate}
   * @param qName the qName that is passed in by {@link SaxEventHandlerDelegate}
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
   * Takes in a sax event from {@link SaxEventHandlerDelegate#parser} and passes it to the {@link
   * SaxEventHandlerDelegate#eventHandlers}
   *
   * @param ch the ch that is passed in by {@link SaxEventHandlerDelegate}
   * @param start the start that is passed in by {@link SaxEventHandlerDelegate}
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
   * Takes in a sax event from {@link SaxEventHandlerDelegate#parser} and passes it to the {@link
   * SaxEventHandlerDelegate#eventHandlers}
   *
   * @param namespaceURI the namespaceURI that is passed in by {@link SaxEventHandlerDelegate}
   * @param localName the localName that is passed in by {@link SaxEventHandlerDelegate}
   * @param qName the qName that is passed in by {@link SaxEventHandlerDelegate}
   * @throws SAXException
   */
  @Override
  public void endElement(String namespaceURI, String localName, String qName) throws SAXException {

    for (SaxEventHandler transformer : eventHandlers) {
      transformer.endElement(namespaceURI, localName, qName);
    }
  }

  /**
   * Takes in a sax event from {@link SaxEventHandlerDelegate#parser} and passes it to the {@link
   * SaxEventHandlerDelegate#eventHandlers}
   *
   * @param prefix the prefix that is passed in by {@link SaxEventHandlerDelegate}
   * @param uri the uri that is passed in by {@link SaxEventHandlerDelegate}
   * @throws SAXException
   */
  @Override
  public void startPrefixMapping(String prefix, String uri) throws SAXException {
    for (SaxEventHandler transformer : eventHandlers) {
      transformer.startPrefixMapping(prefix, uri);
    }
  }

  @Override
  public void endPrefixMapping(String prefix) throws SAXException {
    for (SaxEventHandler transformer : eventHandlers) {
      transformer.endPrefixMapping(prefix);
    }
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

  /**
   * Defines and returns a {@link DynamicMetacardType} based on component Sax Event Handlers and
   * what attributes they populate
   *
   * @return a DynamicMetacardType that describes the type of metacard that is created in this
   *     transformer
   */
  public MetacardType getMetacardType(String id) {
    Set<AttributeDescriptor> attributeDescriptors =
        new HashSet<>(MetacardImpl.BASIC_METACARD.getAttributeDescriptors());

    attributeDescriptors.addAll(
        eventHandlers.stream()
            .map(SaxEventHandler::getSupportedAttributeDescriptors)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet()));

    return new DynamicMetacardType(attributeDescriptors, id);
  }
}

/**
 * A private class used to handle errors that occur during SAX Parsing. It allows all the errors
 * that occur during the parsing of a single document to be easily and succinctly logged at the end
 * of parsing.
 */
class InputTransformerErrorHandler implements ErrorHandler {
  private StringBuilder outWriter;

  /**
   * A helper method to help parse relevant information from the exception
   *
   * @param exception An exception that was passed into the {@link InputTransformerErrorHandler} by
   *     the parser
   * @return a string of relevant information about the exception
   */
  private String getParseExceptionInfo(SAXParseException exception) {
    String systemId = exception.getSystemId();

    if (systemId == null) {
      systemId = "null";
    }

    return "URI=" + systemId + " Line=" + exception.getLineNumber() + ": " + exception.getMessage();
  }

  /**
   * Takes in an warning exception thrown by the parser and writes relevant information about it to
   * BufferedWriter
   *
   * @param exception an exception thrown by the parser
   * @throws SAXException
   */
  @Override
  public void warning(SAXParseException exception) throws SAXException {
    outWriter.append("Warning: ").append(getParseExceptionInfo(exception)).append('\n');
  }

  /**
   * Takes in an error exception thrown by the parser and writes relevant information about it to
   * BufferedWriter
   *
   * @param exception an exception thrown by the parser
   * @throws SAXException
   */
  @Override
  public void error(SAXParseException exception) throws SAXException {
    outWriter.append("Error: ").append(getParseExceptionInfo(exception)).append('\n');
  }

  /**
   * Takes in a fatalError exception thrown by the parser and writes relevant information about it
   * to BufferedWriter Also, throws a new exception, because SAX parsing can not continue after a
   * Fatal Error.
   *
   * @param exception an exception thrown by the parser
   * @throws SAXException
   */
  @Override
  public void fatalError(SAXParseException exception) throws SAXException {
    String message = "Fatal Error: " + getParseExceptionInfo(exception);
    outWriter.append(message).append('\n');
    throw new SAXException(message);
  }

  /**
   * Gets the String value of the outWriter and resets the writer.
   *
   * @return a String containing a log of relevant information about warnings and errors that
   *     occurred during parsing
   */
  public String getParseWarningsErrors() {
    String returnString = outWriter.toString().trim();
    outWriter.setLength(0);
    return returnString;
  }

  public InputTransformerErrorHandler configure(StringBuilder outWriter) {
    this.outWriter = outWriter;
    return this;
  }
}
