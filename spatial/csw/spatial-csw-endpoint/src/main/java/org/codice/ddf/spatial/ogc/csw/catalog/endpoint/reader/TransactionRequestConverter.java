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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.reader;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.CswTransactionRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.InsertTransaction;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;

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

        cswTransactionRequest.setVersion(reader.getAttribute("version"));
        cswTransactionRequest.setService(reader.getAttribute("service"));
        cswTransactionRequest.setVerbose(Boolean.valueOf(reader.getAttribute("verboseResponse")));
        while (reader.hasMoreChildren()) {
            reader.moveDown();

            if (reader.getNodeName().contains("Insert")) {
                String typeName = StringUtils
                        .defaultIfEmpty(reader.getAttribute("typeName"), CswConstants.CSW_RECORD);
                String handle = StringUtils.defaultIfEmpty(reader.getAttribute("handle"), "");
                List<Metacard> metacards = new ArrayList<>();
                // Loop through the <SearchResults>, converting each <csw:Record> into a Metacard
                while (reader.hasMoreChildren()) {
                    reader.moveDown(); // move down to the <csw:Record> tag
                    String name = reader.getNodeName();
                    Metacard metacard = (Metacard) context
                            .convertAnother(null, MetacardImpl.class, cswRecordConverter);
                    if (metacard != null) {
                        metacards.add(metacard);
                    }

                    // move back up to the <SearchResults> parent of the <csw:Record> tags
                    reader.moveUp();
                }
                cswTransactionRequest
                        .setInsertTransaction(new InsertTransaction(typeName, handle, metacards));
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
