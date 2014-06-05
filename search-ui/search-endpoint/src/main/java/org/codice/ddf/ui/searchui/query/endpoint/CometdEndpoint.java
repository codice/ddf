/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package org.codice.ddf.ui.searchui.query.endpoint;

import ddf.catalog.CatalogFramework;
import ddf.catalog.filter.FilterBuilder;
import org.codice.ddf.notifications.store.NotificationStore;
import org.codice.ddf.ui.searchui.query.controller.ActivityController;
import org.codice.ddf.ui.searchui.query.controller.NotificationController;
import org.codice.ddf.ui.searchui.query.controller.SearchController;
import org.codice.ddf.ui.searchui.query.service.SearchService;
import org.cometd.annotation.ServerAnnotationProcessor;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.server.CometdServlet;
import org.cometd.server.DefaultSecurityPolicy;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;

/**
 * The CometdEndpoint binds the SearchService and the CometdServlet together. 
 * This is where the asynchronous endpoint is initially started.
 */
public class CometdEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(CometdEndpoint.class);

    private final CometdServlet cometdServlet;

    private final FilterBuilder filterBuilder;

    private SearchController searchController;
    
    private ServerAnnotationProcessor cometdAnnotationProcessor;

    BayeuxServer bayeuxServer;
    NotificationController notificationController;
    ActivityController activityController;
    SearchService searchService;
    

    /**
     * Create a new CometdEndpoint
     * 
     * @param cometdServlet
     *            - CometdServlet to bind to the SearchService. This field must not be null.
     * @param framework
     *            - CatalogFramework to use for query requests
     * @param filterBuilder
     *            - FilterBuilder for the SearchService to use
     */
    public CometdEndpoint(CometdServlet cometdServlet, CatalogFramework framework, 
            FilterBuilder filterBuilder, NotificationStore notificationStore, BundleContext bundleContext) {
        this.cometdServlet = cometdServlet;
        this.filterBuilder = filterBuilder;
        this.searchController = new SearchController(framework);
        this.notificationController = new NotificationController(notificationStore, bundleContext);
        this.activityController = new ActivityController(notificationStore, bundleContext);
    }

    public void init() throws ServletException {        
        bayeuxServer = (BayeuxServer) cometdServlet.getServletContext().getAttribute(
                BayeuxServer.ATTRIBUTE);
        
        if (bayeuxServer != null) {
            cometdAnnotationProcessor = new ServerAnnotationProcessor(bayeuxServer);

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
                        LOGGER.debug("canHandshake ServerSession: " + session 
                                + "\ncanHandshake ServerMessage: " + message);
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

}
