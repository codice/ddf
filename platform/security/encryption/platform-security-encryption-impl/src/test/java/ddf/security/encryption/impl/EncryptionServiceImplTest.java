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
package ddf.security.encryption.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import ddf.security.SecurityConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncryptionServiceImplTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionServiceImplTest.class);

  private static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    TEMPORARY_FOLDER.create();
    String keysetHome = TEMPORARY_FOLDER.newFolder("keysets").getAbsolutePath();
    String associatedDataHome = TEMPORARY_FOLDER.newFolder("etc").getAbsolutePath();
    System.setProperty(SecurityConstants.KEYSET_DIR, keysetHome);
    System.setProperty(
        SecurityConstants.ASSOCIATED_DATA_PATH,
        associatedDataHome.concat("/associatedData.properties"));
  }

  @After
  public void cleanUp() throws Exception {
    TEMPORARY_FOLDER.delete();
    System.clearProperty(SecurityConstants.KEYSET_DIR);
    System.clearProperty(SecurityConstants.ASSOCIATED_DATA_PATH);
  }

  @Test(expected = RuntimeException.class)
  public void testBadSetup() throws Exception {
    System.setProperty(SecurityConstants.KEYSET_DIR, "!@#$%^&*()");
    new EncryptionServiceImpl();
  }

  @Test
  public void testEncryptDecrypt() throws Exception {
    final String unencryptedPassword = "protect";

    LOGGER.debug("Unencrypted Password: {}", unencryptedPassword);

    final EncryptionServiceImpl encryptionService = new EncryptionServiceImpl();

    final String encryptedPassword = encryptionService.encrypt(unencryptedPassword);
    LOGGER.debug("Encrypted Password: {}", encryptedPassword);

    final String decryptedPassword = encryptionService.decrypt(encryptedPassword);
    LOGGER.debug("Decrypted Password: {}", decryptedPassword);

    assertEquals(unencryptedPassword, decryptedPassword);
  }

  @Test
  public void testEncryptionServiceInteroperability() throws Exception {
    final EncryptionServiceImpl encryptionService1 = new EncryptionServiceImpl();
    final EncryptionServiceImpl encryptionService2 = new EncryptionServiceImpl();

    final String unencryptedPassword = "protect";

    String encryptedPassword1 = encryptionService1.encrypt(unencryptedPassword);
    String encryptedPassword2 = encryptionService2.encrypt(unencryptedPassword);

    String plainPassword1 = encryptionService1.decrypt(encryptedPassword2);
    String plainPassword2 = encryptionService2.decrypt(encryptedPassword1);

    assertEquals(unencryptedPassword, plainPassword1);
    assertEquals(unencryptedPassword, plainPassword2);
  }

  @Test
  public void testWrappingAndEncrypting() {
    final EncryptionServiceImpl encryptionService = new EncryptionServiceImpl();
    final String wrappedEncryptedValue = encryptionService.encryptValue("test");
    assertThat(wrappedEncryptedValue, startsWith("ENC"));
    assertThat(wrappedEncryptedValue, endsWith(")"));
  }

  @Test
  public void testEncryptValueWithEmptyAndNull() {
    final EncryptionServiceImpl encryptionService = new EncryptionServiceImpl();
    String encryptedNull = encryptionService.encryptValue(null);
    assertThat(encryptedNull, is(nullValue()));
    String encryptedEmpty = encryptionService.encryptValue("");
    assertThat(encryptedEmpty, equalTo(""));
  }

  @Test
  public void testUnwrapDecrypt() throws Exception {
    final String expectedDecryptedValue = "test";
    final EncryptionServiceImpl encryptionService = new EncryptionServiceImpl();

    final String wrappedEncryptedValue =
        "ENC(".concat(encryptionService.encrypt(expectedDecryptedValue)).concat(")");

    LOGGER.debug("Original wrapped encrypted value is: {}", wrappedEncryptedValue);

    final String decryptedValue = encryptionService.decryptValue(wrappedEncryptedValue);
    LOGGER.debug("Unwrapped decrypted value is: {}", decryptedValue);

    assertEquals(expectedDecryptedValue, decryptedValue);
  }

  @Test
  public void testUnwrapDecryptNull() throws Exception {
    final String wrappedEncryptedValue = null;

    LOGGER.debug("Original wrapped encrypted value is: null");

    final EncryptionServiceImpl encryptionService = new EncryptionServiceImpl();

    final String decryptedValue = encryptionService.decryptValue(wrappedEncryptedValue);

    assertNull(decryptedValue);
  }

  @Test
  public void testUnwrapDecryptEmpty() throws Exception {
    final String wrappedEncryptedValue = "";

    LOGGER.debug("Original wrapped encrypted value is: <blank>");

    final EncryptionServiceImpl encryptionService = new EncryptionServiceImpl();

    final String decryptedValue = encryptionService.decryptValue(wrappedEncryptedValue);

    assertEquals(decryptedValue, "");
  }

  @Test
  public void testUnwrapDecryptPlainText() throws Exception {
    final String wrappedEncryptedValue = "plaintext";

    LOGGER.debug("Original value is: {}", wrappedEncryptedValue);

    final EncryptionServiceImpl encryptionService = new EncryptionServiceImpl();

    final String decryptedValue = encryptionService.decryptValue(wrappedEncryptedValue);
    LOGGER.debug("Unwrapped decrypted value is: {}", decryptedValue);

    assertEquals(wrappedEncryptedValue, decryptedValue);
  }
}
