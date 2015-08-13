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
 **/
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.reader;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.CswTransactionRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.DeleteTransaction;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.InsertTransaction;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.DefaultCswRecordMap;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.XStreamAttributeCopier;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.CswEndpoint;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import net.opengis.cat.csw.v_2_0_2.DeleteType;

/**
 */
public class TransactionRequestConverter implements Converter {

    private Converter cswRecordConverter;

    public TransactionRequestConverter(Converter cswInputTransformer) {
        this.cswRecordConverter = cswInputTransformer;
    }

    @Override
    public void marshal(Object o, HierarchicalStreamWriter hierarchicalStreamWriter,
            MarshallingContext marshallingContext) {

    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        CswTransactionRequest cswTransactionRequest = new CswTransactionRequest();

        cswTransactionRequest.setVersion(reader.getAttribute(CswConstants.VERSION));
        cswTransactionRequest.setService(reader.getAttribute(CswConstants.SERVICE));
        cswTransactionRequest
                .setVerbose(Boolean.valueOf(reader.getAttribute(CswConstants.VERBOSE_RESPONSE)));

        XStreamAttributeCopier.copyXmlNamespaceDeclarationsIntoContext(reader, context);

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            if (reader.getNodeName().contains("Insert")) {
                String typeName = StringUtils
                        .defaultIfEmpty(reader.getAttribute(CswConstants.TYPE_NAME_PARAMETER),
                                CswConstants.CSW_RECORD);
                String handle = StringUtils
                        .defaultIfEmpty(reader.getAttribute(CswConstants.HANDLE_PARAMETER), "");
                List<Metacard> metacards = new ArrayList<>();
                // Loop through the <SearchResults>, converting each <csw:Record> into a Metacard
                while (reader.hasMoreChildren()) {
                    reader.moveDown(); // move down to the <csw:Record> tag
                    Metacard metacard = (Metacard) context
                            .convertAnother(null, MetacardImpl.class, cswRecordConverter);
                    if (metacard != null) {
                        metacards.add(metacard);
                    }

                    // move back up to the <SearchResults> parent of the <csw:Record> tags
                    reader.moveUp();
                }
                cswTransactionRequest
                        .addInsertTransaction(new InsertTransaction(typeName, handle, metacards));
            } else if (reader.getNodeName().contains("Delete")) {
                Map<String, String> xmlnsAttributeToUriMappings = null;
                Map<String, String> prefixToUriMappings;
                Object namespaceObj = context.get(CswConstants.WRITE_NAMESPACES);
                if (namespaceObj instanceof Map<?, ?>) {
                    xmlnsAttributeToUriMappings = (Map<String, String>) namespaceObj;
                    // The xmlns attributes on the top-level Transaction element have been copied
                    // into the UnmarshallingContext by
                    // XStreamAttributeCopier.copyXmlNamespaceDeclarationsIntoContext(). The map
                    // keys are of the form "xmlns:PREFIX", which is why we don't check that there's
                    // actually a semicolon in the attribute name.
                    prefixToUriMappings = new HashMap<>();
                    for (Entry<String, String> entry : xmlnsAttributeToUriMappings.entrySet()) {
                        String xmlnsAttribute = entry.getKey();
                        String prefix = xmlnsAttribute.split(":")[1];
                        prefixToUriMappings.put(prefix, entry.getValue());
                    }
                } else {
                    prefixToUriMappings = DefaultCswRecordMap.getDefaultCswRecordMap()
                            .getPrefixToUriMapping();
                }

                StringWriter deleteTypeWriter = new StringWriter();
                XStreamAttributeCopier
                        .copyXml(reader, deleteTypeWriter, xmlnsAttributeToUriMappings);

                String deleteTypeXml = deleteTypeWriter.toString();

                JAXBElement<DeleteType> root;
                try {
                    JAXBContext jaxbContext = CswEndpoint.getJaxBContext();
                    InputStream xmlInputStream = IOUtils
                            .toInputStream(deleteTypeXml, StandardCharsets.UTF_8.name());
                    StreamSource xmlStreamSource = new StreamSource(xmlInputStream);
                    root = jaxbContext.createUnmarshaller()
                            .unmarshal(xmlStreamSource, DeleteType.class);
                } catch (IOException | JAXBException e) {
                    throw new ConversionException(e);
                }

                DeleteType deleteType = root.getValue();
                cswTransactionRequest.addDeleteTransaction(
                        new DeleteTransaction(deleteType, prefixToUriMappings));
            }
            reader.moveUp();
        }

        return cswTransactionRequest;
    }

    @Override
    public boolean canConvert(Class aClass) {
        return CswTransactionRequest.class.isAssignableFrom(aClass);
    }
}
