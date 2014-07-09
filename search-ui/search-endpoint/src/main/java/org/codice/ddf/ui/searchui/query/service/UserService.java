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
 * Service to retrieve user related information.
 */
@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    @Session
    private ServerSession serverSession;

    @Listener("/service/user")
    public void getUser(final ServerSession remote, Message message) {
        ServerMessage.Mutable reply = new ServerMessageImpl();
        Subject subject = null;

        try {
            subject = SecurityUtils.getSubject();
        } catch (Exception e) {
            LOGGER.debug("Unable to retrieve user from request.", e);
        }

        if(subject != null) {
            Map<String, String> userMap = new HashMap<String, String>();
            userMap.put("username", SubjectUtils.getName(subject));
            reply.put("user", userMap);
            reply.put(Search.SUCCESSFUL, true);
            remote.deliver(serverSession, "/service/user", reply, null);
        } else {
            reply.put(Search.SUCCESSFUL, false);
            remote.deliver(serverSession, "/service/user", reply, null);
        }
    }
}
