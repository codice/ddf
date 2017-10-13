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
package org.codice.ddf.security.certificate.generator;

public class CertificateGenerator {

  /**
   * Generates new signed certificate. The hostname is used as the certificate's common name.
   * Postcondition is the server keystore is updated to include a private entry. The private entry
   * has the new certificate chain that connects the server to the Demo CA. The matching private key
   * is also stored in the entry. All other private keys will be removed.
   *
   * @return the string used as the common name in the new certificate
   */
  public String configureDemoCertWithDefaultHostname() {
    return configureDemoCert(PkiTools.getHostName());
  }

  /**
   * Generates new signed certificate. The input parameter is used as the certificate's common name.
   * Postcondition is the server keystore is updated to include a private entry. The private entry
   * has the new certificate chain that connects the server to the Demo CA. The matching private key
   * is also stored in the entry. All other private keys will be removed.
   *
   * @param commonName string to use as the common name in the new certificate.
   * @return the string used as the common name in the new certificate
   */
  public String configureDemoCert(String commonName) {
    return CertificateCommand.configureDemoCert(commonName, null);
  }

  public KeyStoreFile getKeyStoreFile() {
    return CertificateCommand.getKeyStoreFile();
  }
}
