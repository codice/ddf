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

import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.create;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.deleteAll;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;

import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.ContentTypeImpl;
import ddf.catalog.source.solr.BaseSolrCatalogProvider;
import ddf.catalog.source.solr.BaseSolrProviderTest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;

public class SolrProviderContentTypes {

  private static final String SAMPLE_CONTENT_TYPE_1 = "contentType1";

  private static final String SAMPLE_CONTENT_TYPE_2 = "contentType2";

  private static final String SAMPLE_CONTENT_TYPE_3 = "content-Type";

  private static final String SAMPLE_CONTENT_TYPE_4 = "ct1=3";

  private static final String SAMPLE_CONTENT_VERSION_1 = "version1";

  private static final String SAMPLE_CONTENT_VERSION_2 = "vers:ion2";

  private static final String SAMPLE_CONTENT_VERSION_3 = "DDFv20";

  private static final String SAMPLE_CONTENT_VERSION_4 = "vers+4";

  private static BaseSolrCatalogProvider provider;

  @BeforeClass
  public static void setUp() {
    provider = BaseSolrProviderTest.getProvider();
  }

  @Test
  public void testGetContentTypesSimple() throws Exception {

    deleteAll(provider);

    MockMetacard metacard1 = new MockMetacard(Library.getFlagstaffRecord());
    MockMetacard metacard2 = new MockMetacard(Library.getShowLowRecord());
    MockMetacard metacard3 = new MockMetacard(Library.getTampaRecord());

    metacard1.setContentTypeName(SAMPLE_CONTENT_TYPE_1);
    metacard2.setContentTypeName(SAMPLE_CONTENT_TYPE_2);
    metacard3.setContentTypeName(SAMPLE_CONTENT_TYPE_2);
    metacard3.setContentTypeVersion(SAMPLE_CONTENT_VERSION_3);

    List<Metacard> list = Arrays.asList(metacard1, metacard2, metacard3);

    create(list, provider);

    Set<ContentType> contentTypes = provider.getContentTypes();
    assertEquals(3, contentTypes.size());

    assertThat(
        contentTypes,
        hasItem(
            (ContentType)
                new ContentTypeImpl(SAMPLE_CONTENT_TYPE_1, MockMetacard.DEFAULT_VERSION)));
    assertThat(
        contentTypes,
        hasItem(
            (ContentType)
                new ContentTypeImpl(SAMPLE_CONTENT_TYPE_2, MockMetacard.DEFAULT_VERSION)));
    assertThat(
        contentTypes,
        hasItem(
            (ContentType) new ContentTypeImpl(SAMPLE_CONTENT_TYPE_2, SAMPLE_CONTENT_VERSION_3)));
  }

  @Test
  public void testGetContentTypesComplicated() throws Exception {

    deleteAll(provider);

    List<Metacard> list = new ArrayList<>();

    // Single content type and version
    MockMetacard metacard1 = new MockMetacard(Library.getFlagstaffRecord());
    metacard1.setContentTypeName(SAMPLE_CONTENT_TYPE_1);
    metacard1.setContentTypeVersion(SAMPLE_CONTENT_VERSION_1);
    list.add(metacard1);

    // one content type with multiple versions
    metacard1 = new MockMetacard(Library.getFlagstaffRecord());
    metacard1.setContentTypeName(SAMPLE_CONTENT_TYPE_2);
    metacard1.setContentTypeVersion(SAMPLE_CONTENT_VERSION_1);
    list.add(metacard1);
    MockMetacard metacard2 = new MockMetacard(Library.getFlagstaffRecord());
    metacard2.setContentTypeName(SAMPLE_CONTENT_TYPE_2);
    metacard2.setContentTypeVersion(SAMPLE_CONTENT_VERSION_2);
    list.add(metacard2);

    // multiple records with different content type but same version
    metacard1 = new MockMetacard(Library.getFlagstaffRecord());
    metacard1.setContentTypeName(SAMPLE_CONTENT_TYPE_3);
    metacard1.setContentTypeVersion(SAMPLE_CONTENT_VERSION_3);
    list.add(metacard1);
    metacard2 = new MockMetacard(Library.getFlagstaffRecord());
    metacard2.setContentTypeName(SAMPLE_CONTENT_TYPE_3);
    metacard2.setContentTypeVersion(SAMPLE_CONTENT_VERSION_4);
    list.add(metacard2);

    // multiple records with different content type and different version
    metacard1 = new MockMetacard(Library.getFlagstaffRecord());
    metacard1.setContentTypeName(SAMPLE_CONTENT_TYPE_4);
    metacard1.setContentTypeVersion(SAMPLE_CONTENT_VERSION_1);
    list.add(metacard1);
    metacard2 = new MockMetacard(Library.getFlagstaffRecord());
    metacard2.setContentTypeName(SAMPLE_CONTENT_TYPE_1);
    metacard2.setContentTypeVersion(SAMPLE_CONTENT_VERSION_4);
    list.add(metacard2);
    metacard1 = new MockMetacard(Library.getFlagstaffRecord());
    metacard1.setContentTypeName(SAMPLE_CONTENT_TYPE_4);
    metacard1.setContentTypeVersion(SAMPLE_CONTENT_VERSION_1);
    list.add(metacard1);
    metacard2 = new MockMetacard(Library.getFlagstaffRecord());
    metacard2.setContentTypeName(SAMPLE_CONTENT_TYPE_1);
    metacard2.setContentTypeVersion(SAMPLE_CONTENT_VERSION_4);
    list.add(metacard2);

    create(list, provider);

    Set<ContentType> contentTypes = provider.getContentTypes();
    assertEquals(7, contentTypes.size());

    assertThat(
        contentTypes,
        hasItem(
            (ContentType) new ContentTypeImpl(SAMPLE_CONTENT_TYPE_1, SAMPLE_CONTENT_VERSION_1)));
    assertThat(
        contentTypes,
        hasItem(
            (ContentType) new ContentTypeImpl(SAMPLE_CONTENT_TYPE_2, SAMPLE_CONTENT_VERSION_1)));
    assertThat(
        contentTypes,
        hasItem(
            (ContentType) new ContentTypeImpl(SAMPLE_CONTENT_TYPE_2, SAMPLE_CONTENT_VERSION_2)));
    assertThat(
        contentTypes,
        hasItem(
            (ContentType) new ContentTypeImpl(SAMPLE_CONTENT_TYPE_3, SAMPLE_CONTENT_VERSION_3)));
    assertThat(
        contentTypes,
        hasItem(
            (ContentType) new ContentTypeImpl(SAMPLE_CONTENT_TYPE_3, SAMPLE_CONTENT_VERSION_4)));
    assertThat(
        contentTypes,
        hasItem(
            (ContentType) new ContentTypeImpl(SAMPLE_CONTENT_TYPE_4, SAMPLE_CONTENT_VERSION_1)));
    assertThat(
        contentTypes,
        hasItem(
            (ContentType) new ContentTypeImpl(SAMPLE_CONTENT_TYPE_1, SAMPLE_CONTENT_VERSION_4)));
  }

