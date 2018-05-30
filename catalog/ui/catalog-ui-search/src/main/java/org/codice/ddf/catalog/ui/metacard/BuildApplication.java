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
package org.codice.ddf.catalog.ui.metacard;

import static spark.Spark.get;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.codice.ddf.catalog.ui.metacard.internal.UserCreatableMetacardType;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import spark.servlet.SparkApplication;

public class BuildApplication implements SparkApplication {

  private final EndpointUtil endpointUtil;

  private final List<UserCreatableMetacardType> userCreatableMetacardTypeList;

  public BuildApplication(
      EndpointUtil endpointUtil, List<UserCreatableMetacardType> userCreatableMetacardTypeList) {
    this.endpointUtil = endpointUtil;
    this.userCreatableMetacardTypeList = userCreatableMetacardTypeList;
  }

  @Override
  public void init() {
    /** Get the available types that were explicitly configured. */
    get(
        "/builder/availabletypes",
        (request, response) -> getAvailableTypes(),
        endpointUtil::getJson);
  }

  @VisibleForTesting
  Map<String, Object> getAvailableTypes() {

    List<Map<String, Object>> availableTypes =
        this.userCreatableMetacardTypeList
            .stream()
            .map(this::convertUserCreatableMetacardTypeToMap)
            .collect(Collectors.toList());

    return Collections.singletonMap("availabletypes", availableTypes);
  }

  private Map<String, Object> convertUserCreatableMetacardTypeToMap(
      UserCreatableMetacardType userCreatableMetacardType) {
    return new ImmutableMap.Builder<String, Object>()
        .put("metacardType", userCreatableMetacardType.getAvailableType())
        .put("visibleAttributes", userCreatableMetacardType.getUserVisibleAttributes())
        .build();
  }
}
