/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.camel.component.catalog.ingest;

import ddf.camel.component.catalog.CatalogEndpoint;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Response;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostIngestPlugin;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Camel Consumer Post Ingest plugin used to provide the responses passed to Post Ingest Plugins to
 * camel routes.
 */
public class PostIngestConsumer extends DefaultConsumer implements PostIngestPlugin {
  public static final String CREATE = "create";

  public static final String UPDATE = "update";

  public static final String DELETE = "delete";

  public static final String ACTION = "action";

  private static final int THREAD_POOL_DEFAULT_SIZE = 5;

  private CatalogEndpoint endpoint;

  private ServiceRegistration registration;

  private BlockingQueue<Runnable> blockingQueue;

  private ExecutorService threadExecutor;

  private static final Logger LOGGER = LoggerFactory.getLogger(PostIngestConsumer.class);

  public PostIngestConsumer(CatalogEndpoint endpoint, Processor processor) {
    super(endpoint, processor);
    this.endpoint = endpoint;

    Integer threadPoolSize =
        Integer.parseInt(
            System.getProperty(
                "org.codice.ddf.system.threadPoolSize", String.valueOf(THREAD_POOL_DEFAULT_SIZE)));

    blockingQueue = new LinkedBlockingQueue<>();

    threadExecutor =
        new ThreadPoolExecutor(
            THREAD_POOL_DEFAULT_SIZE,
            threadPoolSize,
            TimeUnit.MINUTES.toMillis(30),
            TimeUnit.MILLISECONDS,
            blockingQueue,
            StandardThreadFactoryBuilder.newThreadFactory("postIngestConsumerThread"));
  }

  @Override
  public CreateResponse process(CreateResponse input) throws PluginExecutionException {
    processRequest(input, CREATE);
    return input;
  }

  @Override
  public UpdateResponse process(UpdateResponse input) throws PluginExecutionException {
    processRequest(input, UPDATE);
    return input;
  }

  @Override
  public DeleteResponse process(DeleteResponse input) throws PluginExecutionException {
    processRequest(input, DELETE);
    return input;
  }

  @Override
  protected void doStop() throws Exception {
    super.doStop();
    if (registration != null) {
      registration.unregister();
    }
  }

  @Override
  protected void doStart() throws Exception {
    super.doStart();

    Dictionary<String, String> props = new Hashtable<>();

    registration =
        endpoint
            .getComponent()
            .getBundleContext()
            .registerService(PostIngestPlugin.class.getName(), this, props);
  }

  private void processRequest(Response input, String action) {
    Runnable request = () -> sendMessage(input, action);
    threadExecutor.submit(request);
  }

  private void sendMessage(Response input, String action) {
    try {
      Exchange exchange = getEndpoint().createExchange();
      exchange.getIn().setHeader(ACTION, action);
      exchange.getIn().setBody(input);
      getProcessor().process(exchange);
    } catch (Exception e) {
      LOGGER.debug("Unable to backup data", e);
    }
  }
}
