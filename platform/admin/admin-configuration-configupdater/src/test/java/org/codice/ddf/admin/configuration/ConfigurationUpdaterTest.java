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
package org.codice.ddf.admin.configuration;

import static org.codice.ddf.admin.configuration.ConfigurationUpdater.FELIX_FILENAME;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import ddf.security.audit.SecurityLogger;
import ddf.security.encryption.EncryptionService;
import java.io.File;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import org.codice.ddf.admin.core.api.ConfigurationAdmin;
import org.codice.ddf.platform.io.internal.PersistenceStrategy;
import org.codice.felix.cm.internal.ConfigurationContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationUpdaterTest {
  private static final String TEMP_FILE_EXT = "tmp";

  // Will be tracked in the installer's map after init()
  private static final String PID_001 = "001";

  // Will NOT be tracked in the installer's map after init()
  private static final String PID_002 = "002";

  // Metatype ID used for our fake password field
  private static final String PASSWORD_ID = "myPassword";

  private static final String PASSWORD_PLAIN_TEXT = "plaintext";

  private static final String PASSWORD_ENCRYPTED = "ENC(abcdefg)";

  @Mock private ConfigurationAdmin configurationAdmin;

  @Mock private PersistenceStrategy mockStrategy;

  @Mock private EncryptionService mockEncryptionService;

  @Mock private ConfigurationContext mockContext;

  @Mock private ConfigurationContext mockContextForInit;

  private ConfigurationUpdater installer;

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private Set<ConfigurationContext> configs;

  private File fileA;

  private File fileB;

  @Before
  public void before() throws Exception {
    fileA = temporaryFolder.newFile();
    fileB = temporaryFolder.newFile();

    when(mockStrategy.getExtension()).thenReturn(TEMP_FILE_EXT);

    when(mockContextForInit.getServicePid()).thenReturn(PID_001);
    when(mockContextForInit.getConfigFile()).thenReturn(fileB);
    when(mockContextForInit.getSanitizedProperties()).thenReturn(new Hashtable<>());

    configs = Collections.singleton(mockContextForInit);

    when(mockContext.getSanitizedProperties()).thenReturn(new Hashtable<>());

    installer =
        new ConfigurationUpdater(
            configurationAdmin, Collections.singletonList(mockStrategy), mockEncryptionService);
    installer.setSecurityLogger(mock(SecurityLogger.class));
  }

  @Test
  public void testInit() throws Exception {
    installer.initialize(configs);
    Map<String, CachedConfigData> pidFileMap = installer.getPidDataMap();
    assertThat(pidFileMap.entrySet(), hasSize(1));
    assertThat(pidFileMap.get(PID_001).getFelixFile(), is(fileB));
  }

  @Test
  public void testInitNoFelixFile() throws Exception {
    when(mockContextForInit.getConfigFile()).thenReturn(null);
    installer.initialize(configs);
    Map<String, CachedConfigData> pidFileMap = installer.getPidDataMap();
    assertThat(pidFileMap.entrySet(), is(empty()));
  }

  @Test
  public void testHandleStoreWontTrackConfig() throws Exception {
    when(mockContext.getServicePid()).thenReturn(PID_002);
    installer.initialize(configs);
    installer.handleStore(mockContext);

    Map<String, CachedConfigData> pidFileMap = installer.getPidDataMap();
    // Map state from init should not have changed
    assertThat(pidFileMap.entrySet(), hasSize(1));
    assertThat(pidFileMap.get(PID_001).getFelixFile(), is(fileB));
    // Not tracking and won't track, so filesystem should not have changed
    assertThat(fileB.exists(), is(true));
    // No updating files on disk, so we shouldn't have called methods on the strategy
    verifyZeroInteractions(mockStrategy);
  }

  @Test
  public void testHandleStoreConfigAdded() throws Exception {
    when(mockContext.getServicePid()).thenReturn(PID_002);
    when(mockContext.getConfigFile()).thenReturn(fileA);
    installer.initialize(configs);
    installer.handleStore(mockContext);

    Map<String, CachedConfigData> pidFileMap = installer.getPidDataMap();
    assertThat(pidFileMap.entrySet(), hasSize(2));
    assertThat(pidFileMap.get(PID_001).getFelixFile(), is(fileB));
    assertThat(pidFileMap.get(PID_002).getFelixFile(), is(fileA));

    verifyZeroInteractions(mockStrategy);
  }

  @Test
  public void testHandleStoreConfigPropValuesUpdated() throws Exception {
    Dictionary<String, Object> propsOld = new Hashtable<>();
    propsOld.put(PASSWORD_ID, PASSWORD_PLAIN_TEXT);
    when(mockContextForInit.getSanitizedProperties()).thenReturn(propsOld);

    Dictionary<String, Object> propsNew = new Hashtable<>();
    propsNew.put(PASSWORD_ID, PASSWORD_ENCRYPTED);

    when(mockContext.getServicePid()).thenReturn(PID_001);
    when(mockContext.getConfigFile()).thenReturn(fileB);
    when(mockContext.getSanitizedProperties()).thenReturn(propsNew);

    Map<String, CachedConfigData> pidFileMap;
    Dictionary<String, Object> props;

    installer.initialize(configs);

    pidFileMap = installer.getPidDataMap();
    props = pidFileMap.get(PID_001).getProps();
    assertThat(props.get(PASSWORD_ID), is(PASSWORD_PLAIN_TEXT));

    installer.handleStore(mockContext);

    pidFileMap = installer.getPidDataMap();
    props = pidFileMap.get(PID_001).getProps();
    assertThat(props.get(PASSWORD_ID), is(PASSWORD_ENCRYPTED));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHandleStoreNoAppropriateStrategy() throws Exception {
    when(mockStrategy.getExtension()).thenReturn("not_tmp");

    Dictionary<String, Object> propsOld = new Hashtable<>();
    propsOld.put(PASSWORD_ID, PASSWORD_PLAIN_TEXT);
    when(mockContextForInit.getSanitizedProperties()).thenReturn(propsOld);

    Dictionary<String, Object> propsNew = new Hashtable<>();
    propsNew.put(PASSWORD_ID, PASSWORD_ENCRYPTED);

    when(mockContext.getServicePid()).thenReturn(PID_001);
    when(mockContext.getConfigFile()).thenReturn(fileB);
    when(mockContext.getSanitizedProperties()).thenReturn(propsNew);

    installer.initialize(configs);
    installer.handleStore(mockContext);
  }

  /**
   * It's important to note that this tests MUST be run against {@link #PID_001} because our init
   * method pre-populates that pid with an empty prop dictionary. This test implies an updated
   * dictionary with field {@link #PASSWORD_ID} and will trigger a write to disk because the cached
   * props dictionary does not match.
   */
  @Test
  public void testHandleStoreConfigPropsEncrypted() throws Exception {
    when(mockEncryptionService.encryptValue(PASSWORD_PLAIN_TEXT)).thenReturn("ENC(abcdefg)");

    ObjectClassDefinition mockClassDef = mock(ObjectClassDefinition.class);
    AttributeDefinition mockAttributeDef = mock(AttributeDefinition.class);

    when(configurationAdmin.getObjectClassDefinition(PID_001)).thenReturn(mockClassDef);
    when(mockClassDef.getAttributeDefinitions(ObjectClassDefinition.ALL))
        .thenReturn(new AttributeDefinition[] {mockAttributeDef});
    when(mockAttributeDef.getType()).thenReturn(AttributeDefinition.PASSWORD);
    when(mockAttributeDef.getID()).thenReturn(PASSWORD_ID);

    Dictionary<String, Object> propsBefore = new Hashtable<>();
    propsBefore.put(PASSWORD_ID, PASSWORD_PLAIN_TEXT);

    when(mockContext.getServicePid()).thenReturn(PID_001);
    when(mockContext.getConfigFile()).thenReturn(fileB);
    when(mockContext.getSanitizedProperties()).thenReturn(propsBefore);

    installer.initialize(configs);
    installer.handleStore(mockContext);

    Map<String, CachedConfigData> pidFileMap = installer.getPidDataMap();
    Dictionary<String, Object> propsAfter = pidFileMap.get(PID_001).getProps();
    assertThat(propsAfter.get(PASSWORD_ID), is(PASSWORD_ENCRYPTED));

    // Confirm we wrote to disk using the strategy
    verify(mockStrategy).write(anyObject(), eq(propsAfter));
  }

  @Test
  public void testHandleStoreConfigFileRemoved() throws Exception {
    when(mockContext.getServicePid()).thenReturn(PID_001);
    when(mockContext.getConfigFile()).thenReturn(null);
    installer.initialize(configs);
    installer.handleStore(mockContext);

    verify(mockContext)
        .setProperty(eq(FELIX_FILENAME), eq(fileB.getAbsoluteFile().toURI().toString()));
  }

  @Test(expected = IllegalStateException.class)
  public void testHandleStoreConfigFileIllegallyChanged() throws Exception {
    when(mockContext.getServicePid()).thenReturn(PID_001);
    when(mockContext.getConfigFile()).thenReturn(fileA);
    installer.initialize(configs);
    installer.handleStore(mockContext);

    verifyZeroInteractions(mockStrategy);
  }

  @Test
  public void testDeletedEvent() throws Exception {
    installer.initialize(configs);
    installer.handleDelete(PID_001);
    assertThat(installer.getPidDataMap().entrySet(), is(empty()));
    assertThat(fileB.exists(), is(false));
  }
}
