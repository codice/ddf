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

import java.io.File;
import java.nio.file.Files;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncryptionServiceImplTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionServiceImplTest.class);

  private static File ddfHome;

  @Before
  public void setUp() throws Exception {
    ddfHome = Files.createTempDirectory("encrypt").toFile();
    System.setProperty("ddf.home", ddfHome.getAbsolutePath());
    System.setProperty("ddf.etc", ddfHome.getAbsolutePath().concat("/etc"));
    String path = new File(System.getProperty("ddf.etc").concat("/certs")).getCanonicalPath();
    new File(path).mkdirs();
  }

  @After
  public void cleanUp() throws Exception {
    FileUtils.deleteDirectory(ddfHome);
  }

  @Test
  public void testBadSetup() throws Exception {
    System.setProperty("ddf.etc", ddfHome.getAbsolutePath() + System.nanoTime());
    final String unencryptedPassword = "protect";

    final EncryptionServiceImpl encryptionService = new EncryptionServiceImpl();

    final String encryptedPassword = encryptionService.encrypt(unencryptedPassword);
    assertEquals(encryptedPassword, unencryptedPassword);

    final String decryptedPassword = encryptionService.decrypt(encryptedPassword);
    assertEquals(decryptedPassword, encryptedPassword);

    final String wrappedPassword = "ENC(" + unencryptedPassword + ")";
    final String unWrappedDecryptedPassword = encryptionService.decryptValue(wrappedPassword);

    assertEquals(unWrappedDecryptedPassword, unencryptedPassword);
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
  public void testWrappingAndEncrypting() {
    final EncryptionServiceImpl encryptionService = new EncryptionServiceImpl();
    final String wrappedEncryptedValue = encryptionService.encryptValue("test");
    assertThat(wrappedEncryptedValue, startsWith("ENC"));
    assertThat(wrappedEncryptedValue, endsWith(")"));
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

  @Test
  public void testEncryptValueWithEmptyAndNull() {
    final EncryptionServiceImpl encryptionService = new EncryptionServiceImpl();
    String encryptedNull = encryptionService.encryptValue(null);
    assertThat(encryptedNull, is(nullValue()));
    String encryptedEmpty = encryptionService.encryptValue("");
    assertThat(encryptedEmpty, equalTo(""));
  }
}
