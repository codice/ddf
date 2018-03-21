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
package ddf.catalog.ftp;

import ddf.catalog.ftp.ftplets.FtpRequestHandler;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.collections.MapUtils;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerConfigurationException;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpStatistics;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.impl.DefaultFtpServer;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.ClientAuth;
import org.apache.ftpserver.ssl.SslConfigurationFactory;
import org.codice.ddf.configuration.PropertyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Registers the {@link FtpRequestHandler} and starts the FTP server for the FTP Endpoint. */
public class FtpServerManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(FtpServerManager.class);

  private static final String DEFAULT_LISTENER = "default";

  public static final String PORT_PROPERTY_KEY = "port";

  public static final String CLIENT_AUTH_PROPERTY_KEY = "clientAuth";

  public static final String WANT = "want";

  public static final String NEED = "need";

  private static int maxSleepTimeMillis = 60000;

  private static int resetWaitTimeMillis = 5000;

  private int port;

  private ClientAuth clientAuth = ClientAuth.WANT;

  private static FtpServer server;

  private FtpServerFactory serverFactory;

  private UserManager userManager;

  private ListenerFactory listenerFactory;

  private SslConfigurationFactory sslConfigurationFactory;

  private Ftplet ftplet;

  private File keyStoreFile;

  private String keyStorePassword;

  private String keyStoreType;

  private File trustStoreFile;

  private String trustStorePassword;

  private String trustStoreType;

  public FtpServerManager(
      Ftplet ftplet,
      FtpServerFactory serverFactory,
      ListenerFactory listenerFactory,
      UserManager userManager,
      SslConfigurationFactory sslConfigurationFactory) {
    notNull(ftplet, "ftplet");
    notNull(serverFactory, "serverFactory");
    notNull(listenerFactory, "listenerFactory");
    notNull(userManager, "userManager");
    notNull(sslConfigurationFactory, "sslConfigurationFactory");

    this.ftplet = ftplet;
    this.serverFactory = serverFactory;
    this.listenerFactory = listenerFactory;
    this.userManager = userManager;
    this.sslConfigurationFactory = sslConfigurationFactory;
  }

  public void init() {
    configureSslConfigurationFactory();
    configureListenerFactory();
    configureServerFactory();

    server = serverFactory.createServer();
    if (server != null) {
      startServer();
    }
  }

  public void destroy() {
    if (server != null && !isStopped()) {
      server.stop();
      LOGGER.debug("FTP server stopped");
    }
  }

  /**
   * Callback for when the FTP Endpoint configuration is updated through the Admin UI
   *
   * @param properties map of configurable properties
   */
  public void updateConfiguration(Map<String, Object> properties) {
    if (MapUtils.isEmpty(properties)) {
      LOGGER.warn(
          "Received null or empty FTP Endpoint configuration. Check the 'FTP Endpoint' configuration.");
      return;
    }

    LOGGER.debug("Updating FTP Endpoint configuration");
    Boolean restart = false;

    if (properties.get(PORT_PROPERTY_KEY) instanceof String) {
      // using PropertyResolver in case properties.get("port") is ${org.codice.ddf.catalog.ftp.port}
      PropertyResolver propertyResolver = new PropertyResolver((String) properties.get("port"));
      int port = Integer.parseInt(propertyResolver.getResolvedString());
      if (this.port != port) {
        setPort(port);
        restart = true;
      }
    }

    if (properties.get(CLIENT_AUTH_PROPERTY_KEY) instanceof String) {
      String clientAuth = ((String) properties.get("clientAuth")).toLowerCase();
      if (!this.clientAuth.toString().equalsIgnoreCase(clientAuth)) {
        setClientAuth(clientAuth);
        restart = true;
      }
    }

    if (restart) {
      restartDefaultListener();
    }
  }

  private void configureListenerFactory() {
    try {
      listenerFactory.setSslConfiguration(sslConfigurationFactory.createSslConfiguration());
    } catch (FtpServerConfigurationException e) {
      LOGGER.warn(
          "Failed to create an SSL configuration, server will not start. Verify keystore and trustore paths and passwords are correct");
      throw new FtpServerConfigurationException();
    }
    listenerFactory.setPort(port);
  }

  private void restartDefaultListener() {
    LOGGER.debug("Restarting FTP server with new port {}.", port);

    if (server != null) {
      waitForConnections();
      suspendServer();
      destroyDefaultListener();
      configureSslConfigurationFactory();
      configureListenerFactory();
      addDefaultListener();
      startServer();
    }
  }

  private void configureSslConfigurationFactory() {
    sslConfigurationFactory.setClientAuthentication(clientAuth.toString());
    sslConfigurationFactory.setKeystoreFile(keyStoreFile);
    sslConfigurationFactory.setKeystorePassword(keyStorePassword);
    sslConfigurationFactory.setKeystoreType(keyStoreType);
    sslConfigurationFactory.setTruststoreFile(trustStoreFile);
    sslConfigurationFactory.setTruststorePassword(trustStorePassword);
    sslConfigurationFactory.setTruststoreType(trustStoreType);
  }

  private void configureServerFactory() {
    serverFactory.addListener(DEFAULT_LISTENER, listenerFactory.createListener());

    Map<String, Ftplet> ftplets = new HashMap<>();
    ftplets.put(FtpRequestHandler.class.getName(), ftplet);

    serverFactory.setFtplets(ftplets);
    serverFactory.setUserManager(userManager);
  }

  private void startServer() {
    if (isStopped() || isSuspended()) {
      try {
        server.start();
        LOGGER.debug("FTP server started on port {}", port);
      } catch (Exception e) {
        LOGGER.warn("Failed to start FTP server", e);
      }
    }
  }

  private void destroyDefaultListener() {
    Listener defaultListener = getDefaultListener();
    if (!defaultListener.isStopped()) {
      defaultListener.stop();
    }
    ((DefaultFtpServer) server).getListeners().clear();
  }

  private void addDefaultListener() {
    ((DefaultFtpServer) server)
        .getListeners()
        .put(DEFAULT_LISTENER, listenerFactory.createListener());
  }

  private void suspendServer() {
    if (!isSuspended()) {
      server.suspend();
    }
  }

  private void waitForConnections() {
    FtpStatistics serverStatistics =
        ((DefaultFtpServer) server).getServerContext().getFtpStatistics();

    final long end = System.currentTimeMillis() + maxSleepTimeMillis;
    boolean interrupt = false;

    try {
      while (serverStatistics.getCurrentConnectionNumber() > 0) {
        LOGGER.debug(
            "Waiting for {} connections to close before updating configuration",
            serverStatistics.getCurrentConnectionNumber());
        try {
          final long totalWait = end - System.currentTimeMillis();

          if (totalWait > 0L) {
            Thread.sleep(resetWaitTimeMillis);
          } else {
            LOGGER.debug(
                "Waited {} milliseconds for connections to close, updating FTP configuration",
                totalWait);
            break;
          }
        } catch (InterruptedException e) {
          interrupt = true;
          LOGGER.info("Thread interrupted while waiting for FTP connections to close", e);
        }
      }
    } finally {
      if (interrupt) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private boolean isStopped() {
    return server.isStopped();
  }

  private boolean isSuspended() {
    return server.isSuspended();
  }

  private Listener getDefaultListener() {
    return ((DefaultFtpServer) server).getListener(DEFAULT_LISTENER);
  }

  public int getPort() {
    return this.port;
  }

  public ClientAuth getClientAuthMode() {
    return clientAuth;
  }

  public void setClientAuth(String newClientAuth) {
    switch (newClientAuth.toLowerCase()) {
      case WANT:
        clientAuth = ClientAuth.WANT;
        break;
      case NEED:
        clientAuth = ClientAuth.NEED;
        break;
      default:
        LOGGER.debug("Invalid clientAuth configuration, defaulting to WANT");
        clientAuth = ClientAuth.WANT;
    }
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void setKeyStoreFile(String keyStoreFilePath) {
    keyStoreFile = new File(keyStoreFilePath);
  }

  public void setKeyStorePassword(String password) {
    keyStorePassword = password;
  }

  public void setKeyStoreType(String type) {
    keyStoreType = type;
  }

  public void setTrustStoreFile(String trustStoreFilePath) {
    trustStoreFile = new File(trustStoreFilePath);
  }

  public void setTrustStorePassword(String password) {
    trustStorePassword = password;
  }

  public void setTrustStoreType(String type) {
    trustStoreType = type;
  }

  private void notNull(Object object, String name) {
    if (object == null) {
      throw new IllegalArgumentException(name + " cannot be null");
    }
  }

  /** For testing purposes */
  protected void setMaxSleepTime(int seconds) {
    this.maxSleepTimeMillis = seconds;
  }

  /** For testing purposes */
  protected void setResetWaitTime(int seconds) {
    this.resetWaitTimeMillis = seconds;
  }
}
