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
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.concurrent.ConcurrentHashMap;
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
public final class BundleNameCondition extends AbstractCondition implements Condition {

  private static final ConcurrentHashMap<String, Boolean> DECISION_MAP =
      new ConcurrentHashMap<>(100000);

  private Bundle bundle;
  private String[] args;

  public BundleNameCondition(Bundle bundle, ConditionInfo conditionInfo) {
    this.bundle = bundle;
    args = conditionInfo.getArgs();
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
    String bundleName =
        AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> bundle.getHeaders().get("Bundle-SymbolicName"));
    String key = bundleName + Arrays.toString(args);
    Boolean storedResult = DECISION_MAP.get(key);
    if (storedResult != null) {
      return storedResult;
    }
    for (String bundleStr : args) {
      if (bundleName.contains(bundleStr)) {
        DECISION_MAP.put(key, true);
        return true;
      }
    }
    DECISION_MAP.put(key, false);
    return false;
  }
}
