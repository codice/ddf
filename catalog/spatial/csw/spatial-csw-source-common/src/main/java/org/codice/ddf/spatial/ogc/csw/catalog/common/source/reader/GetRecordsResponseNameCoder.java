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
package org.codice.ddf.spatial.ogc.csw.catalog.common.source.reader;

import com.thoughtworks.xstream.io.naming.NameCoder;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;

/**
 * Strips unknown namespace prefixes for GetRecordsResponse nodes so the XStream alias for
 * CswRecordCollection will match.
 */
public class GetRecordsResponseNameCoder implements NameCoder {
  @Override
  public String encodeNode(String name) {
    return name;
  }

  @Override
  public String encodeAttribute(String name) {
    return name;
  }

  @Override
  public String decodeNode(String nodeName) {
    if (nodeName.endsWith(CswConstants.NAMESPACE_DELIMITER + CswConstants.GET_RECORDS_RESPONSE)) {
      return CswConstants.GET_RECORDS_RESPONSE;
    } else {
      return nodeName;
    }
  }

  @Override
  public String decodeAttribute(String attributeName) {
    return attributeName;
  }
}
