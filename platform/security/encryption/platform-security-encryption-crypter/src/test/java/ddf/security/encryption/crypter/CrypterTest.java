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
package ddf.security.encryption.crypter;

import static ddf.security.encryption.crypter.Crypter.CHUNK_SIZE;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.common.io.ByteStreams;
import ddf.security.SecurityConstants;
import ddf.security.encryption.crypter.Crypter.CrypterException;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CrypterTest {
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    String keysetHome = temporaryFolder.newFolder("keysets").getAbsolutePath();
    String associatedDataHome = temporaryFolder.newFolder("etc").getAbsolutePath();
    System.setProperty(SecurityConstants.KEYSET_DIR, keysetHome);
    System.setProperty(
        SecurityConstants.ASSOCIATED_DATA_PATH,
        associatedDataHome.concat("/associatedData.properties"));
  }

  @After
  public void cleanUp() throws Exception {
    System.clearProperty(SecurityConstants.KEYSET_DIR);
    System.clearProperty(SecurityConstants.ASSOCIATED_DATA_PATH);
  }

  @Test(expected = CrypterException.class)
  public void testBadSetup() throws Exception {
    try (OutputStream badKeysetOutputStream =
        new FileOutputStream(System.getProperty(SecurityConstants.KEYSET_DIR) + "/default.json")) {
      badKeysetOutputStream.write("BadKeyset".getBytes());
    }
    new Crypter();
  }

  @Test
  public void testEncryptDecrypt() throws Exception {
    final byte[] plainBytes = new byte[16];
    new SecureRandom().nextBytes(plainBytes);
    final Crypter crypter = new Crypter();

    final byte[] encryptedBytes = crypter.encrypt(plainBytes);

    final byte[] decryptedBytes = crypter.decrypt(encryptedBytes);

    assertArrayEquals(plainBytes, decryptedBytes);
  }

  @Test
  public void testEncryptDecryptString() throws Exception {
    final String plainPassword = "protect";
    final Crypter crypter = new Crypter();

    final String encryptedPassword = crypter.encrypt(plainPassword);

    final String decryptedPassword = crypter.decrypt(encryptedPassword);

    assertEquals(plainPassword, decryptedPassword);
  }

  @Test
  public void testEncryptDecryptStream() throws Exception {
    // make test data larger than chunk size
    final byte[] plainBytes = new byte[CHUNK_SIZE * 3];
    new SecureRandom().nextBytes(plainBytes);
    final InputStream plainInputStream = new ByteArrayInputStream(plainBytes);
    final Crypter crypter = new Crypter();

    final InputStream encryptedInputStream = crypter.encrypt(plainInputStream);

    final InputStream decryptedInputStream = crypter.decrypt(encryptedInputStream);

    final byte[] decryptedBytes = ByteStreams.toByteArray(decryptedInputStream);

    assertArrayEquals(plainBytes, decryptedBytes);
  }

  @Test(expected = CrypterException.class)
  public void testEncryptNull() {
    final Crypter crypter = new Crypter();
    final String nullString = null;

    crypter.encrypt(nullString);
  }

  @Test(expected = CrypterException.class)
  public void testEncryptNullStream() {
    final Crypter crypter = new Crypter();
    final InputStream nullInputStream = null;

    crypter.encrypt(nullInputStream);
  }

  @Test(expected = CrypterException.class)
  public void testEncryptEmpty() {
    final Crypter crypter = new Crypter();
    final String emptyString = "";

    crypter.encrypt(emptyString);
  }

  @Test(expected = CrypterException.class)
  public void testEncryptEmptyStream() {
    final Crypter crypter = new Crypter();
    final InputStream emptyInputStream = new ByteArrayInputStream("".getBytes());

    crypter.encrypt(emptyInputStream);
  }

  @Test(expected = CrypterException.class)
  public void testEncryptBlank() {
    final Crypter crypter = new Crypter();

    crypter.encrypt(" ");
  }

  @Test(expected = CrypterException.class)
  public void testDecryptNull() throws Exception {
    final Crypter crypter = new Crypter();
    final String nullString = null;

    crypter.decrypt(nullString);
  }

  @Test(expected = CrypterException.class)
  public void testDecryptNullStream() throws Exception {
    final Crypter crypter = new Crypter();
    final InputStream nullInputStream = null;

    crypter.decrypt(nullInputStream);
  }

  @Test(expected = CrypterException.class)
  public void testDecryptEmpty() throws Exception {
    final Crypter crypter = new Crypter();

    crypter.decrypt("");
  }

  @Test(expected = CrypterException.class)
  public void testDecryptEmptyStream() {
    final Crypter crypter = new Crypter();
    final InputStream emptyInputStream = new ByteArrayInputStream("".getBytes());

    crypter.decrypt(emptyInputStream);
  }

  @Test
  public void testReusabilityAndInteroperabilitySameKeyset() throws Exception {
    final Crypter crypter1 = new Crypter();
    final Crypter crypter2 = new Crypter();

    assertEquals(crypter1.keysetHandle.getKeysetInfo(), crypter2.keysetHandle.getKeysetInfo());

    final String unencryptedPassword = "protect";

    String encryptedPassword1 = crypter1.encrypt(unencryptedPassword);
    String encryptedPassword2 = crypter2.encrypt(unencryptedPassword);

    String plainPassword1 = crypter1.decrypt(encryptedPassword2);
    String plainPassword2 = crypter2.decrypt(encryptedPassword1);

    assertEquals(unencryptedPassword, plainPassword1);
    assertEquals(unencryptedPassword, plainPassword2);
  }

  @Test
  public void testReusabilityAndInteroperabilityDifferentKeysets() throws Exception {
    final Crypter crypter1 = new Crypter("crypter1");
    final Crypter crypter2 = new Crypter("crypter2");
    final String plaintext = "protect";

    final String encrypted1 = crypter1.encrypt(plaintext);
    final String encrypted2 = crypter2.encrypt(plaintext);

    try {
      crypter1.decrypt(encrypted2);
      fail("Keyset1 should not be able to decrypt a value encrypted by keyset2.");
    } catch (CrypterException expected) {
    }

    try {
      crypter2.decrypt(encrypted1);
      fail("Keyset2 should not be able to decrypt a value encrypted by keyset1.");
    } catch (CrypterException expected) {
    }
  }
}
