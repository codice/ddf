/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

package org.codice.ddf.security.certificate.generator;

import java.lang.management.ManagementFactory;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CertificateGenerator implements CertificateGeneratorMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateGenerator.class);

    /**
     * Constructor. Registers the object as a service provider.
     */
    public CertificateGenerator() {
        registerMbean();
    }

    /**
     * Generates new signed certificate. The hostname is used as the certificate's common name.
     * Postcondition is the server keystore is updated to include a private entry. The private
     * entry has the new certificate chain  that connects the server to the Demo CA. The matching
     * private key is also stored in the entry.
     *
     * @return the string used as the common name in the new certificate
     */
    public String configureDemoCertWithDefaultHostname() {
        return configureDemoCert(PkiTools.getHostName());
    }

    /**
     * Generates new signed certificate. The input parameter is used as the certificate's common name.
     * Postcondition is the server keystore is updated to include a private entry. The private
     * entry has the new certificate chain  that connects the server to the Demo CA. The matching
     * private key is also stored in the entry.
     *
     * @param commonName string to use as the common name in the new certificate
     * @return the string used as the common name in the new certificate
     */
    public String configureDemoCert(String commonName) {
        CertificateAuthority demoCa = new DemoCertificateAuthority();
        CertificateSigningRequest csr = new CertificateSigningRequest();
        csr.setCommonName(commonName);
        KeyStore.PrivateKeyEntry pkEntry = demoCa.sign(csr);
        KeyStoreFile ksFile = getKeyStoreFile();
        ksFile.setEntry(commonName, pkEntry);
        ksFile.save();
        String distinguishedName = ((X509Certificate) pkEntry.getCertificate()).getSubjectDN()
                .getName();
        return distinguishedName;
    }

    private KeyStoreFile getKeyStoreFile() {
        return KeyStoreFile.openFile(System.getProperty("javax.net.ssl.keyStore"),
                System.getProperty("javax.net.ssl.keyStorePassword").toCharArray());
    }

    private void registerMbean() {
        ObjectName objectName = null;
        MBeanServer mBeanServer = null;
        try {
            objectName = new ObjectName(
                    CertificateGenerator.class.getName() + ":service=certgenerator");
            mBeanServer = ManagementFactory.getPlatformMBeanServer();
        } catch (MalformedObjectNameException e) {
            LOGGER.error("Unable to create Certificate Generator MBean.", e);
        }
        if (mBeanServer != null) {
            try {
                try {
                    mBeanServer.registerMBean(this, objectName);
                    LOGGER.info("Registered Certificate Generator MBean under object name: {}",
                            objectName.toString());
                } catch (InstanceAlreadyExistsException e) {
                    LOGGER.info("Re-registered Certificate Generator MBean");
                }
            } catch (Exception e) {
                LOGGER.error("Could not register MBean [{}].", objectName.toString(), e);
            }
        }
    }
}
