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

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicHttpResponse;

/**
 * Provides an asynchronous handle to the synchronous retrieval and download endpoint of a product.
 */
class SynchronousClientDownloader extends ClientDownloader {
    @Override
    public void startDownload(String metacardId) {
        client.start();
        future = client.execute(new HttpGet(ClientDownloader.getSyncDownloadUrl(metacardId)), null);
    }

    @Override
    public void stopDownload() {
        try {
            client.close();
        } catch (IOException e) {
            throw new RuntimeException("Client encountered an error when closing.", e);
        }
    }

    @Override
    protected String processResults() throws ExecutionException {
        if (future == null) {
            throw new IllegalStateException("Download was never started, so no future exists");
        }
        try {
            HttpResponse response = (BasicHttpResponse) future.get();
            return IOUtils.toString(response.getEntity()
                    .getContent(), "UTF-8");
        } catch (InterruptedException e) {
            Thread.currentThread()
                    .interrupt();
            throw new RuntimeException("Interrupted test", e);
        } catch (IOException e) {
            throw new RuntimeException("IO failed when obtaining results", e);
        }
    }
}