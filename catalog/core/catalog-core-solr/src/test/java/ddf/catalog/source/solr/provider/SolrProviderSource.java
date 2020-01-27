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
package ddf.catalog.source.solr.provider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ddf.catalog.source.solr.BaseSolrCatalogProvider;
import ddf.catalog.source.solr.BaseSolrProviderTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrProviderSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrProviderSource.class);

  private static BaseSolrCatalogProvider provider;

  @BeforeClass
  public static void setUp() {
    provider = BaseSolrProviderTest.getProvider();
  }

  @Test
  public void testIsAvalaible() {
    assertTrue(provider.isAvailable());
  }

  /** Test that makes sure sourceId is returned for deletions, creates, and updates. */
  @Test
  public void testSourceId() {
    assertThat(provider.getId(), notNullValue());

    // TODO need more here, how can we test this further
  }

  @Test
  public void testDescribable() {
    LOGGER.debug("version: {}", provider.getVersion());
    LOGGER.debug("description: {}", provider.getDescription());
    LOGGER.debug("org: {}", provider.getOrganization());
    LOGGER.debug("name: {}", provider.getTitle());

    assertNotNull(provider.getOrganization());
    assertNotNull(provider.getVersion());
    assertNotNull(provider.getDescription());
    assertNotNull(provider.getOrganization());
    assertNotNull(provider.getTitle());
  }
}
