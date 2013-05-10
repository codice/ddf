/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.services.transformer.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.TransformerFactory;

import org.apache.log4j.Logger;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.BinaryContentImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.services.transformer.xml.schema.Binary;
import ddf.catalog.services.transformer.xml.schema.Text;
import ddf.catalog.services.transformer.xml.schema.ObjectFactory;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;

public class XmlMetacardTransformer implements MetacardTransformer {

	private static final String TEXT_XML = "text/xml";
	private static final String CDATA_OPENING = "<![CDATA[";
	private static final String CDATA_CLOSING = "]]>";
	private static final Logger logger = Logger.getLogger(XmlMetacardTransformer.class);

	private static JAXBContext jc;
	private TransformerFactory factory;

	public XmlMetacardTransformer() {
		factory = TransformerFactory.newInstance();
	}

	@Override
	public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
			throws CatalogTransformerException {

		BinaryContent transformedContent = null;

		if (metacard == null) {
			logger.warn("Attmpted to transform null metacard");
			throw new CatalogTransformerException("Unable to transform null metacard");
		}

		Set<AttributeDescriptor> descriptors = metacard.getMetacardType().getAttributeDescriptors();

		ObjectFactory of = new ObjectFactory();
		ddf.catalog.services.transformer.xml.schema.Metacard xmlMetacard = of.createMetacard();

		for (AttributeDescriptor descriptor : descriptors) {

			AttributeFormat format = descriptor.getType().getAttributeFormat();
			String name = descriptor.getName();

			switch (format) {
			case STRING:
				Text textPayload = new Text();
				textPayload.setName(name);
				if (metacard.getAttribute(name) != null) {
					textPayload.setValue((String) metacard.getAttribute(name).getValue());
					xmlMetacard.getText().add(textPayload);
				}
				break;
			case BOOLEAN:
				textPayload = new Text();
				textPayload.setName(name);
				if (metacard.getAttribute(name) != null) {
					textPayload.setValue((String) metacard.getAttribute(name).getValue());
					xmlMetacard.getBoolean().add(textPayload);
				}
				break;
			case DATE:
				textPayload = new Text();
				textPayload.setName(name);
				if (metacard.getAttribute(name) != null) {
					textPayload.setValue((String) metacard.getAttribute(name).getValue().toString());
					xmlMetacard.getDate().add(textPayload);
				}
				break;
			case SHORT:
				textPayload = new Text();
				textPayload.setName(name);
				if (metacard.getAttribute(name) != null) {
					textPayload.setValue((String) metacard.getAttribute(name).getValue());
					xmlMetacard.getShort().add(textPayload);
				}
				break;
			case INTEGER:
				textPayload = new Text();
				textPayload.setName(name);
				if (metacard.getAttribute(name) != null) {
					textPayload.setValue((String) metacard.getAttribute(name).getValue());
					xmlMetacard.getInteger().add(textPayload);
				}
				break;
			case LONG:
				textPayload = new Text();
				textPayload.setName(name);
				if (metacard.getAttribute(name) != null) {
					textPayload.setValue((String) metacard.getAttribute(name).getValue());
					xmlMetacard.getLong().add(textPayload);
				}
				break;
			case FLOAT:
				textPayload = new Text();
				textPayload.setName(name);
				if (metacard.getAttribute(name) != null) {
					textPayload.setValue((String) metacard.getAttribute(name).getValue());
					xmlMetacard.getFloat().add(textPayload);
				}				break;
			case DOUBLE:
				textPayload = new Text();
				textPayload.setName(name);
				if (metacard.getAttribute(name) != null) {
					textPayload.setValue((String) metacard.getAttribute(name).getValue());
					xmlMetacard.getDouble().add(textPayload);
				}
				break;
			case GEOMETRY:
				textPayload = new Text();
				textPayload.setName(name);
				if (metacard.getAttribute(name) != null) {
					textPayload.setValue((String) metacard.getAttribute(name).getValue());
					xmlMetacard.getGeometry().add(textPayload);
				}
				break;
			case BINARY:
				Binary binaryPayload = new Binary();
				binaryPayload.setName(name);
				if (metacard.getAttribute(name) != null) {
					ByteArrayOutputStream finalByteArray = new ByteArrayOutputStream();
					try {
						finalByteArray.write(CDATA_OPENING.getBytes("UTF-8"));
						finalByteArray.write((byte[]) metacard.getAttribute(name).getValue());
						finalByteArray.write(CDATA_CLOSING.getBytes("UTF-8"));
					} catch (IOException e) {
						logger.error("IOException building byte array.", e);
						e.printStackTrace();
					}
					binaryPayload.setValue(finalByteArray.toByteArray());
					xmlMetacard.getBinary().add(binaryPayload);
				}
				break;
			case XML:
				textPayload = new Text();
				textPayload.setName(name);
				if (metacard.getAttribute(name) != null) {
					textPayload.setValue(CDATA_OPENING + 
							             (String) metacard.getAttribute(name).getValue() +
							             CDATA_CLOSING);
					xmlMetacard.getTextXml().add(textPayload);
				}
				break;
			case OBJECT:
				binaryPayload = new Binary();
				binaryPayload.setName(name);
				if (metacard.getAttribute(name) != null) {
					ByteArrayOutputStream finalByteArray = new ByteArrayOutputStream();
					try {
						finalByteArray.write(CDATA_OPENING.getBytes("UTF-8"));
						finalByteArray.write((byte[]) metacard.getAttribute(name).getValue());
						finalByteArray.write(CDATA_CLOSING.getBytes("UTF-8"));
					} catch (IOException e) {
						logger.error("IOException building byte array.", e);
						e.printStackTrace();
					}
					binaryPayload.setValue(finalByteArray.toByteArray());
					xmlMetacard.getObject().add(binaryPayload);
				}
				break;
			default:
				throw new RuntimeException("Attribute format is not recognized");
			}
		}

		if (jc == null) {
			try {
				jc = JAXBContext.newInstance(ddf.catalog.services.transformer.xml.schema.Metacard.class);
			} catch (JAXBException e) {
				logger.error("JAXB error: ", e);
			}
		}

		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Marshaller marshaller = jc.createMarshaller();

			Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.marshal(xmlMetacard, os);
			ByteArrayInputStream bais = new ByteArrayInputStream(os.toByteArray()); 
			transformedContent = new BinaryContentImpl(bais, new MimeType(
					TEXT_XML));
		} catch (JAXBException e) {
			logger.error("JAXB error: ", e);
		} catch (MimeTypeParseException e) {
			logger.error("MimeType Parsing error: ", e);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}

		return transformedContent;

	}
}
