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
package org.codice.ddf.ui.searchui.query.servlet;

import javax.servlet.ServletException;

import org.codice.ddf.ui.searchui.query.service.SearchResultService;
import org.codice.ddf.ui.searchui.query.service.SearchStatusService;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.server.DefaultSecurityPolicy;

/**
 * Created by tustisos on 12/10/13.
 */
public class CometdServlet {

    private final org.cometd.server.CometdServlet cometdServlet;

    BayeuxServer bayeuxServer;

    SearchResultService searchResultService;

    SearchStatusService searchStatusService;

    public CometdServlet(org.cometd.server.CometdServlet cometdServlet) {
        this.cometdServlet = cometdServlet;
    }

    public void init() throws ServletException {
        bayeuxServer = (BayeuxServer) cometdServlet.getServletContext().getAttribute(
                BayeuxServer.ATTRIBUTE);
        if (bayeuxServer != null) {

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
            searchResultService = new SearchResultService(bayeuxServer, "SearchResultService");
            searchStatusService = new SearchStatusService(bayeuxServer, "SearchStatusService");
        }
    }

    public void destroy() {

    }

    public BayeuxServer getBayeuxServer() {
        return bayeuxServer;
    }

    public SearchResultService getSearchResultService() {
        return searchResultService;
    }

    public SearchStatusService getSearchStatusService() {
        return searchStatusService;
    }
}
