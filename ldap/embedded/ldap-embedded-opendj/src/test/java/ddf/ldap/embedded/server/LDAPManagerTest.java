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


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import org.apache.camel.test.AvailablePortFinder;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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

    @Test
    public void TestStartServer()
    {
        logger.info("Testing starting and stopping server.");
        BundleContext mockContext = createMockContext(folder.newFolder(TMP_FOLDER_NAME));
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

    @Test
    public void TestStopStopped()
    {
        logger.info("Testing case to stop an already stopped server.");
        BundleContext mockContext = createMockContext(folder.newFolder(TMP_FOLDER_NAME));
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

    @Test( expected = LDAPException.class )
    public void TestNoDataFile() throws LDAPException
    {
        logger.info("Testing case where no location is given to store data.");
        BundleContext mockContext = createMockContext(null);
        LDAPManager manager = new LDAPManager(mockContext);
        manager.setAdminPort(adminPort);
        manager.setLDAPPort(ldapPort);
        manager.setLDAPSPort(ldapsPort);
        assertNotNull(manager);
        logger.info("Starting Server.");
        manager.startServer();
        logger.info("Successfully started server, now stopping.");
        manager.stopServer();
        fail("Sever should not successfully start with null data file.");
    }

    private BundleContext createMockContext( final File dataFolderPath )
    {
        Bundle mockBundle = Mockito.mock(Bundle.class);
        Mockito.when(mockBundle.findEntries(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean())).then(
            new Answer<Enumeration<URL>>()
            {

                @Override
                public Enumeration<URL> answer( InvocationOnMock invocation ) throws Throwable
                {

                    Object[] arguments = invocation.getArguments();
                    String path = arguments[0].toString();
                    String filePattern = arguments[1].toString();
                    boolean recurse = (Boolean) arguments[2];
                    final URL url = this.getClass().getResource(path);
                    File pathFile = null;
                    try
                    {
                        pathFile = new File(url.toURI());
                    }
                    catch (URISyntaxException e)
                    {
                        throw new RuntimeException("Unable to resolve file path", e);
                    }
                    final File[] files = pathFile.listFiles((FileFilter) new WildcardFileFilter(filePattern));
                    Enumeration<URL> enumer = new Enumeration<URL>()
                    {
                        int place = 0;
                        List<File> urlList = Arrays.asList(files);

                        @Override
                        public boolean hasMoreElements()
                        {
                            return place < urlList.size();
                        }

                        @Override
                        public URL nextElement()
                        {
                            File file = urlList.get(place++);
                            try
                            {
                                return file.toURL();
                            }
                            catch (MalformedURLException e)
                            {
                                throw new RuntimeException("Unable to convert to URL", e);
                            }
                        }
                    };
                    return enumer;
                }

            });
        Mockito.when(mockBundle.getResource(Mockito.anyString())).then(new Answer<URL>()
        {

            @Override
            public URL answer( InvocationOnMock invocation ) throws Throwable
            {
                return this.getClass().getResource((String) invocation.getArguments()[0]);
            }

        });
        BundleContext mockContext = Mockito.mock(BundleContext.class);
        Mockito.when(mockContext.getDataFile(Mockito.anyString())).then(new Answer<File>()
        {

            @Override
            public File answer( InvocationOnMock invocation ) throws Throwable
            {
                String filename = invocation.getArguments()[0].toString();
                if (dataFolderPath != null)
                {
                    return new File(dataFolderPath + "/" + filename);
                }
                else
                {
                    return null;
                }
            }

        });
        Mockito.when(mockContext.getBundle()).thenReturn(mockBundle);

        return mockContext;
    }
}
