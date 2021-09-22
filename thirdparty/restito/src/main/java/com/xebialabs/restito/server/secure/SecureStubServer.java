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
package com.xebialabs.restito.server.secure;

import static java.util.Collections.unmodifiableList;

import com.xebialabs.restito.semantics.Call;
import com.xebialabs.restito.semantics.Stub;
import com.xebialabs.restito.server.StubServer;
import com.xebialabs.restito.support.log.CallsHelper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.mina.util.AvailablePortFinder;
import org.glassfish.grizzly.PortRange;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class can be removed if pull request https://github.com/mkotsur/restito/pull/46 is merged
 * into master this just extends StubServer to adds the functionality present in that pull request
 */
public class SecureStubServer extends StubServer {

  private static final Logger LOGGER = LoggerFactory.getLogger(StubServer.class);

  private final List<Call> calls = new CopyOnWriteArrayList<>();

  private final List<Stub> stubs = new CopyOnWriteArrayList<>();

  private final HttpServer simpleServer;

  /** Whether or not the server should run in HTTPS mode. */
  private boolean isSecured = true;

  /** Creates a server based on stubs that are used to determine behavior. */
  public SecureStubServer(Stub... stubs) {
    this(DEFAULT_PORT, AvailablePortFinder.MAX_PORT_NUMBER, stubs);
  }

  /**
   * This constructor allows to specify the port range beside stubs. Grizzly will select the first
   * available port.
   */
  public SecureStubServer(int portRangeStart, int portRangeEnd, Stub... stubs) {
    this.stubs.addAll(Arrays.asList(stubs));
    simpleServer = HttpServer.createSimpleServer(null, new PortRange(portRangeStart, portRangeEnd));
  }

  /**
   * This constructor allows to specify the port beside stubs. If the port is busy, Restito won't
   * try to pick different one and java.net.BindException will be thrown.
   */
  public SecureStubServer(int port, Stub... stubs) {
    this.stubs.addAll(Arrays.asList(stubs));
    simpleServer = HttpServer.createSimpleServer(null, port);
  }

  /** It is possible to add a stub even after the server is started */
  @Override
  public SecureStubServer addStub(Stub s) {
    this.stubs.add(s);
    return this;
  }

  /** Starts the server */
  @Override
  public SecureStubServer run() {
    simpleServer.getServerConfiguration().addHttpHandler(stubsToHandler(), "/");
    try {
      if (isSecured) {
        for (NetworkListener networkListener : simpleServer.getListeners()) {
          networkListener.setSecure(true);
          SSLEngineConfigurator sslEngineConfig =
              new SSLEngineConfigurator(getSslConfig(), false, false, false);
          networkListener.setSSLEngineConfig(sslEngineConfig);
        }
      }
      simpleServer.start();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    return this;
  }

  @SuppressWarnings("squid:S2177")
  private SSLContextConfigurator getSslConfig() throws IOException {
    SSLContextConfigurator defaultConfig = SSLContextConfigurator.DEFAULT_CONFIG;
    if (!defaultConfig.validateConfiguration(true)) {
      String keystoreServer = createCertificateStore("keystore_server");
      String truststoreServer = createCertificateStore("truststore_server");
      defaultConfig.setKeyStoreFile(keystoreServer);
      defaultConfig.setKeyStorePass("secret");
      defaultConfig.setTrustStoreFile(truststoreServer);
      defaultConfig.setTrustStorePass("secret");
    }
    return defaultConfig;
  }

  /**
   * Copy the Certificate store to the temporary directory, as it needs to be in a real file, not
   * inside a jar for Grizzly to pick it up.
   *
   * @param resourceName The Store to copy
   * @return The absolute path to the temporary keystore.
   * @throws IOException If the store could not be copied.
   */
  @SuppressWarnings("squid:S2177")
  private String createCertificateStore(String resourceName) throws IOException {
    URL resource = StubServer.class.getResource("/" + resourceName);
    File store = File.createTempFile(resourceName, "store");
    try (InputStream input = resource.openStream()) {
      Files.copy(input, store.toPath(), StandardCopyOption.REPLACE_EXISTING);
    } finally {
      store.deleteOnExit();
    }
    return store.getAbsolutePath();
  }

  /** Alias for StubServer.run() */
  @Override
  public void start() {
    run();
  }

  /** Stops the server */
  @Override
  public SecureStubServer stop() {
    simpleServer.shutdownNow();
    return this;
  }

  /** Sets the Server in Secure mode. If it is already running, ignores the call. */
  @Override
  public SecureStubServer secured() {
    if (!simpleServer.isStarted()) {
      this.isSecured = true;
    }
    return this;
  }

  /** Returns the port which the server is running at */
  @Override
  public int getPort() {
    return simpleServer.getListeners().iterator().next().getPort();
  }

  /**
   * Returns calls performed to the server. Returned list is actually a copy of the original one.
   * This is done to prevent concurrency issues. See <a
   * href="https://github.com/mkotsur/restito/issues/33">#33</a>.
   */
  @Override
  public List<Call> getCalls() {
    return unmodifiableList(calls);
  }

  /**
   * Returns stubs associated with the server. Returned list is actually a copy of the original one.
   * This is done to prevent concurrency issues. See <a
   * href="https://github.com/mkotsur/restito/issues/33">#33</a>.
   */
  @Override
  public List<Stub> getStubs() {
    return unmodifiableList(stubs);
  }

  @SuppressWarnings("squid:S2177")
  private HttpHandler stubsToHandler() {
    return new HttpHandler() {
      @Override
      public void service(Request request, Response response) throws Exception {
        Call call = Call.fromRequest(request);

        CallsHelper.logCall(call);

        boolean processed = false;
        ListIterator<Stub> iterator = stubs.listIterator(stubs.size());
        while (iterator.hasPrevious()) {
          Stub stub = iterator.previous();
          if (!stub.isApplicable(call)) {
            continue;
          }

          stub.apply(response);
          processed = true;
          break;
        }

        if (!processed) {
          response.setStatus(HttpStatus.NOT_FOUND_404);
          LOGGER.debug(
              "Request {} hasn't been covered by any of {} stubs.",
              request.getRequestURI(),
              stubs.size());
        }

        calls.add(call);
      }
    };
  }
}
