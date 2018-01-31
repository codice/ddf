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
package org.codice.ddf.resourcemanagement.usage;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentStore;
import org.codice.ddf.persistence.attributes.AttributesStore;
import org.codice.ddf.resourcemanagement.usage.service.DataUsage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

public class DataUsageTest {

  private static final String USER_BOB = "Bob";

  private static final String USER_BEN = "Ben";

  private static final String USER_TXT = "user_txt";

  private static final String DATA_LIMIT = "data_limit_lng";

  private static final String DATA_USAGE = "data_usage_lng";

  private static final String TIME = "12:30";

  private static final String DEFAULT_TIME = "23:30";

  private static final String CRON_TIME = "0+30+12+*+*+?";

  private DataUsage dataUsage;

  private AttributesStore attributesStore;

  private PersistentStore persistentStore;

  private List<Map<String, Object>> userList;

  @Before
  public void setUp() throws PersistenceException {
    attributesStore = mock(AttributesStore.class);
    persistentStore = mock(PersistentStore.class);

    userList = generateUserList();
    when(attributesStore.getAllUsers()).thenReturn(userList);

    doAnswer(
            (InvocationOnMock invocationOnMock) -> {
              Object[] args = invocationOnMock.getArguments();
              String user = (String) args[0];
              long dataUsage = (long) args[1];
              for (Map<String, Object> map : userList) {
                if (map.get(USER_TXT).equals(user)) {
                  map.put(DATA_LIMIT, dataUsage);
                }
              }
              return null;
            })
        .when(attributesStore)
        .setDataLimit(anyString(), anyLong());
    dataUsage = new DataUsage(attributesStore, persistentStore);
  }

  @Test
  public void testUserMap() {
    Map<String, List<Long>> map = dataUsage.userMap();
    assertThat(map.get(USER_BOB), notNullValue());
    assertThat(map.get(USER_BEN), notNullValue());

    List<Long> userLongList = map.get(USER_BOB);
    assertThat(userLongList.size(), is(2));
    assertThat(userLongList.get(0), is(2L));
    assertThat(userLongList.get(1), is(1L));

    userLongList = map.get(USER_BEN);
    assertThat(userLongList.size(), is(2));
    assertThat(userLongList.get(0), is(4L));
    assertThat(userLongList.get(1), is(3L));
  }

  @Test
  public void testUpdateUserDataLimit() {
    Map<String, Long> map = new HashMap<>();
    map.put(USER_BOB, 12L);
    map.put(USER_BEN, 12L);
    dataUsage.updateUserDataLimit(map);

    Map<String, List<Long>> resultMap = dataUsage.userMap();
    assertThat(resultMap.get(USER_BOB), notNullValue());
    assertThat(resultMap.get(USER_BEN), notNullValue());

    List<Long> userLongList = resultMap.get(USER_BOB);
    assertThat(userLongList.size(), is(2));
    assertThat(userLongList.get(0), is(2L));
    assertThat(userLongList.get(1), is(12L));

    userLongList = resultMap.get(USER_BEN);
    assertThat(userLongList.size(), is(2));
    assertThat(userLongList.get(0), is(4L));
    assertThat(userLongList.get(1), is(12L));
  }

  @Test
  public void testUpdateCronTime() {
    dataUsage.updateCronTime(TIME);
    assertThat(TIME, is(dataUsage.cronTime()));
  }

  @Test
  public void testGetPersistentCronTime() throws PersistenceException {
    List<Map<String, Object>> mapList = new ArrayList<>();
    Map<String, Object> stringObjectMap = new HashMap<>();
    stringObjectMap.put(DataUsage.ID + DataUsage.TXT_PREFIX, DataUsage.CRON_TIME_ID_KEY);
    stringObjectMap.put(DataUsage.CRON_TIME_KEY + DataUsage.TXT_PREFIX, CRON_TIME);
    mapList.add(stringObjectMap);
    when(persistentStore.get(anyString())).thenReturn(mapList);
    dataUsage.init();
    assertThat(dataUsage.cronTime(), is(TIME));
  }

  @Test
  public void testGetPersistentCronTimeDefault() throws PersistenceException {
    List<Map<String, Object>> mapList = new ArrayList<>();
    Map<String, Object> stringObjectMap = new HashMap<>();
    mapList.add(stringObjectMap);
    when(persistentStore.get(anyString())).thenReturn(mapList);
    dataUsage.init();
    assertThat(dataUsage.cronTime(), is(DEFAULT_TIME));
  }

  @Test
  public void testGetPersistentCronTimeDefaultWithException() throws PersistenceException {
    List<Map<String, Object>> mapList = new ArrayList<>();
    Map<String, Object> stringObjectMap = new HashMap<>();
    mapList.add(stringObjectMap);
    when(persistentStore.get(anyString())).thenThrow(new PersistenceException());
    dataUsage.init();
    assertThat(dataUsage.cronTime(), is(DEFAULT_TIME));
  }

  private List<Map<String, Object>> generateUserList() {
    List<Map<String, Object>> userList = new ArrayList<>();
    userList.add(generateUserMap(USER_BOB, 1, 2));
    userList.add(generateUserMap(USER_BEN, 3, 4));
    return userList;
  }

  private Map<String, Object> generateUserMap(String user, long dataLimit, long dataUsage) {
    Map<String, Object> stringObjectMap = new HashMap<>();
    stringObjectMap.put(USER_TXT, user);
    stringObjectMap.put(DATA_LIMIT, dataLimit);
    stringObjectMap.put(DATA_USAGE, dataUsage);
    return stringObjectMap;
  }
}
