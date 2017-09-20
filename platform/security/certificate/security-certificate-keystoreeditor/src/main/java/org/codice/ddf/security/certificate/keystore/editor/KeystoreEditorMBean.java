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
package org.codice.ddf.security.certificate.keystore.editor;

import java.util.List;
import java.util.Map;

public interface KeystoreEditorMBean {

  List<Map<String, Object>> getKeystore();

  List<Map<String, Object>> getTruststore();

  void addPrivateKey(
      String alias,
      String keyPassword,
      String storePassword,
      String data,
      String type,
      String fileName)
      throws KeystoreEditor.KeystoreEditorException;

  void addTrustedCertificate(
      String alias,
      String keyPassword,
      String storePassword,
      String data,
      String type,
      String fileName)
      throws KeystoreEditor.KeystoreEditorException;

  List<Map<String, Object>> addTrustedCertificateFromUrl(String url);

  List<Map<String, Object>> certificateDetails(String url);

  /**
   * Replaces the system stores (keystore and truststore) with the passed in stores. All entries in
   * the current stores will be lost.
   *
   * @param fqdn fully qualified domain name used to validate the keystore. The keystore must
   *     contain a key with an alias matching the fqdn
   * @param keyPassword password for private key
   * @param keystorePassword password for the keystoreData
   * @param keystoreData keystore file data (base 64 encoded)
   * @param keystoreFileName keystore filename
   * @param truststorePassword password for the truststoreData
   * @param truststoreData truststore file data (base 64 encoded)
   * @param truststoreFileName truststore filename
   * @return Returns a list containing any error messages. If call was successfull this will be an
   *     empty list.
   * @throws KeystoreEditor.KeystoreEditorException
   */
  List<String> replaceSystemStores(
      String fqdn,
      String keyPassword,
      String keystorePassword,
      String keystoreData,
      String keystoreFileName,
      String truststorePassword,
      String truststoreData,
      String truststoreFileName)
      throws KeystoreEditor.KeystoreEditorException;

  void deletePrivateKey(String alias);

  void deleteTrustedCertificate(String alias);
}
