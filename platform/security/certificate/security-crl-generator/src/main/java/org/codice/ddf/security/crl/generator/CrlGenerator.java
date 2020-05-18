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
package org.codice.ddf.security.crl.generator;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteSource;
import com.google.common.io.FileBackedOutputStream;
import ddf.security.audit.SecurityLogger;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.wss4j.common.crypto.Merlin;
import org.codice.ddf.configuration.AbsolutePathResolver;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.codice.ddf.platform.util.properties.PropertiesLoader;
import org.codice.ddf.system.alerts.NoticePriority;
import org.codice.ddf.system.alerts.SystemNotice;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The CrlGenerator downloads and saves a Certificate Revocation List from a URL specified by {@code
 * crlLocationUrl}.
 */
public class CrlGenerator implements Runnable {
  private static final Logger LOGGER = LoggerFactory.getLogger(CrlGenerator.class);
  private static final PropertiesLoader PROPERTIES_LOADER = PropertiesLoader.getInstance();
  private static final String HTTPS = "https://";
  private static final int INITIAL_DELAY = 0;
  private static final int SCHEDULER_INTERVAL = 30;
  private static final int SHUTDOWN_TIMEOUT_SECONDS = 60;
  private static final int FILE_BACKED_STREAM_THRESHOLD = 1_000_000;
  static final String CRL_PROPERTY_KEY = Merlin.OLD_PREFIX + Merlin.X509_CRL_FILE;
  static final String DEM_CRL = "/localCrl.crl";
  static final String PEM_CRL = "/localCrl.pem";

  @VisibleForTesting
  static String issuerEncryptionPropertiesLocation =
      new AbsolutePathResolver("etc/ws-security/issuer/encryption.properties").getPath();

  @VisibleForTesting
  static String issuerSignaturePropertiesLocation =
      new AbsolutePathResolver("etc/ws-security/issuer/signature.properties").getPath();

  @VisibleForTesting
  static String serverEncryptionPropertiesLocation =
      new AbsolutePathResolver("etc/ws-security/server/encryption.properties").getPath();

  @VisibleForTesting
  static String serverSignaturePropertiesLocation =
      new AbsolutePathResolver("etc/ws-security/server/signature.properties").getPath();

  @VisibleForTesting
  static String crlFileLocation = new AbsolutePathResolver("etc/keystores").getPath();

  private final ClientFactoryFactory factory;
  private final EventAdmin eventAdmin;
  private final ScheduledExecutorService scheduler;
  private String crlLocationUrl;
  private boolean crlByUrlEnabled;
  private ScheduledFuture<?> handle;
  private Future<?> removalHandle;
  private SecurityLogger securityLogger;

  public CrlGenerator(ClientFactoryFactory factory, EventAdmin eventAdmin) {
    this.factory = factory;
    this.eventAdmin = eventAdmin;
    this.scheduler =
        Executors.newSingleThreadScheduledExecutor(
            StandardThreadFactoryBuilder.newThreadFactory("crlThread"));
  }

  @VisibleForTesting
  CrlGenerator(
      ClientFactoryFactory factory, EventAdmin eventAdmin, ScheduledExecutorService scheduler) {
    this.factory = factory;
    this.eventAdmin = eventAdmin;
    this.scheduler = scheduler;
  }

