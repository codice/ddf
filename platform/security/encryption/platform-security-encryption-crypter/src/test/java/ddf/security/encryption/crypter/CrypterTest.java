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
import java.io.File;
import java.nio.file.InvalidPathException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CrypterTest {
  private static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();
  private static File ddfHome;

  @Before
  public void setUp() throws Exception {
    TEMPORARY_FOLDER.create();
    ddfHome = TEMPORARY_FOLDER.newFolder("encrypt");
    System.setProperty("ddf.home", ddfHome.getAbsolutePath());
    System.setProperty("ddf.etc", ddfHome.getAbsolutePath().concat("/etc"));
    System.setProperty(
        SecurityConstants.KEYSET_DIR, ddfHome.getAbsolutePath().concat("/etc").concat("/keysets"));
    System.setProperty(
        SecurityConstants.ASSOCIATED_DATA_PATH,
        ddfHome.getAbsolutePath().concat("/etc").concat("/associatedData.properties"));

    new File(System.getProperty(SecurityConstants.KEYSET_DIR)).mkdirs();
    new File(System.getProperty("ddf.etc").concat("/certs")).mkdirs();
  }

  @After
  public void cleanUp() throws Exception {
    System.clearProperty("ddf.home");
    System.clearProperty("ddf.etc");
    System.clearProperty(SecurityConstants.ASSOCIATED_DATA_PATH);
  }

  @Test(expected = InvalidPathException.class)
  public void testBadSetup() throws Exception {
    System.setProperty(SecurityConstants.KEYSET_DIR, "!@#$%^&*()");
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
