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
import org.apache.ftpserver.impl.DefaultFtpServer;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.core.ftp.ftplets.FtpRequestHandler;

/**
 * Registers the {@link FtpRequestHandler} and starts the FTP server for the FTP Endpoint.
 */
public class FtpServerStarter {
    private static final Logger LOGGER = LoggerFactory.getLogger(FtpServerStarter.class);

    private static final String DEFAULT_LISTENER = "default";

    private int port = 8021;

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
        listenerFactory.setPort(port);

        serverFactory.addListener(DEFAULT_LISTENER, listenerFactory.createListener());

        Map<String, Ftplet> ftplets = new HashMap<>();
        ftplets.put(FtpRequestHandler.class.getName(), ftplet);
        serverFactory.setFtplets(ftplets);
        serverFactory.setUserManager(userManager);

        server = serverFactory.createServer();
        startServer();
    }

    public void destroy() {
        if (!isStopped()) {
            server.stop();
            LOGGER.debug("FTP server stopped");
        }
    }

    public void setPort(int port) {
        if (this.port != port) {
            this.port = port;
            restartDefaultListener();
        }
    }

    public int getPort() {
        return this.port;
    }

    private void restartDefaultListener() {
        LOGGER.debug("Restarting FTP server with new port {}.", port);

        suspendServer();
        destroyDefaultListener();
        addDefaultListener();
        startServer();
    }

    private void startServer() {
        if (isStopped() || isSuspended()) {
            try {
                server.start();
                LOGGER.debug("FTP server started on port {}", port);
            } catch (Exception e) {
                LOGGER.error("Failed to start FTP server", e);
            }
        }
    }

    private void destroyDefaultListener() {
        Listener defaultListener = getDefaultListener();
        if (!defaultListener.isStopped()) {
            defaultListener.stop();
        }
        ((DefaultFtpServer) server).getListeners()
                .clear();
    }

    private void addDefaultListener() {
        listenerFactory.setPort(port);
        ((DefaultFtpServer) server).getListeners()
                .put(DEFAULT_LISTENER, listenerFactory.createListener());
    }

    private void suspendServer() {
        if (!isSuspended()) {
            server.suspend();
        }
    }

    private boolean isStopped() {
        return server.isStopped();
    }

    private boolean isSuspended() {
        return server.isSuspended();
    }

    protected Listener getDefaultListener() {
        return ((DefaultFtpServer) server).getListener(DEFAULT_LISTENER);
    }

    private void notNull(Object object, String name) {
        if (object == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
    }
}
