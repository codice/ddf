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

import static org.apache.commons.lang3.Validate.notNull;

import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.boon.json.annotations.JsonProperty;
import org.codice.ddf.catalog.ui.forms.api.FilterNode;

public class FunctionFilterNode implements FilterNode {

  @JsonProperty("type")
  private final String operator;

  /** If changed, update the {@link FilterNodeValueSerializer} as well. */
  private String value;

  @SuppressWarnings("FieldCanBeLocal" /* field needed for json serialization */)
  @JsonProperty("property")
  private Map<String, Object> templateProperties;

  public FunctionFilterNode(FilterNode node, Map<String, Object> templateProperties) {
    notNull(node);
    notNull(templateProperties);

    this.operator = node.getOperator();

    ValueVisitor valueVisitor = new ValueVisitor();
    node.accept(valueVisitor);
    this.value = valueVisitor.getValue().orElse(null);

    this.templateProperties = templateProperties;
  }

  @Override
  public String getOperator() {
    return operator;
  }

  @Override
  public void accept(Visitor visitor) {
    visitor.visit(this);
  }

  public Map<String, Object> getFunctionArguments() {
    return templateProperties;
  }

  @Nullable
  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    notNull(value);
    this.value = value;
  }

  private static class ValueVisitor implements Visitor {

    private String value;

    @Override
    public void visit(IntermediateFilterNode filterNode) {}

    @Override
    public void visit(LeafFilterNode filterNode) {
      value = filterNode.getValue();
    }

    @Override
    public void visit(FunctionFilterNode filterNode) {
      value = filterNode.getValue();
    }

    public Optional<String> getValue() {
      return Optional.ofNullable(value);
    }
  }
}