  @Test
  public void testGetContentTypesOne() throws Exception {

    deleteAll(provider);

    MockMetacard metacard1 = new MockMetacard(Library.getFlagstaffRecord());

    metacard1.setContentTypeName(SAMPLE_CONTENT_TYPE_1);

    List<Metacard> list = Collections.singletonList(metacard1);

    create(list, provider);

    Set<ContentType> contentTypes = provider.getContentTypes();
    assertEquals(1, contentTypes.size());

    assertThat(
        contentTypes,
        hasItem(
            (ContentType)
                new ContentTypeImpl(SAMPLE_CONTENT_TYPE_1, MockMetacard.DEFAULT_VERSION)));
  }

  @Test
  public void testGetContentTypesOneNoVersion() throws Exception {

    deleteAll(provider);

    MockMetacard metacard1 = new MockMetacard(Library.getFlagstaffRecord());

    metacard1.setContentTypeName(SAMPLE_CONTENT_TYPE_1);
    metacard1.setContentTypeVersion(null);

    List<Metacard> list = Collections.singletonList(metacard1);

    create(list, provider);

    Set<ContentType> contentTypes = provider.getContentTypes();
    assertEquals(1, contentTypes.size());

    assertThat(
        contentTypes, hasItem((ContentType) new ContentTypeImpl(SAMPLE_CONTENT_TYPE_1, null)));
  }

  @Test
  public void testGetContentTypesVersionsAndNullVersions() throws Exception {

    deleteAll(provider);

    MockMetacard metacard1 = new MockMetacard(Library.getFlagstaffRecord());
    MockMetacard metacard2 = new MockMetacard(Library.getShowLowRecord());
    MockMetacard metacard3 = new MockMetacard(Library.getTampaRecord());

    metacard1.setContentTypeName(SAMPLE_CONTENT_TYPE_1);
    metacard1.setContentTypeVersion(null);
    metacard2.setContentTypeName(SAMPLE_CONTENT_TYPE_2);
    metacard3.setContentTypeName(SAMPLE_CONTENT_TYPE_2);
    metacard3.setContentTypeVersion(SAMPLE_CONTENT_VERSION_3);

    List<Metacard> list = Arrays.asList(metacard1, metacard2, metacard3);

    create(list, provider);

    Set<ContentType> contentTypes = provider.getContentTypes();
    assertEquals(3, contentTypes.size());

    assertThat(
        contentTypes, hasItem((ContentType) new ContentTypeImpl(SAMPLE_CONTENT_TYPE_1, null)));
    assertThat(
        contentTypes,
        hasItem(
            (ContentType)
                new ContentTypeImpl(SAMPLE_CONTENT_TYPE_2, MockMetacard.DEFAULT_VERSION)));
    assertThat(
        contentTypes,
        hasItem(
            (ContentType) new ContentTypeImpl(SAMPLE_CONTENT_TYPE_2, SAMPLE_CONTENT_VERSION_3)));
  }

  @Test
  public void testGetContentTypesNone() throws Exception {

    deleteAll(provider);

    assertEquals(0, provider.getContentTypes().size());
  }
}
