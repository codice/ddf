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

package org.codice.ddf.spatial.ogc.csw.catalog.common.source.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.CswTransactionRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.TransactionRequestConverter;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.io.xml.Xpp3Driver;

public class CswTransactionRequestWriter implements MessageBodyWriter<CswTransactionRequest> {

    private Converter delegatingTransformer;

    public CswTransactionRequestWriter(Converter delegatingTransformer) {
        this.delegatingTransformer = delegatingTransformer;
    }

    @Override
    public boolean isWriteable(Class<?> aClass, Type type, Annotation[] annotations,
            MediaType mediaType) {
        return CswTransactionRequest.class.isAssignableFrom(aClass);
    }

    @Override
    public long getSize(CswTransactionRequest cswTransactionRequest, Class<?> aClass, Type type,
            Annotation[] annotations, MediaType mediaType) {
        return 0;
    }

    @Override
    public void writeTo(CswTransactionRequest cswTransactionRequest, Class<?> aClass, Type type,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> multivaluedMap, OutputStream outputStream)
            throws IOException, WebApplicationException {
        XStream xStream = new XStream(new Xpp3Driver());
        xStream.registerConverter(new TransactionRequestConverter(delegatingTransformer));
        xStream.alias("csw:" + CswConstants.TRANSACTION, CswTransactionRequest.class);

        xStream.toXML(cswTransactionRequest, outputStream);
    }
}
