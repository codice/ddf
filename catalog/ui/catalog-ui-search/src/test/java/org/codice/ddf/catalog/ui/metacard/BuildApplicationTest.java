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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.codice.ddf.catalog.ui.metacard.internal.UserCreatableMetacardType;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.junit.Before;
import org.junit.Test;

public class BuildApplicationTest {

  private EndpointUtil endpointUtil;

  @Before
  public void setup() {
    endpointUtil = new EndpointUtil(null, null, null, null, null, null);
  }

  @Test
  public void testEmpty() {
    Map<String, Object> map = createBuildApplicationAndRun();
    assertThat(map, is(buildAvailableMetacardTypes()));
  }

  @Test
  public void testOneAvailableMetacardType() {
    UserCreatableMetacardType userCreatableMetacardType =
        createAvailableMetacardType("x", Collections.emptySet());
    Map<String, Object> map = createBuildApplicationAndRun(userCreatableMetacardType);
    assertThat(map, is(buildAvailableMetacardTypes("x")));
  }

  @Test
  public void testTwoAvailableMetacardType() {
    UserCreatableMetacardType availableMetacardType1 =
        createAvailableMetacardType("x", Collections.emptySet());
    UserCreatableMetacardType availableMetacardType2 =
        createAvailableMetacardType("y", Collections.emptySet());
    Map<String, Object> map =
        createBuildApplicationAndRun(availableMetacardType1, availableMetacardType2);
    assertThat(map, is(buildAvailableMetacardTypes("x", "y")));
  }

  private UserCreatableMetacardType createAvailableMetacardType(
      String typeName, Set<String> visibleAttributes) {
    UserCreatableMetacardType userCreatableMetacardType = mock(UserCreatableMetacardType.class);
    when(userCreatableMetacardType.getAvailableType()).thenReturn(typeName);
    when(userCreatableMetacardType.getUserVisibleAttributes()).thenReturn(visibleAttributes);
    return userCreatableMetacardType;
  }

  private Map<String, Object> createBuildApplicationAndRun(
      UserCreatableMetacardType... userCreatableMetacardTypes) {
    BuildApplication buildApplication =
        new BuildApplication(endpointUtil, Arrays.asList(userCreatableMetacardTypes));
    return buildApplication.getAvailableTypes();
  }

  private Map<String, Object> buildAvailableMetacardTypes(String... metacardTypeNames) {
    return Collections.singletonMap(
        "availabletypes",
        Arrays.stream(metacardTypeNames)
            .map(this::buildAvailableMetacardType)
            .collect(Collectors.toList()));
  }

  private Map<String, Object> buildAvailableMetacardType(String metacardTypeName) {
    return ImmutableMap.<String, Object>builder()
        .put("metacardType", metacardTypeName)
        .put("visibleAttributes", Collections.emptySet())
        .build();
  }
}
