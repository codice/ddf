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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.transformer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import org.apache.commons.lang.StringUtils;

/**
 * Manages a reference list of {@link CswActionTransformer}'s by mapping them to the QName's they
 * apply to.
 */
public class CswActionTransformerProvider {
  private Map<QName, CswActionTransformer> transformerMap = new ConcurrentHashMap<>();

  private final Map<String, QName> typeNameQNameMap = new HashMap<>();

  public synchronized void bind(CswActionTransformer cswActionTransformer) {
    if (cswActionTransformer == null) {
      return;
    }

    List<QName> namespaces = cswActionTransformer.getQNames();
    for (QName namespace : namespaces) {
      transformerMap.put(namespace, cswActionTransformer);
      List<String> typeNames = cswActionTransformer.getTypeNames();

      for (String typeName : typeNames) {
        typeNameQNameMap.put(typeName, namespace);
      }
    }
  }

  public synchronized void unbind(CswActionTransformer cswActionTransformer) {
    if (cswActionTransformer == null) {
      return;
    }

    List<QName> namespaces = cswActionTransformer.getQNames();
    for (QName namespace : namespaces) {
      transformerMap.remove(namespace);
      List<String> typeNames = cswActionTransformer.getTypeNames();
      for (String typeName : typeNames) {
        typeNameQNameMap.remove(typeName, namespace);
      }
    }
  }

  public synchronized Optional<CswActionTransformer> getTransformer(@Nullable String typeName) {
    if (StringUtils.isEmpty(typeName)) {
      return Optional.empty();
    }

    QName qName = typeNameQNameMap.get(typeName);
    if (qName == null) {
      return Optional.empty();
    }

    return Optional.ofNullable(transformerMap.get(qName));
  }

  public synchronized Optional<CswActionTransformer> getTransformer(QName qName) {
    if (qName == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(transformerMap.get(qName));
  }
}
