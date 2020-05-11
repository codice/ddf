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
package org.codice.ddf.persistence.attributes.internal;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.codice.ddf.persistence.PersistentStore.PersistenceType;
import org.codice.ddf.persistence.attributes.AttributesStore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class AttributesStoreImplTest {

  private AttributesStoreImpl attributesStore;

  private List<Map<String, Object>> attributesList;

  private PersistentStore persistentStore = mock(PersistentStore.class);

  private static final String USER = "user";

  private static final String CQL = String.format("%s = '%s'", AttributesStoreImpl.USER_KEY, USER);

  private static final String DATA_USAGE_LONG =
      AttributesStore.DATA_USAGE_KEY + PersistentItem.LONG_SUFFIX;

  private static final String DATA_LIMIT_LONG =
      AttributesStore.DATA_USAGE_LIMIT_KEY + PersistentItem.LONG_SUFFIX;

  private static final Long LONG_1 = 100L;

  private static final Long LONG_2 = 200L;

  private static final Long LONG_5 = 500L;

  @Before
  public void setup() {
    attributesStore = new AttributesStoreImpl(persistentStore);
  }

  @Test
  public void testGetDataUsage() throws PersistenceException {
    attributesList = new ArrayList<>();
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(DATA_USAGE_LONG, LONG_1);
    attributesList.add(attributes);
    when(persistentStore.get(anyString(), anyString())).thenReturn(attributesList);

    long usage = attributesStore.getCurrentDataUsageByUser(USER);

    assertThat(usage, is(LONG_1));
  }

  @Test
  public void testUpdateDataUsage() throws PersistenceException {
    ArgumentCaptor<String> keyArg1 = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> keyArg2 = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> cqlArg = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<PersistentItem> itemArg = ArgumentCaptor.forClass(PersistentItem.class);

    attributesList = new ArrayList<>();
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(DATA_USAGE_LONG, LONG_1);
    attributes.put(DATA_LIMIT_LONG, LONG_1);
    attributesList.add(attributes);
    when(persistentStore.get(anyString(), anyString())).thenReturn(attributesList);

    attributesStore.updateUserDataUsage(USER, LONG_5);

    verify(persistentStore, atLeast(2)).get(keyArg1.capture(), cqlArg.capture());
    verify(persistentStore).add(keyArg2.capture(), itemArg.capture());

    assertThat(keyArg1.getValue(), is(PersistenceType.USER_ATTRIBUTE_TYPE.toString()));
    assertThat(keyArg2.getValue(), is(PersistenceType.USER_ATTRIBUTE_TYPE.toString()));

    assertThat(itemArg.getValue().getLongProperty(AttributesStore.DATA_USAGE_KEY), is(600L));

    assertThat(cqlArg.getValue(), is(CQL));
  }

  @Test
  public void testSetDataUsage() throws PersistenceException {

    final long DATA_USAGE = LONG_5;
    ArgumentCaptor<String> keyArg = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<PersistentItem> itemArg = ArgumentCaptor.forClass(PersistentItem.class);
    attributesStore.setDataUsage(USER, DATA_USAGE);
    verify(persistentStore).add(keyArg.capture(), itemArg.capture());
    assertThat(keyArg.getValue(), is(PersistenceType.USER_ATTRIBUTE_TYPE.toString()));

    assertThat(itemArg.getValue().getLongProperty(AttributesStore.DATA_USAGE_KEY), is(DATA_USAGE));
  }

  @Test
  public void testSetDataUsageSizeLessThanZero() throws PersistenceException {

    long dataUsage = -1L;
    attributesStore.setDataUsage(USER, dataUsage);
    verify(persistentStore, never()).add(anyString(), anyMap());
  }

  @Test
  public void testUpdateDataUsageSizeLessThanZero() throws PersistenceException {

    long dataUsage = -1L;
    attributesStore.updateUserDataUsage(USER, dataUsage);
    verify(persistentStore, never()).add(anyString(), anyMap());
  }

  @Test(expected = PersistenceException.class)
  public void testSetDataUsageNullUsername() throws PersistenceException {

    attributesStore.setDataUsage(null, LONG_5);
  }

  @Test(expected = PersistenceException.class)
  public void testUpdateDataUsageNullUsername() throws PersistenceException {
    attributesStore.updateUserDataUsage(null, LONG_5);
  }

  @Test(expected = PersistenceException.class)
  public void testGetDataUsageNullUsername() throws PersistenceException {
    attributesStore.getCurrentDataUsageByUser(null);
  }

  @Test
  public void testPersistenceStoreReturnsNull() throws PersistenceException {
    when(persistentStore.get(anyString(), anyString())).thenReturn(null);
    assertThat(attributesStore.getCurrentDataUsageByUser(USER), is(0L));
  }

  @Test
  public void testPersistenceStoreReturnsEmptyList() throws PersistenceException {
    when(persistentStore.get(anyString(), anyString())).thenReturn(new ArrayList<>());
    assertThat(attributesStore.getCurrentDataUsageByUser(USER), is(0L));
  }

  @Test(expected = PersistenceException.class)
  public void testPersistenceStoreThrowsExceptionOnGet() throws PersistenceException {
    when(persistentStore.get(anyString(), anyString())).thenThrow(new PersistenceException());

    attributesStore.updateUserDataUsage(USER, LONG_5);
  }

  @Test
  public void testGetAllUsers() throws PersistenceException {
    List<Map<String, Object>> mapList = attributesStore.getAllUsers();
    assertThat(mapList.isEmpty(), is(true));
  }

  @Test(expected = PersistenceException.class)
  public void testGetAllUsersThrowsException() throws PersistenceException {
    when(persistentStore.get(anyString())).thenThrow(new PersistenceException());
    attributesStore.getAllUsers();
  }

  @Test
  public void testSetDataLimit() throws PersistenceException {
    final long DATA_LIMIT = LONG_5;
    ArgumentCaptor<String> keyArg = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<PersistentItem> itemArg = ArgumentCaptor.forClass(PersistentItem.class);
    attributesStore.setDataLimit(USER, DATA_LIMIT);
    verify(persistentStore).add(keyArg.capture(), itemArg.capture());
    assertThat(keyArg.getValue(), is(PersistenceType.USER_ATTRIBUTE_TYPE.toString()));

    assertThat(
        itemArg.getValue().getLongProperty(AttributesStore.DATA_USAGE_LIMIT_KEY), is(DATA_LIMIT));
  }

  @Test
  public void testSetInvalidDataLimit() throws PersistenceException {
    long dataUsage = -2L; // -1 indicates unlimited data limit
    attributesStore.setDataLimit(USER, dataUsage);
    verify(persistentStore, never()).add(anyString(), anyMap());
  }

  @Test
  public void testSetNoDataLimit() throws PersistenceException {
    final long DATA_LIMIT = -1;
    ArgumentCaptor<String> keyArg = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<PersistentItem> itemArg = ArgumentCaptor.forClass(PersistentItem.class);
    attributesStore.setDataLimit(USER, DATA_LIMIT);
    verify(persistentStore).add(keyArg.capture(), itemArg.capture());
    assertThat(keyArg.getValue(), is(PersistenceType.USER_ATTRIBUTE_TYPE.toString()));

    assertThat(
        itemArg.getValue().getLongProperty(AttributesStore.DATA_USAGE_LIMIT_KEY), is(DATA_LIMIT));
  }

  @Test(expected = PersistenceException.class)
  public void testSetDataLimitNullUsername() throws PersistenceException {
    attributesStore.setDataLimit(null, LONG_5);
  }

  @Test(expected = PersistenceException.class)
  public void testGetDataLimitNullUsername() throws PersistenceException {
    attributesStore.getDataLimitByUser(null);
  }

  @Test
  public void testGetDataLimit() throws PersistenceException {
    attributesList = new ArrayList<>();
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(DATA_USAGE_LONG, LONG_2);
    attributes.put(DATA_LIMIT_LONG, LONG_1);
    attributesList.add(attributes);
    when(persistentStore.get(anyString(), anyString())).thenReturn(attributesList);
    long usage = attributesStore.getDataLimitByUser(USER);
    assertThat(usage, is(LONG_1));
  }

  @Test
  public void resetUserDataUsages() throws PersistenceException {
    attributesList = new ArrayList<>();
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(DATA_USAGE_LONG, LONG_2);
    attributes.put(DATA_LIMIT_LONG, LONG_1);
    attributesList.add(attributes);
    when(persistentStore.get(anyString())).thenReturn(attributesList);

    ArgumentCaptor<String> keyArg = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<PersistentItem> itemArg = ArgumentCaptor.forClass(PersistentItem.class);
    attributesStore.resetUserDataUsages();
    verify(persistentStore).add(keyArg.capture(), itemArg.capture());
    assertThat(keyArg.getValue(), is(PersistenceType.USER_ATTRIBUTE_TYPE.toString()));

    assertThat(itemArg.getValue().getLongProperty(AttributesStore.DATA_USAGE_KEY), is(0L));
    assertThat(
        itemArg.getValue().getLongProperty(AttributesStore.DATA_USAGE_LIMIT_KEY), is(LONG_1));
  }
}
