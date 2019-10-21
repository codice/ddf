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
package ddf.catalog.plugin.facetattributeaccess;

import static ddf.catalog.Constants.EXPERIMENTAL_FACET_PROPERTIES_KEY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.TermFacetProperties;
import ddf.catalog.plugin.AccessPlugin;
import ddf.catalog.plugin.StopProcessingException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

public class FacetAttributePluginTest {

  private static final String ATTR_1 = "attr1";

  private AccessPlugin accessPlugin;

  private QueryRequest queryRequest;

  private TermFacetProperties facetProperties;

  private Set<String> facetAttributes;

  private Map<String, Serializable> queryProperties;

  @Before
  public void setUp() {
    FacetWhitelistConfiguration config = new FacetWhitelistConfiguration();
    config.setFacetAttributeWhitelist(Arrays.asList(ATTR_1));
    accessPlugin = new FacetAttributeAccessPlugin(config);

    queryRequest = mock(QueryRequest.class);
    queryProperties = new HashMap<>();
    when(queryRequest.getProperties()).thenReturn(queryProperties);

    facetProperties = mock(TermFacetProperties.class);
    facetAttributes = new HashSet<>();
    when(facetProperties.getFacetAttributes()).thenReturn(facetAttributes);
  }

  @Test
  public void testValidFacetAttribute() throws StopProcessingException {
    queryProperties.put(EXPERIMENTAL_FACET_PROPERTIES_KEY, facetProperties);
    facetAttributes.add(ATTR_1);
    assertThat(queryRequest, is(accessPlugin.processPreQuery(queryRequest)));
  }

  @Test(expected = StopProcessingException.class)
  public void testInvalidFacetAttribute() throws StopProcessingException {
    queryProperties.put(EXPERIMENTAL_FACET_PROPERTIES_KEY, facetProperties);
    facetAttributes.add("invalidAttribute");
    accessPlugin.processPreQuery(queryRequest);
  }

  @Test
  public void testQueryWithNoFacets() throws StopProcessingException {
    assertThat(queryRequest, is(accessPlugin.processPreQuery(queryRequest)));
  }
}
