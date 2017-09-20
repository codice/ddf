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
package org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.source.reader;

import javax.ws.rs.ext.Provider;
import org.codice.ddf.spatial.ogc.wfs.catalog.source.reader.XmlSchemaMessageBodyReader;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants;

@Provider
public class XmlSchemaMessageBodyReaderWfs20 extends XmlSchemaMessageBodyReader {

  public XmlSchemaMessageBodyReaderWfs20() {
    super();
    wfsUriResolver.setGmlNamespace(Wfs20Constants.GML_3_2_NAMESPACE);
    wfsUriResolver.setWfsNamespace(Wfs20Constants.WFS_2_0_NAMESPACE);
  }
}
