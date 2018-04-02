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

import static java.util.Collections.enumeration;
import static java.util.Collections.singletonList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.osgi.framework.Constants.SERVICE_PID;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;
import org.apache.felix.cm.PersistenceManager;
import org.codice.felix.cm.internal.ConfigurationInitializable;
import org.codice.felix.cm.internal.ConfigurationPersistencePlugin;
import org.codice.felix.cm.internal.ConfigurationStoragePlugin;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

@RunWith(MockitoJUnitRunner.class)
public class DelegatingPersistenceManagerTest {
  private static final String TEST_PID = "org.codice.test.ServiceFactory";

  private static final String SOME_PROPERTY_KEY = "somePropertyKey";

  private static final String SOME_PROPERTY_VALUE = "somePropertyValue";

  private static final Long BUNDLE_ID = 120L;

  private static final String BUNDLE_SYMBOLIC_NAME = "test-bundle";

  @Mock private PersistenceManager mockManager;

  @Mock private ServiceTracker mockTracker;

  @Mock private ConfigurationContextImpl mockContext;

  // Used as part of the services array
  @Mock private ConfigurationPersistencePlugin plugin1;

  // Used as part of the services array
  @Mock private ConfigurationPersistencePlugin plugin2;

  // Used for verifying passed config set on service initialization
  @Mock private ConfigurationPersistencePlugin mockPersistencePlugin;

  @Mock private ConfigurationStoragePlugin mockStoragePlugin;

  @Mock private ServiceReference<ConfigurationInitializable> mockReference;

  private final Dictionary<String, Object> testProps = new Hashtable<>();

  private DelegatingPersistenceManager delegatingPersistenceManager;

  private DelegatingPersistenceManager.PluginTrackerCustomizer pluginTrackerCustomizer;

  @Before
  public void setup() {
    testProps.put(SERVICE_PID, TEST_PID);
    testProps.put(SOME_PROPERTY_KEY, SOME_PROPERTY_VALUE);

    Bundle mockBundle = mock(Bundle.class);
    when(mockReference.getBundle()).thenReturn(mockBundle);
    when(mockBundle.getSymbolicName()).thenReturn(BUNDLE_SYMBOLIC_NAME);
    when(mockBundle.getBundleId()).thenReturn(BUNDLE_ID);

    Object[] services = new Object[] {plugin1, plugin2, new Object()};

    when(mockTracker.getServices()).thenReturn(services);
    when(mockContext.shouldBeVisibleToPlugins()).thenReturn(true);

    delegatingPersistenceManager =
        new DelegatingPersistenceManager(mockManager, mockTracker) {
          @Override
          ConfigurationContextImpl createContext(String pid, Dictionary props) {
            return mockContext;
          }

          @Override
          ConfigurationInitializable retrieveServiceObject(
              ServiceReference<ConfigurationInitializable> serviceReference) {
            return mockPersistencePlugin;
          }
        };
    pluginTrackerCustomizer = delegatingPersistenceManager.new PluginTrackerCustomizer();

    verify(mockTracker).open();
  }

  @Test
  public void testStoragePluginLoads() throws IOException {
    Enumeration mockEnum = mock(Enumeration.class);
    when(mockManager.getDictionaries()).thenReturn(mockEnum);
    when(mockEnum.hasMoreElements()).thenReturn(false);
    delegatingPersistenceManager =
        new DelegatingPersistenceManager(mockManager, mockTracker) {
          @Override
          ConfigurationContextImpl createContext(String pid, Dictionary props) {
            return mockContext;
          }

          @Override
          ConfigurationInitializable retrieveServiceObject(
              ServiceReference<ConfigurationInitializable> serviceReference) {
            return mockStoragePlugin;
          }
        };

    pluginTrackerCustomizer = delegatingPersistenceManager.new PluginTrackerCustomizer();
    pluginTrackerCustomizer.addingService(mockReference);

    delegatingPersistenceManager.delete(TEST_PID);
    verify(mockStoragePlugin).delete(eq(TEST_PID));
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

  @Test
  public void testAddingService() throws IOException {
    when(mockManager.getDictionaries()).thenReturn(enumeration(singletonList(testProps)));
    pluginTrackerCustomizer.addingService(mockReference);
    ArgumentCaptor<Set> captor = ArgumentCaptor.forClass(Set.class);
    verify(mockPersistencePlugin).initialize(captor.capture());
    assertThat(captor.getValue().size(), is(1));
  }

  @Test
  public void testAddingServiceInvalidDictionary() throws IOException {
    when(mockManager.getDictionaries()).thenReturn(enumeration(singletonList(new Hashtable<>())));
    pluginTrackerCustomizer.addingService(mockReference);
    ArgumentCaptor<Set> captor = ArgumentCaptor.forClass(Set.class);
    verify(mockPersistencePlugin).initialize(captor.capture());
    assertThat(captor.getValue().size(), is(0));
  }

  @Test
  public void testAddingServiceThrowsException() throws IOException {
    when(mockManager.getDictionaries()).thenThrow(IOException.class);
    pluginTrackerCustomizer.addingService(mockReference);
    ArgumentCaptor<Set> captor = ArgumentCaptor.forClass(Set.class);
    verify(mockPersistencePlugin).initialize(captor.capture());
    assertThat(captor.getValue().size(), is(0));
  }

  @Test
  public void testModifiedServiceNoOp() {
    pluginTrackerCustomizer.modifiedService(null, null);
    verifyZeroInteractions(mockManager);
  }

  @Test
  public void testRemovedServiceNoOp() {
    pluginTrackerCustomizer.removedService(null, null);
    verifyZeroInteractions(mockManager);
  }
}
