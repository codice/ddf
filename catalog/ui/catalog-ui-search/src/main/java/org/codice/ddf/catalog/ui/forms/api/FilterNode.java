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
package org.codice.ddf.catalog.ui.forms.api;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Represents a single node in a filter data structure.
 *
 * <p><i>This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library.</i>
 */
public interface FilterNode {

  /**
   * Determine if this filter node is terminal. Terminal filter nodes will safely return a property
   * name and a target value.
   *
   * @return true if this node is terminal, false otherwise.
   */
  boolean isLeaf();

  /**
   * Determine if this filter node is templated. Templated filter nodes must be terminal. Templated
   * filter nodes will safely return a map of template properties.
   *
   * @return true if this node is templated, false otherwise.
   */
  boolean isTemplated();

  /**
   * Determine if this filter node is a function.
   *
   * @return true if this node is a function, false otherwise.
   */
  boolean isFunction();

  /**
   * Get the operator assocated with this node. Every node has an operator.
   *
   * @return this node's operator.
   */
  String getOperator();

  /**
   * If this node is a non-terminal node, fetch its list of children.
   *
   * @return a collection of filter nodes.
   * @throws IllegalStateException if this node was a terminal node and does not have children.
   * @throws IllegalArgumentException if invalid nodes were encountered while reading the children.
   */
  List<FilterNode> getChildren();

  /**
   * If this node is a templated node, fetch the properties associated with the template.
   *
   * @return a map of properties.
   * @throws IllegalStateException if this node is not a templated node.
   */
  Map<String, Object> getTemplateProperties();

  /**
   * If this node is a terminal node, fetch the property name associated with this node.
   *
   * @return a property name, or null if the value has not been set.
   * @throws IllegalStateException if this node is not a terminal node.
   */
  @Nullable
  String getProperty();

  /**
   * If the property itself is non-terminal, fetch the function object for the property. Does not
   * throw exceptions like the other methods.
   *
   * @return a property visitable, or null if the property is terminal.
   */
  @Nullable
  FilterNode getPropertyFunction();

  /**
   * If this node is a terminal node, fetch the target value associated with this node.
   *
   * @return the value associated with the property of this node, or null if the value has not been
   *     set.
   * @throws IllegalStateException if this node is not a terminal node.
   */
  @Nullable
  Object getValue();

  /**
   * If this node is a terminal node, fetch the distance value associated with this node.
   *
   * @return the distance, or null if the distance has not been set.
   * @throws IllegalStateException if this node is not a terminal node.
   */
  @Nullable
  Double getDistance();

  /**
   * If this node is a function node, fetch the name of the function.
   *
   * @return the name of the function.
   * @throws IllegalStateException if this node is not a function node.
   */
  @Nullable
  String getFunctionName();

  /**
   * If this node is a function node, fetch the parameters of the function.
   *
   * @return the parameters of the function.
   * @throws IllegalStateException if this node is not a function node.
   */
  @Nullable
  List<Object> getParams();

  /**
   * Set this node's property name.
   *
   * @param property the property name to use.
   * @throws NullPointerException if the given property name is null.
   */
  void setProperty(Object property);

  /**
   * Set this node's target value.
   *
   * @param value the target value to use.
   * @throws NullPointerException if the given target value is null.
   */
  void setValue(Object value);

  /**
   * Set this node's distance property, if applicable.
   *
   * @param distance the distance to use.
   */
  void setDistance(Double distance);

  /**
   * If this node is a function node, add an additional argument to the arg list.
   *
   * @param arg the argument to add.
   */
  void addArg(Object arg);
}
