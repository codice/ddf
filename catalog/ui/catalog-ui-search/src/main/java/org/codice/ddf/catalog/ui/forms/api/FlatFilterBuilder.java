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

/**
 * The traditional {@link ddf.catalog.filter.FilterBuilder} interface allows the construction of
 * Filter 1.1 compliant data through a fluent-API, consisting of multiple interfaces, which provide
 * compile-time validation of the built data.
 *
 * <p>For using the builder in a visitor pattern, this approach can be cumbersome, and requires the
 * caching of various APIs since the data isn't known until it is visited. The {@link
 * FlatFilterBuilder} API allows any element to be built from the top level interface at the cost of
 * compile-time safety. Incorrect use of a {@link FlatFilterBuilder} is reported at runtime using
 * {@link IllegalStateException}s.
 *
 * <p>{@link FlatFilterBuilder}s are only used once, then discarded. Once {@link #getResult()} is
 * invoked, no more builder methods can be invoked. {@link #getResult()} can be called multiple
 * times without issue, but no further mutation of the data structure is permitted.
 *
 * <p>New implementations of {@link FlatFilterBuilder} should register themselves as part of the
 * parameterized {@code FlatFilterBuilderTest} to ensure the implementation conforms.
 *
 * @param <T> the object type to return upon which the filter is built.
 */
public interface FlatFilterBuilder<T> {

  T getResult();

  FlatFilterBuilder beginBinaryLogicType(String operator);

  FlatFilterBuilder endBinaryLogicType();

  FlatFilterBuilder beginBinaryComparisonType(String operator);

  FlatFilterBuilder beginPropertyIsLikeType(String operator, boolean matchCase);

  FlatFilterBuilder beginBinaryTemporalType(String operator);

  FlatFilterBuilder beginBinarySpatialType(String operator);

  FlatFilterBuilder beginNilType();

  FlatFilterBuilder addFunctionType(String functionName, List<Object> args);

  FlatFilterBuilder addBetweenType(String property, Number lower, Number upper);

  FlatFilterBuilder endTerminalType();

  FlatFilterBuilder setProperty(String property);

  FlatFilterBuilder setProperty(String functionName, List<Object> args);

  FlatFilterBuilder setValue(String value);

  FlatFilterBuilder setDistance(Double distance);

  FlatFilterBuilder setTemplatedValues(Map<String, Object> templateProps);
}
