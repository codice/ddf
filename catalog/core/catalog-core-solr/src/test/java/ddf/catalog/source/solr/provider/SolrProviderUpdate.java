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

import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.ONE_HIT;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.create;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.deleteAll;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.getFilterBuilder;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.update;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.source.solr.ConfigurationStore;
import ddf.catalog.source.solr.SolrCatalogProvider;
import ddf.catalog.source.solr.SolrProviderTest;
import java.io.Serializable;
import java.net.URI;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class SolrProviderUpdate {

  private static SolrCatalogProvider provider;

  @BeforeClass
  public static void setUp() {
    provider = SolrProviderTest.getProvider();
  }

  /** Testing that if records are properly updated. */
  @Test
  public void testUpdateOperationSimple() throws IngestException, UnsupportedQueryException {

    // Single Update

    deleteAll(provider);

    MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());

    CreateResponse createResponse = create(metacard, provider);

    String id = createResponse.getCreatedMetacards().get(0).getId();

    metacard.setContentTypeName("newContentType");

    UpdateResponse response = update(id, metacard, provider);

    Update update = response.getUpdatedMetacards().get(0);

    Metacard newMetacard = update.getNewMetacard();

    Metacard oldMetacard = update.getOldMetacard();

    assertEquals(1, response.getUpdatedMetacards().size());

    assertEquals("newContentType", newMetacard.getContentTypeName());
    assertEquals(MockMetacard.DEFAULT_TYPE, oldMetacard.getContentTypeName());
  }

  /** Tests if a partial update is handled appropriately. */
  @Test
  public void testUpdatePartial() throws IngestException, UnsupportedQueryException {

    deleteAll(provider);

    MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());

    CreateResponse createResponse = create(metacard, provider);

    String id = createResponse.getCreatedMetacards().get(0).getId();

    metacard.setContentTypeName("newContentType");

    String[] ids = {id, "no_such_record"};

    UpdateResponse response = update(ids, Arrays.asList(metacard, metacard), provider);

    assertEquals(1, response.getUpdatedMetacards().size());
  }

  /** Tests what happens when the whole request is null. */
  @Test(expected = IngestException.class)
  public void testUpdateNull() throws IngestException, UnsupportedQueryException {

    deleteAll(provider);

    provider.update(null);

    fail();
  }

  /** Tests null list in UpdateRequest */
  @Test
  public void testUpdateNullList() throws IngestException, UnsupportedQueryException {

    deleteAll(provider);

    UpdateResponse response = provider.update(new UpdateRequestImpl(null, Core.ID, null));

    assertEquals(0, response.getUpdatedMetacards().size());
  }

  /** Tests empty list in UpdateRequest */
  @Test
  public void testUpdateEmptyList() throws IngestException, UnsupportedQueryException {

    deleteAll(provider);

    UpdateResponse response =
        provider.update(new UpdateRequestImpl(new ArrayList<>(), Core.ID, null));

    assertEquals(0, response.getUpdatedMetacards().size());
  }

  @Test
  public void testUpdateByMetacardId() throws Exception {
    deleteAll(provider);

    MockMetacard metacard1 = new MockMetacard(Library.getFlagstaffRecord());
    MockMetacard metacard2 = new MockMetacard(Library.getShowLowRecord());

    String uri1 = "http://youwillfindme.com/here";
    String uri2 = "http://youwillfindme.com/there";

    metacard1.setResourceURI(new URI(uri1));
    metacard1.setContentTypeName("oldNitf");
    metacard2.setResourceURI(new URI(uri2));
    metacard2.setContentTypeName("oldNitf2");
    metacard2.setResourceSize("25L");

    List<Metacard> list = Arrays.asList(metacard1, metacard2);

    CreateResponse createResponse = create(list, provider);

    List<String> responseStrings = MockMetacard.toStringList(createResponse.getCreatedMetacards());

    assertEquals(2, responseStrings.size());

    // UPDATE
    MockMetacard updatedMetacard1 = new MockMetacard(Library.getTampaRecord());
    MockMetacard updatedMetacard2 = new MockMetacard(Library.getFlagstaffRecord());

    updatedMetacard1.setId(metacard1.getId());
    updatedMetacard1.setContentTypeName("nitf");

    updatedMetacard2.setId(metacard2.getId());
    updatedMetacard2.setResourceURI(new URI(uri2));
    updatedMetacard2.setContentTypeName("nitf2");
    updatedMetacard2.setResourceSize("50L");

    list = Arrays.asList(updatedMetacard1, updatedMetacard2);

    String[] ids = {metacard1.getId(), metacard2.getId()};

    UpdateResponse updateResponse = update(ids, list, provider);
    verifyUpdates(uri1, uri2, updateResponse);

    // READ
    QueryImpl query =
        new QueryImpl(getFilterBuilder().attribute(Core.RESOURCE_URI).is().equalTo().text(uri2));
    query.setRequestsTotalResultsCount(true);

    QueryRequestImpl queryRequest = new QueryRequestImpl(query);
    SourceResponse sourceResponse = provider.query(queryRequest);

    assertEquals(1, sourceResponse.getResults().size());

    for (Result r : sourceResponse.getResults()) {
      assertTrue(r.getMetacard().getMetadata().contains("Cardinals"));
      assertEquals(uri2, r.getMetacard().getResourceURI().toString());
    }
  }

  private void verifyUpdates(String uri1, String uri2, UpdateResponse updateResponse) {
    assertEquals("Testing Update operation: ", 2, updateResponse.getUpdatedMetacards().size());

    List<Update> updatedMetacards = updateResponse.getUpdatedMetacards();

    for (Update up : updatedMetacards) {

      Metacard newCard = up.getNewMetacard();
      Metacard oldCard = up.getOldMetacard();

      assertNotNull(oldCard.getResourceURI());
      assertEquals(provider.getId(), oldCard.getSourceId());
      assertEquals(provider.getId(), newCard.getSourceId());

      switch (oldCard.getContentTypeName()) {
        case "oldNitf":
          assertEquals("nitf", newCard.getContentTypeName());

          // TPA is unique to the document
          assertTrue(newCard.getMetadata().contains("TPA"));
          assertThat(newCard.getResourceURI(), is(nullValue()));
          assertThat(oldCard.getResourceURI().toString(), equalTo(uri1));

          assertEquals(oldCard.getId(), newCard.getId());
          // Title
          assertEquals(MockMetacard.DEFAULT_TITLE, oldCard.getTitle());
          assertEquals(MockMetacard.DEFAULT_TITLE, newCard.getTitle());
          // Location (decimal points make them not exact Strings POINT(1
          // 0) as opposed to POINT( 1.0 0.0) )
          assertEquals(
              MockMetacard.DEFAULT_LOCATION.substring(0, 8), oldCard.getLocation().substring(0, 8));
          assertEquals(
              MockMetacard.DEFAULT_LOCATION.substring(0, 8), newCard.getLocation().substring(0, 8));
          // Metadata
          assertNotNull(oldCard.getMetadata());
          assertNotNull(newCard.getMetadata());
          assertTrue(!oldCard.getMetadata().isEmpty());
          assertTrue(!newCard.getMetadata().isEmpty());
          // Created Date
          assertFalse(((Date) oldCard.getAttribute(Core.CREATED).getValue()).after(new Date()));
          assertFalse(((Date) newCard.getAttribute(Core.CREATED).getValue()).after(new Date()));
          assertTrue(
              ((Date) newCard.getAttribute(Core.CREATED).getValue())
                  .after((Date) oldCard.getAttribute(Core.CREATED).getValue()));
          // Modified Date
          assertTrue(
              ((Date) newCard.getAttribute(Core.MODIFIED).getValue())
                  .after((Date) oldCard.getAttribute(Core.MODIFIED).getValue()));
          // Effective Date
          assertTrue(newCard.getEffectiveDate().after(oldCard.getEffectiveDate()));
          // Expiration Date
          assertTrue(newCard.getExpirationDate().after(oldCard.getExpirationDate()));
          // Thumbnail
          assertTrue(Arrays.equals(newCard.getThumbnail(), oldCard.getThumbnail()));

          break;
        case "oldNitf2":
          assertEquals("nitf2", newCard.getContentTypeName());

          // Cardinals is unique to the document
          assertTrue(newCard.getMetadata().contains("Cardinals"));

          assertTrue("50L".equals(newCard.getResourceSize()));

          assertEquals(uri2, newCard.getResourceURI().toString());

          assertEquals(oldCard.getId(), newCard.getId());
          // Title
          assertEquals(MockMetacard.DEFAULT_TITLE, oldCard.getTitle());
          assertEquals(MockMetacard.DEFAULT_TITLE, newCard.getTitle());
          // Location (decimal points make them not exact in Strings
          assertEquals(
              MockMetacard.DEFAULT_LOCATION.substring(0, 8), oldCard.getLocation().substring(0, 8));
          assertEquals(
              MockMetacard.DEFAULT_LOCATION.substring(0, 8), newCard.getLocation().substring(0, 8));
          // Metadata
          assertNotNull(oldCard.getMetadata());
          assertNotNull(newCard.getMetadata());
          assertTrue(!oldCard.getMetadata().isEmpty());
          assertTrue(!newCard.getMetadata().isEmpty());
          // Created Date
          assertFalse(((Date) oldCard.getAttribute(Core.CREATED).getValue()).after(new Date()));
          assertFalse(((Date) newCard.getAttribute(Core.CREATED).getValue()).after(new Date()));
          assertTrue(
              ((Date) newCard.getAttribute(Core.CREATED).getValue())
                  .after((Date) oldCard.getAttribute(Core.CREATED).getValue()));
          // Modified Date
          assertTrue(
              ((Date) newCard.getAttribute(Core.MODIFIED).getValue())
                  .after((Date) oldCard.getAttribute(Core.MODIFIED).getValue()));
          // Effective Date
          assertTrue(newCard.getEffectiveDate().after(oldCard.getEffectiveDate()));
          // Expiration Date
          assertTrue(newCard.getExpirationDate().after(oldCard.getExpirationDate()));
          // Thumbnail
          assertTrue(Arrays.equals(newCard.getThumbnail(), oldCard.getThumbnail()));

          break;
        default:
          Assert.fail("Expecting one or the other of the updated records.");
          break;
      }
    }
  }

  @Test
  public void testUpdateWithNullThumbnail() throws Exception {
    verifyAttributeUpdate(Core.THUMBNAIL, null);
  }

  @Test
  public void testUpdateWithNullExpirationDate() throws Exception {
    verifyAttributeUpdate(Core.EXPIRATION, null);
  }

  @Test
  public void testUpdateWithNullContentType() throws Exception {
    verifyAttributeUpdate(Metacard.CONTENT_TYPE, null);
  }

  @Test
  public void testUpdateWithEmptyContentType() throws Exception {
    verifyAttributeUpdate(Metacard.CONTENT_TYPE, "");
  }

  @Test
  public void testUpdateWithNullContentTypeVersion() throws Exception {
    verifyAttributeUpdate(Metacard.CONTENT_TYPE_VERSION, null);
  }

  @Test
  public void testUpdateWithEmptyContentTypeVersion() throws Exception {
    verifyAttributeUpdate(Metacard.CONTENT_TYPE_VERSION, "");
  }

  @Test
  public void testUpdateWithNewResourceUri() throws Exception {
    String uri1 = "http://youwillfindme.com/here/now";
    verifyAttributeUpdate(Core.RESOURCE_URI, uri1);
  }

  private void verifyAttributeUpdate(String attributeName, Serializable updatedValue)
      throws Exception {
    deleteAll(provider);
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(0);

    // CREATE
    MockMetacard metacard1 =
        new MockMetacard(Library.getFlagstaffRecord(), MetacardImpl.BASIC_METACARD, calendar);
    metacard1.setResourceURI(new URI("http://youwillfindme.com/here"));
    create(metacard1, provider);

    // UPDATE
    MockMetacard updatedMetacard1 =
        new MockMetacard(Library.getTampaRecord(), MetacardImpl.BASIC_METACARD, calendar);
    updatedMetacard1.setAttribute(attributeName, updatedValue);
    UpdateResponse updateResponse = update(metacard1.getId(), updatedMetacard1, provider);

    // VERIFY
    assertEquals(
        "Testing Update operation: ", ONE_HIT, updateResponse.getUpdatedMetacards().size());

    Metacard newCard = updateResponse.getUpdatedMetacards().get(0).getNewMetacard();
    Metacard oldCard = updateResponse.getUpdatedMetacards().get(0).getOldMetacard();

    Serializable oldValue = null;
    if (oldCard.getAttribute(attributeName) != null) {
      oldValue = oldCard.getAttribute(attributeName).getValue();
    }
    assertThat(oldValue, is(metacard1.getAttribute(attributeName).getValue()));

    Serializable newValue = null;
    if (newCard.getAttribute(attributeName) != null) {
      newValue = newCard.getAttribute(attributeName).getValue();
    }
    assertThat(newValue, is(updatedValue));
  }

  @Test
  public void testNullUpdate() throws IngestException {
    UpdateResponse updateResponse =
        provider.update(
            new UpdateRequest() {
              @Override
              public boolean hasProperties() {
                return false;
              }

              @Override
              public Serializable getPropertyValue(String name) {
                return null;
              }

              @Override
              public Set<String> getPropertyNames() {
                return null;
              }

              @Override
              public Map<String, Serializable> getProperties() {
                return null;
              }

              @Override
              public boolean containsPropertyName(String name) {
                return false;
              }

              @Override
              public List<Map.Entry<Serializable, Metacard>> getUpdates() {
                return null;
              }

              @Override
              public String getAttributeName() {
                return UpdateRequest.UPDATE_BY_ID;
              }
            });

    assertTrue(updateResponse.getUpdatedMetacards().isEmpty());
  }

  /** Testing that if no records are found to update, the provider returns an empty list. */
  @Test
  public void testUpdateOperationWithNoResults() throws IngestException, UnsupportedQueryException {

    deleteAll(provider);

    MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());

    UpdateResponse response = update("BAD_ID", metacard, provider);

    assertEquals(0, response.getUpdatedMetacards().size());
  }

  /** Testing update operation of alternative attribute. Should return positive results. */
  @Test
  public void testUpdateAlternativeAttribute() throws IngestException, UnsupportedQueryException {

    deleteAll(provider);

    final MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());

    create(metacard, provider);

    UpdateResponse response =
        provider.update(
            new UpdateRequest() {

              @Override
              public boolean hasProperties() {
                return false;
              }

              @Override
              public Serializable getPropertyValue(String name) {
                return null;
              }

              @Override
              public Set<String> getPropertyNames() {
                return null;
              }

              @Override
              public Map<String, Serializable> getProperties() {
                return null;
              }

              @Override
              public boolean containsPropertyName(String name) {
                return false;
              }

              @Override
              public List<Map.Entry<Serializable, Metacard>> getUpdates() {

                MetacardImpl newMetacard = new MetacardImpl(metacard);

                newMetacard.setContentTypeName("newContentName");

                List<Map.Entry<Serializable, Metacard>> updateList = new ArrayList<>();

                updateList.add(
                    new AbstractMap.SimpleEntry<>(MockMetacard.DEFAULT_TITLE, newMetacard));

                return updateList;
              }

              @Override
              public String getAttributeName() {
                return Core.TITLE;
              }
            });

    Update update = response.getUpdatedMetacards().get(0);

    assertThat(update.getNewMetacard().getId(), is(equalTo(update.getOldMetacard().getId())));

    assertEquals(1, response.getUpdatedMetacards().size());
  }

  /** Tests if we catch properly the case that the attribute value matches multiple Metacards. */
  @Test(expected = IngestException.class)
  public void testUpdateNonUniqueAttributeValue()
      throws IngestException, UnsupportedQueryException {

    deleteAll(provider);

    MockMetacard m1 = new MockMetacard(Library.getFlagstaffRecord());
    MockMetacard m2 = new MockMetacard(Library.getFlagstaffRecord());
    MockMetacard m3 = new MockMetacard(Library.getFlagstaffRecord());

    List<Metacard> list = Arrays.asList(m1, m2, m3);

    create(list, provider);

    provider.update(
        new UpdateRequest() {

          @Override
          public boolean hasProperties() {
            return false;
          }

          @Override
          public Serializable getPropertyValue(String name) {
            return null;
          }

          @Override
          public Set<String> getPropertyNames() {
            return null;
          }

          @Override
          public Map<String, Serializable> getProperties() {
            return null;
          }

          @Override
          public boolean containsPropertyName(String name) {
            return false;
          }

          @Override
          public List<Map.Entry<Serializable, Metacard>> getUpdates() {

            MockMetacard newMetacard = new MockMetacard(Library.getShowLowRecord());

            List<Map.Entry<Serializable, Metacard>> updateList = new ArrayList<>();

            updateList.add(new AbstractMap.SimpleEntry<>(MockMetacard.DEFAULT_TITLE, newMetacard));

            return updateList;
          }

          @Override
          public String getAttributeName() {
            return Core.TITLE;
          }
        });
  }

  /**
   * Tests if we catch a rare case where some attribute value match multiple Metacards while others
   * do not match any records.
   */
  @Test(expected = IngestException.class)
  public void testUpdateNonUniqueAttributeValue2()
      throws IngestException, UnsupportedQueryException {

    deleteAll(provider);

    MockMetacard m1 = new MockMetacard(Library.getFlagstaffRecord());
    MockMetacard m2 = new MockMetacard(Library.getFlagstaffRecord());

    List<Metacard> list = Arrays.asList(m1, m2);

    create(list, provider);

    provider.update(
        new UpdateRequest() {

          @Override
          public boolean hasProperties() {
            return false;
          }

          @Override
          public Serializable getPropertyValue(String name) {
            return null;
          }

          @Override
          public Set<String> getPropertyNames() {
            return null;
          }

          @Override
          public Map<String, Serializable> getProperties() {
            return null;
          }

          @Override
          public boolean containsPropertyName(String name) {
            return false;
          }

          @Override
          public List<Map.Entry<Serializable, Metacard>> getUpdates() {

            MockMetacard newMetacard = new MockMetacard(Library.getShowLowRecord());

            List<Map.Entry<Serializable, Metacard>> updateList = new ArrayList<>();

            updateList.add(new AbstractMap.SimpleEntry<>(MockMetacard.DEFAULT_TITLE, newMetacard));
            updateList.add(new AbstractMap.SimpleEntry<>(Library.TAMPA_QUERY_PHRASE, newMetacard));

            return updateList;
          }

          @Override
          public String getAttributeName() {
            return Core.TITLE;
          }
        });
  }

  /** Testing if exception is thrown with a <code>null</code> property. */
  @Test(expected = IngestException.class)
  public void testUpdateNullAttribute() throws IngestException {
    provider.update(
        new UpdateRequest() {

          @Override
          public boolean hasProperties() {
            return false;
          }

          @Override
          public Serializable getPropertyValue(String name) {
            return null;
          }

          @Override
          public Set<String> getPropertyNames() {
            return null;
          }

          @Override
          public Map<String, Serializable> getProperties() {
            return null;
          }

          @Override
          public boolean containsPropertyName(String name) {
            return false;
          }

          @Override
          public List<Map.Entry<Serializable, Metacard>> getUpdates() {
            return null;
          }

          @Override
          public String getAttributeName() {
            return null;
          }
        });
  }

  /** Testing update operation of unknown attribute. */
  @Test(expected = IngestException.class)
  public void testUpdateUnknownAttribute() throws IngestException, UnsupportedQueryException {
    deleteAll(provider);

    provider.update(
        new UpdateRequest() {

          @Override
          public boolean hasProperties() {
            return false;
          }

          @Override
          public Serializable getPropertyValue(String name) {
            return null;
          }

          @Override
          public Set<String> getPropertyNames() {
            return null;
          }

          @Override
          public Map<String, Serializable> getProperties() {
            return null;
          }

          @Override
          public boolean containsPropertyName(String name) {
            return false;
          }

          @Override
          public List<Map.Entry<Serializable, Metacard>> getUpdates() {
            MockMetacard newMetacard = new MockMetacard(Library.getShowLowRecord());

            List<Map.Entry<Serializable, Metacard>> updateList = new ArrayList<>();

            updateList.add(new AbstractMap.SimpleEntry<>(MockMetacard.DEFAULT_TITLE, newMetacard));

            return updateList;
          }

          @Override
          public String getAttributeName() {
            return "dataAccess";
          }
        });
  }

  @Test
  public void testUpdatePendingNrtIndex() throws Exception {
    deleteAll(provider);
    ConfigurationStore.getInstance().setForceAutoCommit(false);

    try {
      MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());

      CreateResponse createResponse = create(metacard, provider);

      String id = createResponse.getCreatedMetacards().get(0).getId();

      MockMetacard updatedMetacard = new MockMetacard(Library.getFlagstaffRecord());
      updatedMetacard.setContentTypeName("first");
      UpdateResponse firstUpdateResponse = update(id, updatedMetacard, provider);

      updatedMetacard = new MockMetacard(Library.getFlagstaffRecord());
      updatedMetacard.setContentTypeName("second");
      UpdateResponse secondUpdateResponse = update(id, updatedMetacard, provider);

      verifyContentTypeUpdate(firstUpdateResponse, MockMetacard.DEFAULT_TYPE, "first");
      verifyContentTypeUpdate(secondUpdateResponse, "first", "second");
    } finally {
      ConfigurationStore.getInstance().setForceAutoCommit(true);
    }
  }

  private void verifyContentTypeUpdate(
      UpdateResponse response, String oldContentType, String newContentType) {
    Update update = response.getUpdatedMetacards().get(0);

    Metacard newMetacard = update.getNewMetacard();

    Metacard oldMetacard = update.getOldMetacard();

    assertThat(response.getUpdatedMetacards().size(), is(1));

    assertThat(oldMetacard.getContentTypeName(), is(oldContentType));
    assertThat(newMetacard.getContentTypeName(), is(newContentType));
  }
}
