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
package org.codice.ddf.spatial.ogc.csw.catalog.common.source.writer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.io.xml.Xpp3Driver;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import org.codice.ddf.spatial.ogc.csw.catalog.actions.CswTransactionRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.api.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.CswTransactionRequestImpl;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.TransactionRequestConverter;

public class CswTransactionRequestWriter implements MessageBodyWriter<CswTransactionRequest> {

  private XStream xStream;

  public CswTransactionRequestWriter(Converter delegatingTransformer) {
    xStream = new XStream(new Xpp3Driver());
    xStream.allowTypesByWildcard(new String[] {"ddf.**", "org.codice.**"});
    xStream.registerConverter(new TransactionRequestConverter(delegatingTransformer, null));
    xStream.alias(CswConstants.CSW_TRANSACTION, CswTransactionRequestImpl.class);
  }

  @Override
  public boolean isWriteable(
      Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
    return CswTransactionRequestImpl.class.isAssignableFrom(aClass);
  }

  @Override
  public long getSize(
      CswTransactionRequest cswTransactionRequest,
      Class<?> aClass,
      Type type,
      Annotation[] annotations,
      MediaType mediaType) {
    return 0;
  }

  @Override
  public void writeTo(
      CswTransactionRequest cswTransactionRequest,
      Class<?> aClass,
      Type type,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, Object> multivaluedMap,
      OutputStream outputStream)
      throws IOException, WebApplicationException {
    xStream.toXML(cswTransactionRequest, outputStream);
  }
}
