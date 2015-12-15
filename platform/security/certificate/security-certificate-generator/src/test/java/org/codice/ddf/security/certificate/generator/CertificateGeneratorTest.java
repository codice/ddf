/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.security.certificate.generator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CertificateGeneratorTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    File systemKeystoreFile = null;

    @Before
    public void setup() throws IOException {

        systemKeystoreFile = temporaryFolder.newFile("serverKeystore.jks");
        FileOutputStream systemKeyOutStream = new FileOutputStream(systemKeystoreFile);
        InputStream systemKeyStream = CertificateGenerator.class.getResourceAsStream(
                "/serverKeystore.jks");
        IOUtils.copy(systemKeyStream, systemKeyOutStream);

        IOUtils.closeQuietly(systemKeyOutStream);
        IOUtils.closeQuietly(systemKeyStream);

        System.setProperty("javax.net.ssl.keyStoreType", "jks");
        System.setProperty("javax.net.ssl.keyStore", systemKeystoreFile.getAbsolutePath());
        System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
    }

    @Test
    public void testConfigureDemoCert() throws Exception {
        CertificateGenerator generator = new CertificateGenerator() {
            @Override
            public void registerMbean() {
                //do nothing
            }
        };
        KeyStoreFile ksf = generator.getKeyStoreFile();
        assertThat(ksf.aliases()
                .size(), is(2));
        assertThat(generator.configureDemoCert("my-fqdn"), is("CN=my-fqdn"));
        generator.configureDemoCert("test2");
        ksf = generator.getKeyStoreFile();
        assertThat(ksf.aliases()
                .size(), is(4));
        assertThat(ksf.isKey("my-fqdn"), is(true));
    }
}
