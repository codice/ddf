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
package org.codice.ddf.ui.admin.api;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.management.MalformedObjectNameException;

import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.configuration.SystemInfo;
import org.codice.ddf.security.certificate.generator.CertificateGeneratorMBean;
import org.codice.ddf.security.certificate.keystore.editor.KeystoreEditor;
import org.codice.ddf.security.certificate.keystore.editor.KeystoreEditorMBean;
import org.codice.ddf.ui.admin.api.impl.SystemPropertiesAdmin;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SystemPropertiesAdminTest {
    File etcFolder = null;

    File systemPropsFile = null;

    File userPropsFile = null;

    SystemBaseUrl sbu = null;

    SystemInfo info = null;

    @Before
    public void setUp() throws IOException {
        System.setProperty(SystemBaseUrl.HOST, "host");
        System.setProperty(SystemBaseUrl.PORT, "1234");
        System.setProperty(SystemBaseUrl.HTTP_PORT, "4567");
        System.setProperty(SystemBaseUrl.HTTPS_PORT, "8901");
        System.setProperty(SystemBaseUrl.PROTOCOL, "https://");
        System.setProperty(SystemInfo.ORGANIZATION, "org");
        System.setProperty(SystemInfo.SITE_CONTACT, "contact");
        System.setProperty(SystemInfo.SITE_NAME, "site");
        System.setProperty(SystemInfo.VERSION, "version");

        TemporaryFolder temporaryFolder = new TemporaryFolder();
        temporaryFolder.create();
        etcFolder = temporaryFolder.newFolder("etc");
        System.setProperty("karaf.etc", etcFolder.getAbsolutePath());

        systemPropsFile = new File(etcFolder, "system.properties");
        userPropsFile = new File(etcFolder, "users.properties");

        sbu = new SystemBaseUrl();
        info = new SystemInfo();
    }

    @Test
    public void testReadSystemProperties() {
        SystemPropertiesAdmin spa = new SystemPropertiesAdmin();
        List<SystemPropertyDetails> details = spa.readSystemProperties();
        assertThat(details.size(), is(9));
    }

    @Test
    public void testWriteSystemPropertiesNullProps() {
        SystemPropertiesAdmin spa = new SystemPropertiesAdmin();
        spa.writeSystemProperties(null);
        List<SystemPropertyDetails> details = spa.readSystemProperties();
        assertThat(details.size(), is(9));
    }

    @Test
    public void testWriteSystemPropertie() throws Exception {

        Properties userProps = new Properties();
        userProps.put("admin", "admin,group,somethingelse");
        userProps.put("host", "host,group,somethingelse");
        userProps.store(new FileOutputStream(userPropsFile), null);

        SystemPropertiesAdmin spa = new SystemPropertiesAdmin();
        Map<String, String> map = new HashMap<>();
        map.put(SystemBaseUrl.HOST, "newhost");
        spa.writeSystemProperties(map);
        List<SystemPropertyDetails> details = spa.readSystemProperties();
        assertThat(details.size(), is(9));
        assertThat(sbu.getHost(), equalTo("newhost"));

        //only writes out the changed props
        assertTrue(systemPropsFile.exists());
        Properties sysProps = new Properties();
        sysProps.load(new FileReader(systemPropsFile));
        assertThat(sysProps.size(), is(1));
        assertThat(sysProps.getProperty(SystemBaseUrl.HOST), equalTo("newhost"));

        userProps = new Properties();
        userProps.load(new FileReader(userPropsFile));
        assertThat(userProps.size(), is(2));
        assertThat(userProps.getProperty("newhost"), equalTo("host,group,somethingelse"));

        map.put(SystemBaseUrl.HOST, "anotherhost");
        spa.writeSystemProperties(map);
        userProps = new Properties();
        userProps.load(new FileReader(userPropsFile));
        assertThat(userProps.size(), is(2));
        assertThat(userProps.getProperty("anotherhost"), equalTo("host,group,somethingelse"));
        assertNull(userProps.getProperty("newhost"));
        assertNull(userProps.getProperty("host"));
    }

    @Test
    public void testSetSystemCertsNullProps() {
        SystemPropertiesAdmin spa = new SystemPropertiesAdmin();
        List<String> errors = spa.setSystemCerts(null);
        assertThat(errors.size(), is(1));
    }

    @Test
    public void testSetSystemCertsEmptyProps() {
        SystemPropertiesAdmin spa = new SystemPropertiesAdmin();
        List<String> errors = spa.setSystemCerts(new HashMap<String, Object>());
        assertThat(errors.size(), is(2));
    }

    @Test
    public void testSetSystemCertDevMode() throws Exception {
        SystemPropertiesAdmin spa = new SystemPropertiesAdmin() {
            protected KeystoreEditorMBean getKeystorEditorMbean()
                    throws MalformedObjectNameException {
                KeystoreEditorMBean kem = mock(KeystoreEditorMBean.class);
                return kem;
            }

            protected CertificateGeneratorMBean getCertificateGeneratorMBean()
                    throws MalformedObjectNameException {
                CertificateGeneratorMBean cgm = mock(CertificateGeneratorMBean.class);
                return cgm;
            }
        };
        Map<String, Object> map = new HashMap<>();
        map.put("devMode", true);
        List<String> errors = spa.setSystemCerts(map);
        assertThat(errors.size(), is(0));
    }

    @Test
    public void testSetSystemCert() throws Exception {
        KeystoreEditorMBean kem = mock(KeystoreEditorMBean.class);

        when(kem.keystoreContainsEntry(eq("newhost"), anyString(), anyString(), anyString(),
                anyString())).thenReturn(true);

        SystemPropertiesAdmin spa = new SystemPropertiesAdmin() {
            protected KeystoreEditorMBean getKeystorEditorMbean()
                    throws MalformedObjectNameException {
                return kem;
            }
        };
        System.setProperty(SystemBaseUrl.HOST, "newhost");
        List<String> errors = spa.setSystemCerts(getDefaultProps());
        assertThat(errors.size(), is(0));
    }

    @Test
    public void testSetSystemCertInvalidCert() throws Exception {
        KeystoreEditorMBean kem = mock(KeystoreEditorMBean.class);

        when(kem.keystoreContainsEntry(anyString(), anyString(), anyString(), anyString(),
                anyString())).thenReturn(false);

        SystemPropertiesAdmin spa = new SystemPropertiesAdmin() {
            protected KeystoreEditorMBean getKeystorEditorMbean()
                    throws MalformedObjectNameException {
                return kem;
            }
        };
        System.setProperty(SystemBaseUrl.HOST, "badHost");
        List<String> errors = spa.setSystemCerts(getDefaultProps());
        assertThat(errors.size(), is(1));
    }

    @Test
    public void testSetSystemCertEditorError() throws Exception {
        KeystoreEditorMBean kem = mock(KeystoreEditorMBean.class);

        when(kem.keystoreContainsEntry(anyString(), anyString(), anyString(), anyString(),
                anyString())).thenReturn(true);
        doThrow(new KeystoreEditor.KeystoreEditorException("")).when(kem)
                .addAllKeystoreEntries(anyString(), anyString(), anyString(), anyString(),
                        anyString());

        SystemPropertiesAdmin spa = new SystemPropertiesAdmin() {
            protected KeystoreEditorMBean getKeystorEditorMbean()
                    throws MalformedObjectNameException {
                return kem;
            }
        };

        List<String> errors = spa.setSystemCerts(getDefaultProps());
        assertThat(errors.size(), is(1));
    }

    private Map<String, Object> getDefaultProps() {
        Map<String, Object> map = new HashMap<>();
        map.put("keystoreFile", "data");
        map.put("keystoreFileName", "something.jks");
        map.put("keystorePass", "pass1");
        map.put("keyPass", "pass2");
        map.put("truststoreFile", "data2");
        map.put("truststoreFileName", "something.jks");
        map.put("truststorePass", "pass3");
        map.put("devMode", false);
        return map;
    }

}
