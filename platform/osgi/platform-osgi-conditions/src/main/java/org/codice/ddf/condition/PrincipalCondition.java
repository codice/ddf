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
import java.util.Arrays;
import java.util.Dictionary;
import java.util.stream.Collectors;
import org.osgi.framework.Bundle;
import org.osgi.service.condpermadmin.Condition;
import org.osgi.service.condpermadmin.ConditionInfo;

public class PrincipalCondition implements Condition {

  private Bundle bundle;
  private String[] args;

  public PrincipalCondition(Bundle bundle, ConditionInfo conditionInfo) {
    this.bundle = bundle;
    args = conditionInfo.getArgs();
  }

  @Override
  public boolean isPostponed() {
    return false;
  }

  @Override
  public boolean isSatisfied() {
    javax.security.auth.Subject subject =
        javax.security.auth.Subject.getSubject(AccessController.getContext());
    return subject
        .getPrincipals()
        .stream()
        .map(Principal::getName)
        .collect(Collectors.toList())
        .containsAll(Arrays.asList(args));
  }

  @Override
  public boolean isMutable() {
    return false;
  }

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
