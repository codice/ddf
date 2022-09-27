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
import java.io.InputStream;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.CswTransactionRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.CswRecordConverter;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.TransactionRequestConverter;

public class TransactionMessageBodyReader {
  private Converter cswRecordConverter;

  private MetacardType metacardType;

  private AttributeRegistry registry;

  public TransactionMessageBodyReader(
      Converter converter, MetacardType metacardType, AttributeRegistry registry) {
    this.cswRecordConverter = converter;
    this.metacardType = metacardType;
    this.registry = registry;
  }

  public CswTransactionRequest readFrom(InputStream inputStream) {
    XStream xStream = new XStream(new Xpp3Driver(new NoNameCoder()));
    xStream.addPermission(NoTypePermission.NONE);
    TransactionRequestConverter transactionRequestConverter =
        new TransactionRequestConverter(cswRecordConverter, registry);
    transactionRequestConverter.setCswRecordConverter(new CswRecordConverter(metacardType));
    xStream.registerConverter(transactionRequestConverter);
    xStream.allowTypeHierarchy(CswTransactionRequest.class);
    xStream.alias("csw:" + CswConstants.TRANSACTION, CswTransactionRequest.class);
    xStream.alias(CswConstants.TRANSACTION, CswTransactionRequest.class);
    return (CswTransactionRequest) xStream.fromXML(inputStream);
  }
}
