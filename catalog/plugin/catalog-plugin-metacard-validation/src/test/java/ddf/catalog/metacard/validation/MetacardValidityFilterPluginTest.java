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
package ddf.catalog.metacard.validation;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Validation;
import ddf.catalog.plugin.PolicyResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class MetacardValidityFilterPluginTest {

  private MetacardValidityFilterPlugin metacardValidityFilterPlugin;

  @Before
  public void setUp() {
    metacardValidityFilterPlugin = new MetacardValidityFilterPlugin();
  }

  @Test
  public void testSetAttributeMapping() {
    List<String> attributeMapping = Collections.singletonList("sample=test1,test2");
    metacardValidityFilterPlugin.setAttributeMap(attributeMapping);
    Map<String, List<String>> assertMap = metacardValidityFilterPlugin.getAttributeMap();
    assertThat(assertMap.size(), is(1));
    assertThat(assertMap.containsKey("sample"), is(true));
    assertThat(assertMap.get("sample").contains("test1"), is(true));
    assertThat(assertMap.get("sample").contains("test2"), is(true));
  }

  @Test
  public void testResetAttributeMappingEmptyList() {
    metacardValidityFilterPlugin.setAttributeMap(new ArrayList<String>());
    assertThat(
        metacardValidityFilterPlugin.getAttributeMap(), is(new HashMap<String, List<String>>()));
  }

  @Test
  public void testResetAttributeMappingEmptyString() {
    metacardValidityFilterPlugin.setAttributeMap(Arrays.asList(""));
    assertThat(
        metacardValidityFilterPlugin.getAttributeMap(), is(new HashMap<String, List<String>>()));
  }

  @Test
  public void testValidMetacards() throws Exception {
    Result result = mock(Result.class);

    PolicyResponse response = filterPluginResponseHelper(result, getValidMetacard(), true, false);
    assertThat(response.itemPolicy().size(), is(0));
  }

  @Test
  public void testInvalidMetacards() throws Exception {
    Result result = mock(Result.class);

    PolicyResponse response = filterPluginResponseHelper(result, getErrorsMetacard(), true, false);
    assertThat(response.itemPolicy().get("sample").contains("test1"), is(true));
  }

  @Test
  public void testNullMetacard() throws Exception {
    Result result = mock(Result.class);

    PolicyResponse response = filterPluginResponseHelper(result, null, true, false);
    assertThat(response.itemPolicy().isEmpty(), is(true));
  }

  @Test
  public void testNullResults() throws Exception {
    PolicyResponse response = filterPluginResponseHelper(null, getValidMetacard(), true, false);

    assertThat(response.itemPolicy().isEmpty(), is(true));
  }

  @Test
  public void testFilterErrorsOnly() throws Exception {
    Result result = mock(Result.class);

    PolicyResponse response = filterPluginResponseHelper(result, getErrorsMetacard(), true, false);
    assertThat(response.itemPolicy().get("sample").contains("test1"), is(true));
  }

  @Test
  public void testFilterWarningsOnly() throws Exception {
    Result result = mock(Result.class);

    PolicyResponse response =
        filterPluginResponseHelper(result, getWarningsMetacard(), false, true);
    assertThat(response.itemPolicy().get("sample").contains("test1"), is(true));
  }

  @Test
  public void testFilterErrorsAndWarnings() throws Exception {
    Result result = mock(Result.class);

    PolicyResponse response = filterPluginResponseHelper(result, getWarningsMetacard(), true, true);
    assertThat(response.itemPolicy().get("sample").contains("test1"), is(true));

    response = filterPluginResponseHelper(result, getErrorsMetacard(), true, true);
    assertThat(response.itemPolicy().get("sample").contains("test1"), is(true));
  }

  @Test
  public void testFilterNone() throws Exception {
    Result result = mock(Result.class);

    PolicyResponse response = filterPluginResponseHelper(result, getErrorsMetacard(), false, false);
    assertThat(response.itemPolicy().size(), is(0));

    response = filterPluginResponseHelper(result, getWarningsMetacard(), false, false);
    assertThat(response.itemPolicy().size(), is(0));
  }

  private MetacardImpl getValidMetacard() {
    return new MetacardImpl();
  }

  private MetacardImpl getErrorsMetacard() {
    MetacardImpl returnMetacard = new MetacardImpl();
    returnMetacard.setAttribute(
        new AttributeImpl(
            Validation.VALIDATION_ERRORS, Collections.singletonList("sample-validator")));
    return returnMetacard;
  }

  private MetacardImpl getWarningsMetacard() {
    MetacardImpl returnMetacard = new MetacardImpl();
    returnMetacard.setAttribute(
        new AttributeImpl(
            Validation.VALIDATION_WARNINGS, Collections.singletonList("sample-validator")));
    return returnMetacard;
  }

  private PolicyResponse filterPluginResponseHelper(
      Result result, Metacard metacard, boolean filterErrors, boolean filterWarnings)
      throws Exception {
    List<String> attributeMapping = Collections.singletonList("sample=test1,test2");
    metacardValidityFilterPlugin.setAttributeMap(attributeMapping);
    metacardValidityFilterPlugin.setFilterErrors(filterErrors);
    metacardValidityFilterPlugin.setFilterWarnings(filterWarnings);

    if (result != null) {
      when(result.getMetacard()).thenReturn(metacard);
    }

    PolicyResponse response =
        metacardValidityFilterPlugin.processPostQuery(result, new HashMap<>());
    return response;
  }
}
