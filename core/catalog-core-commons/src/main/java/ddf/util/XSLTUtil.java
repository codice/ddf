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
package ddf.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public final class XSLTUtil {
    private static final DocumentBuilderFactory DBF = DocumentBuilderFactory.newInstance(
            org.apache.xerces.jaxp.DocumentBuilderFactoryImpl.class.getName(),
            org.apache.xerces.jaxp.DocumentBuilderFactoryImpl.class.getClassLoader());

    private static final Logger LOGGER = LoggerFactory.getLogger(XSLTUtil.class);

    private XSLTUtil() {
        throw new UnsupportedOperationException(
                "This is a utility class - it should never be instantiated");
    }

    /**
     * Performs an xsl transformation against an XML document
     * 
     * @param template
     *            The compiled XSL template to be run
     * @param xmlDoc
     *            xml document to be transformed
     * @param xslProperties
     *            default classification
     * @return the transformed document.
     * @throws TransformerException
     */
    public static Document transform(Templates template, Document xmlDoc,
            Map<String, Object> parameters) throws TransformerException {
        ByteArrayOutputStream baos;
        ByteArrayInputStream bais = null;
        Document resultDoc;
        try {
            Transformer transformer = template.newTransformer();

            DBF.setNamespaceAware(true);
            DocumentBuilder builder = DBF.newDocumentBuilder();
            StreamResult resultOutput = null;
            Source source = new DOMSource(xmlDoc);
            baos = new ByteArrayOutputStream();
            try {
                resultOutput = new StreamResult(baos);
                if (parameters != null && !parameters.isEmpty()) {
                    for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                        LOGGER.debug("Adding parameter key: {} value: {}", entry.getKey(), entry.getValue());
                        String key = entry.getKey();
                        Object value = entry.getValue();
                        if (key != null && !key.isEmpty() && value != null) {
                            transformer.setParameter(key, value);
                        } else {
                            LOGGER.debug("Null or empty value for parameter: {}", entry.getKey());

                        }
                    }
                } else {
                    LOGGER.warn("All properties were null.  Using \"last-resort\" defaults: U, USA, MTS");
                }

                transformer.transform(source, resultOutput);
                bais = new ByteArrayInputStream(baos.toByteArray());
                resultDoc = builder.parse(bais);
            } finally {
                IOUtils.closeQuietly(bais);
                IOUtils.closeQuietly(baos);
            }

            return resultDoc;
        } catch (TransformerException e) {
            LOGGER.warn(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            LOGGER.warn(e.getMessage(), e);
            throw new TransformerException("Error while transforming document: " + e.getMessage(),
                    e);
        }
    }

}
