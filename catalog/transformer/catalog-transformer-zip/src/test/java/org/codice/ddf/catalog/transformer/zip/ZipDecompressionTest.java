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
package org.codice.ddf.catalog.transformer.zip;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import ddf.catalog.data.Metacard;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class ZipDecompressionTest {

  private static final String ZIP_FILE_NAME = "/signed.zip";

  private ZipDecompression zipDecompression;

  private InputStream zipInputStream;

  private Map<String, Serializable> arguments;

  private List<String> zipContentList = Arrays.asList("id1", "id2", "id3");

  @Before
  public void setUp() throws Exception {
    zipDecompression = new ZipDecompression();
    zipInputStream = getClass().getResourceAsStream(ZIP_FILE_NAME);
    arguments = new HashMap<>();
    arguments.put(ZipDecompression.FILE_PATH, "target/");
    arguments.put(ZipDecompression.FILE_NAME, ZIP_FILE_NAME);
  }

  @Test(expected = CatalogTransformerException.class)
  public void testDecompressionWithNullArguments() throws Exception {
    List<Metacard> result = zipDecompression.transform(zipInputStream, null);
    assertThat(result, nullValue());
  }

  @Test(expected = CatalogTransformerException.class)
  public void testDecompressionWithEmptyArguments() throws Exception {
    List<Metacard> result = zipDecompression.transform(zipInputStream, new HashMap<>());
    assertThat(result, nullValue());
  }

  @Test(expected = CatalogTransformerException.class)
  public void testDecompressionWithNoValidArguments() throws Exception {
    Map<String, Serializable> badMap = new HashMap<>();
    badMap.put("bad", "arg");
    List<Metacard> result = zipDecompression.transform(zipInputStream, badMap);
    assertThat(result, nullValue());
  }

  @Test(expected = CatalogTransformerException.class)
  public void testDecompressionWithNoFileNameArgument() throws Exception {
    Map<String, Serializable> badMap = new HashMap<>();
    badMap.put(ZipDecompression.FILE_PATH, "arg");
    List<Metacard> result = zipDecompression.transform(zipInputStream, badMap);
    assertThat(result, nullValue());
  }

  @Test(expected = CatalogTransformerException.class)
  public void testDecompressionWithNullStream() throws Exception {
    List<Metacard> result = zipDecompression.transform(null, arguments);
    assertThat(result, nullValue());
  }

  @Test
  public void testDecompressionValidZip() throws Exception {
    List<Metacard> result = zipDecompression.transform(zipInputStream, arguments);
    assertThat(result, notNullValue());
    assertMetacardList(result);
  }

  public void assertMetacardList(List<Metacard> metacardList) {
    for (Metacard metacard : metacardList) {
      assertThat(zipContentList, hasItem(metacard.getId()));
    }
    assertThat(metacardList.size(), is(zipContentList.size()));
  }
}
