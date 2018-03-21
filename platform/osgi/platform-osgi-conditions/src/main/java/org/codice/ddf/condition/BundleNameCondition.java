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
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.osgi.framework.Bundle;
import org.osgi.service.condpermadmin.Condition;
import org.osgi.service.condpermadmin.ConditionInfo;

/**
 * The interface implemented by a Condition. Conditions are bound to Permissions using Conditional
 * Permission Info. The Permissions of a ConditionalPermission Info can only be used if the
 * associated Conditions are satisfied.
 *
 * <p>Selects bundles based on the name. @ThreadSafe
 */
public class BundleNameCondition implements Condition {

  private static final ConcurrentHashMap<String, Boolean> DECISION_MAP =
      new ConcurrentHashMap<>(100000);

  private static final Pattern REGEX = Pattern.compile("/");

  private Bundle bundle;
  private String[] args;
  private String[] bundles;

  public BundleNameCondition(Bundle bundle, ConditionInfo conditionInfo) {
    this.bundle = bundle;
    args = conditionInfo.getArgs();
    bundles = REGEX.split(args[0]);
  }

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
   * Returns whether the Condition is satisfied. This method is only called for immediate Condition
   * objects or immutable postponed conditions, and must always be called inside a permission check.
   * Mutable postponed Condition objects will be called with the grouped version {@link
   * #isSatisfied(Condition[],Dictionary)} at the end of the permission check.
   *
   * @return {@code true} to indicate the Conditions is satisfied. Otherwise, {@code false} if the
   *     Condition is not satisfied.
   */
  @Override
  public boolean isSatisfied() {
    if (args.length == 0) {
      return false;
    }
    String bundleName = bundle.getHeaders().get("Bundle-SymbolicName");
    String key = bundleName + args[0];
    Boolean storedResult = DECISION_MAP.get(key);
    if (storedResult != null) {
      return storedResult;
    }
    for (String bundleStr : bundles) {
      if (bundleName.contains(bundleStr)) {
        DECISION_MAP.put(key, true);
        return true;
      }
    }
    DECISION_MAP.put(key, false);
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
