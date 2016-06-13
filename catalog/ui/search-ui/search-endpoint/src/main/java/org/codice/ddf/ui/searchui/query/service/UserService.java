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
package org.codice.ddf.ui.searchui.query.service;

import java.io.StringReader;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.inject.Inject;

import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.codice.ddf.ui.searchui.query.model.Search;
import org.cometd.annotation.Listener;
import org.cometd.annotation.Service;
import org.cometd.annotation.Session;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.common.JSONContext;
import org.cometd.common.Jackson1JSONContextClient;
import org.cometd.server.Jackson1JSONContextServer;
import org.cometd.server.ServerMessageImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.SubjectUtils;

/**
 * Service to retrieve user related information.
 */
@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    public static final String ROLES_CLAIM_URI =
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";

    private PersistentStore persistentStore;

    @Inject
    private BayeuxServer bayeux;

    @Session
    private ServerSession serverSession;

    public UserService(PersistentStore persistentStore) {
        this.persistentStore = persistentStore;
    }

    @Listener("/service/user")
    public void getUser(final ServerSession remote, Message message) {
        ServerMessage.Mutable reply = new ServerMessageImpl();
        Map<String, Object> data = message.getDataAsMap();
        Subject subject = (Subject) bayeux.getContext()
                .getRequestAttribute(SecurityConstants.SECURITY_SUBJECT);

        if (subject != null) {
            if (data == null || data.isEmpty()) {
                Map<String, Object> userMap = new HashMap<>();
                String username = SubjectUtils.getName(subject);
                userMap.put("username", username);
                userMap.put("isGuest", String.valueOf(subject.isGuest()));
                userMap.put("roles",
                        new TreeSet<>(SubjectUtils.getAttribute(subject, ROLES_CLAIM_URI)));
                List<Map<String, Object>> preferencesList;
                try {
                    preferencesList = persistentStore.get(PersistentStore.PREFERENCES_TYPE,
                            "user = '" + username + "'");
                    if (preferencesList.size() == 1) {
                        Map<String, Object> preferences = preferencesList.get(0);
                        JSONContext.Client jsonContext = new Jackson1JSONContextClient();
                        String json = (String) preferences.get("preferences_json_txt");
                        LOGGER.debug("preferences extracted JSON text:\n {}", json);
                        Map preferencesMap;
                        try {
                            preferencesMap = jsonContext.getParser()
                                    .parse(new StringReader(json), Map.class);
                            userMap.put("preferences", preferencesMap);
                        } catch (ParseException e) {
                            LOGGER.info(
                                    "ParseException while trying to convert persisted preferences for user {} from JSON",
                                    username,
                                    e);
                        }
                    }
                } catch (PersistenceException e) {
                    LOGGER.info(
                            "PersistenceException while trying to retrieve persisted preferences for user {}",
                            username,
                            e);
                }
                reply.put("user", userMap);
                reply.put(Search.SUCCESSFUL, true);
                remote.deliver(serverSession, "/service/user", reply);
            } else {
                JSONContext.Server jsonContext = new Jackson1JSONContextServer();
                String json = jsonContext.getGenerator()
                        .generate(data);
                LOGGER.debug("preferences JSON text:\n {}", json);
                String username = SubjectUtils.getName(subject);
                PersistentItem item = new PersistentItem();
                item.addIdProperty(username);
                item.addProperty("user", username);
                item.addProperty("preferences_json", json);
                try {
                    persistentStore.add(PersistentStore.PREFERENCES_TYPE, item);
                } catch (PersistenceException e) {
                    LOGGER.info(
                            "PersistenceException while trying to persist preferences for user {}",
                            username,
                            e);
                }
            }
        } else {
            reply.put(Search.SUCCESSFUL, false);
            remote.deliver(serverSession, "/service/user", reply);
        }
    }
}
