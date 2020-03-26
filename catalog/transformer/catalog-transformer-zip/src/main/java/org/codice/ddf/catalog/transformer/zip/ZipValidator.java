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
package org.codice.ddf.catalog.transformer.zip;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.codice.ddf.platform.util.properties.PropertiesLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class verifies the signature of a Zip file using techniques found in the Java 8
 * documentation provided by Oracle.
 *
 * <p>http://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/HowToImplAProvider.html#VerifySigs
 */
public class ZipValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(ZipValidator.class);

  private String signaturePropertiesPath;

  private Merlin merlin;

  public void init() {
    try {
      merlin =
          new Merlin(
              PropertiesLoader.getInstance().loadProperties(signaturePropertiesPath),
              ZipDecompression.class.getClassLoader(),
              null);
    } catch (WSSecurityException | IOException e) {
      LOGGER.warn("Unable to read merlin properties file. Unable to validate signatures.", e);
    }
  }

  /**
   * Validates a Zip file with the specified path. If the Zip file does not have a manifest, is not
   * signed, or does not match a certificate in the DDF KeyStore, the validation fails.
   *
   * @param filePath - the path to the zip file to validate
   * @return true when the zip file is valid (signed by a trusted entity).
   */
  public boolean validateZipFile(String filePath) throws ZipValidationException {
    try (JarFile jarFile = new JarFile(filePath)) {

      Manifest man = jarFile.getManifest();

      if (man == null) {
        throw new ZipValidationException("Zip validation failed, missing manifest file.");
      }

      List<JarEntry> entriesVec = new ArrayList<>();

      byte[] buffer = new byte[ZipDecompression.BUFFER_SIZE];

      Enumeration entries = jarFile.entries();

      while (entries.hasMoreElements()) {
        JarEntry je = (JarEntry) entries.nextElement();
        if (je.isDirectory()) {
          continue;
        }

        entriesVec.add(je);
        try (InputStream is = jarFile.getInputStream(je)) {

          while (is.read(buffer, 0, buffer.length) != -1) {
            // Read JarFile
          }
          is.close();
        } catch (IOException e) {
          throw new ZipValidationException(
              String.format(
                  "Zip validation failed, unable to get input stream for entry %s", je.getName()));
        }
      }

      Iterator<JarEntry> iter = entriesVec.iterator();
      while (iter.hasNext()) {
        JarEntry je = iter.next();
        Certificate[] certs = je.getCertificates();

        if ((certs == null) || (certs.length == 0)) {
          if (!je.getName().startsWith("META-INF")) {
            throw new ZipValidationException(
                String.format(
                    "Zip validation failed, unable to get certificates for entry %s",
                    je.getName()));
          }
        } else {
          int startIndex = 0;
          X509Certificate[] certChain;

          while ((certChain = getAChain(certs, startIndex)) != null) {
            try {
              merlin.verifyTrust(certChain[0].getPublicKey());
            } catch (WSSecurityException e1) {
              throw new ZipValidationException(
                  String.format(
                      "Zip validation failed, untrusted certificates for entry %s", je.getName()));
            }
            startIndex += certChain.length;
          }
        }
      }
    } catch (IOException e) {
      throw new ZipValidationException(
          String.format("Zip validation failed for file : %s", filePath));
    }
    return true;
  }

  private X509Certificate[] getAChain(Certificate[] certs, int startIndex) {
    if (startIndex > certs.length - 1) {
      return null;
    }

    int i;
    for (i = startIndex; i < certs.length - 1; i++) {
      if (!((X509Certificate) certs[i + 1])
          .getSubjectDN()
          .equals(((X509Certificate) certs[i]).getIssuerDN())) {
        break;
      }
    }

    int certChainSize = (i - startIndex) + 1;
    X509Certificate[] ret = new X509Certificate[certChainSize];
    for (int j = 0; j < certChainSize; j++) {
      ret[j] = (X509Certificate) certs[startIndex + j];
    }
    return ret;
  }

  public String getSignaturePropertiesPath() {
    return this.signaturePropertiesPath;
  }

  public void setSignaturePropertiesPath(String signaturePropertiesPath) {
    this.signaturePropertiesPath = signaturePropertiesPath;
  }
}
