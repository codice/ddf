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
package ddf.catalog.core.ftp;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;

import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.impl.DefaultFtpServer;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;
import org.junit.Before;
import org.junit.Test;

import ddf.catalog.core.ftp.ftplets.FtpRequestHandler;

public class FtpServerStarterTest {

    private static final String DEFAULT_LISTENER = "default";

    private FtpServerStarter ftpServerStarter;

    private FtpServerFactory ftpServerFactory;

    private Listener defaultListener;

    private DefaultFtpServer server;

    HashSet<FtpIoSession> sessions;

    @Before
    public void setup() {
        Ftplet ftplet = mock(FtpRequestHandler.class);
        ftpServerFactory = mock(FtpServerFactory.class);
        ListenerFactory listenerFactory = mock(ListenerFactory.class);
        UserManager userManager = mock(UserManagerImpl.class);

        defaultListener = mock(Listener.class);
        server = mock(DefaultFtpServer.class);

        sessions = new HashSet<>();

        ftpServerStarter = new FtpServerStarter(ftplet,
                ftpServerFactory,
                listenerFactory,
                userManager);
    }

    @Test
    public void portChangeTest() {
        when(ftpServerFactory.createServer()).thenReturn(server);
        when(server.getListener(DEFAULT_LISTENER)).thenReturn(defaultListener);
        when(defaultListener.getActiveSessions()).thenReturn(sessions);

        ftpServerStarter.init();

        assertEquals(8021, ftpServerStarter.getPort());

        when(server.isSuspended()).thenReturn(true);

        ftpServerStarter.setPort(8022);
        assertEquals(8022, ftpServerStarter.getPort());
    }

}
