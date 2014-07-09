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
package org.codice.ddf.ui.searchui.query.service;

import ddf.security.SubjectUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.codice.ddf.ui.searchui.query.model.Search;
import org.cometd.annotation.Listener;
import org.cometd.annotation.Service;
import org.cometd.annotation.Session;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.server.ServerMessageImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing workspaces.
 */
@Service
public class WorkspaceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceService.class);

    private final Map<String, Object> workspaceMap = new HashMap<String, Object>();

    @Session
    private ServerSession serverSession;

    @Listener("/service/workspaces")
    public void getWorkspaces(final ServerSession remote, Message message) {
        ServerMessage.Mutable reply = new ServerMessageImpl();
        Map<String, Object> data = message.getDataAsMap();
        String username = "guest";
        try{
            Subject subject = SecurityUtils.getSubject();
            username = SubjectUtils.getName(subject);
        } catch (Exception e) {
            LOGGER.debug("Unable to retrieve user from request.", e);
        }

        if(data == null || data.isEmpty() || data.get("workspaces") == null) {
            Map<? extends String, ?> workspaces = (Map<? extends String, ?>) workspaceMap.get(username);
            if(workspaces != null) {
                reply.putAll(workspaces);
            }
            reply.put(Search.SUCCESSFUL, true);
            remote.deliver(serverSession, "/service/workspaces", reply, null);
        } else {
            workspaceMap.put(username, data);
            reply.put(Search.SUCCESSFUL, true);
            remote.deliver(serverSession, "/service/workspaces", reply, null);
        }
    }
}
