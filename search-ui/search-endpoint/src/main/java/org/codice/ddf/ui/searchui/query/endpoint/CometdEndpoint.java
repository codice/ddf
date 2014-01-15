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

import javax.servlet.ServletException;

import org.codice.ddf.ui.searchui.query.controller.SearchController;
import org.codice.ddf.ui.searchui.query.service.SearchService;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.server.CometdServlet;
import org.cometd.server.DefaultSecurityPolicy;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.CatalogFramework;
import ddf.catalog.filter.FilterBuilder;

/**
 * Created by tustisos on 12/10/13.
 */
public class CometdEndpoint {

    private static final XLogger LOGGER = new XLogger(
            LoggerFactory.getLogger(CometdEndpoint.class));

    private final CometdServlet cometdServlet;

    private final FilterBuilder filterBuilder;

    private SearchController searchController;

    BayeuxServer bayeuxServer;

    SearchService searchService;

    public CometdEndpoint(CometdServlet cometdServlet, CatalogFramework framework, FilterBuilder filterBuilder) {
        this.cometdServlet = cometdServlet;
        this.filterBuilder = filterBuilder;
        this.searchController = new SearchController(framework);
    }

    public void init() throws ServletException {
        bayeuxServer = (BayeuxServer) cometdServlet.getServletContext().getAttribute(
                BayeuxServer.ATTRIBUTE);
        if (bayeuxServer != null) {

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
            searchService = new SearchService(bayeuxServer, "SearchService", filterBuilder, searchController);
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
