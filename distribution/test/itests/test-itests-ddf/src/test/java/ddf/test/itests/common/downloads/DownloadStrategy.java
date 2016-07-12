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
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.util.HttpURLConnection;
import org.apache.commons.lang.NotImplementedException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.message.BasicHttpResponse;

import ddf.test.itests.AbstractIntegrationTest;

/**
 * Contract describing how downloads can be canceled based on how they are started.
 */
public interface DownloadStrategy {

    String CSW_STUB_SOURCE_ID = "cswStubServer";

    /**
     * Steps needed to initialize and start the download stream, regardless if it is actually
     * flowing to the local http client or another location.
     *
     * @param metacardId ID of the metacard representing the product to download.
     * @return A future containing the result of the download.
     */
    Future<String> startDownload(String metacardId, CyclicBarrier barrier);

    /**
     * Stop a download in progress before it completes. This may abruptly close underlying resources.
     */
    void cancelDownload();

    /**
     * Get the abstraction used for synchronizing downloads.
     *
     * @return An AbstractIndexBlocker for controlling data flow.
     */
    AbstractIndexBlocker getIndexBlocker();

    /**
     * Create a synchronous strategy for the caller.
     */
    static DownloadStrategy Synchronous() {
        return new DownloadStrategy() {

            BlockingInputStreamReader reader;

            HttpURLConnection client;

            @Override
            public AbstractIndexBlocker getIndexBlocker() {
                return reader;
            }

            @Override
            public Future<String> startDownload(String metacardId, CyclicBarrier barrier) {
                URL resourceUrl;
                try {
                    resourceUrl = URI.create(getSyncDownloadUrl(metacardId))
                            .toURL();
                } catch (MalformedURLException e) {
                    throw new RuntimeException("Error creating URI with given metacardId", e);
                }

                client = new HttpURLConnection(new GetMethod(), resourceUrl);

                try {
                    client.connect();
                    reader = new BlockingInputStreamReader(metacardId,
                            client.getInputStream(),
                            barrier);
                } catch (IOException e) {
                    throw new RuntimeException(
                            "IOException when connecting to server and resolving stream",
                            e);
                }

                return reader.begin();
            }

            @Override
            public void cancelDownload() {
                if (client == null) {
                    throw new IllegalStateException("Cannot cancel a download that was not started");
                }

                try {
                    reader.close();
                } catch (IOException e) {
                    throw new RuntimeException("Client encountered an error when closing.", e);
                }
            }
        };
    }

    /**
     * Create an asynchronous strategy for the caller and hit the downloads endpoint for status
     * updates.
     */
    static DownloadStrategy Asynchronous() {
        return new DownloadStrategy() {

            BlockingCacheChecker checker;

            @Override
            public AbstractIndexBlocker getIndexBlocker() {
                return checker;
            }

            @Override
            public Future<String> startDownload(String metacardId, CyclicBarrier barrier) {
                CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                client.start();

                Future downloadRequestFuture = client.execute(new HttpGet(getAsyncDownloadUrl(
                        metacardId)), null);
                try {
                    HttpResponse response = (BasicHttpResponse) downloadRequestFuture.get();
                    int responseCode = response.getStatusLine()
                            .getStatusCode();
                    assertThat("Asynchronous request was not successful (responseCode != 200)",
                            responseCode,
                            is(200));
                } catch (InterruptedException e) {
                    Thread.currentThread()
                            .interrupt();
                } catch (ExecutionException e) {
                    throw new RuntimeException("Exception occurred in future's thread", e);
                }

                if (downloadRequestFuture.isCancelled()) {
                    throw new IllegalStateException("Future was cancelled");
                }
                if (!downloadRequestFuture.isDone()) {
                    throw new IllegalStateException("Future has not finished");
                }

                checker = new BlockingCacheChecker(metacardId, barrier);
                return checker.begin();
            }

            @Override
            public void cancelDownload() {
                /* TODO: Look into invoking the cancel endpoint */
                throw new NotImplementedException("Async cancel has not yet been implemented");
            }
        };
    }

    static String getSyncDownloadUrl(String metacardId) {
        return String.format("%s/catalog/sources/%s/%s?transform=resource",
                AbstractIntegrationTest.SERVICE_ROOT.getUrl(),
                CSW_STUB_SOURCE_ID,
                metacardId);
    }

    static String getAsyncDownloadUrl(String metacardId) {
        return String.format("%s?source=%s&metacard=%s",
                AbstractIntegrationTest.RESOURCE_DOWNLOAD_ENDPOINT_ROOT.getUrl(),
                CSW_STUB_SOURCE_ID,
                metacardId);
    }

    static String getDownloadsListUrl() {
        return String.format("%s/catalog/downloads", AbstractIntegrationTest.SERVICE_ROOT.getUrl());
    }
}
