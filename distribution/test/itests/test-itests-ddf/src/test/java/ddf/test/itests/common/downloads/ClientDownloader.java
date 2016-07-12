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
 */
package ddf.test.itests.common.downloads;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.http.ConnectionClosedException;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.hamcrest.Matchers;

import ddf.test.itests.AbstractIntegrationTest;

/**
 * Download client abstraction for code re-use within integration tests.
 */
public abstract class ClientDownloader {
    private static final String CSW_STUB_SOURCE_ID = "cswStubServer";

    final CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();

    private Throwable caughtException = null;

    Future future = null;

    public static SynchronousClientDownloader createSynchronous() {
        return new SynchronousClientDownloader();
    }

    public static AsynchronousClientDownloader createAsynchronous() {
        return new AsynchronousClientDownloader();
    }

    protected abstract String processResults() throws ExecutionException;

    public abstract void startDownload(String metacardId);

    public abstract void stopDownload();

    public CloseableHttpAsyncClient getClient() {
        return client;
    }

    public String getResults() {
        try {
            return processResults();
        } catch (ExecutionException e) {
            Throwable causeOfException = e.getCause();
            assertThat("Execution failed for a reason other than the connection being closed",
                    causeOfException instanceof ConnectionClosedException,
                    Matchers.is(Boolean.TRUE));
            return causeOfException.toString();
        }
    }

    public static String getSyncDownloadUrl(String metacardId) {
        return String.format("%s/catalog/sources/%s/%s?transform=resource",
                AbstractIntegrationTest.SERVICE_ROOT.getUrl(),
                CSW_STUB_SOURCE_ID,
                metacardId);
    }

    public static String getAsyncDownloadUrl(String metacardId) {
        return String.format("%s?source=%s&metacard=%s",
                AbstractIntegrationTest.RESOURCE_DOWNLOAD_ENDPOINT_ROOT.getUrl(),
                CSW_STUB_SOURCE_ID,
                metacardId);
    }
}