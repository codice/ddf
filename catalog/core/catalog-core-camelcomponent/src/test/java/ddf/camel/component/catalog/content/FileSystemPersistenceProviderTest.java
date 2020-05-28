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
package ddf.camel.component.catalog.content;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

public class FileSystemPersistenceProviderTest {

  final String testKey = "testKey";

  final String testObj = "testObj";

  FileSystemPersistenceProvider fileSystemPersistenceProvider;

  FileSystemDataAccessObject mockFileSystemDao;

  @Before
  public void setup() {
    // mock out dao
    mockFileSystemDao = mock(FileSystemDataAccessObject.class);
    doReturn(testObj).when(mockFileSystemDao).loadFromPersistence(any(), any(), any());
    doReturn(ImmutableSet.of(testKey)).when(mockFileSystemDao).loadAllKeys(any(), any(), any());

    // create and verify instance, inject mock dao
    fileSystemPersistenceProvider = new FileSystemPersistenceProvider("test");
    fileSystemPersistenceProvider.fileSystemDataAccessObject = mockFileSystemDao;
    assertThat(
        fileSystemPersistenceProvider.getMapStorePath(),
        is(
            Paths.get(fileSystemPersistenceProvider.getPersistencePath(), "test").toString()
                + File.separator));
  }

  @Test
  public void testStore() throws Exception {
    fileSystemPersistenceProvider.store(testKey, testObj);
    verify(mockFileSystemDao, times(1)).store(any(), any(), eq(testKey), eq(testObj));
  }

  @Test
  public void testStoreAll() throws Exception {
    fileSystemPersistenceProvider.storeAll(
        ImmutableMap.of(testKey, testObj, "test2Key", "test2Obj"));
    verify(mockFileSystemDao, times(2)).store(any(), any(), any(), any());
  }

  @Test
  public void testLoadFromPersistence() throws Exception {
    String result = (String) fileSystemPersistenceProvider.loadFromPersistence(testKey);
    verify(mockFileSystemDao, times(1)).loadFromPersistence(any(), any(), eq(testKey));
    assertThat(result, is(testObj));
  }

  @Test
  public void testLoadAll() throws Exception {
    Map result = fileSystemPersistenceProvider.loadAll(ImmutableList.of(testKey, "otherKey"));
    verify(mockFileSystemDao, times(2)).loadFromPersistence(any(), any(), any());
    assertThat(result.size(), is(2));
  }

  @Test
  public void testLoadKeys() throws Exception {
    Set keys = fileSystemPersistenceProvider.loadAllKeys();
    verify(mockFileSystemDao, times(1)).loadAllKeys(any(), any(), any());
    assertThat(keys.contains(testKey), is(true));
  }

  @Test
  public void testDelete() throws Exception {
    File testFile = new File(fileSystemPersistenceProvider.getMapStorePath() + testKey + ".ser");
    fileSystemPersistenceProvider.delete(testKey);
    assertThat(testFile.exists(), is(false));
  }

  @Test
  public void testDeleteAll() throws Exception {
    File testFile = new File(fileSystemPersistenceProvider.getMapStorePath() + testKey + ".ser");
    File testFile2 =
        new File(fileSystemPersistenceProvider.getMapStorePath() + "testKey2" + ".ser");
    fileSystemPersistenceProvider.deleteAll(ImmutableList.of(testKey, "testKey2"));
    assertThat(testFile.exists(), is(false));
    assertThat(testFile2.exists(), is(false));
  }
}
