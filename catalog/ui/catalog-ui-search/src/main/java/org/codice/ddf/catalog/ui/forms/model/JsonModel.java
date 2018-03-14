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
package org.codice.ddf.catalog.ui.forms.model;

import static org.codice.ddf.catalog.ui.util.AccessUtils.safeGet;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.boon.json.annotations.JsonIgnore;
import org.boon.json.annotations.JsonProperty;

/**
 * Provides data model pojos that can be annotated and sent to Boon for JSON serialization.
 *
 * <p>{@link CommonTemplate} is the base used for transporting template data completely separate
 * from any filter structures that may be needed. These properties apply to both query templates and
 * result templates.
 *
 * <p>{@link FieldFilter}, also known as "detail level", specifies a result template, which is a
 * list of attribute descriptor names that represent the fields that should be visible while
 * browsing metacards after a query is executed.
 *
 * <p>{@link FormTemplate} specifies a query template, which is a special type of filter whose nodes
 * and expressions may be annotated with additional data. Contains a {@link FilterNode} which is the
 * root of the filter tree.
 *
 * <p>{@link FilterNode} is the simplest representation of a filter node and the top of the node
 * inheritence hierarchy. It almost always contains child nodes in {@link FilterNode#getNodes()}
 * because it typically represents logical operations. Currently {@link FilterNode#isLeaf()} means
 * it's safe to cast to {@link FilterLeafNode} but that may not hold true in the future.
 *
 * <p>{@link FilterLeafNode} represents basic terminal nodes such as comparisons, and supports all
 * node types that reference a property and a value. {@link FilterLeafNode#getNodes()} should always
 * return null because this class of node should not have children.
 *
 * <p>This type of node, along with subclasses, support additional fields to be attached to the node
 * beyond the UI's defaults. For example, {@link FilterLeafNode#templated} helps the UI determine if
 * additional processing is necessary because additional template data is present. Currently {@link
 * FilterLeafNode#isTemplated()} means it's safe to cast to {@link FilterTemplatedLeafNode} but that
 * may not hold true in the future.
 *
 * <p>{@link FilterTemplatedLeafNode} represents a {@link FilterLeafNode} whose value is not yet
 * known, but can be plugged in later. This includes fields for a default value if none is provided,
 * an ID to correlate sources of information to nodes that want them, and some access controls.
 *
 * <p><i>This code is experimental. While it is functional and tested, it may change or be removed
 * in a future version of the library.</i>
 */
public class JsonModel {
  private JsonModel() {
    // Should not be instantiated
  }

  public static class CommonTemplate {
    private final String id;

    private final String title;

    private final String description;

    private final Date created;

    private final String owner;

    public CommonTemplate(Metacard metacard) {
      this.id = safeGet(metacard, Core.ID, String.class);
      this.title = safeGet(metacard, Core.TITLE, String.class);
      this.description = safeGet(metacard, Core.DESCRIPTION, String.class);
      this.created = safeGet(metacard, Core.CREATED, Date.class);
      this.owner = safeGet(metacard, Core.METACARD_OWNER, String.class);
    }

    public String getId() {
      return id;
    }

    public String getTitle() {
      return title;
    }

    public String getDescription() {
      return description;
    }

    public Date getCreated() {
      return created;
    }

    public String getOwner() {
      return owner;
    }
  }

  public static class FieldFilter extends CommonTemplate {
    @SuppressWarnings("squid:S1068" /* Needed for serialization */)
    private final Set<String> descriptors;

    public FieldFilter(Metacard metacard, Set<String> descriptors) {
      super(metacard);
      this.descriptors = descriptors;
    }
  }

  public static class FormTemplate extends CommonTemplate {
    @JsonProperty("filterTemplate")
    private FilterNode root;

    public FormTemplate(Metacard metacard, FilterNode root) {
      super(metacard);
      this.root = root;
    }

    public FilterNode getRoot() {
      return root;
    }
  }

  public static class FilterNode {
    // The operator for this node represented as a string
    private final String type;

    @JsonIgnore private boolean isLeaf;

    @JsonProperty("filters")
    private List<FilterNode> nodes;

    public FilterNode(String type, List<FilterNode> nodes) {
      this.type = type;
      this.nodes = nodes;
      this.isLeaf = false;
    }

    public String getType() {
      return type;
    }

    public boolean isLeaf() {
      return isLeaf;
    }

    @Nullable
    public List<FilterNode> getNodes() {
      return nodes;
    }

    protected void setLeaf(boolean isLeaf) {
      this.isLeaf = isLeaf;
    }

    public void setNodes(List<FilterNode> nodes) {
      this.nodes = nodes;
    }
  }

  public static class FilterLeafNode extends FilterNode {
    private String property;

    // If changed, update the FilterNodeValueSerializer as well
    private String value;

    private boolean templated;

    public FilterLeafNode(String type) {
      super(type, null);
      setLeaf(true);
      setTemplated(false);
    }

    public FilterLeafNode(FilterLeafNode node) {
      super(node.getType(), null);
      setLeaf(true);
      setProperty(node.getProperty());
      setValue(node.getValue());
      setTemplated(node.isTemplated());
    }

    public String getProperty() {
      return property;
    }

    public String getValue() {
      return value;
    }

    public boolean isTemplated() {
      return templated;
    }

    public void setProperty(String property) {
      this.property = property;
    }

    public void setValue(String value) {
      this.value = value;
    }

    public void setTemplated(boolean templated) {
      this.templated = templated;
    }
  }

  public static class FilterTemplatedLeafNode extends FilterLeafNode {
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
}
