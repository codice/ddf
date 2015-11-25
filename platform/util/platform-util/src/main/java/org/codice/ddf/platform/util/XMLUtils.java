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

import java.io.StringWriter;
import java.io.Writer;
import java.util.Map.Entry;

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

    private static void transformation(Source sourceXml, TransformerProperties transformProperties,
            Result result) {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(XMLUtils.class.getClassLoader());
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
            LOGGER.warn("Unable to transform XML.", e);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }
}