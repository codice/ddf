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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.concurrent.ExecutorService;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractor;
import org.codice.ddf.spatial.geocoding.GeoEntryIndexer;
import org.codice.ddf.spatial.geocoding.ProgressCallback;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.stubbing.Answer;

public class IndexInitializerTest {

  private GeoEntryExtractor extractor;

  private GeoEntryIndexer indexer;

  private ExecutorService executor;

  private IndexInitializer indexInitializer;

  @Rule public TemporaryFolder tempDir = new TemporaryFolder();

  private File dataDir;

  private File geonamesZip;

  private File geoIndexDir;

  @Before
  public void setup() throws Exception {
    extractor = mock(GeoEntryExtractor.class);
    indexer = mock(GeoEntryIndexer.class);
    executor = mock(ExecutorService.class);
    indexInitializer = new IndexInitializer();
    indexInitializer.setExecutor(executor);
    indexInitializer.setExtractor(extractor);
    indexInitializer.setIndexer(indexer);
    dataDir = tempDir.newFolder("data");
    geonamesZip = new File(dataDir, "default_geonames_data.zip");
    geoIndexDir = new File(dataDir, "geonames-index");
    indexInitializer.setDefaultGeonamesDataPath(geonamesZip.getAbsolutePath());
    indexInitializer.setIndexLocationPath(geoIndexDir.getAbsolutePath());
  }

  @Test
  public void testIndexInitializerNoDataFile() throws Exception {
    indexInitializer.init();
    verify(executor, never()).submit(any(Runnable.class));
  }

  @Test
  public void testIndexInitializerExistingIndex() throws Exception {
    geoIndexDir.mkdirs();
    new File(geoIndexDir, "somefile.txt").createNewFile();
    geonamesZip.createNewFile();
    indexInitializer.init();
    verify(executor, never()).submit(any(Runnable.class));
  }

  @Test
  public void testIndexInitializerEmptyIndex() throws Exception {
    geoIndexDir.mkdirs();
    geonamesZip.createNewFile();
    indexInitializer.init();
    verify(executor).submit(any(Runnable.class));
  }

  @Test
  public void testIndexInitializerRun() throws Exception {
    when(executor.submit(any(Runnable.class)))
        .then(
            (Answer)
                invocationOnMock -> {
                  invocationOnMock.getArgumentAt(0, Runnable.class).run();
                  return null;
                });
    geoIndexDir.mkdirs();
    geonamesZip.createNewFile();
    indexInitializer.init();
    verify(indexer)
        .updateIndex(
            anyString(), any(GeoEntryExtractor.class), anyBoolean(), any(ProgressCallback.class));
  }
}
