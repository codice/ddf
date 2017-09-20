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
package ddf.catalog.filter.proxy.builder;

import ddf.catalog.filter.AttributeBuilder;
import ddf.catalog.filter.ExpressionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GeotoolsAttributeBuilder extends GeotoolsExpressionBuilder
    implements AttributeBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(GeotoolsAttributeBuilder.class);

  GeotoolsAttributeBuilder(String attribute) {
    setAttribute(attribute);
    setOperator(Operator.EQ);
  }

  /*
   * (non-Javadoc)
   *
   * @see ddf.catalog.filter.AttributeBuilder#is()
   */
  @Override
  public ExpressionBuilder is() {
    LOGGER.debug("is: operator: {}, attribute: {}", getOperator(), getAttribute());
    return this;
  }
}
