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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicHttpResponse;

/**
 * Provides an asynchronous handle to the asynchronous request and synchronous retrieval of a
 * product utilizing both endpoints in the process.
 */
class AsynchronousClientDownloader extends ClientDownloader {
    private Future downloadRequestFuture = null;

    private String metacardId = null;

    @Override
    public void startDownload(String metacardId) {
        client.start();
        this.metacardId = metacardId;
        downloadRequestFuture = client.execute(new HttpGet(ClientDownloader.getAsyncDownloadUrl(
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
    }

    @Override
    public void stopDownload() {
        // TODO: [DDF-2287] Cannot implement until async cancel is available
        throw new NotImplementedException(
                "API currently does not support the cancellation of async downloads");
    }

    @Override
    protected String processResults() throws ExecutionException {
        if (downloadRequestFuture == null) {
            throw new IllegalStateException("Download was never started, so no future exists");
        }

        Future future =
                client.execute(new HttpGet(ClientDownloader.getSyncDownloadUrl(this.metacardId)),
                        null);

        try {
            HttpResponse response = (BasicHttpResponse) future.get();
            return IOUtils.toString(response.getEntity()
                    .getContent(), "UTF-8");
        } catch (InterruptedException e) {
            Thread.currentThread()
                    .interrupt();
            throw new RuntimeException("Thread interrupted while obtaining results", e);
        } catch (IOException e) {
            throw new RuntimeException("IO failed when obtaining results", e);
        }
    }
}
