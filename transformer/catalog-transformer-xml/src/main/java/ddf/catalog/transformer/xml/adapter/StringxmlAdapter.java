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
package ddf.catalog.transformer.xml.adapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.xml.binding.StringxmlElement;
import ddf.catalog.transformer.xml.binding.StringxmlElement.Value;

public class StringxmlAdapter extends XmlAdapter<StringxmlElement, Attribute> {

    private static final String TRANSFORMATION_FAILED_ERROR_MESSAGE = "Transformation failed.  Could not transform XML Attribute.";

    private static DocumentBuilderFactory factory;

    private static Templates templates = null;

    static {
        factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        // Create Transformer
        TransformerFactory transFactory = TransformerFactory.newInstance();
        Source xsltSource = new StreamSource(StringxmlAdapter.class.getClassLoader()
                .getResourceAsStream("stringxml.xslt"));
        try {
            templates = transFactory.newTemplates(xsltSource);
        } catch (TransformerConfigurationException e) {
        }

    }

    @Override
    public StringxmlElement marshal(Attribute attribute) throws CatalogTransformerException {
        return marshalFrom(attribute);
    }

    /**
     * @param attribute
     * @return JAXB representable attribute
     * @throws CatalogTransformerException
     */
    public static StringxmlElement marshalFrom(Attribute attribute)
        throws CatalogTransformerException {

        StringxmlElement element = new StringxmlElement();
        element.setName(attribute.getName());
        if (attribute.getValue() != null) {
            for (Serializable value : attribute.getValues()) {
                if (!(value instanceof String)) {
                    continue;
                }
                String xmlString = (String) value;
                Element anyElement = null;
                DocumentBuilder builder = null;
                try {
                    synchronized (factory) {
                        builder = factory.newDocumentBuilder();
                    }
                    anyElement = builder.parse(new ByteArrayInputStream(xmlString.getBytes(
                            StandardCharsets.UTF_8))).getDocumentElement();
                } catch (ParserConfigurationException e) {
                    throw new CatalogTransformerException(TRANSFORMATION_FAILED_ERROR_MESSAGE, e);
                } catch (SAXException e) {
                    throw new CatalogTransformerException(TRANSFORMATION_FAILED_ERROR_MESSAGE, e);
                } catch (IOException e) {
                    throw new CatalogTransformerException(TRANSFORMATION_FAILED_ERROR_MESSAGE, e);
                }
                Value anyValue = new StringxmlElement.Value();
                anyValue.setAny(anyElement);
                element.getValue().add(anyValue);
            }
        }
        return element;
    }

    @Override
    public Attribute unmarshal(StringxmlElement element) throws CatalogTransformerException,
        TransformerException, JAXBException {
        return unmarshalFrom(element);
    }

    public static Attribute unmarshalFrom(StringxmlElement element)
        throws CatalogTransformerException, TransformerException, JAXBException {
        AttributeImpl attribute = null;

        if (templates == null) {
            throw new CatalogTransformerException(
                    "Could not transform XML due to internal configuration error.");
        }

        for (Value xmlValue : element.getValue()) {

            String xmlString = "";

            Element anyNode = xmlValue.getAny();

            StringWriter buffer = new StringWriter();

            Transformer transformer = templates.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(anyNode), new StreamResult(buffer));
            xmlString = buffer.toString();

            // Document document = anyNode.getOwnerDocument();
            // DOMImplementationLS domImplLS = (DOMImplementationLS) document
            // .getImplementation();
            // LSSerializer serializer = domImplLS.createLSSerializer();
            // DOMConfiguration domConfig = serializer.getDomConfig();
            //
            // domConfig.setParameter("xml-declaration", Boolean.FALSE);
            //
            // xmlString = serializer.writeToString(anyNode);

            if (attribute == null) {
                attribute = new AttributeImpl(element.getName(), xmlString);
            } else {
                attribute.addValue(xmlString);
            }
        }
        return attribute;
    }

}