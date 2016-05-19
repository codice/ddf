/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.transformer.zip;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.wss4j.common.crypto.Merlin;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ddf.security.PropertiesLoader;
import ddf.security.SecurityConstants;

public class TestZipValidator {

    private ZipValidator zipValidator;

    private Merlin merlin;

    private static Properties properties;

    private static final String UNSIGNED_ZIP = "unsigned.zip";

    private static final String UNSIGNED_ZIP_PATH = TestZipCompression.class.getResource(
            File.separator + UNSIGNED_ZIP)
            .getPath();

    private static final String SIGNED_ZIP = "signed.zip";

    private static final String SIGNED_ZIP_PATH = TestZipCompression.class.getResource(
            File.separator + SIGNED_ZIP)
            .getPath();

    private static final String NO_CERT_ZIP = "noCert.zip";

    private static final String NO_CERT_ZIP_PATH = TestZipCompression.class.getResource(
            File.separator + NO_CERT_ZIP)
            .getPath();

    private static final String BAD_CERT_ZIP = "badCert.zip";

    private static final String BAD_CERT_ZIP_PATH = TestZipCompression.class.getResource(
            File.separator + BAD_CERT_ZIP)
            .getPath();

    private static final String ALTERED_ZIP = "addedFile.zip";

    private static final String ALTERED_ZIP_PATH = TestZipCompression.class.getResource(
            File.separator + ALTERED_ZIP)
            .getPath();

    private static final String MODIFIED_MANIFEST_ZIP = "addedFile.zip";

    private static final String MODIFIED_MANIFEST_ZIP_PATH = TestZipCompression.class.getResource(
            File.separator + MODIFIED_MANIFEST_ZIP)
            .getPath();

    private static final String MODIFIED_EXISTING_FILE_ZIP = "addedFile.zip";

    private static final String MODIFIED_EXISTING_FILE_ZIP_PATH =
            TestZipCompression.class.getResource(File.separator + MODIFIED_EXISTING_FILE_ZIP)
                    .getPath();

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
        zipValidator.setSignaturePropertiesPath(TestZipValidator.class.getResource(
                "/signature.properties")
                .getPath());
        zipValidator.init();

        try {
            KeyStore trustStore = KeyStore.getInstance(System.getProperty(
                    "javax.net.ssl.keyStoreType"));
            InputStream trustFIS =
                    TestZipValidator.class.getResourceAsStream("/serverKeystore.jks");
            try {
                trustStore.load(trustFIS, "changeit".toCharArray());
            } catch (CertificateException e) {
                fail(e.getMessage());
            } finally {
                IOUtils.closeQuietly(trustFIS);
            }

            merlin = new Merlin(PropertiesLoader.loadProperties(TestZipValidator.class.getResource(
                    "/signature.properties")
                    .getPath()), ZipValidator.class.getClassLoader(), null);
        } catch (Exception e) {
            fail(e.getMessage());
        }
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
