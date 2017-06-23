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

package org.codice.ddf.itests.common;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilderFactory;

import org.codice.ddf.platform.util.XMLUtils;
import org.w3c.dom.Document;

/**
 * Simpler helper class for creating document object models (DOM) searches.
 * The purpose is to reduce the amount of duplicated code used in the test classes.
 * <p>
 * This class is not thread-safe. It not optimized-- it creates new factories and
 * builders for every invocation. It is not suitable for use in production code.
 * <p>
 * This class does not catch errors. It passes errors up the call chain to provide developers with
 * the most information possible when debugging test cases.
 * <p>
 * To format (pretty print) XML, use the ddf.util.XPathHelper or
 * org.codice.ddf.platform.util.XMLUtils classes.
 */
public class XmlDocument {

    private static final XMLUtils XML_UTILS = XMLUtils.getInstance();

    /**
     * Create an DOM from a string representation of an XML document.
     *
     * @param input            string that represents an XML document
     * @param isNamespaceAware true or false
     * @return
     * @throws Exception
     */
    public static Document build(String input, boolean isNamespaceAware) throws Exception {
        DocumentBuilderFactory factory = XML_UTILS.getSecureDocumentBuilderFactory();
        factory.setNamespaceAware(isNamespaceAware);
        factory.setExpandEntityReferences(false);
        return factory
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Create an DOM from a string representation of an XML document.
     * The document builder is namespace aware.
     *
     * @param input string that represents an XML document
     * @return
     * @throws Exception
     */
    public static Document build(String input) throws Exception {
        return build(input, true);
    }
}
