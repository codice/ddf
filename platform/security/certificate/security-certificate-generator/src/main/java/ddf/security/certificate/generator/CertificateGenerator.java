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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import org.apache.shiro.util.CollectionUtils;
//import org.codice.ddf.ui.admin.api.ConfigurationAdminExt;
//import org.osgi.framework.Bundle;
//import org.osgi.framework.BundleContext;
//import org.osgi.framework.FrameworkUtil;
//import org.osgi.framework.InvalidSyntaxException;
//import org.osgi.framework.ServiceReference;
//import org.osgi.service.cm.Configuration;
//import org.osgi.service.cm.ConfigurationAdmin;
//import org.osgi.service.metatype.ObjectClassDefinition;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import ddf.catalog.service.ConfiguredService;
//import ddf.catalog.source.ConnectedSource;
//import ddf.catalog.source.FederatedSource;
//import ddf.catalog.source.Source;

public class CertificateGenerator implements CertificateGeneratorMBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateGenerator.class);

    private static final String SERVICE_NAME = ":service=demo-certificate-generation-service";

    private final ObjectName objectName;

    private final MBeanServer mBeanServer;

    private PkiTools pkiTools = new PkiTools();

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

    public boolean installCertificate(String commonName) {
        CertificateAuthority demoCa = new DemoCertificateAuthority();
        CertificateSigningRequest csr = new CertificateSigningRequest();
        csr.setCommonName(commonName);
        KeyStore.PrivateKeyEntry pkEntry = demoCa.sign(csr);
        KeyStoreFile ksFile = null;
        try {
            ksFile = KeyStoreFile.openFile(System.getProperty("javax.net.ssl.keyStore"),
                    System.getProperty("javax.net.ssl.keyStorePassword").toCharArray());
            //Ask Eric or someone about what to do here. Hide the checked errors behind unchecked errors?
        } catch (IOException e) {
            return false;
        } catch (GeneralSecurityException e) {
            return false;
        }
        ksFile.setEntry(commonName, pkEntry);
        ksFile.save();
        return true;
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
