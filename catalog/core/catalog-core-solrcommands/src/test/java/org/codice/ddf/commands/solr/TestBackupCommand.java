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
import org.junit.Test;



public class TestBackupCommand {

    private static final String DEFAULT_CORE_NAME = "catalog";

    @Test
    public void testNoArgBackup() throws Exception {

        ConsoleOutput consoleOutput = new ConsoleOutput();
        consoleOutput.interceptSystemOut();

        final HttpWrapper mockHttpWrapper = mock(HttpWrapper.class);

        HttpResponse httpResponse = prepareResponse(HttpStatus.SC_OK, "");
        ResponseWrapper responseWrapper = new ResponseWrapper(httpResponse);
        when(mockHttpWrapper.execute(any(URI.class))).thenReturn(responseWrapper);

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

        ConsoleOutput consoleOutput = new ConsoleOutput();
        consoleOutput.interceptSystemOut();

        final HttpWrapper mockHttpWrapper = mock(HttpWrapper.class);

        HttpResponse httpResponse = prepareResponse(HttpStatus.SC_OK, "");
        ResponseWrapper responseWrapper = new ResponseWrapper(httpResponse);
        when(mockHttpWrapper.execute(any(URI.class))).thenReturn(responseWrapper);

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

        ConsoleOutput consoleOutput = new ConsoleOutput();
        consoleOutput.interceptSystemOut();

        final HttpWrapper mockHttpWrapper = mock(HttpWrapper.class);

        HttpResponse httpResponse = prepareResponse(HttpStatus.SC_NOT_FOUND, "");
        ResponseWrapper responseWrapper = new ResponseWrapper(httpResponse);
        when(mockHttpWrapper.execute(any(URI.class))).thenReturn(responseWrapper);

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