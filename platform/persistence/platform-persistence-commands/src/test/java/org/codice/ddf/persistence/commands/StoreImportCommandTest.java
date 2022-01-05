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
package org.codice.ddf.persistence.commands;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentStore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StoreImportCommandTest {
  @Mock private PersistentStore persistentStore;

  @Captor private ArgumentCaptor<List<Map<String, Object>>> itemsCaptor;

  private final StoreImportCommand command = new StoreImportCommand();

  @Before
  public void setUp() {
    command.persistentStore = persistentStore;
  }

  @Test
  public void testImportFile() throws PersistenceException {
    command.type = "metacard";
    command.filePath = getClass().getResource("/test.json").getPath();
    command.storeCommand();

    verify(persistentStore).add(eq("metacard"), itemsCaptor.capture());

    final List<Map<String, Object>> items = itemsCaptor.getValue();
    assertThat(items, hasSize(1));

    final Map<String, Object> item = items.get(0);
    // test_bin in the JSON is the string "foobar" base64-encoded. These are the ASCII byte values
    // of those characters.
    assertThat(item, hasEntry("test_bin", new byte[] {102, 111, 111, 98, 97, 114}));
    assertThat(item, hasEntry("test_tdt", new Date(1639676820000L)));
    assertThat(item, hasEntry("test_lng", 1L));
    assertThat(item, hasEntry("test_int", 2));
    assertThat(item, hasEntry("test_txt", "Some text"));
    assertThat(item, hasEntry("test_xml", "<xml>test</xml>"));
    assertThat(item, hasEntry("test_arr_lng", Arrays.asList(3L, 4L)));
  }
}
