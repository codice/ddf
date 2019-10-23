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
package org.codice.ddf.catalog.ui.config;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.platform.resource.bundle.locator.ResourceBundleLocator;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.codice.proxy.http.HttpProxyService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class ConfigurationApplicationTest {

  private static final String PROXY_SERVER = "http://www.example.com/wms";

  private static final String BUNDLE_SYMBOLIC_NAME = "mySymbolicName";

  private ConfigurationApplication configurationApplication;

  private ResourceBundleLocator resourceBundleLocator;

  @Before
  public void setUp() {
    configurationApplication = new ConfigurationApplication(mock(UuidGenerator.class));
    resourceBundleLocator = mock(ResourceBundleLocator.class);
  }

  @Test
  public void testSetImageryProviders() throws Exception {
    // Setup
    HttpProxyService mockHttpProxyService = mock(HttpProxyService.class);
    BundleContext mockBundleContext = mock(BundleContext.class);
    Bundle mockBundle = mock(Bundle.class);
    when(mockBundleContext.getBundle()).thenReturn(mockBundle);
    when(mockBundle.getSymbolicName()).thenReturn(BUNDLE_SYMBOLIC_NAME);
    when(mockHttpProxyService.start(anyString(), anyString(), anyInt())).thenReturn(PROXY_SERVER);

    configurationApplication.setHttpProxy(mockHttpProxyService);
    configurationApplication.setImageryProviders(
        IOUtils.toString(getClass().getResourceAsStream("/imagery-providers.json")));

    // Verify
    for (Map<String, Object> provider : configurationApplication.getImageryProviderUrlMaps()) {
      assertTrue(provider.get(ConfigurationApplication.URL).toString().contains(PROXY_SERVER));
    }
  }

  @Test
  public void testSetTerrainProvider() throws Exception {
    // Setup
    HttpProxyService mockHttpProxyService = mock(HttpProxyService.class);
    BundleContext mockBundleContext = mock(BundleContext.class);
    Bundle mockBundle = mock(Bundle.class);
    when(mockBundleContext.getBundle()).thenReturn(mockBundle);
    when(mockBundle.getSymbolicName()).thenReturn(BUNDLE_SYMBOLIC_NAME);
    when(mockHttpProxyService.start(anyString(), anyString(), anyInt())).thenReturn(PROXY_SERVER);

    configurationApplication.setHttpProxy(mockHttpProxyService);
    configurationApplication.setTerrainProvider(
        IOUtils.toString(getClass().getResourceAsStream("/terrain-provider.json")));

    // Verify
    assertTrue(
        configurationApplication
            .getProxiedTerrainProvider()
            .get(ConfigurationApplication.URL)
            .toString()
            .contains(PROXY_SERVER));
  }

  @Test
  public void testContentTypeMappings() throws Exception {
    // Setup
    configurationApplication.setTypeNameMapping(
        (String[])
            Arrays.asList(
                    "foo=bar,foo=baz",
                    "foo=qux",
                    "alpha=beta, alpha = omega ",
                    "=,=,",
                    "bad,input",
                    "name=,=type")
                .toArray());

    // Verify
    assertThat(configurationApplication.getTypeNameMapping().size(), is(2));
    assertThat(
        configurationApplication.getTypeNameMapping(),
        hasEntry("foo", Sets.newSet("bar", "baz", "qux")));
    assertThat(
        configurationApplication.getTypeNameMapping(),
        hasEntry("alpha", Sets.newSet("beta", "omega")));
  }

  @Test
  public void testContentTypeMappingsList() throws Exception {
    // Setup
    configurationApplication.setTypeNameMapping(
        Arrays.asList(
            "foo=bar,foo=baz",
            "foo=qux",
            "alpha=beta, alpha = omega ",
            "=,=,",
            "bad,input",
            "name=,=type"));

    // Verify
    assertThat(configurationApplication.getTypeNameMapping().size(), is(2));
    assertThat(
        configurationApplication.getTypeNameMapping(),
        hasEntry("foo", Sets.newSet("bar", "baz", "qux")));
    assertThat(
        configurationApplication.getTypeNameMapping(),
        hasEntry("alpha", Sets.newSet("beta", "omega")));
  }

  @Test
  public void testContentTypeMappingsListString() throws Exception {
    // Setup
    configurationApplication.setTypeNameMapping(
        "foo=bar,foo=baz,foo=qux,alpha=beta, alpha = omega , =,=,bad,input,name=,=type");

    // Verify
    assertThat(configurationApplication.getTypeNameMapping().size(), is(2));
    assertThat(
        configurationApplication.getTypeNameMapping(),
        hasEntry("foo", Sets.newSet("bar", "baz", "qux")));
    assertThat(
        configurationApplication.getTypeNameMapping(),
        hasEntry("alpha", Sets.newSet("beta", "omega")));
  }

  @Test
  public void testsetAttributeAliases() throws Exception {
    configurationApplication.setAttributeAliases(Arrays.asList(" a = b ", " x = y "));
    assertThat(configurationApplication.getAttributeAliases(), is(Arrays.asList("a=b", "x=y")));
  }

  @Test
  public void testSetAttributeEnumMap() {
    configurationApplication.setAttributeEnumMap(
        Arrays.asList(
            "",
            "unrestrictedAttribute",
            "restrictedAttributeWithEmptyEnum=",
            "=blank,attribute,name",
            " restrictedAttributeWithSpaces  = list,of,  possible  ,values",
            "restrictedAttributeWithDuplicates=duplicate, duplicate, values, values",
            "duplicateAttribute=value1",
            "duplicateAttribute=value2"));
    Map<String, Set<String>> attributeEnumMap = configurationApplication.getAttributeEnumMap();

    assertThat(configurationApplication.getEditorAttributes().size(), is(5));
    assertThat(attributeEnumMap.size(), is(3));
    assertThat(attributeEnumMap, not(hasKey("restrictedAttributeWithEmptyEnum")));
    assertThat(attributeEnumMap, not(hasKey("")));
    assertThat(attributeEnumMap.get("restrictedAttributeWithSpaces"), hasItem("possible"));
    assertThat(attributeEnumMap.get("restrictedAttributeWithDuplicates").size(), is(2));
    assertThat(attributeEnumMap.get("duplicateAttribute").size(), is(2));
  }

  @Test
  public void testSetRequiredAttributes() {
    configurationApplication.setRequiredAttributes(Arrays.asList("", "attribute"));
    Set<String> requiredAttributes = configurationApplication.getRequiredAttributes();

    assertThat(requiredAttributes.size(), is(1));
    assertThat(requiredAttributes, hasItem("attribute"));
  }

  @Test
  public void testGetDefaultKeywords() {
    assertThat(configurationApplication.getI18n(), is(Collections.emptyMap()));
  }

  @Test
  public void testGetKeywords() throws IOException {
    doReturn(ResourceBundle.getBundle("IntrigueBundle"))
        .when(resourceBundleLocator)
        .getBundle(any(String.class));
    configurationApplication.setI18n(resourceBundleLocator);

    Map<String, String> keywords = configurationApplication.getI18n();

    assertThat(keywords.size(), is(4));
    assertThat(keywords.get("Source"), is("Source"));
    assertThat(keywords.get("source"), is("source"));
    assertThat(keywords.get("Sources"), is("Sources"));
    assertThat(keywords.get("sources"), is("sources"));
  }
}
