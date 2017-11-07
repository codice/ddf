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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import org.apache.felix.cm.PersistenceManager;
import org.codice.felix.cm.internal.ConfigurationPersistencePlugin;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.util.tracker.ServiceTracker;

@RunWith(MockitoJUnitRunner.class)
public class DelegatingPersistenceManagerTest {
  private static final String TEST_PID = "org.codice.test.ServiceFactory";

  @Mock private PersistenceManager mockManager;

  @Mock private ServiceTracker mockTracker;

  @Mock private ConfigurationContextImpl mockContext;

  @Mock private ConfigurationPersistencePlugin plugin1;

  @Mock private ConfigurationPersistencePlugin plugin2;

  private DelegatingPersistenceManager delegatingPersistenceManager;

  @Before
  public void setup() {
    Object[] services = new Object[] {plugin1, plugin2, new Object()};

    when(mockTracker.getServices()).thenReturn(services);
    when(mockContext.shouldBeVisibleForProcessing()).thenReturn(true);

    delegatingPersistenceManager =
        new DelegatingPersistenceManager(mockManager, mockTracker) {
          @Override
          ConfigurationContextImpl createContext(String pid, Dictionary props) {
            return mockContext;
          }
        };

    verify(mockTracker).open();
  }

  @Test
  public void testClose() throws Exception {
    delegatingPersistenceManager.close();
    verify(mockTracker).close();
  }

  @Test
  public void testStore() throws IOException {
    delegatingPersistenceManager.store(TEST_PID, new Hashtable());
    verify(plugin1).handleStore(mockContext);
    verify(plugin2).handleStore(mockContext);
    verifyNoMoreInteractions(plugin1, plugin2);
  }

  @Test
  public void testStoreNullServices() throws IOException {
    when(mockTracker.getServices()).thenReturn(null);
    delegatingPersistenceManager.store(TEST_PID, new Hashtable());
    verifyZeroInteractions(plugin1, plugin2);
  }

  @Test
  public void testDelete() throws IOException {
    delegatingPersistenceManager.delete(TEST_PID);
    verify(plugin1).handleDelete(TEST_PID);
    verify(plugin2).handleDelete(TEST_PID);
    verifyNoMoreInteractions(plugin1, plugin2);
  }
}
