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

import static java.lang.String.format;
import static org.codice.felix.cm.file.ConfigurationContextImpl.FELIX_FILENAME;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.osgi.framework.Constants.SERVICE_PID;
import static org.osgi.service.cm.ConfigurationAdmin.SERVICE_FACTORYPID;

import java.util.Dictionary;
import java.util.Hashtable;
import org.apache.felix.cm.PersistenceManager;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EncryptingPersistenceManagerTest {
  private static final String PID = "my_pid";

  private static final String PLAINTEXT_TO_ENCRYPT = "some_string";

  private static final String ENC_VAL_TO_ENCRYPT = "ENC(HsOcGt8seSKc34sRUYpakQ==)";

  private static final Dictionary<String, Object> TEST_PROPS = new Hashtable<>();

  static {
    // We don't encrypt integers
    TEST_PROPS.put("1", 3);
    // We SHOULD encrypt strings, even ones already encrypted
    TEST_PROPS.put("2", PLAINTEXT_TO_ENCRYPT);
    TEST_PROPS.put("3", ENC_VAL_TO_ENCRYPT);
    // Except the following three strings which are special for OSGi / Felix
    TEST_PROPS.put(SERVICE_PID, PID);
    TEST_PROPS.put(SERVICE_FACTORYPID, PID);
    TEST_PROPS.put(FELIX_FILENAME, PID);
  }

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock private PersistenceManager mockManager;

  private String pwDirectory;

  @Before
  public void setup() throws Exception {
    pwDirectory = temporaryFolder.newFolder().getAbsolutePath();
  }

  @Test
  public void testKeysetReusability() throws Exception {
    final EncryptingPersistenceManager persistenceManager1 =
        new EncryptingPersistenceManager(mockManager, pwDirectory);
    final EncryptingPersistenceManager persistenceManager2 =
        new EncryptingPersistenceManager(mockManager, pwDirectory);

    assertEquals(
        persistenceManager1.agent.keysetHandle.getKeysetInfo(),
        persistenceManager2.agent.keysetHandle.getKeysetInfo());
  }

  @Test
  public void testPropertyEncryptionRoundTrip() throws Exception {
    EncryptingPersistenceManager persistenceManager =
        new EncryptingPersistenceManager(mockManager, pwDirectory);

    ArgumentCaptor<Dictionary> captor = ArgumentCaptor.forClass(Dictionary.class);
    persistenceManager.store(PID, TEST_PROPS);
    verify(mockManager).store(eq(PID), captor.capture());

    Dictionary<String, Object> capturedProps = captor.getValue();

    String encryptedPlaintext = (String) capturedProps.get("2");
    String encryptedEncWrappedValue = (String) capturedProps.get("3");

    assertThat(capturedProps.size(), is(6));
    assertSpecialStringsDidNotChange(capturedProps);

    assertSuccessfulEncryption("PLAINTEXT_TO_ENCRYPT", PLAINTEXT_TO_ENCRYPT, encryptedPlaintext);
    assertSuccessfulEncryption(
        "ENC_VALUE_TO_ENCRYPT", ENC_VAL_TO_ENCRYPT, encryptedEncWrappedValue);

    when(mockManager.load(PID)).thenReturn(capturedProps);

    Dictionary<String, Object> loadedProps = persistenceManager.load(PID);

    assertThat(loadedProps.size(), is(6));
    assertSpecialStringsDidNotChange(loadedProps);

    assertThat(
        "PLAINTEXT_TO_ENCRYPT should be its original value prior to encryption",
        loadedProps.get("2"),
        is(PLAINTEXT_TO_ENCRYPT));
  }

  private static void assertSuccessfulEncryption(String name, String original, String encrypted) {
    assertThat(name + " was not actually encrypted", encrypted, is(not(original)));
    assertThat(
        "Encrypting " + name + " resulted in a super-sequence of the original",
        encrypted,
        not(containsString(original)));
    assertThat(
        name + " had an encrypted value that is too short",
        encrypted.length(),
        is(greaterThan(original.length() * 2)));
  }

  private static void assertSpecialStringsDidNotChange(Dictionary<String, Object> props) {
    String servicePid = (String) props.get(SERVICE_PID);
    String serviceFactoryPid = (String) props.get(SERVICE_FACTORYPID);
    String felixFilename = (String) props.get(FELIX_FILENAME);

    assertThat(reason(SERVICE_PID, servicePid), servicePid, is(PID));
    assertThat(reason(SERVICE_FACTORYPID, serviceFactoryPid), serviceFactoryPid, is(PID));
    assertThat(reason(FELIX_FILENAME, felixFilename), felixFilename, is(PID));
  }

  private static String reason(String name, String value) {
    return format(
        "%s is a special property and its value [%s] should not have been tampered with",
        name, value);
  }
}
