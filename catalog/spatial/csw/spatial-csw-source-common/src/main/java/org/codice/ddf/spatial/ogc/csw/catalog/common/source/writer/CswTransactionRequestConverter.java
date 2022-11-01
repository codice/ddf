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
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.CswTransactionRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.TransactionRequestConverter;

public class CswTransactionRequestConverter {

  private XStream xStream;

  public CswTransactionRequestConverter(Converter delegatingTransformer) {
    xStream = new XStream(new Xpp3Driver());
    xStream.allowTypesByWildcard(new String[] {"ddf.**", "org.codice.**"});
    xStream.registerConverter(new TransactionRequestConverter(delegatingTransformer, null));
    xStream.alias(CswConstants.CSW_TRANSACTION, CswTransactionRequest.class);
  }

  public String convert(CswTransactionRequest cswTransactionRequest) {
    return xStream.toXML(cswTransactionRequest);
  }
}
