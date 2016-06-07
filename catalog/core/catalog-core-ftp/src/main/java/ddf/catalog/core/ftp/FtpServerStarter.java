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

import java.util.HashMap;
import java.util.Map;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.core.ftp.ftplets.FtpRequestHandler;

/**
 * Registers the {@link FtpRequestHandler} and starts the FTP server for the FTP Endpoint.
 */
public class FtpServerStarter {
    private static final Logger LOGGER = LoggerFactory.getLogger(FtpServerStarter.class);

    private static final int PORT = 8021;

    private static final String DEFAULT_LISTENER = "default";

    private static FtpServer server;

    private static FtpServerFactory serverFactory;

    private static UserManager userManager;

    private static ListenerFactory listenerFactory;

    private Ftplet ftplet;

    public FtpServerStarter(Ftplet ftplet, FtpServerFactory serverFactory,
            ListenerFactory listenerFactory, UserManager userManager) {
        notNull(ftplet, "ftplet");
        notNull(serverFactory, "serverFactory");
        notNull(listenerFactory, "listenerFactory");
        notNull(userManager, "userManager");

        this.ftplet = ftplet;
        this.serverFactory = serverFactory;
        this.listenerFactory = listenerFactory;
        this.userManager = userManager;
    }

    public void init() {
        listenerFactory.setPort(PORT);

        serverFactory.addListener(DEFAULT_LISTENER, listenerFactory.createListener());

        Map<String, Ftplet> ftplets = new HashMap<>();
        ftplets.put(FtpRequestHandler.class.getName(), ftplet);
        serverFactory.setFtplets(ftplets);
        serverFactory.setUserManager(userManager);

        server = serverFactory.createServer();
        try {
            server.start();
            LOGGER.debug("FTP server started on port {}", PORT);
        } catch (Exception e) {
            LOGGER.error("Failed to start FTP server", e);
        }
    }

    public void destroy() {
        if (!server.isStopped()) {
            server.stop();
            LOGGER.debug("FTP server on port {} was stopped", PORT);
        }
    }

    private void notNull(Object object, String name) {
        if (object == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
    }
}
