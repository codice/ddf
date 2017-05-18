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

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

/**
 * Simpler helper class for XPath searches. Use with the XmlDocument helper class.
 * <p>
 * The purpose is to reduce the amount of duplicated code used in the test classes.
 * <p>
 * This class is not thread-safe. It not optimized-- it creates new factories and
 * builders for every invocation. It is not suitable for use in production code.
 * <p>
 * This class does not catch errors. It passes errors up the call chain to provide developers with
 * the most information possible when debugging test cases.
 * <p>
 * For XML related test assertions, use an org.custommonkey.xmlunit.XMLAssert methods like
 * assertXpathEvaluatesTo().
 * <p>
 * For XML related test assertions in REST Assured responses,
 * use the Hamcrest matcher method hasXpath().
 */
public class XmlSearch {

    /**
     * @param xPathExpression string representation of an XPath expression
     * @param xml             string representation of an XML document
     * @return string representation of query result
     * @throws Exception
     */
    public static String evaluate(String xPathExpression, String xml) throws Exception {
        return evaluate(xPathExpression, xml, true);
    }

    /**
     * @param xPathExpression  string representation of an XPath expression
     * @param xml              string representation of an XML document
     * @param isNamespaceAware true if xpath expression uses namespaces, other wise pass false.
     *                         For example, if the XPath query "/csw:GetRecordsResponse", pass true.
     *                         If the Xpath query is "/GetRecordsResponse", pass false.
     * @return string representation of query result
     * @throws Exception
     */
    public static String evaluate(String xPathExpression, String xml, boolean isNamespaceAware)
            throws Exception {
        return compile(xPathExpression).evaluate(XmlDocument.build(xml, isNamespaceAware));
    }

    /**
     * @param xPathExpression string representation of an xpath expression
     * @return instance of XPathExpression
     * @throws Exception
     */
    public static XPathExpression compile(String xPathExpression) throws Exception {
        return getXPath().compile(xPathExpression);
    }

    protected static XPath getXPath() {
        return XPathFactory.newInstance()
                .newXPath();
    }

}