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
package org.codice.ddf.platform.util;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 * Utility for handling XML
 */
public class XMLUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(XMLUtils.class);

    protected static volatile XMLInputFactory xmlInputFactory;

    private static void initializeXMLInputFactory() {
        if (xmlInputFactory == null) {
            xmlInputFactory = XMLInputFactory.newInstance();
        }
        xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD,
                Boolean.FALSE); // This disables DTDs entirely for that factory
        xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
    }

    /**
     * Formats XML into a String
     *
     * @param sourceXml           to transform a given Source
     * @param transformProperties settings for transformer
     * @return XML string
     */
    public static String format(Source sourceXml, TransformerProperties transformProperties) {
        Writer buffer = new StringWriter();
        Result streamResult = new StreamResult(buffer);
        transformation(sourceXml, transformProperties, streamResult);
        return buffer.toString();
    }

    /**
     * @param nodeXml               to transform a given Node
     * @param transformerProperties settings for transformer
     * @return XML String
     */
    public static String format(Node nodeXml, TransformerProperties transformerProperties) {
        return format(new DOMSource(nodeXml), transformerProperties);
    }

    /**
     * Nicely formats XML into a String
     *
     * @param sourceXml to transform a given Source
     * @return XML string
     */
    public static String prettyFormat(Source sourceXml) {
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
    public static String prettyFormat(Node nodeXml) {
        return prettyFormat(new DOMSource(nodeXml));
    }

    /**
     * Transforms XML into a Result
     *
     * @param sourceXml           to transform a given Source
     * @param transformProperties settings for transformer
     * @param result              Result to transform into
     * @return XML Result
     */
    public static Result transform(Source sourceXml, TransformerProperties transformProperties,
            Result result) {
        transformation(sourceXml, transformProperties, result);

        return result;
    }

    /**
     * @param nodeXml               to transform a given Node
     * @param transformerProperties settings for transformer
     * @param result                Result to transform into
     * @return XML Result
     */
    public static Result transform(Node nodeXml, TransformerProperties transformerProperties,
            Result result) {
        return transform(new DOMSource(nodeXml), transformerProperties, result);
    }

    /**
     * @param xml The XML whose root namespace you want
     * @return Root Namespace
     */
    public static String getRootNamespace(String xml) {

        if (xml == null) {
            return null;
        }

        ProcessingResult<String> result = new ProcessingResult();
        return processElements(xml, result, (xmlStreamReader) -> {
            result.setValue(xmlStreamReader.getNamespaceURI());
            result.done();
        });
    }

    public static boolean hasNamespace(String xml, String namespace) {
        List<String> ns = new ArrayList<>();
        ProcessingResult<Boolean> result = new ProcessingResult();
        processElements(xml, result, (xmlStreamReader) -> {
            result.setValue(xmlStreamReader.getNamespaceURI()
                    .equals(namespace));
            ns.add(xmlStreamReader.getNamespaceURI());
            LOGGER.warn(xmlStreamReader.getNamespaceURI());
            //result.done();

        });

        int x = 8;
        return false;
    }

    /**
     *
     * @param xml   The XML to process
     * @param result    Instance of processing result
     * @param processElementFunction    Function that accepts an instance of XMLStreamReader
     *                                  and updates the processing result
     * @param <T> The type that of the processing result's value
     * @return Processing results's value
     */
    public static <T> T processElements(String xml, ProcessingResult<T> result,
            Consumer<XMLStreamReader> processElementFunction) {

        initializeXMLInputFactory();
        XMLStreamReader xmlStreamReader = null;

        try (StringReader strReader = new StringReader(xml)) {
            xmlStreamReader = xmlInputFactory.createXMLStreamReader(strReader);
            while (xmlStreamReader.hasNext()) {
                int event = xmlStreamReader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    processElementFunction.accept(xmlStreamReader);
                    if (result.isDone) {
                        break;
                    }
                }
            }
        } catch (XMLStreamException e) {
            LOGGER.debug(XMLUtils.class.getSimpleName() + " is unable to parse XML", e);
        } finally {
            if (xmlStreamReader != null) {
                try {
                    xmlStreamReader.close();
                } catch (XMLStreamException e) {
                    // ignore
                }
            }
        }

        return result.getValue();
    }

    private static void transformation(Source sourceXml, TransformerProperties transformProperties,
            Result result) {
        ClassLoader tccl = Thread.currentThread()
                .getContextClassLoader();
        Thread.currentThread()
                .setContextClassLoader(XMLUtils.class.getClassLoader());
        try {
            TransformerFactory transFactory = TransformerFactory.newInstance();
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
            Thread.currentThread()
                    .setContextClassLoader(tccl);
        }
    }

    // This class is used with the processElements method. Pass a function and an instance of
    // ProcessingResult into the processElements method. Inside the function, set
    // use setResult to the return value and call done() to stop processing the XML document.
    public static class ProcessingResult<T> {
        boolean isDone = false;

        T value;

        public ProcessingResult(T value) {
            this.value = value;
        }

        public ProcessingResult() {
        }

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }

        public boolean isDone() {
            return isDone;
        }

        public void setDone(boolean done) {
            isDone = done;
        }

        public void done() {
            setDone(true);
        }
    }

}