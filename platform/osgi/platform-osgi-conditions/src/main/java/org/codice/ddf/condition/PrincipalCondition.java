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
public final class PrincipalCondition extends AbstractCondition implements Condition {

  private String[] args;
  private List<String> argsAsList;

  @SuppressWarnings("squid:S1172") /* required for reflection to work */
  public PrincipalCondition(Bundle bundle, ConditionInfo conditionInfo) {
    args = conditionInfo.getArgs();
    argsAsList = Arrays.asList(args);
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
    if (subject != null) {
      Set<Principal> principals = subject.getPrincipals();
      List<String> principalNameList = new ArrayList<>();
      for (Principal principal : principals) {
        principalNameList.add(principal.getName());
      }
      return principalNameList.containsAll(argsAsList);
    }
    return false;
  }
}
