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
     * Transforms XML into a String
     *
     * @param sourceXml           to transform a given DOMSource
     * @param transformProperties settings for transformer
     * @return XML string
     */
    public static String transformToXml(Source sourceXml,
            TransformerProperties transformProperties) {

        Writer buffer = new StringWriter();
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(XMLUtils.class.getClassLoader());
        try {
            TransformerFactory transFactory = TransformerFactory.newInstance();
            Transformer transformer = transFactory.newTransformer();

            for (String outputProperty : transformProperties.getTransformTypes()) {
                transformer.setOutputProperty(outputProperty,
                        transformProperties.getTransformValue(outputProperty));
            }
            if (transformProperties.getErrorListener() != null) {
                transformer.setErrorListener(transformProperties.getErrorListener());
            }
            transformer.transform(sourceXml, new StreamResult(buffer));
        } catch (TransformerException e) {
            LOGGER.warn("Unable to transform XML to a String.", e);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
        return buffer.toString();
    }

    /**
     * @param nodeXml               to transform a given Node
     * @param transformerProperties settings for transformer
     * @return XML String
     */
    public static String transformToXml(Node nodeXml, TransformerProperties transformerProperties) {
        return transformToXml(new DOMSource(nodeXml), transformerProperties);
    }
}