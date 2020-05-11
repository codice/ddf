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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.util.concurrent.ExecutorService;
import org.codice.ddf.security.impl.Security;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractor;
import org.codice.ddf.spatial.geocoding.GeoEntryIndexer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class IndexInitializerTest {

  @Rule public TemporaryFolder tempDir = new TemporaryFolder();

  private GeoEntryIndexer indexer;

  private ExecutorService executor;

  private IndexInitializer indexInitializer;

  private File geonamesZip;

  private File geoIndexDir;

  @Before
  public void setUp() throws Exception {
    GeoEntryExtractor extractor = mock(GeoEntryExtractor.class);
    indexer = mock(GeoEntryIndexer.class);
    executor = mock(ExecutorService.class);
    indexInitializer = new IndexInitializer(new Security());
    indexInitializer.setExecutor(executor);
    indexInitializer.setExtractor(extractor);
    indexInitializer.setIndexer(indexer);
    File dataDir = tempDir.newFolder("data");
    geonamesZip = new File(dataDir, "default_geonames_data.zip");
    geoIndexDir = new File(dataDir, "geonames-index");
    indexInitializer.setDefaultGeoNamesDataPath(geonamesZip.getAbsolutePath());
  }

  @Test
  public void testIndexInitializerNoDataFile() throws Exception {
    indexInitializer.init();
    verify(executor, never()).submit(any(Runnable.class));
  }

  @Test
  public void testIndexInitializer() throws Exception {
    geoIndexDir.mkdirs();
    geonamesZip.createNewFile();
    indexInitializer.init();
    verify(executor).submit(any(Runnable.class));
  }
}
