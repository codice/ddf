package org.codice.ddf.catalog.ui.forms.model;

import static org.apache.commons.lang3.Validate.notNull;

import javax.annotation.Nullable;
import org.boon.json.annotations.JsonProperty;
import org.codice.ddf.catalog.ui.forms.api.FilterNode;

/** Represents a comparison between a simple property and value. */
public class LeafFilterNode implements FilterNode {
  @JsonProperty("type")
  private final String operator;

  private String property;

  /** If changed, update the {@link FilterNodeValueSerializer} as well. */
  private String value;

  public LeafFilterNode(String operator) {
    notNull(operator);
    this.operator = operator;

    this.property = null;
    this.value = null;
  }

  @Override
  public String getOperator() {
    return operator;
  }

  @Override
  public void accept(Visitor visitor) {
    visitor.visit(this);
  }

  /**
   * If this node is a terminal node, fetch the property name associated with this node.
   *
   * @return a property name, or null if the value has not been set.
   * @throws IllegalStateException if this node is not a terminal node.
   */
  @Nullable
  public String getProperty() {
    return property;
  }

  @Nullable
  public String getValue() {
    return value;
  }

  public void setProperty(String property) {
    notNull(property);
    this.property = property;
  }

  public void setValue(String value) {
    notNull(value);
    this.value = value;
  }
}
