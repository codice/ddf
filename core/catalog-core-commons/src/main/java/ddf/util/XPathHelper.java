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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.apache.xml.serializer.OutputPropertiesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSException;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author Ashraf Barakat, rodgersh
 * 
 */
public class XPathHelper {
    private static final String UTF_8 = "UTF-8";

    /** The XML document being worked on by this XPathHelper utility class. */
    private Document document;

    /** The Logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(XPathHelper.class);

    private final DocumentBuilderFactory dbf;

    private final TransformerFactory tf;

    public XPathHelper() {
        dbf = DocumentBuilderFactory.newInstance(
                org.apache.xerces.jaxp.DocumentBuilderFactoryImpl.class.getName(), this.getClass()
                        .getClassLoader());
        tf = TransformerFactory.newInstance(org.apache.xalan.processor.TransformerFactoryImpl.class
                .getName(), this.getClass().getClassLoader());
    }

    /**
     * @param document
     *            - To parse
     */
    public XPathHelper(Document document) // throws NullPointerException
    {
        // Hugh Rodgers - 6/7/2011
        // This code seemed to work except it was not reliable for namespace aware
        // xpath expressions. The String constructor always worked, hence the new code
        // to always use the String constructor.

        // this.document = (Document) document.cloneNode(true);
        // this.document.getDocumentElement().normalize();

        this(xmlToString(document));
    }

    public XPathHelper(Document document, boolean cloneAndNormalize) // throws NullPointerException
    {
        this();

        if (cloneAndNormalize) {
            this.document = (Document) document.cloneNode(true);
            this.document.getDocumentElement().normalize();
        } else {
            this.document = document;
        }
    }

    /**
     * @param xmlText
     */
    public XPathHelper(String xmlText) {
        this();

        InputSource is = new InputSource(new StringReader(xmlText));

        dbf.setNamespaceAware(true);

        DocumentBuilder builder;
        org.w3c.dom.Document doc = null;
        try {
            Thread thread = Thread.currentThread();
            ClassLoader loader = thread.getContextClassLoader();
            thread.setContextClassLoader(XPathHelper.class.getClassLoader());

            try {
                builder = dbf.newDocumentBuilder();
                doc = builder.parse(is);
                doc.getDocumentElement().normalize();
                this.document = doc;
            } finally {
                thread.setContextClassLoader(loader);
            }
        } catch (ParserConfigurationException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (SAXException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Convenience method for evaluating xpath expressions that uses as default for its
     * {@link NamespaceContext}, the {@link NamespaceResolver} class
     * 
     * @param xpathExpressionKey
     * @return a String of the matched xpath evaluation
     * @throws XPathExpressionException
     */
    public String evaluate(String xpathExpressionKey) throws XPathExpressionException {
        return (String) this.evaluate(xpathExpressionKey, XPathConstants.STRING,
                XPathCache.getNamespaceResolver());
    }

    /**
     * @param xpathExpressionKey
     * @param nsContext
     * @return
     * @throws XPathExpressionException
     */
    public String evaluate(String xpathExpressionKey, NamespaceContext nsContext)
        throws XPathExpressionException // , NullPointerException
    {
        return (String) this.evaluate(xpathExpressionKey, XPathConstants.STRING, nsContext);
    }

    /**
     * Convenience method for evaluating xpaths that uses as default for its
     * {@link NamespaceContext}, the {@link NamespaceResolver} class. Allows you to also change the
     * how the type is returned.
     * 
     * @param xpathExpression
     * @param returnType
     * @throws XPathExpressionException
     */
    public Object evaluate(String xpathExpressionKey, QName returnType)
        throws XPathExpressionException {
        return this.evaluate(xpathExpressionKey, returnType, XPathCache.getNamespaceResolver());
    }

    /**
     * @param xpathExpression
     * @param returnType
     * @param nsContext
     * @return
     * @throws XPathExpressionException
     */
    public synchronized Object evaluate(String xpathExpressionKey, QName returnType,
            NamespaceContext nsContext) throws XPathExpressionException {
        XPathCache.getXPath().setNamespaceContext(nsContext);

        XPathExpression compiledExpression = XPathCache.getCompiledExpression(xpathExpressionKey);

        Thread thread = Thread.currentThread();
        ClassLoader loader = thread.getContextClassLoader();
        thread.setContextClassLoader(this.getClass().getClassLoader());

        try {
            return compiledExpression.evaluate(document, returnType);
        } finally {
            thread.setContextClassLoader(loader);
        }
    }

    /**
     * Prints a given node as a String. This is a convenience method that uses the default character
     * encoding.
     * 
     * @param n
     *            - the node to print as a String
     * @return the Node as a String, null if an exception is thrown or null is passed in.
     */
    public static String print(Node n) {
        return print(n, UTF_8);
    }

    /**
     * Prints a given node as a String
     * 
     * @param n
     *            - the node to print as a String
     * @param encoding
     *            - the character encoding to use for the returned String
     * @return the Node as a String, null if an exception is thrown or null is passed in.
     */
    public static String print(Node n, String encoding) {
        if (n == null) {
            return null;
        }

        try {
            Document document = null;

            if (n instanceof Document) {
                document = (Document) n;
            } else {
                document = n.getOwnerDocument();
            }
            StringWriter stringOut = new StringWriter();

            DOMImplementationLS domImpl = (DOMImplementationLS) document.getImplementation();
            LSSerializer serializer = domImpl.createLSSerializer();
            LSOutput lsOut = domImpl.createLSOutput();
            lsOut.setEncoding(encoding);
            lsOut.setCharacterStream(stringOut);

            serializer.write(n, lsOut);

            return stringOut.toString();
        } catch (DOMException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (LSException e) {
            LOGGER.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     * @param xmlDeclaration
     * @param indent
     * @return
     */
    public String print(String xmlDeclaration, String indent) {
        Transformer serializer;
        try {
            serializer = tf.newTransformer();
            serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, xmlDeclaration);
            serializer.setOutputProperty(OutputKeys.INDENT, indent);
            serializer.setOutputProperty(OutputPropertiesFactory.S_KEY_CONTENT_HANDLER,
                    org.apache.xml.serializer.ToXMLStream.class.getName());
            StringWriter writer = new StringWriter();
            serializer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.toString();
        } catch (TransformerConfigurationException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (TransformerFactoryConfigurationError e) {
            LOGGER.error(e.getMessage(), e);
        } catch (TransformerException e) {
            LOGGER.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     * Retrieve the XML document being worked on by this XPathHelper utility class.
     * 
     * @return the XML document
     */
    public Document getDocument() {
        return document;
    }

    /**
     * Convert an XML Node to a string representation.
     * 
     * @param node
     *            the XML node to be converted
     * 
     * @return the string representation of the XML node
     */
    public static String xmlToString(Node node) {
        return print(node);
    }

}
