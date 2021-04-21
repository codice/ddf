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
package org.codice.ddf.platform.util;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/** Utility for handling XML */
public class XMLUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(XMLUtils.class);

  private static final XMLUtils INSTANCE = new XMLUtils();

  private static final String FACTORY_NOT_WHITELISTED_MSG =
      "Factory %s is not a whitelisted implementation. Aborting attempt to load.";

  private static final String XML_SAX_FEATURES_EXTERNAL_GENERAL_ENTITIES =
      "http://xml.org/sax/features/external-general-entities";

  private static final String XML_SAX_FEATURES_EXTERNAL_PARAMETER_ENTITIES =
      "http://xml.org/sax/features/external-parameter-entities";

  private static final String APACHE_FEATURES_DISALLOW_DOCTYPE_DECL =
      "http://apache.org/xml/features/disallow-doctype-decl";

  private static final String APACHE_FEATURES_LOAD_EXTERNAL_DTD =
      "http://apache.org/xml/features/nonvalidating/load-external-dtd";

  private static final String APACHE_FEATURES_LOAD_DTD_GRAMMAR =
      "http://apache.org/xml/features/nonvalidating/load-dtd-grammar";

  private static final String SAXON_PARSER_FEATURE_BASE_URI =
      "http://saxon.sf.net/feature/parserFeature?uri=";

  static final List<String> DOCUMENT_BUILDER_FACTORY_IMPL_WHITELIST =
      ImmutableList.<String>builder()
          .add("com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl")
          .build();

  static final List<String> TRANSFORMER_FACTORY_IMPL_WHITELIST =
      ImmutableList.<String>builder()
          .add("com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl")
          .add("net.sf.saxon.TransformerFactoryImpl")
          .build();

  static final List<String> SAX_PARSER_FACTORY_IMPL_WHITELIST =
      ImmutableList.<String>builder()
          .add("com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl")
          .build();

  static final List<String> XML_READER_IMPL_WHITELIST =
      ImmutableList.<String>builder()
          .add("com.sun.org.apache.xerces.internal.parsers.SAXParser")
          .build();

  protected XMLInputFactory xmlInputFactory;

  public static synchronized XMLUtils getInstance() {
    INSTANCE.initializeXMLInputFactory();
    return INSTANCE;
  }

  /**
   * Formats XML into a String
   *
   * @param sourceXml to transform a given Source
   * @param transformProperties settings for transformer
   * @return XML string
   */
  public String format(Source sourceXml, TransformerProperties transformProperties) {
    Writer buffer = new StringWriter();
    Result streamResult = new StreamResult(buffer);
    transformation(sourceXml, transformProperties, streamResult);
    return buffer.toString();
  }

  /**
   * @param nodeXml to transform a given Node
   * @param transformerProperties settings for transformer
   * @return XML String
   */
  public String format(Node nodeXml, TransformerProperties transformerProperties) {
    return format(new DOMSource(nodeXml), transformerProperties);
  }

  /**
   * Nicely formats XML into a String
   *
   * @param sourceXml to transform a given Source
   * @return XML string
   */
  public String prettyFormat(Source sourceXml) {
    TransformerProperties transformerProperties = new TransformerProperties();
    transformerProperties.addOutputProperty(OutputKeys.INDENT, "yes");
    transformerProperties.addOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

    return format(sourceXml, transformerProperties);
  }

  /**
   * Nicely formats XML into a String
   *
   * @param nodeXml to transform a given Node
   * @return XML string
   */
  public String prettyFormat(Node nodeXml) {
    return prettyFormat(new DOMSource(nodeXml));
  }

  /**
   * Transforms XML into a Result
   *
   * @param sourceXml to transform a given Source
   * @param transformProperties settings for transformer
   * @param result Result to transform into
   * @return XML Result
   */
  public Result transform(
      Source sourceXml, TransformerProperties transformProperties, Result result) {
    transformation(sourceXml, transformProperties, result);

    return result;
  }

  /**
   * @param nodeXml to transform a given Node
   * @param transformerProperties settings for transformer
   * @param result Result to transform into
   * @return XML Result
   */
  public Result transform(
      Node nodeXml, TransformerProperties transformerProperties, Result result) {
    return transform(new DOMSource(nodeXml), transformerProperties, result);
  }

  /**
   * @param xml The XML whose root namespace you want
   * @return Root Namespace
   */
  public String getRootNamespace(String xml) {

    if (xml == null) {
      return null;
    }

    return processElements(
        xml,
        (result, xmlStreamReader) -> {
          result.set(xmlStreamReader.getNamespaceURI());
          return false;
        });
  }

  private void transformation(
      Source sourceXml, TransformerProperties transformProperties, Result result) {
    ClassLoader tccl = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(XMLUtils.class.getClassLoader());
    try {
      TransformerFactory transFactory =
          TransformerFactory.newInstance(
              TRANSFORMER_FACTORY_IMPL_WHITELIST.get(0), XMLUtils.class.getClassLoader());
      setSecureTransformerFactorySettings(transFactory);
      Transformer transformer = transFactory.newTransformer();

      for (Entry<String, String> entry : transformProperties.getOutputProperties()) {
        transformer.setOutputProperty(entry.getKey(), entry.getValue());
      }
      if (transformProperties.getErrorListener() != null) {
        transformer.setErrorListener(transformProperties.getErrorListener());
      }
      transformer.transform(sourceXml, result);
    } catch (TransformerException e) {
      LOGGER.debug("Unable to transform XML.", e);
    } finally {
      Thread.currentThread().setContextClassLoader(tccl);
    }
  }

  /**
   * Iterate through the elements of an XML document. The processor calls the processElementFunction
   * for each element. Call result.set() to change the value that will be returned.
   *
   * <p>If the function returns true, processing continues to the next element. When the last
   * element in the document is processed, the value in the result is returned.
   *
   * <p>If the lambda function returns false, processing stops. The value of the result is returned.
   *
   * <p>If the function encounters a processing exception, processing stops and null is returned.
   *
   * @param xml The XML to process
   * @param processElementFunction Function that accepts an instance of XMLStreamReader and result
   *     holder. The function must return a boolean.
   * @return <T> The result of the processing
   */
  public <T> T processElements(
      String xml, BiFunction<ResultHolder<T>, XMLStreamReader, Boolean> processElementFunction) {

    initializeXMLInputFactory();
    XMLStreamReader xmlStreamReader = null;
    ResultHolder<T> result = new ResultHolder<>();
    boolean keepProcessing = true;

    try (StringReader strReader = new StringReader(xml)) {
      synchronized (XMLUtils.class) {
        xmlStreamReader = xmlInputFactory.createXMLStreamReader(strReader);
      }
      while (keepProcessing && xmlStreamReader.hasNext()) {
        int event = xmlStreamReader.next();
        if (event == XMLStreamConstants.START_ELEMENT) {
          keepProcessing = processElementFunction.apply(result, xmlStreamReader);
        }
      }
    } catch (XMLStreamException e) {
      result.set(null);
      LOGGER.debug("{} ", XMLUtils.class.getSimpleName(), e);
    } finally {
      if (xmlStreamReader != null) {
        try {
          xmlStreamReader.close();
        } catch (XMLStreamException e) {
          // ignore
        }
      }
    }

    return result.get();
  }

  /**
   * This class is used with the processElements method. Inside the function, set the value of the
   * result holder. That value is then returned by the processElementsFunction.
   */
  public static class ResultHolder<T> {

    T value;

    public T get() {
      return value;
    }

    public void set(T value) {
      this.value = value;
    }

    public boolean isEmpty() {
      return get() == null;
    }

    public void setIfEmpty(T value) {
      if (isEmpty()) {
        set(value);
      }
    }
  }

  private void initializeXMLInputFactory() {
    if (xmlInputFactory == null) {
      xmlInputFactory = XMLInputFactory.newFactory();
    }
    setSecureXMLInputFactorySettings(xmlInputFactory);
  }

  public XMLInputFactory getSecureXmlInputFactory() {
    return xmlInputFactory;
  }

  private void setSecureXMLInputFactorySettings(XMLInputFactory xmlInputFactory) {
    xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
    xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
    xmlInputFactory.setProperty(
        XMLInputFactory.SUPPORT_DTD, Boolean.FALSE); // This disables DTDs entirely for that factory
    xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
  }

  public DocumentBuilderFactory getSecureDocumentBuilderFactory(
      String className, ClassLoader classLoader) {
    if (DOCUMENT_BUILDER_FACTORY_IMPL_WHITELIST.contains(className)) {
      DocumentBuilderFactory domFactory =
          DocumentBuilderFactory.newInstance(className, classLoader);
      setSecureDocumentBuilderSettings(domFactory);
      return domFactory;
    }
    throw new SecurityException(String.format(FACTORY_NOT_WHITELISTED_MSG, className));
  }

  public DocumentBuilderFactory getSecureDocumentBuilderFactory() {
    return getSecureDocumentBuilderFactory(
        DOCUMENT_BUILDER_FACTORY_IMPL_WHITELIST.get(0), XMLUtils.class.getClassLoader());
  }

  public DocumentBuilder getSecureDocumentBuilder(boolean namespaceAware)
      throws ParserConfigurationException {
    DocumentBuilderFactory domFactory = getSecureDocumentBuilderFactory();
    domFactory.setNamespaceAware(namespaceAware);
    return domFactory.newDocumentBuilder();
  }

  public Document parseDocument(InputStream inputStream, boolean namespaceAware)
      throws ParserConfigurationException, IOException, SAXException {
    DocumentBuilder builder = getSecureDocumentBuilder(namespaceAware);
    return builder.parse(inputStream);
  }

  /**
   * Given a DocumentBuilderFactory, sets secure features according to OWASP guidelines
   *
   * @param domFactory DocumentBuilderFactory to be configured
   */
  private void setSecureDocumentBuilderSettings(DocumentBuilderFactory domFactory) {
    try {
      domFactory.setFeature(APACHE_FEATURES_DISALLOW_DOCTYPE_DECL, true);
      domFactory.setFeature(XML_SAX_FEATURES_EXTERNAL_GENERAL_ENTITIES, false);
      domFactory.setFeature(XML_SAX_FEATURES_EXTERNAL_PARAMETER_ENTITIES, false);
      domFactory.setFeature(APACHE_FEATURES_LOAD_EXTERNAL_DTD, false);
      domFactory.setFeature(APACHE_FEATURES_LOAD_DTD_GRAMMAR, false);
      domFactory.setXIncludeAware(false);
      domFactory.setExpandEntityReferences(false);
    } catch (ParserConfigurationException e) {
      LOGGER.debug("Unable to set features on document builder.", e);
    }
  }

  public TransformerFactory getSecureXmlTransformerFactory(
      String className, ClassLoader classLoader) {
    if (TRANSFORMER_FACTORY_IMPL_WHITELIST.contains(className)) {
      TransformerFactory transformerFactory =
          TransformerFactory.newInstance(className, classLoader);
      setSecureTransformerFactorySettings(transformerFactory);
      return transformerFactory;
    }
    throw new SecurityException(String.format(FACTORY_NOT_WHITELISTED_MSG, className));
  }

  public TransformerFactory getSecureXmlTransformerFactory() {
    return getSecureXmlTransformerFactory(
        TRANSFORMER_FACTORY_IMPL_WHITELIST.get(0), XMLUtils.class.getClassLoader());
  }

  public Transformer getXmlTransformer(boolean omitXml) throws TransformerException {
    TransformerFactory transformerFactory = getSecureXmlTransformerFactory();
    Transformer transformer = transformerFactory.newTransformer();
    if (omitXml) {
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    }
    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
    return transformer;
  }

  /**
   * Given a TransformerFactory, sets secure settings according to OWASP guidelines
   *
   * @param tf TransformerFactory to be configured
   */
  private void setSecureTransformerFactorySettings(TransformerFactory tf) {
    try {
      tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      switch (tf.getClass().getName()) {
        case "net.sf.saxon.TransformerFactoryImpl":
          tf.setFeature(
              SAXON_PARSER_FEATURE_BASE_URI + XMLConstants.FEATURE_SECURE_PROCESSING, true);
          tf.setFeature(
              SAXON_PARSER_FEATURE_BASE_URI + XML_SAX_FEATURES_EXTERNAL_PARAMETER_ENTITIES, false);
          tf.setFeature(
              SAXON_PARSER_FEATURE_BASE_URI + XML_SAX_FEATURES_EXTERNAL_GENERAL_ENTITIES, false);
          break;
        default:
          tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
          tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
          break;
      }
    } catch (IllegalArgumentException | TransformerConfigurationException e) {
      LOGGER.debug("XMLUtils failed to set secure attributes on TransformerFactory.");
    }
  }

  public SAXParserFactory getSecureSAXParserFactory(String className, ClassLoader classLoader) {
    if (SAX_PARSER_FACTORY_IMPL_WHITELIST.contains(className)) {
      SAXParserFactory saxParserFactory = SAXParserFactory.newInstance(className, classLoader);
      setSecureSAXParserSettings(saxParserFactory);
      return saxParserFactory;
    }
    throw new SecurityException(String.format(FACTORY_NOT_WHITELISTED_MSG, className));
  }

  public SAXParserFactory getSecureSAXParserFactory() {
    return getSecureSAXParserFactory(
        SAX_PARSER_FACTORY_IMPL_WHITELIST.get(0), XMLUtils.class.getClassLoader());
  }

  public SAXParser getSecureSAXParser(boolean namespaceAware)
      throws ParserConfigurationException, SAXException {
    SAXParserFactory saxParserFactory = getSecureSAXParserFactory();
    saxParserFactory.setNamespaceAware(namespaceAware);
    return saxParserFactory.newSAXParser();
  }

  /**
   * Given a SAXParserFactory, sets secure settings according to OWASP guidelines
   *
   * @param saxFactory SAXParserFactory to be configured
   */
  private void setSecureSAXParserSettings(SAXParserFactory saxFactory) {
    try {
      saxFactory.setFeature(XML_SAX_FEATURES_EXTERNAL_GENERAL_ENTITIES, false);
      saxFactory.setFeature(XML_SAX_FEATURES_EXTERNAL_PARAMETER_ENTITIES, false);
      saxFactory.setFeature(APACHE_FEATURES_LOAD_EXTERNAL_DTD, false);
      saxFactory.setFeature(APACHE_FEATURES_LOAD_DTD_GRAMMAR, false);
      saxFactory.setXIncludeAware(false);

    } catch (ParserConfigurationException
        | SAXNotSupportedException
        | SAXNotRecognizedException e) {
      LOGGER.debug("Unable to set features on sax parser factory.", e);
    }
  }

  public XMLReader getSecureXmlParser(String className) throws SAXException {
    if (XML_READER_IMPL_WHITELIST.contains(className)) {
      XMLReader xmlParser = XMLReaderFactory.createXMLReader(className);
      setSecureXMLReaderSettings(xmlParser);
      return xmlParser;
    }
    throw new SecurityException(String.format(FACTORY_NOT_WHITELISTED_MSG, className));
  }

  public XMLReader getSecureXmlParser() throws SAXException {
    return getSecureXmlParser(XML_READER_IMPL_WHITELIST.get(0));
  }

  /**
   * Given an XMLReader, sets secure settings according to OWASP guidelines
   *
   * @param xmlReader XMLReader to be configured
   * @throws SAXException Thrown if the XMLReader was unable to be properly configured
   */
  private void setSecureXMLReaderSettings(XMLReader xmlReader) throws SAXException {
    xmlReader.setFeature(XML_SAX_FEATURES_EXTERNAL_GENERAL_ENTITIES, false);
    xmlReader.setFeature(XML_SAX_FEATURES_EXTERNAL_PARAMETER_ENTITIES, false);
    xmlReader.setFeature(APACHE_FEATURES_LOAD_EXTERNAL_DTD, false);
  }
}
