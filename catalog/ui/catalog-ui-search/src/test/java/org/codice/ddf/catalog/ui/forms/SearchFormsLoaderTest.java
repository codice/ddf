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
package org.codice.ddf.catalog.ui.forms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.Metacard;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import javax.xml.bind.JAXBException;
import org.codice.ddf.catalog.ui.forms.data.AttributeGroupMetacard;
import org.codice.ddf.catalog.ui.forms.data.QueryTemplateMetacard;
import org.codice.ddf.catalog.ui.forms.filter.FilterWriter;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SearchFormsLoaderTest {
  private @Mock CatalogFramework catalogFramework;

  private @Mock EndpointUtil endpointUtil;

  private @Mock AttributeRegistry registry;

  private static final URL LOADER_RESOURCES_DIR =
      SearchFormsLoaderTest.class.getResource("/forms/loader");

  private static final String ROOT;

  static {
    String filePath;
    try {
      filePath = Paths.get(LOADER_RESOURCES_DIR.toURI()).toFile().toString();
    } catch (URISyntaxException e) {
      filePath = "";
    }
    ROOT = filePath;
  }

  private TemplateTransformer transformer;

  @Before
  public void setUp() throws JAXBException {
    when(registry.lookup(any())).thenReturn(Optional.empty());
    transformer = new TemplateTransformer(new FilterWriter(false), registry);
  }

  @Test
  public void testEmptyConfigurationDirectory() {
    List<Metacard> metacards =
        new SearchFormsLoader(catalogFramework, transformer, endpointUtil)
            .retrieveSystemTemplateMetacards();
    expectedCounts(metacards, 0, 0, 0);
  }

  @Test
  public void testValidConfigurationWithFallbackValues() {
    List<Metacard> metacards =
        new SearchFormsLoader(
                catalogFramework, transformer, endpointUtil, ROOT + "/valid", null, null)
            .retrieveSystemTemplateMetacards();
    expectedCounts(metacards, 3, 2, 1);
  }

  @Test
  public void testValidConfigurationWithExplicitValues() {
    List<Metacard> metacards =
        new SearchFormsLoader(
                catalogFramework,
                transformer,
                endpointUtil,
                ROOT + "/valid",
                "forms.json",
                "results.json")
            .retrieveSystemTemplateMetacards();
    expectedCounts(metacards, 3, 2, 1);
  }

  @Test
  public void testMissingXmlLinksExplicitValues() {
    List<Metacard> metacards =
        new SearchFormsLoader(
                catalogFramework,
                transformer,
                endpointUtil,
                ROOT + "/missing",
                "forms.json",
                "results.json")
            .retrieveSystemTemplateMetacards();
    expectedCounts(metacards, 1, 0, 1);
  }

  @Test
  public void testInvalidStructureFallbackValues() {
    List<Metacard> metacards =
        new SearchFormsLoader(
                catalogFramework,
                transformer,
                endpointUtil,
                ROOT + "/invalid-structure",
                null,
                null)
            .retrieveSystemTemplateMetacards();
    expectedCounts(metacards, 1, 0, 1);
  }

  @Test
  public void testInvalidEntriesFallbackValues() {
    List<Metacard> metacards =
        new SearchFormsLoader(
                catalogFramework, transformer, endpointUtil, ROOT + "/invalid-entries", null, null)
            .retrieveSystemTemplateMetacards();
    expectedCounts(metacards, 4, 1, 3);
  }

  private static void expectedCounts(
      List<Metacard> metacards, int total, int queryTemplates, int resultTemplates) {
    assertThat(
        "Expected total number of generated metacards to be " + total, metacards, hasSize(total));
    assertThat(
        "Expected number of generated query template metacards to be " + queryTemplates,
        metacards.stream().filter(QueryTemplateMetacard::isQueryTemplateMetacard).count(),
        is((long) queryTemplates));
    assertThat(
        "Expected number of generated result template metacards to be " + resultTemplates,
        metacards.stream().filter(AttributeGroupMetacard::isAttributeGroupMetacard).count(),
        is((long) resultTemplates));
  }
}
