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

import java.security.AccessController;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.Set;
import org.osgi.framework.Bundle;
import org.osgi.service.condpermadmin.Condition;
import org.osgi.service.condpermadmin.ConditionInfo;

/**
 * The interface implemented by a Condition. Conditions are bound to Permissions using Conditional
 * Permission Info. The Permissions of a ConditionalPermission Info can only be used if the
 * associated Conditions are satisfied.
 *
 * <p>Selects bundles based on the identity of the principal executing the call. @ThreadSafe
 */
public class PrincipalCondition implements Condition {

  private String[] args;
  private List<String> argsAsList;

  @SuppressWarnings("squid:S1172") /* required for reflection to work */
  public PrincipalCondition(Bundle bundle, ConditionInfo conditionInfo) {
    args = conditionInfo.getArgs();
    argsAsList = Arrays.asList(args);
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
    javax.security.auth.Subject subject =
        javax.security.auth.Subject.getSubject(AccessController.getContext());
    Set<Principal> principals = subject.getPrincipals();
    List<String> principalNameList = new ArrayList<>();
    for (Principal principal : principals) {
      principalNameList.add(principal.getName());
    }
    return principalNameList.containsAll(argsAsList);
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
