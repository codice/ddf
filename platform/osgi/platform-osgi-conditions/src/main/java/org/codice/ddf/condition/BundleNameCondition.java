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

public class BundleNameCondition implements Condition {

  private static final ConcurrentHashMap<String, Boolean> decisionMap =
      new ConcurrentHashMap<>(100000);

  private static final Pattern REGEX = Pattern.compile("/");

  private Bundle bundle;
  private String[] args;

  public BundleNameCondition(Bundle bundle, ConditionInfo conditionInfo) {
    this.bundle = bundle;
    args = conditionInfo.getArgs();
  }

  @Override
  public boolean isPostponed() {
    return false;
  }

  @Override
  public boolean isSatisfied() {
    if (args.length == 0) {
      return false;
    }
    String bundleName = bundle.getHeaders().get("Bundle-SymbolicName");
    String key = bundleName + args[0];
    Boolean storedResult = decisionMap.get(key);
    if (storedResult != null) {
      return storedResult;
    }
    String[] bundles = REGEX.split(args[0]);
    for (String bundleStr : bundles) {
      if (bundleName.contains(bundleStr)) {
        decisionMap.put(key, true);
        return true;
      }
    }
    decisionMap.put(key, false);
    return false;
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
