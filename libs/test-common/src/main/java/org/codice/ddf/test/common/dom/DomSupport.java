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
package org.codice.ddf.test.common.dom;

import java.util.function.Predicate;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** This class provides support for handling DOM trees while testing. */
public class DomSupport {
  private DomSupport() {}

  /**
   * Returns a new predicate that will report <code>true</code> if any of the nodes in the hierarchy
   * of the tested node yields <code>true</code> when tested with the provided predicate.
   *
   * @param predicate the predicate to test with
   * @return a new predicate that will test the given predicate against all nodes in the hierarchy
   *     of its tested node until one report <code>true</code>
   */
  public static Predicate<Node> anyNodesInTheHierarchy(Predicate<Node> predicate) {
    return n -> DomSupport.anyNodeInTheHierarchyMatches(n, predicate);
  }

  /**
   * Checks the given node's complete hierarchy (i.e. the node itself, its ancestors, and its
   * lineage) to see if the provided predicate reports <code>true</code> for at least one node. This
   * method will stop searching as soon as the predicate reports <code>true</code> for any node in
   * the hierarchy.
   *
   * @param node the node to test the predicate against its hierarchy
   * @param predicate the predicate to test with
   * @return <code>true</code> if the predicate reports <code>true</code> for at least one node;
   *     <code>false</code> otherwise
   */
  public static boolean anyNodeInTheHierarchyMatches(Node node, Predicate<Node> predicate) {
    return predicate.test(node)
        || DomSupport.anyNodesInTheAncestryMatches(node, predicate)
        || DomSupport.anyNodesInTheLineageMatches(node, predicate);
  }

  /**
   * Checks the given node's ancestors to see if the provided predicate reports <code>true</code>
   * for at least one node. This method will stop searching as soon as the predicate reports <code>
   * true</code> for any node in the ancestry.
   *
   * @param node the node to test the predicate against its ancestry
   * @param predicate the predicate to test with
   * @return <code>true</code> if the predicate reports <code>true</code> for at least one node;
   *     <code>false</code> otherwise
   */
  public static boolean anyNodesInTheAncestryMatches(Node node, Predicate<Node> predicate) {
    final Node parent = node.getParentNode();

    return (parent != null)
        && (predicate.test(parent) || DomSupport.anyNodesInTheAncestryMatches(parent, predicate));
  }

  /**
   * Checks the given node's descendants to see if the provided predicate reports <code>true</code>
   * for at least one node. This method will stop searching as soon as the predicate reports <code>
   * true</code> for any node in the lineage.
   *
   * @param node the node to test the predicate against its lineage
   * @param predicate the predicate to test with
   * @return <code>true</code> if the predicate reports <code>true</code> for at least one node;
   *     <code>false</code> otherwise
   */
  public static boolean anyNodesInTheLineageMatches(Node node, Predicate<Node> predicate) {
    final NodeList list = node.getChildNodes();

    // first do a quick check of all siblings before going in depth
    for (int i = 0; i < list.getLength(); i++) {
      final Node child = list.item(i);

      if (predicate.test(child)) {
        return true;
      }
    }
    for (int i = 0; i < list.getLength(); i++) {
      if (DomSupport.anyNodesInTheLineageMatches(list.item(i), predicate)) {
        return true;
      }
    }
    return false;
  }
}
