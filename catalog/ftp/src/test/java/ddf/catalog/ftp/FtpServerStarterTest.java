/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.ftp;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.ftpserver.FtpServerConfigurationException;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpStatistics;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.impl.DefaultFtpServer;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.ClientAuth;
import org.apache.ftpserver.ssl.SslConfigurationFactory;
import org.junit.Before;
import org.junit.Test;

import ddf.catalog.ftp.ftplets.FtpRequestHandler;

public class FtpServerStarterTest {

    private static final String DEFAULT_LISTENER = "default";

    private FtpServerStarter ftpServerStarter;

    private FtpServerFactory ftpServerFactory;

    private Listener defaultListener;

    private DefaultFtpServer server;

    private ListenerFactory listenerFactory;

    private SslConfigurationFactory sslConfigurationFactory;

    HashSet<FtpIoSession> sessions;

    @Before
    public void setup() {
        Ftplet ftplet = mock(FtpRequestHandler.class);
        ftpServerFactory = mock(FtpServerFactory.class);
        listenerFactory = mock(ListenerFactory.class);
        UserManager userManager = mock(UserManagerImpl.class);
        sslConfigurationFactory = mock(SslConfigurationFactory.class);

        defaultListener = mock(Listener.class);
        server = mock(DefaultFtpServer.class, RETURNS_DEEP_STUBS);

        sessions = new HashSet<>();

        ftpServerStarter = new FtpServerStarter(ftplet,
                ftpServerFactory,
                listenerFactory,
                userManager,
                sslConfigurationFactory);
    }

    @Test
    public void updateConfigurationWithoutActiveConnectionsTest() {
        FtpStatistics stats = mock(FtpStatistics.class);

        Map<String, Object> properties = createProperties(8022, "need");

        when(ftpServerFactory.createServer()).thenReturn(server);
        when(server.getListener(DEFAULT_LISTENER)).thenReturn(defaultListener);
        when(server.getServerContext()
                .getFtpStatistics()).thenReturn(stats);
        when(stats.getCurrentConnectionNumber()).thenReturn(0);
        when(server.isSuspended()).thenReturn(true);

        ftpServerStarter.init();
        ftpServerStarter.updateConfiguration(properties);

        assertEquals(8022, ftpServerStarter.getPort());
        assertEquals(ClientAuth.NEED, ftpServerStarter.getClientAuthMode());
    }

    @Test
    public void updateConfigurationWithActiveConnectionsTest() {
        FtpStatistics stats = mock(FtpStatistics.class);

        Map<String, Object> properties = createProperties(8022, "need");

        when(ftpServerFactory.createServer()).thenReturn(server);
        when(server.getListener(DEFAULT_LISTENER)).thenReturn(defaultListener);
        when(server.getServerContext()
                .getFtpStatistics()).thenReturn(stats);
        when(stats.getCurrentConnectionNumber()).thenReturn(1);
        when(server.isSuspended()).thenReturn(true);

        ftpServerStarter.setMaxSleepTime(2);
        ftpServerStarter.setResetWaitTime(1);

        ftpServerStarter.init();
        ftpServerStarter.updateConfiguration(properties);

        assertEquals(8022, ftpServerStarter.getPort());
        assertEquals(ClientAuth.NEED, ftpServerStarter.getClientAuthMode());
    }

    @Test
    public void updateConfigurationInvalidClientAuth() {
        FtpStatistics stats = mock(FtpStatistics.class);

        Map<String, Object> properties = createProperties(8021, "bologna");

        when(ftpServerFactory.createServer()).thenReturn(server);
        when(server.getListener(DEFAULT_LISTENER)).thenReturn(defaultListener);
        when(server.getServerContext()
                .getFtpStatistics()).thenReturn(stats);
        when(stats.getCurrentConnectionNumber()).thenReturn(0);
        when(server.isSuspended()).thenReturn(true);

        ftpServerStarter.init();
        ftpServerStarter.updateConfiguration(properties);

        assertEquals(ClientAuth.WANT, ftpServerStarter.getClientAuthMode());
    }

    @Test(expected = FtpServerConfigurationException.class)
    public void sslConfigurationCreationFail() {
        when(sslConfigurationFactory.createSslConfiguration()).thenThrow(
                FtpServerConfigurationException.class);

        ftpServerStarter.init();
    }

    @Test
    public void testSetClientAuthWant() {
        ftpServerStarter.setClientAuth(ClientAuth.WANT.toString());
        assertEquals(ClientAuth.WANT, ftpServerStarter.getClientAuthMode());
    }

    private Map<String, Object> createProperties(int port, String clientAuth) {
        Map<String, Object> properties = new HashMap<>();

        properties.put("port", port);
        properties.put("clientAuth", clientAuth);

        return properties;
    }

}