  /** Pulls down the CRL file provided via URL and writes it locally. */
  @Override
  public synchronized void run() {
    if (!crlByUrlEnabled || crlLocationUrl == null) {
      return;
    }

    if (!crlLocationUrl.startsWith(HTTPS)) {
      postErrorEvent("The provided URL was not an HTTPS URL.");
      return;
    }

    Object entity = getRemoteCrl();
    if (!(entity instanceof InputStream)) {
      postErrorEvent("Unable to download the remote CRL.");
      return;
    }

    FileBackedOutputStream fileBackedOutputStream = null;
    try {
      // Read the response content and get the byte source
      ByteSource byteSource;
      try (InputStream entityStream = (InputStream) entity) {
        fileBackedOutputStream = new FileBackedOutputStream(FILE_BACKED_STREAM_THRESHOLD);
        IOUtils.copy(entityStream, fileBackedOutputStream);
        fileBackedOutputStream.close();
        byteSource = fileBackedOutputStream.asByteSource();
      }

      File crlFile = getCrlFile(byteSource);
      // Verify that the CRL is valid
      if (!crlIsValid(byteSource)) {
        postErrorEvent("An error occurred while validating the CRL.");
        return;
      }
      writeCrlToFile(byteSource, crlFile);
    } catch (IOException e) {
      LOGGER.warn("Unable to copy CRL to local CRL. {}", e.getMessage());
      postErrorEvent("An error occurred while downloading the CRL.");
    } finally {
      // Cleanup temp file
      if (fileBackedOutputStream != null) {
        try {
          fileBackedOutputStream.reset();
        } catch (IOException e) {
          LOGGER.warn("Error occurred while deleting the temporary file. {}", e.getMessage());
        }
      }
    }
  }
  /**
   * Get the CRL from the given URL.
   *
   * @return - the response body
   */
  private Object getRemoteCrl() {
    SecureCxfClientFactory cxfClientFactory =
        factory.getSecureCxfClientFactory(crlLocationUrl, WebClient.class);
    WebClient client = cxfClientFactory.getWebClient();
    Response response = client.get();
    return response.getEntity();
  }
  /**
   * Determines the CRL encoding and creates a CRL file.
   *
   * @param byteSource - CRL content as a byte source
   * @return - the created file
   * @throws IOException
   */
  private File getCrlFile(ByteSource byteSource) throws IOException {
    // Determine the file extension
    File crlFile;
    try (InputStream inputStream = byteSource.slice(0, 300).openStream()) {
      if (IOUtils.toString(inputStream, StandardCharsets.UTF_8).contains("-----BEGIN")) {
        crlFile = new File(crlFileLocation + PEM_CRL);
      } else {
        crlFile = new File(crlFileLocation + DEM_CRL);
      }
    }
    return crlFile;
  }
  /**
   * Validates the given CRL by attempting to create a {@link CRL}
   *
   * @param byteSource - CRL byte source
   * @return - True if the CRL is valid. False if its invalid
   */
  private boolean crlIsValid(ByteSource byteSource) {
    try (InputStream inputStream = byteSource.openStream()) {
      CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
      certificateFactory.generateCRL(inputStream);
    } catch (CertificateException | CRLException | IOException e) {
      LOGGER.warn("An error occurred while validating the CRL. {}", e.getMessage());
      return false;
    }
    return true;
  }
  /**
   * Writes out the CRL to the given file and adds its path to the property files.
   *
   * @param byteSource - CRL byte source
   * @param crlFile - the file to write the CRL to
   */
  private void writeCrlToFile(ByteSource byteSource, File crlFile) {
    try {
      AccessController.doPrivileged(
          (PrivilegedExceptionAction<Void>)
              () -> {
                // Write out to file
                try (OutputStream outStream = new FileOutputStream(crlFile);
                    InputStream inputStream = byteSource.openStream()) {
                  IOUtils.copy(inputStream, outStream);
                  securityLogger.audit(
                      "Copied the content of the CRl at {} to the local CRL at {}.",
                      crlLocationUrl,
                      crlFile.getPath());
                  setCrlFileLocationInPropertiesFile(crlFile.getPath());
                }
                return null;
              });
    } catch (PrivilegedActionException e) {
      LOGGER.warn("Unable to save the CRL.");
      LOGGER.debug("Unable to save the CRL. {}", e.getCause());
      postErrorEvent("Unable to save the CRL.");
    }
  }
  /**
   * Sets the org.apache.ws.security.crypto.merlin.x509crl.file property in the signature.properties
   * and encryption.properties files.
   *
   * @param localCrlPath - CRL file path
   */
  @VisibleForTesting
  void setCrlFileLocationInPropertiesFile(String localCrlPath) {
    addProperty(issuerSignaturePropertiesLocation, localCrlPath);
    addProperty(issuerEncryptionPropertiesLocation, localCrlPath);
    addProperty(serverSignaturePropertiesLocation, localCrlPath);
    addProperty(serverEncryptionPropertiesLocation, localCrlPath);
    securityLogger.audit(
        "Setting the {} property to {} as signature and encryption properties.",
        CRL_PROPERTY_KEY,
        localCrlPath);
  }
  /**
   * Adds the CRL file location to the given property file.
   *
   * @param propertyFilePath - Property file path
   * @param localCrlPath - CRL file path
   */
  private void addProperty(String propertyFilePath, String localCrlPath) {
    Properties properties =
        PROPERTIES_LOADER.loadPropertiesWithoutSystemPropertySubstitution(propertyFilePath, null);
    properties.put(CRL_PROPERTY_KEY, localCrlPath);
    try (OutputStream outputStream = new FileOutputStream(propertyFilePath)) {
      properties.store(outputStream, null);
    } catch (IOException e) {
      LOGGER.warn(
          "Unable to add the {} property to the property file {}.",
          CRL_PROPERTY_KEY,
          propertyFilePath);
    }
  }
  /**
   * Removes the org.apache.ws.security.crypto.merlin.x509crl.file property in the
   * signature.properties and encryption.properties files.
   */
  @VisibleForTesting
  void removeCrlFileLocationInPropertiesFile() {
    try {
      AccessController.doPrivileged(
          (PrivilegedExceptionAction<Void>)
              () -> {
                removeProperty(issuerSignaturePropertiesLocation);
                removeProperty(issuerEncryptionPropertiesLocation);
                removeProperty(serverSignaturePropertiesLocation);
                removeProperty(serverEncryptionPropertiesLocation);
                securityLogger.audit(
                    "Removing the {} property from signature and encryption properties.",
                    CRL_PROPERTY_KEY);
                return null;
              });
    } catch (PrivilegedActionException e) {
      LOGGER.warn(
          "Unable to remove the CRL property from the signature.properties and encryption.properties files.");
      LOGGER.debug(
          "Unable to remove the CRL property from the signature.properties and encryption.properties files. {}",
          e.getCause());
      postErrorEvent(
          "Unable to remove the CRL property from the signature.properties and encryption.properties files.");
    }
  }
  /**
   * Removes the CRL file location from the given property file.
   *
   * @param propertyFilePath - Property file path
   */
  private void removeProperty(String propertyFilePath) {
    Properties properties =
        PROPERTIES_LOADER.loadPropertiesWithoutSystemPropertySubstitution(propertyFilePath, null);
    properties.remove(CRL_PROPERTY_KEY);
    try (OutputStream outputStream = new FileOutputStream(propertyFilePath)) {
      properties.store(outputStream, null);
    } catch (IOException e) {
      LOGGER.warn(
          "Unable to remove the {} property to the property file {}.",
          CRL_PROPERTY_KEY,
          propertyFilePath);
    }
  }
  /**
   * Sets the URL to download the CRL from and starts a task to download the CRL every 30 minutes.
   *
   * @param crlLocationUrl - CRL's URL location
   */
  public synchronized void setCrlLocationUrl(String crlLocationUrl) {
    this.crlLocationUrl = crlLocationUrl;
    if (handle != null) {
      handle.cancel(false);
    }
    handle =
        scheduler.scheduleAtFixedRate(this, INITIAL_DELAY, SCHEDULER_INTERVAL, TimeUnit.MINUTES);
  }
  /**
   * Enables or disables the CRL download. If it's disabled, starts a task to remove the CRL file
   * path from property files.
   *
   * @param crlByUrlEnabled - whether the feature is enabled or not
   */
  public synchronized void setCrlByUrlEnabled(boolean crlByUrlEnabled) {
    if (this.crlByUrlEnabled && !crlByUrlEnabled) {
      if (removalHandle != null) {
        removalHandle.cancel(false);
      }
      removalHandle = scheduler.submit(this::removeCrlFileLocationInPropertiesFile);
    }

    this.crlByUrlEnabled = crlByUrlEnabled;
  }

  /** Destroy method to shutdown the scheduler when the configuration is deleted. */
  public void destroy() {
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
        if (!scheduler.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
          LOGGER.error("CRL thread was unable to terminate successfully.");
        }
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Posts and error message to the Admin Console.
   *
   * @param errorMessage - The reason for the error.
   */
  private void postErrorEvent(String errorMessage) {
    String title =
        "Unable to download the Certificate Revocation List (CRL) from " + crlLocationUrl;
    Set<String> details = new HashSet<>();
    details.add(
        "The provided CRL could not be downloaded. Please check the provided URL and/or the contents of the given CRL.");
    details.add(errorMessage);
    details.add("To recover, resolve the issue and save the configuration.");
    eventAdmin.postEvent(
        new Event(
            SystemNotice.SYSTEM_NOTICE_BASE_TOPIC + "crl",
            new SystemNotice(this.getClass().getName(), NoticePriority.CRITICAL, title, details)
                .getProperties()));
    securityLogger.audit(title);
    securityLogger.audit(errorMessage);
  }

  public void setSecurityLogger(SecurityLogger securityLogger) {
    this.securityLogger = securityLogger;
  }
}
