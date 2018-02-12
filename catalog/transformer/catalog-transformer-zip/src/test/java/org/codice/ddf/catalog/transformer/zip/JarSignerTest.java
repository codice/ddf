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
package org.codice.ddf.catalog.transformer.zip;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JarSignerTest {

  private JarSigner jarSigner;

  private ZipValidator zipValidator;

  private String keyStorePath;

  private File unsignedZipFile;

  @SuppressWarnings("ConstantConditions")
  @Before
  public void setUp() throws IOException {
    File zipFile =
        new File(JarSignerTest.class.getClassLoader().getResource("unsigned.zip").getPath());
    unsignedZipFile =
        new File(zipFile.getParentFile().getAbsolutePath() + File.separator + "newZip.zip");

    FileUtils.copyFile(zipFile, unsignedZipFile);
    keyStorePath = JarSignerTest.class.getClassLoader().getResource("serverKeystore.jks").getPath();
    zipValidator = new ZipValidator();

    zipValidator.setSignaturePropertiesPath(
        JarSignerTest.class.getResource("/signature.properties").getPath());
    zipValidator.init();
    jarSigner = new JarSigner();
  }

  @After
  public void tearDown() throws IOException {
    FileUtils.forceDelete(unsignedZipFile);
  }

  @Test
  public void testSignWithValidKeystore() throws Exception {
    jarSigner.signJar(unsignedZipFile, "localhost", "changeit", keyStorePath, "changeit");
    assertThat(zipValidator.validateZipFile(unsignedZipFile.getPath()), is(true));
  }

  @Test(expected = ZipValidationException.class)
  public void testSignWithInvalidKeystoreCredentials() throws Exception {
    jarSigner.signJar(unsignedZipFile, "localhost", "wrong", keyStorePath, "wrong");
    assertThat(zipValidator.validateZipFile(unsignedZipFile.getPath()), is(false));
  }
}
