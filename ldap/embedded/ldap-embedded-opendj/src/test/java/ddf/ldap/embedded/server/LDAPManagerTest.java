/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.ldap.embedded.server;


import org.apache.camel.test.AvailablePortFinder;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


public class LDAPManagerTest
{

    private static final String TMP_FOLDER_NAME = "test_folder";

    private static Logger logger = LoggerFactory.getLogger(LDAPManagerTest.class);
    static int adminPort;
    static int ldapPort;
    static int ldapsPort;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void getPorts()
    {
        adminPort = AvailablePortFinder.getNextAvailable();
        logger.info("Using admin port: " + adminPort);
        ldapPort = AvailablePortFinder.getNextAvailable();
        logger.info("Using ldap port: " + adminPort);
        ldapsPort = AvailablePortFinder.getNextAvailable();
        logger.info("Using ldaps port: " + adminPort);
    }

    //tests temporarily ignored to figure out a way to resolve the keystore issue
    @Ignore
    @Test
    public void TestStartServer()
    {
        logger.info("Testing starting and stopping server.");
        LDAPBundleContext mockContext = new LDAPBundleContext(folder.newFolder(TMP_FOLDER_NAME).getAbsolutePath());
        LDAPManager manager = new LDAPManager(mockContext);
        manager.setAdminPort(adminPort);
        manager.setLDAPPort(ldapPort);
        manager.setLDAPSPort(ldapsPort);
        assertNotNull(manager);
        try
        {
            logger.info("Starting Server.");
            manager.startServer();
            logger.info("Successfully started server, now stopping.");
            manager.stopServer();
        }
        catch (LDAPException le)
        {
            le.printStackTrace();
            fail(le.getMessage());
        }
        finally
        {
            manager.stopServer();
        }

    }

    @Ignore
    @Test
    public void TestStopStopped()
    {
        logger.info("Testing case to stop an already stopped server.");
        LDAPBundleContext mockContext = new LDAPBundleContext(folder.newFolder(TMP_FOLDER_NAME).getAbsolutePath());
        LDAPManager manager = new LDAPManager(mockContext);
        manager.setAdminPort(adminPort);
        manager.setLDAPPort(ldapPort);
        manager.setLDAPSPort(ldapsPort);
        assertNotNull(manager);
        try
        {
            manager.stopServer();
        }
        catch (Exception le)
        {
            fail("Server should not throw exception when trying to stop an already stopped server.");
        }
    }

    @Ignore
    @Test
    public void TestNoDataFile()
    {
        logger.info("Testing case where no location is given to store data.");
        LDAPBundleContext mockContext = new LDAPBundleContext(null);
        LDAPManager manager = new LDAPManager(mockContext);
        manager.setAdminPort(adminPort);
        manager.setLDAPPort(ldapPort);
        manager.setLDAPSPort(ldapsPort);
        assertNotNull(manager);
        try
        {
            logger.info("Starting Server.");
            manager.startServer();
            logger.info("Successfully started server, now stopping.");
            manager.stopServer();
            fail("Sever should not successfully start with null data file.");
        }
        catch (LDAPException le)
        {
            logger.info("Server successfully failed startup.");
        }
    }
}
