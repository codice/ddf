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

package ddf.security.certificate.generator;

import java.lang.management.ManagementFactory;
import java.security.KeyStore;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CertificateGenerator implements CertificateGeneratorMBean {
    public static final String SERVICE_NAME = ":service=demo-certificate-generation-service";

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateGenerator.class);

    ObjectName objectName;

    MBeanServer mBeanServer;

    PkiTools pkiTools = new PkiTools();

    public CertificateGenerator() {
        mBeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName objName = null;

        try {
            objName = new ObjectName(CertificateGenerator.class.getName() + SERVICE_NAME);

        } catch (MalformedObjectNameException mone) {
            LOGGER.info("Could not create objectName.", mone);
        }

        objectName = objName;
    }

    public void installCertificate(String commonName) {
        CertificateAuthority demoCa = new DemoCertificateAuthority();
        CertificateSigningRequest csr = new CertificateSigningRequest();
        csr.setCommonName(commonName);
        KeyStore.PrivateKeyEntry pkEntry = demoCa.sign(csr);
        KeyStoreFile ksFile = getKeyStoreFile();
        ksFile.setEntry(commonName, pkEntry);
        ksFile.save();

    }

    KeyStoreFile getKeyStoreFile() {
        return KeyStoreFile.openFile(System.getProperty("javax.net.ssl.keyStore"),
                System.getProperty("javax.net.ssl.keyStorePassword").toCharArray());
    }

    public void init() {
        try {
            try {
                mBeanServer.registerMBean(this, objectName);
            } catch (InstanceAlreadyExistsException iaee) {
                LOGGER.info("Re-registering Certificate Generator MBean");
                mBeanServer.unregisterMBean(objectName);
                mBeanServer.registerMBean(this, objectName);
            }
        } catch (Exception e) {
            LOGGER.warn("Could not register MBean.", e);
        }
    }

    public void destroy() {
        try {
            if (objectName != null && mBeanServer != null) {
                mBeanServer.unregisterMBean(objectName);
            }
        } catch (Exception e) {
            LOGGER.warn("Exception unregistering MBean: ", e);
        }
    }
}
