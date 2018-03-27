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
package org.codice.ddf.catalog.ui.forms.model.pojo;

/**
 * Provides data model pojo that can be annotated and sent to Boon for JSON serialization.
 *
 * <p>{@link FilterTemplatedLeafNode} represents a {@link FilterLeafNode} whose value is not yet
 * known, but can be plugged in later. This includes fields for a default value if none is provided,
 * an ID to correlate sources of information to nodes that want them, and some access controls.
 *
 * <p><i>This code is experimental. While it is functional and tested, it may change or be removed
 * in a future version of the library.</i>
 */
public class FilterTemplatedLeafNode extends FilterLeafNode {
  // If changed, update the FilterNodeValueSerializer as well
  private String defaultValue;

  private String nodeId;

  private boolean isVisible;

  private boolean isReadOnly;

  public FilterTemplatedLeafNode(String type) {
    super(type);
    setTemplated(true);
    setDefaultValue(null);
    setNodeId(null);
    setVisible(true);
    setReadOnly(false);
  }

  public FilterTemplatedLeafNode(
      FilterLeafNode node,
      String defaultValue,
      String nodeId,
      boolean isVisible,
      boolean isReadOnly) {
    super(node);
    setTemplated(true);
    setDefaultValue(defaultValue);
    setNodeId(nodeId);
    setVisible(isVisible);
    setReadOnly(isReadOnly);
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public String getNodeId() {
    return nodeId;
  }

  public void setNodeId(String nodeId) {
    this.nodeId = nodeId;
  }

  public boolean isVisible() {
    return isVisible;
  }

  public void setVisible(boolean visible) {
    isVisible = visible;
  }

  public boolean isReadOnly() {
    return isReadOnly;
  }

  public void setReadOnly(boolean readOnly) {
    isReadOnly = readOnly;
  }
}
