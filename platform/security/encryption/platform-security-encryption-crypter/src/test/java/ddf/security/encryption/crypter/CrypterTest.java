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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import ddf.security.SecurityConstants;
import ddf.security.encryption.crypter.Crypter.CrypterException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CrypterTest {
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
    final String unencryptedPassword = "protect";
    final Crypter crypter = new Crypter();

    final String encryptedPassword = crypter.encrypt(unencryptedPassword);

    final String decryptedPassword = crypter.decrypt(encryptedPassword);

    assertEquals(unencryptedPassword, decryptedPassword);
  }

  @Test(expected = CrypterException.class)
  public void testEncryptNull() {
    final Crypter crypter = new Crypter();

    crypter.encrypt(null);
  }

  @Test(expected = CrypterException.class)
  public void testEncryptEmpty() {
    final Crypter crypter = new Crypter();

    crypter.encrypt("");
  }

  @Test(expected = CrypterException.class)
  public void testEncryptBlank() {
    final Crypter crypter = new Crypter();

    crypter.encrypt(" ");
  }

  @Test(expected = CrypterException.class)
  public void testDecryptNull() throws Exception {
    final Crypter crypter = new Crypter();

    crypter.decrypt(null);
  }

  @Test(expected = CrypterException.class)
  public void testDecryptEmpty() throws Exception {
    final Crypter crypter = new Crypter();

    crypter.decrypt("");
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
