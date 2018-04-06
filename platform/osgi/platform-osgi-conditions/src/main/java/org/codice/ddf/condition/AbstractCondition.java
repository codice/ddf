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
package org.codice.ddf.condition;

import java.util.Dictionary;
import org.osgi.service.condpermadmin.Condition;

public abstract class AbstractCondition implements Condition {

  /**
   * Returns whether the evaluation must be postponed until the end of the permission check. If this
   * method returns {@code false} (or this Condition is immutable), then this Condition must be able
   * to directly answer the {@link #isSatisfied()} method. In other words, isSatisfied() will return
   * very quickly since no external sources, such as for example users or networks, need to be
   * consulted. <br>
   * This method must always return the same value whenever it is called so that the Conditional
   * Permission Admin can cache its result.
   *
   * @return {@code true} to indicate the evaluation must be postponed. Otherwise, {@code false} if
   *     the evaluation can be performed immediately.
   */
  @Override
  public boolean isPostponed() {
    return false;
  }

  /**
   * Returns whether the Condition is mutable. A Condition can go from mutable ({@code true}) to
   * immutable ({@code false}) over time but never from immutable ({@code false}) to mutable ({@code
   * true}).
   *
   * @return {@code true} {@link #isSatisfied()} can change. Otherwise, {@code false} if the value
   *     returned by {@link #isSatisfied()} will not change for this condition.
   */
  @Override
  public boolean isMutable() {
    return false;
  }

  /**
   * Returns whether the specified set of Condition objects are satisfied. Although this method is
   * not static, it must be implemented as if it were static. All of the passed Condition objects
   * will be of the same type and will correspond to the class type of the object on which this
   * method is invoked. This method must be called inside a permission check only.
   *
   * @param conditions The array of Condition objects, which must all be of the same class and
   *     mutable. The receiver must be one of those Condition objects.
   * @param context A Dictionary object that implementors can use to track state. If this method is
   *     invoked multiple times in the same permission check, the same Dictionary will be passed
   *     multiple times. The SecurityManager treats this Dictionary as an opaque object and simply
   *     creates an empty dictionary and passes it to subsequent invocations if multiple invocations
   *     are needed.
   * @return {@code true} if all the Condition objects are satisfied. Otherwise, {@code false} if
   *     one of the Condition objects is not satisfied.
   */
  @Override
  public boolean isSatisfied(Condition[] conditions, Dictionary<Object, Object> context) {
    for (Condition condition : conditions) {
      if (!condition.isSatisfied()) {
        return false;
      }
    }
    return true;
  }
}
