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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FunctionFilterNode implements FilterNode {

  private static final Logger LOGGER = LoggerFactory.getLogger(FunctionFilterNode.class);

  private static final ThreadLocal<ValueVisitor> VALUE_VISITOR_THREAD_LOCAL =
      ThreadLocal.withInitial(ValueVisitor::new);

  @JsonProperty("type")
  private final String operator;

  /** If changed, update the {@link FilterNodeValueSerializer} as well. */
  private String value;

  @SuppressWarnings("FieldCanBeLocal" /* field needed for json serialization */)
  @JsonProperty("property")
  private Map<String, Object> functionProperties;

  public FunctionFilterNode(FilterNode node, Map<String, Object> functionProperties) {
    notNull(node);
    notNull(functionProperties);

    this.operator = node.getOperator();

    ValueVisitor valueVisitor = VALUE_VISITOR_THREAD_LOCAL.get().clearValue();
    node.accept(valueVisitor);
    this.value = valueVisitor.getValue().orElse(null);

    this.functionProperties = functionProperties;
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
    return functionProperties;
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
    public void visit(IntermediateFilterNode filterNode) {
      LOGGER.debug(
          "Attempting to get the value off an IntermediateFilterNode, which doesn't support value attributes.");
    }

    @Override
    public void visit(LeafFilterNode filterNode) {
      value = filterNode.getValue();
    }

    @Override
    public void visit(FunctionFilterNode filterNode) {
      value = filterNode.getValue();
    }

    private Optional<String> getValue() {
      return Optional.ofNullable(value);
    }

    private ValueVisitor clearValue() {
      value = null;
      return this;
    }
  }
}
