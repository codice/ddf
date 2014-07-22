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

import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.codice.ddf.ui.searchui.query.model.Search;
import org.cometd.annotation.Listener;
import org.cometd.annotation.Service;
import org.cometd.annotation.Session;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.common.JSONContext;
import org.cometd.common.JacksonJSONContextClient;
import org.cometd.server.JacksonJSONContextServer;
import org.cometd.server.ServerMessageImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.SubjectUtils;

/**
 * Service for managing workspaces.
 */
@Service
public class WorkspaceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceService.class);

    @Session
    private ServerSession serverSession;
    
    private PersistentStore persistentStore;
    
    
    public WorkspaceService(PersistentStore persistentStore) {
        this.persistentStore = persistentStore;
    }

    @SuppressWarnings("unchecked")
    @Listener("/service/workspaces")
    public void getWorkspaces(final ServerSession remote, Message message) {
        ServerMessage.Mutable reply = new ServerMessageImpl();
        Map<String, Object> data = message.getDataAsMap();
        String username = "";
        try{
            Subject subject = SecurityUtils.getSubject();
            username = SubjectUtils.getName(subject);
        } catch (Exception e) {
            LOGGER.debug("Unable to retrieve user from request.", e);
        }

        // Only persist/retrieve workspaces if this is a logged in user.
        // No workspaces persisted for an anonymous user (whose username="")
        if (StringUtils.isNotBlank(username)) {
            if (data == null || data.isEmpty() || data.get("workspaces") == null) {
                List<Map<String, Object>> workspacesList = new ArrayList<Map<String, Object>>();
                try {
                    workspacesList = persistentStore.get(PersistentStore.WORKSPACE_TYPE, 
                            "user = '" + username + "'");
                    if (workspacesList.size() == 1) {
                        // Convert workspace's JSON representation back to nested maps of Map<String, Object>
                        Map<String, Object> workspaces = (Map<String, Object>) workspacesList.get(0);
                        JSONContext.Client jsonContext = new JacksonJSONContextClient();
                        String json = (String) workspaces.get("workspaces_json_txt");
                        LOGGER.debug("workspaces extracted JSON text:\n {}", json);
                        Map<String, Object> workspacesMap;
                        try {
                            workspacesMap = jsonContext.getParser().parse(new StringReader(json), Map.class);
                            reply.putAll(workspacesMap);
                        } catch (ParseException e) {
                            LOGGER.info("ParseException while trying to convert persisted workspaces's for user {} from JSON", username, e);
                        }
                    }
                } catch (PersistenceException e) {
                    LOGGER.info("PersistenceException while trying to retrieve persisted workspaces for user {}", username, e);
                }
                reply.put(Search.SUCCESSFUL, true);
                remote.deliver(serverSession, "/service/workspaces", reply, null);
            } else {
                LOGGER.debug("Persisting workspaces for username = {}", username);
                // Use JSON serializer so that only "data" component is serialized, not entire Message
                JSONContext.Server jsonContext = new JacksonJSONContextServer();
                String json = jsonContext.getGenerator().generate(data);
                LOGGER.debug("workspaces JSON text:\n {}", json);
                PersistentItem item = new PersistentItem();
                item.addIdProperty(username);
                item.addProperty("user", username);
                item.addProperty("workspaces_json", json);
                try {
                    persistentStore.add(PersistentStore.WORKSPACE_TYPE, item);
                } catch (PersistenceException e) {
                    LOGGER.info("PersistenceException while trying to persist workspaces for user {}", username, e);
                }
                reply.put(Search.SUCCESSFUL, true);
                remote.deliver(serverSession, "/service/workspaces", reply, null);
            }
        }
    }
}
