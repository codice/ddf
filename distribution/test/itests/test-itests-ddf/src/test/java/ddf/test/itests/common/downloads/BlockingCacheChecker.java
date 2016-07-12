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
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.message.BasicHttpResponse;
import org.boon.json.JsonFactory;
import org.boon.json.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Special async poller for synchronizing with the current cache download status.
 */
public class BlockingCacheChecker extends AbstractIndexBlocker implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockingCacheChecker.class);

    private final ObjectMapper mapper = JsonFactory.create();

    public BlockingCacheChecker(String metacardId, CyclicBarrier barrier) {
        super(metacardId, barrier);
    }

    @Override
    public void close() throws IOException {
        executorService.shutdownNow();
    }

    @Override
    public Future<String> begin() {
        return executorService.submit(() -> {
            try {
                List json = getDownloadList();
                while (!json.isEmpty()) {
                    boolean sync = true;
                    for (Object object : json) {
                        Map map = (Map) object;
                        int bytesDownloaded = (int) map.get("bytesDownloaded");
                        if (bytesDownloaded < waitAt) {
                            sync = false;
                            break;
                        }
                    }
                    if (sync) {
                        barrier.await();
                    }
                    Thread.sleep(100);
                    json = getDownloadList();
                }
            } catch (BrokenBarrierException | InterruptedException e) {
                LOGGER.error("Exception occurred during download status processing", e);
            }
            return retrieveActualProduct();
        });
    }

    private List getDownloadList() {
        CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
        client.start();
        Future downloadListFuture =
                client.execute(new HttpGet(DownloadStrategy.getDownloadsListUrl()), null);
        try {
            HttpResponse response = (BasicHttpResponse) downloadListFuture.get();
            int responseCode = response.getStatusLine()
                    .getStatusCode();
            if (responseCode == 200) {
                return mapper.fromJson(response.getEntity()
                        .getContent(), List.class);
            }
        } catch (InterruptedException e) {
            Thread.currentThread()
                    .interrupt();
        } catch (IOException | ExecutionException e) {
            throw new RuntimeException("Exception occurred while polling downloads", e);
        }
        throw new RuntimeException("HttpResponse did not have response code of 200");
    }

    private String retrieveActualProduct() {
        CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
        client.start();
        Future productFuture = client.execute(new HttpGet(DownloadStrategy.getSyncDownloadUrl(
                metacardId)), null);
        try {
            HttpResponse response = (BasicHttpResponse) productFuture.get();
            return IOUtils.toString(response.getEntity()
                    .getContent());
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new RuntimeException("Exception occurred while retriving main product", e);
        }
    }
}