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
package ddf.catalog.metacard.validation;

import ddf.catalog.filter.impl.SimpleFilterDelegate;
import java.util.List;

public class ValidationQueryDelegate extends SimpleFilterDelegate<Boolean> {

  private String validationType;

  public ValidationQueryDelegate(String validationType) {
    this.validationType = validationType;
  }
  /**
   * These three Boolean operations AND, OR, and NOT are not boolean operations in the traditional
   * sense They should return true if any of the operands are true. This is because we want the
   * delegate to return true whenever {@link #validationType} occur in the filter For example: if we
   * get a filter that is like [ validation-warnings is test ] AND [ AnyText is * ] we want {@link
   * ValidationQueryDelegate#and} to return true, so we need an or operation underneath if we get a
   * filter that is like [ validation-warnings is test ] OR [ AnyText is * ] we want {@link
   * ValidationQueryDelegate#or} to return true, so we need an or operation underneath if we get a
   * filter this is like NOT [ validation-warnings is test ] we want {@link
   * ValidationQueryDelegate#not} to return true, so we need to pass the operand as is
   */
  @Override
  public <S> Boolean defaultOperation(
      Object property, S literal, Class<S> literalClass, Enum operation) {
    return false;
  }

  @Override
  public Boolean and(List<Boolean> operands) {
    return operands.stream().reduce(false, (a, b) -> a || b);
  }

  @Override
  public Boolean or(List<Boolean> operands) {
    return operands.stream().reduce(false, (a, b) -> a || b);
  }

  @Override
  public Boolean not(Boolean operand) {
    return operand;
  }

  /*
   *The three propertyIs... methods return true if the propertyName (the property they are filtering for)
   * match validationType. This boolean is used to determine whether
   * the query needs to be modified to query for only valid metacards or not
   *
   * If the original query does not have a "true" return value from this FilterDelegate,
   * it means the query and its filters do not specify the validity of metacards they are searching for,
   * and therefore the query needs to only query for valid metacards by adding a propertyIsNull
   * filter for both VALIDATION_ERRORS and VALIDATION_WARNINGS
   */

  @Override
  public Boolean propertyIsEqualTo(String propertyName, String literal, boolean isCaseSensitive) {
    return propertyName.equals(validationType);
  }

  @Override
  public Boolean propertyIsNull(String propertyName) {
    return propertyName.equals(validationType);
  }

  @Override
  public Boolean propertyIsLike(String propertyName, String pattern, boolean isCaseSensitive) {
    return propertyName.equals(validationType);
  }

  @Override
  public Boolean propertyIsEqualTo(String functionName, List<Object> arguments, Object literal) {
    return arguments != null && arguments.stream().anyMatch(a -> a.equals(validationType));
  }
}
