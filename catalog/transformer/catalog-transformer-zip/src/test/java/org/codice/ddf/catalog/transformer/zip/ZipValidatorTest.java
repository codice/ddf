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

import ddf.security.SecurityConstants;
import java.util.Properties;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ZipValidatorTest {

  private ZipValidator zipValidator;

  private static Properties properties;

  private static final String UNSIGNED_ZIP = "unsigned.zip";

  private static final String UNSIGNED_ZIP_PATH =
      ZipCompressionTest.class.getClassLoader().getResource(UNSIGNED_ZIP).getPath();

  private static final String SIGNED_ZIP = "signed.zip";

  private static final String SIGNED_ZIP_PATH =
      ZipCompressionTest.class.getClassLoader().getResource(SIGNED_ZIP).getPath();

  private static final String NO_CERT_ZIP = "noCert.zip";

  private static final String NO_CERT_ZIP_PATH =
      ZipCompressionTest.class.getClassLoader().getResource(NO_CERT_ZIP).getPath();

  private static final String BAD_CERT_ZIP = "badCert.zip";

  private static final String BAD_CERT_ZIP_PATH =
      ZipCompressionTest.class.getClassLoader().getResource(BAD_CERT_ZIP).getPath();

  private static final String ALTERED_ZIP = "addedFile.zip";

  private static final String ALTERED_ZIP_PATH =
      ZipCompressionTest.class.getClassLoader().getResource(ALTERED_ZIP).getPath();

  private static final String MODIFIED_MANIFEST_ZIP = "addedFile.zip";

  private static final String MODIFIED_MANIFEST_ZIP_PATH =
      ZipCompressionTest.class.getClassLoader().getResource(MODIFIED_MANIFEST_ZIP).getPath();

  private static final String MODIFIED_EXISTING_FILE_ZIP = "addedFile.zip";

  private static final String MODIFIED_EXISTING_FILE_ZIP_PATH =
      ZipCompressionTest.class.getClassLoader().getResource(MODIFIED_EXISTING_FILE_ZIP).getPath();

  @BeforeClass
  public static void setUpBeforeClass() {
    properties = System.getProperties();
    System.setProperty(SecurityConstants.KEYSTORE_TYPE, "jks");
  }

  @AfterClass
  public static void tearDownAfterClass() {
    System.setProperties(properties);
  }

  @Before
  public void setUp() {
    zipValidator = new ZipValidator();
    zipValidator.setSignaturePropertiesPath(
        ZipValidatorTest.class.getResource("/signature.properties").getPath());
    zipValidator.init();
  }

  @Test
  public void testGetSignaturePropertiesPath() {
    assertThat(
        ZipValidatorTest.class.getResource("/signature.properties").getPath(),
        is(zipValidator.getSignaturePropertiesPath()));
  }

  @Test(expected = ZipValidationException.class)
  public void testValidateUnsignedZip() throws Exception {
    boolean result = zipValidator.validateZipFile(UNSIGNED_ZIP_PATH);
    assertThat(result, is(false));
  }

  @Test
  public void testValidateSignedZip() throws Exception {
    boolean result = zipValidator.validateZipFile(SIGNED_ZIP_PATH);
    assertThat(result, is(true));
  }

  @Test(expected = ZipValidationException.class)
  public void testNoCertZip() throws Exception {
    boolean result = zipValidator.validateZipFile(NO_CERT_ZIP_PATH);
    assertThat(result, is(false));
  }

  @Test(expected = ZipValidationException.class)
  public void testBadCertZip() throws Exception {
    boolean result = zipValidator.validateZipFile(BAD_CERT_ZIP_PATH);
    assertThat(result, is(false));
  }

  @Test(expected = SecurityException.class)
  public void testZipWithAddedFile() throws Exception {
    boolean result = zipValidator.validateZipFile(ALTERED_ZIP_PATH);
    assertThat(result, is(false));
  }

  @Test(expected = SecurityException.class)
  public void testZipWithModifiedManifest() throws Exception {
    boolean result = zipValidator.validateZipFile(MODIFIED_MANIFEST_ZIP_PATH);
    assertThat(result, is(false));
  }

  @Test(expected = SecurityException.class)
  public void testZipWithModifiedExistingFile() throws Exception {
    boolean result = zipValidator.validateZipFile(MODIFIED_EXISTING_FILE_ZIP_PATH);
    assertThat(result, is(false));
  }
}
