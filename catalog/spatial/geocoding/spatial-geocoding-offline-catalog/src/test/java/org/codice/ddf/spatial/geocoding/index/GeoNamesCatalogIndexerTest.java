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
package org.codice.ddf.spatial.geocoding.index;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.codice.ddf.spatial.geocoding.GeoCodingConstants;
import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryAttributes;
import org.codice.ddf.spatial.geocoding.GeoEntryCreator;
import org.codice.ddf.spatial.geocoding.ProgressCallback;
import org.codice.ddf.spatial.geocoding.extract.GeoNamesFileExtractor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class GeoNamesCatalogIndexerTest {

  private static final String GOOD_FILE_PATH =
      GeoNamesCatalogIndexerTest.class
          .getClassLoader()
          .getResource("goodGeoNamesFile.txt")
          .getPath();

  private static final String LARGE_FILE_PATH =
      GeoNamesCatalogIndexerTest.class
          .getClassLoader()
          .getResource("largeGeoNamesFile.txt")
          .getPath();

  private static final String GEO_ENTRY_DATA_SOURCE = "AD.zip";

  private static final String ZIP_FILE_PATH =
      GeoNamesCatalogIndexerTest.class
          .getClassLoader()
          .getResource(GEO_ENTRY_DATA_SOURCE)
          .getPath();

  private static final GeoEntry GEO_ENTRY =
      new GeoEntry.Builder().featureCode(GeoCodingConstants.POPULATED_PLACE).name("Test").build();

  private static final Metacard METACARD = new MetacardImpl();

  private GeoNamesCatalogIndexer geoNamesCatalogIndexer;

  private CatalogFramework catalogFramework;

  private ProgressCallback progressCallback;

  private CreateResponse createResponse;

  private QueryResponse queryResponse;

  private GeoNamesFileExtractor geoEntryExtractor;

  private GeoEntryCreator geoEntryCreator;

  private CatalogProvider catalogProvider;

  private UuidGenerator uuidGenerator;

  @Before
  public void setUp() throws Exception {

    geoEntryCreator = mock(GeoEntryCreator.class);
    when(geoEntryCreator.createGeoEntry(anyString(), anyString())).thenReturn(GEO_ENTRY);
    geoEntryExtractor = new GeoNamesFileExtractor();
    geoEntryExtractor.setGeoEntryCreator(geoEntryCreator);

    catalogFramework = mock(CatalogFramework.class);
    uuidGenerator = mock(UuidGenerator.class);
    createResponse = mock(CreateResponse.class);
    when(createResponse.getCreatedMetacards()).thenReturn(Collections.singletonList(METACARD));
    when(catalogFramework.create(any(CreateRequest.class))).thenReturn(createResponse);
    when(uuidGenerator.generateUuid()).thenReturn(UUID.randomUUID().toString());

    catalogProvider = mock(CatalogProvider.class);
    DeleteResponse deleteResponse = mock(DeleteResponse.class);
    when(deleteResponse.getDeletedMetacards()).thenReturn(Collections.singletonList(METACARD));
    when(catalogProvider.delete(any(DeleteRequest.class))).thenReturn(deleteResponse);

    queryResponse = mock(QueryResponse.class);
    when(queryResponse.getResults())
        .thenReturn(Collections.singletonList(new ResultImpl(new MetacardImpl())));
    when(catalogFramework.query(any(QueryRequest.class))).thenReturn(queryResponse);

    progressCallback = progress -> progress++;
    geoNamesCatalogIndexer =
        new GeoNamesCatalogIndexer(
            catalogFramework,
            uuidGenerator,
            new GeoEntryAttributes(),
            new GeotoolsFilterBuilder(),
            Collections.singletonList(catalogProvider));
  }

  @After
  public void tearDown() {
    File goodFile = new File(GOOD_FILE_PATH + ".processed");
    if (goodFile.exists()) {
      goodFile.delete();
    }

    File largeFile = new File(LARGE_FILE_PATH + ".processed");
    if (largeFile.exists()) {
      largeFile.delete();
    }

    File zipFile = new File(ZIP_FILE_PATH + ".processed");
    if (zipFile.exists()) {
      zipFile.delete();
    }
  }

  @Test
  public void testUpdateIndexEmptyString() throws Exception {
    geoNamesCatalogIndexer.updateIndex(null, geoEntryExtractor, false, progressCallback);
    verify(catalogFramework, never()).create(any(CreateRequest.class));
  }

  @Test
  public void testUpdateIndex() throws Exception {
    geoNamesCatalogIndexer.updateIndex(GOOD_FILE_PATH, geoEntryExtractor, false, progressCallback);
    verify(catalogFramework, times(1)).create(any(CreateRequest.class));
  }

  @Test
  public void testUpdateIndexWithCreate() throws Exception {
    setupTestForCreate();
    geoNamesCatalogIndexer.updateIndex(GOOD_FILE_PATH, geoEntryExtractor, true, progressCallback);
    verify(catalogFramework, times(1)).create(any(CreateRequest.class));
    verify(catalogProvider, times(1)).delete(any(DeleteRequest.class));
    verify(catalogFramework, times(2)).query(any(QueryRequest.class));
  }

  @Test
  public void testUpdateIndexWithCreateIngestException() throws Exception {
    when(catalogProvider.delete(any(DeleteRequest.class))).thenThrow(IngestException.class);
    setupTestForCreate();
    geoNamesCatalogIndexer.updateIndex(GOOD_FILE_PATH, geoEntryExtractor, true, progressCallback);
    verify(catalogFramework, times(1)).create(any(CreateRequest.class));
    verify(catalogProvider, times(1)).delete(any(DeleteRequest.class));
    verify(catalogFramework, times(2)).query(any(QueryRequest.class));
  }

  @Test
  public void testUpdateIndexWithCreateNoCatalogProvider() throws Exception {
    geoNamesCatalogIndexer =
        new GeoNamesCatalogIndexer(
            catalogFramework,
            uuidGenerator,
            new GeoEntryAttributes(),
            new GeotoolsFilterBuilder(),
            Collections.emptyList());
    geoNamesCatalogIndexer.updateIndex(GOOD_FILE_PATH, geoEntryExtractor, true, progressCallback);
    verify(catalogFramework, times(1)).create(any(CreateRequest.class));
    verify(catalogProvider, times(0)).delete(any(DeleteRequest.class));
    verify(catalogFramework, times(0)).query(any(QueryRequest.class));
  }

  @Test
  public void testUpdateIndexWithCreateNoResults() throws Exception {
    when(queryResponse.getResults()).thenReturn(Collections.emptyList());
    geoNamesCatalogIndexer.updateIndex(GOOD_FILE_PATH, geoEntryExtractor, true, progressCallback);
    verify(catalogFramework, times(1)).create(any(CreateRequest.class));
    verify(catalogProvider, times(0)).delete(any(DeleteRequest.class));
    verify(catalogFramework, times(1)).query(any(QueryRequest.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testUpdateIndexWithFailedDeleteIngestException() throws Exception {
    setupTestForCreate();
    when(catalogFramework.delete(any(DeleteRequest.class))).thenThrow(IngestException.class);
    geoNamesCatalogIndexer.updateIndex(GOOD_FILE_PATH, geoEntryExtractor, true, progressCallback);
    verify(catalogFramework, times(1)).create(any(CreateRequest.class));
    verify(catalogProvider, times(1)).delete(any(DeleteRequest.class));
    verify(catalogFramework, times(2)).query(any(QueryRequest.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testUpdateIndexWithFailedDeleteSourceUnavailableException() throws Exception {
    setupTestForCreate();
    when(catalogFramework.delete(any(DeleteRequest.class)))
        .thenThrow(SourceUnavailableException.class);
    geoNamesCatalogIndexer.updateIndex(GOOD_FILE_PATH, geoEntryExtractor, true, progressCallback);
    verify(catalogFramework, times(1)).create(any(CreateRequest.class));
    verify(catalogProvider, times(1)).delete(any(DeleteRequest.class));
    verify(catalogFramework, times(2)).query(any(QueryRequest.class));
  }

  @Test
  public void testUpdateIndexBadGeoEntry() throws Exception {
    when(geoEntryCreator.createGeoEntry(anyString(), anyString()))
        .thenReturn(new GeoEntry.Builder().name("Test").build());
    geoNamesCatalogIndexer.updateIndex(GOOD_FILE_PATH, geoEntryExtractor, false, progressCallback);
    verify(catalogFramework, times(0)).create(any(CreateRequest.class));
  }

  @Test
  public void testUpdateIndexZip() throws Exception {
    geoNamesCatalogIndexer.updateIndex(ZIP_FILE_PATH, geoEntryExtractor, false, progressCallback);
    verify(catalogFramework, times(13)).create(any(CreateRequest.class));
  }

  @Test
  public void testUpdateIndexNullCallback() throws Exception {
    geoNamesCatalogIndexer.updateIndex(GOOD_FILE_PATH, geoEntryExtractor, false, null);
    verify(catalogFramework, times(1)).create(any(CreateRequest.class));
  }

  @Test
  public void testUpdateIndexBatches() throws Exception {
    List<Metacard> metacardList = new ArrayList<>();
    int totalExpectedEntries = 360;
    for (int i = 0; i < totalExpectedEntries; i++) {
      metacardList.add(METACARD);
    }

    ArgumentCaptor<CreateRequest> createRequestArgumentCaptor =
        ArgumentCaptor.forClass(CreateRequest.class);

    when(createResponse.getCreatedMetacards()).thenReturn(metacardList);

    geoNamesCatalogIndexer.updateIndex(LARGE_FILE_PATH, geoEntryExtractor, false, progressCallback);
    verify(catalogFramework, times(2)).create(createRequestArgumentCaptor.capture());

    List<CreateRequest> createRequestList = createRequestArgumentCaptor.getAllValues();
    int totalEntries = 0;
    for (CreateRequest createRequest : createRequestList) {
      totalEntries += createRequest.getMetacards().size();
    }
    assertThat(totalEntries, is(totalExpectedEntries));
  }

  @Test
  public void testUpdateIndexNullResponse() throws Exception {
    when(createResponse.getCreatedMetacards()).thenReturn(null);
    geoNamesCatalogIndexer.updateIndex(GOOD_FILE_PATH, geoEntryExtractor, false, progressCallback);
    verify(catalogFramework, times(1)).create(any(CreateRequest.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testUpdateIndexIngestException() throws Exception {
    when(catalogFramework.create(any(CreateRequest.class))).thenThrow(IngestException.class);
    geoNamesCatalogIndexer.updateIndex(GOOD_FILE_PATH, geoEntryExtractor, false, progressCallback);
    verify(catalogFramework, times(1)).create(any(CreateRequest.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testUpdateIndexSourceUnavailableException() throws Exception {
    when(catalogFramework.create(any(CreateRequest.class)))
        .thenThrow(SourceUnavailableException.class);
    geoNamesCatalogIndexer.updateIndex(GOOD_FILE_PATH, geoEntryExtractor, false, progressCallback);
    verify(catalogFramework, times(1)).create(any(CreateRequest.class));
  }

  @Test
  public void testUpdateIndexWithGeoEntryList() throws Exception {
    List<GeoEntry> geoEntries = Collections.singletonList(GEO_ENTRY);
    geoNamesCatalogIndexer.updateIndex(geoEntries, false, progressCallback, GEO_ENTRY_DATA_SOURCE);
    verify(catalogFramework, times(1)).create(any(CreateRequest.class));
    verify(catalogProvider, times(0)).delete(any(DeleteRequest.class));
    verify(catalogFramework, times(0)).query(any(QueryRequest.class));
  }

  @Test
  public void testUpdateIndexWithGeoEntryListWithCreate() throws Exception {
    setupTestForCreate();

    List<GeoEntry> geoEntries = Collections.singletonList(GEO_ENTRY);
    geoNamesCatalogIndexer.updateIndex(geoEntries, true, progressCallback, GEO_ENTRY_DATA_SOURCE);
    verify(catalogFramework, times(1)).create(any(CreateRequest.class));
    verify(catalogProvider, times(1)).delete(any(DeleteRequest.class));
    verify(catalogFramework, times(2)).query(any(QueryRequest.class));
  }

  private void setupTestForCreate() throws Exception {
    queryResponse = mock(QueryResponse.class);
    when(queryResponse.getResults()).thenReturn(Collections.emptyList());
    when(queryResponse.getResults())
        .thenReturn(Collections.singletonList(new ResultImpl(new MetacardImpl())))
        .thenReturn(Collections.emptyList());
    when(catalogFramework.query(any(QueryRequest.class))).thenReturn(queryResponse);
  }
}
