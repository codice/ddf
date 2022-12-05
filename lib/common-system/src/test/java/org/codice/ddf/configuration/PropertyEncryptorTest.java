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
package org.codice.ddf.configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import org.junit.Test;

public class PropertyEncryptorTest {

  @Test
  public void defaultEncrypt() {
    PropertyEncryptor encryptor = new PropertyEncryptor();

    String encrypted = encryptor.encrypt("test");
    assertThat(encrypted, startsWith("ENC("));

    String decrypted = encryptor.decrypt(encrypted);
    assertThat(decrypted, is("test"));
  }

  @Test
  public void defaultDecrypt() {
    PropertyEncryptor encryptor = new PropertyEncryptor();

    String decrypted =
        encryptor.decrypt("ENC(Mladw9oC5ljakOgeimtCe1jzU8zQ2euHiYqmP4HG4NjUrAjI5VFal3AG0wf9pTym)");
    assertThat(decrypted, is("test"));
  }

  @Test
  public void differentAlgo() {
    try {
      System.setProperty("encryption.algorithm", "PBEWithMD5AndDES");
      PropertyEncryptor encryptor = new PropertyEncryptor();

      String encrypted = encryptor.encrypt("test");
      assertThat(encrypted, startsWith("ENC("));

      String decrypted = encryptor.decrypt(encrypted);
      assertThat(decrypted, is("test"));
    } finally {
      System.clearProperty("encryption.algorithm");
    }
  }

  @Test
  public void differentAlgoDecrypt() {
    try {
      System.setProperty("encryption.algorithm", "PBEWithMD5AndDES");
      PropertyEncryptor encryptor = new PropertyEncryptor();

      String decrypted = encryptor.decrypt("ENC(28R6B0Wkm/R/tOObabUhBw4KRLy78SJg)");
      assertThat(decrypted, is("test"));
    } finally {
      System.clearProperty("encryption.algorithm");
    }
  }

  @Test
  public void encryptPassthrough() {
    PropertyEncryptor encryptor = new PropertyEncryptor();

    String encrypted = encryptor.encrypt("ENC(test)");
    assertThat(encrypted, is("ENC(test)"));
  }

  @Test
  public void decryptPassthrough() {
    PropertyEncryptor encryptor = new PropertyEncryptor();

    String encrypted = encryptor.decrypt("test");
    assertThat(encrypted, is("test"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void badAlgo() {
    try {
      System.setProperty("encryption.algorithm", "PBEWithNoAndNo");
      new PropertyEncryptor();
    } finally {
      System.clearProperty("encryption.algorithm");
    }
  }
}
