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
package org.codice.ddf.security.handler.pki;

import ddf.security.common.audit.SecurityLogger;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.cert.CRL;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang.time.DateUtils;
import org.apache.wss4j.common.crypto.Merlin;
import org.codice.ddf.configuration.AbsolutePathResolver;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.codice.ddf.platform.util.properties.PropertiesLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrlChecker {

  public static final String CRL_PROPERTY_KEY = Merlin.OLD_PREFIX + Merlin.X509_CRL_FILE;

  private static final Logger LOGGER = LoggerFactory.getLogger(CrlChecker.class);

  private static String encryptionPropertiesLocation =
      new AbsolutePathResolver("etc/ws-security/server/encryption.properties").getPath();

  private static AtomicReference<CRL> crlCache = new AtomicReference<>();

  private static final CrlRefresh REFRESH = new CrlRefresh();

  static {
    Executors.newScheduledThreadPool(
            1, StandardThreadFactoryBuilder.newThreadFactory("crlCheckerThread"))
        .scheduleWithFixedDelay(REFRESH, 0, 1, TimeUnit.HOURS);
  }

  /**
   * Checks the given certs against the CRL. The CRL is configured in this class's constructor
   * method.
   *
   * @param certs certificates to be checked against the CRL
   * @return true if one of the certs passes the CRL or if CRL revocation is disabled, false if they
   *     are all revoked.
   */
  public boolean passesCrlCheck(X509Certificate[] certs) {
    if (crlCache.get() == null) {
      String errorMsg = "CRL is not set. Skipping CRL check";
      LOGGER.trace(errorMsg);
      return true;
    }
    LOGGER.trace("Checking request certs against CRL.");
    return passesCrl(certs);
  }

  /**
   * Checks if the provided cert is listed in the CRL.
   *
   * @param certs List of certs to be checked against the CRL
   * @return boolean value Whether or not the client presenting the certs should be let through
   */
  private boolean passesCrl(X509Certificate[] certs) {
    if (certs != null) {
      LOGGER.debug("Got {} certificate(s) in the incoming request", certs.length);
      for (X509Certificate curCert : certs) {
        if (crlCache.get() != null && crlCache.get().isRevoked(curCert)) {
          SecurityLogger.audit(
              "Denying access for subject DN: "
                  + curCert.getSubjectDN()
                  + " due to certificate being revoked by CRL.");
          return false;
        }
      }
    } else {
      LOGGER.debug(
          "Allowing message through CRL check. There were no certificates sent by the client.");
      return true;
    }
    return true;
  }

  /**
   * Sets the location of the CRL. Enables CRL checking if property is set, disables it otherwise
   *
   * @param location Location of the DER-encoded CRL file that should be used to check certificate
   *     revocation.
   */
  public void setCrlLocation(String location) {
    REFRESH.setCrlLocation(location);
  }

  static URL urlFromPath(String location) {
    try {
      return new URL(location);
    } catch (MalformedURLException e) {
      return null;
    }
  }

  /**
   * Loads the properties from a given location.
   *
   * @param location location of properties file
   * @return Properties from the file
   */
  static Properties loadProperties(String location) {
    return PropertiesLoader.getInstance().loadProperties(location);
  }

  /**
   * Runnable to refresh the CRL from the URL when either the nextUpdate time has elapsed or it has
   * rolled over to the next day.
   */
  private static class CrlRefresh implements Runnable {
    private Lock lock = new ReentrantLock();

    private Calendar start = Calendar.getInstance();

    public void run() {
      String crlLocation =
          loadProperties(encryptionPropertiesLocation).getProperty(CRL_PROPERTY_KEY);

      lock.lock();
      try {
        CRL crl = crlCache.get();
        if (crl == null) {
          setCrlLocation(crlLocation);
        }

        if (crl instanceof X509CRL && checkCrlUpdate((X509CRL) crl)) {
          setCrlLocation(crlLocation);
        }
      } finally {
        lock.unlock();
      }
    }

    /**
     * Sets the location of the CRL. Enables CRL checking if property is set, disables it otherwise
     *
     * @param location Location of the DER-encoded CRL file that should be used to check certificate
     *     revocation.
     */
    void setCrlLocation(String location) {
      lock.lock();
      try {
        if (location == null) {
          LOGGER.info(
              "CRL property in {} is not set. Certs will not be checked against a CRL",
              encryptionPropertiesLocation);
          crlCache.set(null);
        } else {
          CRL crl = createCrl(location);
          if (crl != null) {
            crlCache.set(crl);
            LOGGER.info("CRL has been updated from {}.", location);
          }
        }
        start = Calendar.getInstance();
      } finally {
        lock.unlock();
      }
    }

    /**
     * Generates a new CRL object from the given location.
     *
     * @param location File Path or URL to the CRL file
     * @return A CRL object constructed from the given file path or URL. Null if an error occurred
     *     while attempting to read the file.
     */
    private CRL createCrl(String location) {
      URL url = urlFromPath(location);

      // If we get a URL, use URL, otherwise use as local file path
      try (InputStream is =
          url != null ? url.openStream() : new FileInputStream(new File(location))) {

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return cf.generateCRL(is);
      } catch (IOException e) {
        LOGGER.warn("An error occurred while accessing {}", location, e);
        return null;
      } catch (GeneralSecurityException e) {
        LOGGER.warn(
            "Encountered an error while generating CRL from file {}. CRL checking may not work correctly. Check the CRL file.",
            location,
            e);
        return null;
      }
    }

    private boolean checkCrlUpdate(X509CRL x509Crl) {
      return x509Crl.getNextUpdate() == null
          || Calendar.getInstance().getTime().after(x509Crl.getNextUpdate())
          || rolledOverDay();
    }

    private boolean rolledOverDay() {
      return !DateUtils.isSameDay(Calendar.getInstance(), start);
    }
  }
}
