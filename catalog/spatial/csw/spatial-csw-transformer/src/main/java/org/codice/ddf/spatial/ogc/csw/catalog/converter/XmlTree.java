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
package org.codice.ddf.spatial.ogc.csw.catalog.converter;

import java.util.LinkedHashSet;
import java.util.Set;

public class XmlTree {

  private final Set<XmlTree> children = new LinkedHashSet<>();

  private final String node;

  XmlTree(String node) {
    this.node = node;
  }

  void accept(XstreamTreeWriter visitor) {
    visitor.startVisit(node);

    for (XmlTree child : children) {
      child.accept(visitor);
    }
    visitor.endVisit(node);
  }

  XmlTree addChild(String node) {
    for (XmlTree child : children) {
      if (child.node.equals(node)) {
        return child;
      }
    }

    return addChild(new XmlTree(node));
  }

  XmlTree addChild(XmlTree child) {
    children.add(child);
    return child;
  }
}
