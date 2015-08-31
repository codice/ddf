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
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.CswTransactionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.io.xml.Xpp3Driver;

/**
 */
public class TransactionMessageBodyReader implements MessageBodyReader<CswTransactionRequest> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(TransactionMessageBodyReader.class);

    private Converter cswRecordConverter;

    public TransactionMessageBodyReader(Converter converter) {
        this.cswRecordConverter = converter;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        return CswTransactionRequest.class.isAssignableFrom(type);
    }

    @Override
    public CswTransactionRequest readFrom(Class<CswTransactionRequest> aClass, Type type,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> multivaluedMap, InputStream inputStream)
            throws IOException, WebApplicationException {
        XStream xStream = new XStream(new Xpp3Driver());
        xStream.registerConverter(new TransactionRequestConverter(cswRecordConverter));
        xStream.alias("csw:" + CswConstants.TRANSACTION, CswTransactionRequest.class);
        xStream.alias(CswConstants.TRANSACTION, CswTransactionRequest.class);
        return (CswTransactionRequest) xStream.fromXML(inputStream);
    }
}