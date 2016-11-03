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
 **/
package org.codice.ddf.ui.searchui.query.endpoint;

import java.util.concurrent.ExecutorService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.codice.ddf.persistence.PersistentStore;
import org.codice.ddf.ui.searchui.query.controller.ActivityController;
import org.codice.ddf.ui.searchui.query.controller.NotificationController;
import org.codice.ddf.ui.searchui.query.controller.SearchController;
import org.codice.ddf.ui.searchui.query.service.SearchService;
import org.codice.ddf.ui.searchui.query.service.UserService;
import org.codice.ddf.ui.searchui.query.service.WorkspaceService;
import org.cometd.annotation.ServerAnnotationProcessor;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.server.CometDServlet;
import org.cometd.server.DefaultSecurityPolicy;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.action.ActionRegistry;
import ddf.catalog.CatalogFramework;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;

/**
 * The CometdEndpoint binds the SearchService and the CometdServlet together.
 * This is where the asynchronous endpoint is initially started.
 */
public class CometdEndpoint extends CometDServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(CometdEndpoint.class);

    private final transient FilterBuilder filterBuilder;

    private transient BayeuxServer bayeuxServer;

    transient NotificationController notificationController;

    private transient ActivityController activityController;

    private transient SearchService searchService;

    private transient PersistentStore persistentStore;

    private transient SearchController searchController;

    /**
     * Create a new CometdEndpoint
     *
     * @param framework     - CatalogFramework to use for query requests
     * @param filterBuilder - FilterBuilder for the SearchService to use
     */
    public CometdEndpoint(CatalogFramework framework, FilterBuilder filterBuilder,
            FilterAdapter filterAdapter, PersistentStore persistentStore,
            BundleContext bundleContext, EventAdmin eventAdmin, ActionRegistry actionRegistry,
            ExecutorService executorService) {
        LOGGER.trace("Constructing Cometd Endpoint");
        this.filterBuilder = filterBuilder;
        this.searchController = new SearchController(framework,
                actionRegistry,
                filterAdapter,
                executorService);
        this.persistentStore = persistentStore;
        this.notificationController = new NotificationController(persistentStore,
                bundleContext,
                eventAdmin);
        this.activityController = new ActivityController(persistentStore,
                bundleContext,
                eventAdmin);

        LOGGER.trace("Exiting CometdEndpoint constructor. ");
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        bayeuxServer = (BayeuxServer) config.getServletContext()
                .getAttribute(BayeuxServer.ATTRIBUTE);

        if (bayeuxServer != null) {
            ServerAnnotationProcessor cometdAnnotationProcessor = new ServerAnnotationProcessor(
                    bayeuxServer);

            //TODO: don't do this, we need some sort of policy
            bayeuxServer.setSecurityPolicy(new DefaultSecurityPolicy() {

                @Override
                public boolean canCreate(BayeuxServer server, ServerSession session,
                        String channelId, ServerMessage message) {
                    return true;
                }

                @Override
                public boolean canHandshake(BayeuxServer server, ServerSession session,
                        ServerMessage message) {

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("canHandshake ServerSession: {}   canHandshake ServerMessage: {}", session, message);
                    }

                    notificationController.registerUserSession(session, message);
                    activityController.registerUserSession(session, message);
                    return true;
                }

                @Override
                public boolean canPublish(BayeuxServer server, ServerSession session,
                        ServerChannel channel, ServerMessage message) {
                    return true;
                }

                @Override
                public boolean canSubscribe(BayeuxServer server, ServerSession session,
                        ServerChannel channel, ServerMessage message) {
                    return true;
                }

            });

            searchController.setBayeuxServer(bayeuxServer);
            searchService = new SearchService(filterBuilder, searchController);
            UserService userService = new UserService(persistentStore);
            WorkspaceService workspaceService = new WorkspaceService(persistentStore);
            cometdAnnotationProcessor.process(userService);
            cometdAnnotationProcessor.process(workspaceService);
            cometdAnnotationProcessor.process(searchService);
            cometdAnnotationProcessor.process(notificationController);
            cometdAnnotationProcessor.process(activityController);
        }
    }

    public void destroy() {
        searchController.destroy();
    }

    public BayeuxServer getBayeuxServer() {
        return bayeuxServer;
    }

    public SearchService getSearchService() {
        return searchService;
    }

    public void setCacheDisabled(Boolean cacheDisabled) {
        this.searchController.setCacheDisabled(cacheDisabled);
    }

    public void setNormalizationDisabled(Boolean normalizationDisabled) {
        this.searchController.setNormalizationDisabled(normalizationDisabled);
    }
}