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
package org.codice.ddf.commands.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DigitalSignatureTest {

  private static final String KEYSTORE_TYPE = "JKS";

  private static final String ALIAS = "foobar";

  private static final String KEYSTORE_PASS = "changeit";

  private static final String EXAMPLE_EXPORT = "/example.zip";

  private static final String ARCHIVE_EXPORT = "/archive.zip";

  private DigitalSignature digitalSignature;

  @Before
  public void setup() throws Exception {
    KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
    InputStream inputStream = DigitalSignatureTest.class.getResourceAsStream("/keystore.jks");
    keyStore.load(inputStream, KEYSTORE_PASS.toCharArray());

    digitalSignature = new DigitalSignature(keyStore);
  }

  @Test
  public void testCreateDigitalSignature() throws Exception {
    InputStream inputStream = DigitalSignatureTest.class.getResourceAsStream(EXAMPLE_EXPORT);
    byte[] signature = digitalSignature.createDigitalSignature(inputStream, ALIAS, KEYSTORE_PASS);

    assertThat(signature, is(notNullValue()));
  }

  @Test
  public void testVerifyDigitalSignature() throws Exception {
    InputStream inputStream = DigitalSignatureTest.class.getResourceAsStream(EXAMPLE_EXPORT);
    byte[] signature = digitalSignature.createDigitalSignature(inputStream, ALIAS, KEYSTORE_PASS);

    inputStream = DigitalSignatureTest.class.getResourceAsStream(EXAMPLE_EXPORT);
    boolean verified =
        digitalSignature.verifyDigitalSignature(
            inputStream, new ByteArrayInputStream(signature), ALIAS);

    assertThat(verified, is(true));
  }

  @Test
  public void testInvalidDigitalSignature() throws Exception {
    InputStream inputStream = DigitalSignatureTest.class.getResourceAsStream(EXAMPLE_EXPORT);
    byte[] signature = digitalSignature.createDigitalSignature(inputStream, ALIAS, KEYSTORE_PASS);

    inputStream = DigitalSignatureTest.class.getResourceAsStream(ARCHIVE_EXPORT);
    boolean verified =
        digitalSignature.verifyDigitalSignature(
            inputStream, new ByteArrayInputStream(signature), ALIAS);

    assertThat(verified, is(false));
  }

  @Test(expected = CatalogCommandRuntimeException.class)
  public void testCreateDigitalSignaturePrivateKeyNotInKeyStore() throws Exception {
    digitalSignature.createDigitalSignature(null, "hello", "world");
  }

  @Test(expected = CatalogCommandRuntimeException.class)
  public void testVerifyDigitalSignatureCertificateNotInKeyStore() throws Exception {
    InputStream inputStream = new ByteArrayInputStream(new byte[0]);
    digitalSignature.verifyDigitalSignature(null, inputStream, "hello");
  }
}
