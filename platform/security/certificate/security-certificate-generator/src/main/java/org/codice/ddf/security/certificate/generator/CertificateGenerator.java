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

import java.lang.management.ManagementFactory;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CertificateGenerator implements CertificateGeneratorMBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(CertificateGenerator.class);

  public CertificateGenerator() {
    registerMbean();
  }

  /**
   * Generates new signed certificate. The hostname is used as the certificate's common name.
   * Postcondition is the server keystore is updated to include a private entry. The private entry
   * has the new certificate chain that connects the server to the Demo CA. The matching private key
   * is also stored in the entry. All other private keys will be removed.
   *
   * @return the string used as the common name in the new certificate
   */
  @Override
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
  @Override
  public String configureDemoCert(String commonName) {
    return CertificateCommand.configureDemoCert(commonName, null);
  }

  public KeyStoreFile getKeyStoreFile() {
    return CertificateCommand.getKeyStoreFile();
  }

  protected void registerMbean() {
    try {
      ObjectName objectName =
          new ObjectName(CertificateGenerator.class.getName() + ":service=certgenerator");
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      tryRegisterMBean(mBeanServer, objectName);
    } catch (MalformedObjectNameException e) {
      LOGGER.error("Unable to create Certificate Generator MBean.", e);
    }
  }

  private void tryRegisterMBean(MBeanServer mBeanServer, ObjectName objectName) {
    try {
      mBeanServer.registerMBean(this, objectName);
      LOGGER.debug("Registered Certificate Generator MBean under object name: {}", objectName);
    } catch (InstanceAlreadyExistsException e) {
      LOGGER.error("Re-registered Certificate Generator MBean");
    } catch (MBeanRegistrationException | NotCompliantMBeanException e) {
      LOGGER.error("Could not register MBean [{}].", objectName);
    }
  }
}
