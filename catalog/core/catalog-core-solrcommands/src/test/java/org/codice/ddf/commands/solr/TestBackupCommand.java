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
package org.codice.ddf.commands.solr;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestBackupCommand {

    private static final String DEFAULT_CORE_NAME = "catalog";

    HttpWrapper mockHttpWrapper;

    ConsoleOutput consoleOutput;

    private static String cipherSuites;

    private static String protocols;

    @Before
    public void before() {
        cipherSuites = System.getProperty("https.cipherSuites");
        System.setProperty("https.cipherSuites",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_DSS_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_128_CBC_SHA");
        protocols = System.getProperty("https.protocols");
        System.setProperty("https.protocols", "TLSv1.1, TLSv1.2");
        consoleOutput = new ConsoleOutput();
        consoleOutput.interceptSystemOut();

        mockHttpWrapper = mock(HttpWrapper.class);
    }

    @After
    public void after() {
        consoleOutput.resetSystemOut();
        if (cipherSuites != null) {
            System.setProperty("https.cipherSuites", cipherSuites);
        } else {
            System.clearProperty("https.cipherSuites");
        }
        if (protocols != null) {
            System.setProperty("https.protocols", protocols);
        } else {
            System.clearProperty("https.protocols");
        }
    }

    @Test
    public void testNoArgBackup() throws Exception {

        when(mockHttpWrapper.execute(any(URI.class)))
                .thenReturn(mockResponse(HttpStatus.SC_OK, ""));

        BackupCommand backupCommand = new BackupCommand() {
            @Override
            protected HttpWrapper getHttpClient() {
                return mockHttpWrapper;
            }
        };
        backupCommand.doExecute();

        assertThat(consoleOutput.getOutput(), containsString(String.format("Backup of [%s] complete.", DEFAULT_CORE_NAME)));

    }

    @Test
    public void testBackupSpecificCore() throws Exception {
        final String coreName = "core";

        when(mockHttpWrapper.execute(any(URI.class)))
                .thenReturn(mockResponse(HttpStatus.SC_OK, ""));

        BackupCommand backupCommand = new BackupCommand() {
            @Override
            protected HttpWrapper getHttpClient() {
                return mockHttpWrapper;
            }
        };

        backupCommand.coreName = coreName;
        backupCommand.doExecute();

        assertThat(consoleOutput.getOutput(), containsString(String.format("Backup of [%s] complete.", coreName)));

    }

    @Test
    public void testBackupInvalidCore() throws Exception {
        final String coreName = "badCoreName";

        when(mockHttpWrapper.execute(any(URI.class)))
                .thenReturn(mockResponse(HttpStatus.SC_NOT_FOUND, ""));

        BackupCommand backupCommand = new BackupCommand() {
            @Override
            protected HttpWrapper getHttpClient() {
                return mockHttpWrapper;
            }
        };

        backupCommand.coreName = coreName;
        backupCommand.doExecute();

        assertThat(consoleOutput.getOutput(), containsString(String.format("Backup command failed due to: %d", HttpStatus.SC_NOT_FOUND)));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSystemPropertiesNotSet() throws Exception {

        BackupCommand backupCommand = new BackupCommand();
        backupCommand.doExecute();
    }


    private ResponseWrapper mockResponse(int statusCode, String responseBody) {
        return new ResponseWrapper(prepareResponse(statusCode, responseBody));
    }

    private HttpResponse prepareResponse(int statusCode, String responseBody) {
        HttpResponse httpResponse = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), statusCode, ""));
        httpResponse.setStatusCode(statusCode);
        try {
            httpResponse.setEntity(new StringEntity(responseBody));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }

        return httpResponse;
    }
}