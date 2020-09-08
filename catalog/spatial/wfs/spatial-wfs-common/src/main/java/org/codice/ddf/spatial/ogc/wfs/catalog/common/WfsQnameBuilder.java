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
package org.codice.ddf.spatial.ogc.wfs.catalog.common;

import javax.xml.namespace.QName;
import org.apache.commons.lang.StringUtils;

public class WfsQnameBuilder {

  private WfsQnameBuilder() {}

  public static QName buildQName(String metacardTypeName, String contentTypeName) {
    if (StringUtils.isEmpty(metacardTypeName) || StringUtils.isEmpty(contentTypeName)) {
      return null;
    }
    // ensure no "illegal" characters are used
    metacardTypeName = metacardTypeName.replace(":", "_");
    contentTypeName = contentTypeName.replace(":", "_");

    if (contentTypeName.contains(" ")) {
      contentTypeName = contentTypeName.replaceAll(" ", "");
    }

    // Build the QName to uniquely identify this content type
    String namespace;
    String prefix;
    if (metacardTypeName.equals(contentTypeName)) {
      prefix = contentTypeName;
      namespace = WfsConstants.NAMESPACE_URN_ROOT + metacardTypeName;
    } else {
      prefix = metacardTypeName + "." + contentTypeName;
      namespace = WfsConstants.NAMESPACE_URN_ROOT + metacardTypeName + "." + contentTypeName;
    }

    QName qname = new QName(namespace, contentTypeName, prefix);
    return qname;
  }
}
