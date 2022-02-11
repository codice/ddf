/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.reader;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.xml.Xpp3Driver;
import com.thoughtworks.xstream.security.NoTypePermission;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.MetacardType;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import org.codice.ddf.spatial.ogc.csw.catalog.actions.CswTransactionRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.api.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.CswTransactionRequestImpl;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.CswRecordConverter;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.TransactionRequestConverter;

@Provider
@Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
public class TransactionMessageBodyReader implements MessageBodyReader<CswTransactionRequest> {
  private Converter cswRecordConverter;

  private MetacardType metacardType;

  private AttributeRegistry registry;

  public TransactionMessageBodyReader(
      Converter converter, MetacardType metacardType, AttributeRegistry registry) {
    this.cswRecordConverter = converter;
    this.metacardType = metacardType;
    this.registry = registry;
  }

  @Override
  public boolean isReadable(
      Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return CswTransactionRequest.class.isAssignableFrom(type);
  }

  @Override
  public CswTransactionRequest readFrom(
      Class<CswTransactionRequest> aClass,
      Type type,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, String> multivaluedMap,
      InputStream inputStream)
      throws IOException, WebApplicationException {
    XStream xStream = new XStream(new Xpp3Driver(new NoNameCoder()));
    xStream.addPermission(NoTypePermission.NONE);
    TransactionRequestConverter transactionRequestConverter =
        new TransactionRequestConverter(cswRecordConverter, registry);
    transactionRequestConverter.setCswRecordConverter(new CswRecordConverter(metacardType));
    xStream.registerConverter(transactionRequestConverter);
    xStream.allowTypeHierarchy(CswTransactionRequestImpl.class);
    xStream.alias("csw:" + CswConstants.TRANSACTION, CswTransactionRequestImpl.class);
    xStream.alias(CswConstants.TRANSACTION, CswTransactionRequestImpl.class);
    return (CswTransactionRequest) xStream.fromXML(inputStream);
  }
}
