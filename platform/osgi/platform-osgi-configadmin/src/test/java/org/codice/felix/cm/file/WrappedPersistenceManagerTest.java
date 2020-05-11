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
package org.codice.felix.cm.file;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.Dictionary;
import java.util.Hashtable;
import org.apache.felix.cm.PersistenceManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WrappedPersistenceManagerTest {
  private static final String PID = "myPid";

  @Mock private WrappedPersistenceManager mockPersistenceManager;

  private WrappedPersistenceManager persistenceManager;

  @Before
  public void setup() throws Exception {
    persistenceManager = new WrappedPersistenceManager(mockPersistenceManager);
  }

  @Test
  public void testClose() throws Exception {
    persistenceManager.close();
    verify(mockPersistenceManager).close();
    verifyNoMoreInteractions(mockPersistenceManager);
  }

  @Test
  public void testCloseWhenInnerPmNotWrappedPm() throws Exception {
    PersistenceManager mockSomeOtherPm = mock(PersistenceManager.class);
    persistenceManager = new WrappedPersistenceManager(mockSomeOtherPm);
    persistenceManager.close();
    verifyZeroInteractions(mockSomeOtherPm);
  }

  @Test
  public void testExists() throws Exception {
    persistenceManager.exists(PID);
    verify(mockPersistenceManager).exists(eq(PID));
    verifyNoMoreInteractions(mockPersistenceManager);
  }

  @Test
  public void testLoad() throws Exception {
    persistenceManager.load(PID);
    verify(mockPersistenceManager).load(eq(PID));
    verifyNoMoreInteractions(mockPersistenceManager);
  }

  @Test
  public void testGetDictionaries() throws Exception {
    persistenceManager.getDictionaries();
    verify(mockPersistenceManager).getDictionaries();
    verifyNoMoreInteractions(mockPersistenceManager);
  }

  @Test
  public void testStore() throws Exception {
    Dictionary props = new Hashtable();
    persistenceManager.store(PID, props);
    verify(mockPersistenceManager).store(eq(PID), eq(props));
    verifyNoMoreInteractions(mockPersistenceManager);
  }

  @Test
  public void testDelete() throws Exception {
    persistenceManager.delete(PID);
    verify(mockPersistenceManager).delete(eq(PID));
    verifyNoMoreInteractions(mockPersistenceManager);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructWithNullDelegate() throws Exception {
    persistenceManager = new WrappedPersistenceManager(null);
  }
}
