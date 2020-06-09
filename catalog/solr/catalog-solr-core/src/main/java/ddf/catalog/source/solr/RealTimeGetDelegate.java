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
package ddf.catalog.source.solr;

import ddf.catalog.data.Metacard;
import ddf.catalog.filter.impl.SimpleFilterDelegate;
import java.util.List;

/**
 * When a {@link FilterAdapter} visits a {@link org.opengis.filter.Filter} with this delegate it
 * will return a boolean indicating whether this query should be performed as a real time get. We
 * only want to do a real time get on queries for metacards with specific ids. Here are some
 * examples:
 *
 * <pre>
 * id = 123 -> should return true
 * (id =123) AND (name = foo) -> true
 * (id = 123) OR ((ID =456) AND (name = foo)) -> true
 * (id = 123) OR (name = foo) -> false
 * NOT (id =123) -> false
 * </pre>
 */
public class RealTimeGetDelegate extends SimpleFilterDelegate<Boolean> {

  @Override
  public <S> Boolean defaultOperation(
      Object property, S literal, Class<S> literalClass, Enum operation) {
    return false;
  }

  @Override
  public Boolean and(List<Boolean> operands) {
    // "id =" queries can be ANDed with another query and still be a real time get so return true if
    // at least one of the operands contains an "id =" (is true).
    return operands.contains(Boolean.TRUE);
  }

  @Override
  public Boolean or(List<Boolean> operands) {
    // We don't want to do a real time get on a filter that contains an "id =" filter ORed with a
    // filter that doesn't specify an id. If that happens, the operand that doesn't
    // specify an id will be ignored. So if one of the operands doesn't contain an "id =" filter (is
    // false)
    // we'll return false.
    return !operands.contains(Boolean.FALSE);
  }

  @Override
  public Boolean propertyIsEqualTo(String propertyName, String literal, boolean isCaseSensitive) {
    return Metacard.ID.equals(propertyName);
  }
}
