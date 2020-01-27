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
package ddf.catalog.solr.collectionprovider;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultSolrCollectionProviderTest {

  private DefaultSolrCollectionProvider indexProvider = new DefaultSolrCollectionProvider();

  @Test
  public void testGetCollection() throws Exception {
    Metacard mockCard = getMockMetacard("workspace");
    String collection = indexProvider.getCollection(mockCard);
    MatcherAssert.assertThat(
        collection, Matchers.is(DefaultSolrCollectionProvider.DEFAULT_COLLECTION));
  }

  @Test
  public void testNotSupportedTag() {
    Metacard mockCard = getMockMetacard("resource");
    String collection = indexProvider.getCollection(mockCard);
    MatcherAssert.assertThat(
        collection, Matchers.is(DefaultSolrCollectionProvider.DEFAULT_COLLECTION));
  }

  @Test
  public void testNoTags() {
    Metacard mockCard = Mockito.mock(Metacard.class);
    Mockito.when(mockCard.getAttribute(Core.METACARD_TAGS)).thenReturn(null);
    String collection = indexProvider.getCollection(mockCard);
    MatcherAssert.assertThat(
        collection, Matchers.is(DefaultSolrCollectionProvider.DEFAULT_COLLECTION));
  }

  @Test
  public void testNullMetacard() {
    String collection = indexProvider.getCollection(null);
    MatcherAssert.assertThat(
        collection, Matchers.is(DefaultSolrCollectionProvider.DEFAULT_COLLECTION));
  }

  @Test
  public void testMatches() {
    boolean matches = indexProvider.matches(null);
    MatcherAssert.assertThat(matches, Matchers.is(true));
  }

  @Test
  public void testMetacardMatches() {
    boolean matches = indexProvider.matches(getMockMetacard("anything"));
    MatcherAssert.assertThat(matches, Matchers.is(true));
  }

  @Test
  public void testGetCollectionName() {
    String collectionName = indexProvider.getCollectionName();
    MatcherAssert.assertThat(
        collectionName, Matchers.is(DefaultSolrCollectionProvider.DEFAULT_COLLECTION));
  }

  private Metacard getMockMetacard(String tag) {
    Metacard metacard = Mockito.mock(Metacard.class);
    Attribute tagAttr = Mockito.mock(Attribute.class);
    Mockito.when(tagAttr.getValues()).thenReturn(Collections.singletonList(tag));
    Mockito.when(metacard.getAttribute(Core.METACARD_TAGS)).thenReturn(tagAttr);
    return metacard;
  }
}
