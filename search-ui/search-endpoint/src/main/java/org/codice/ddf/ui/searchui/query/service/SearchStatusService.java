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

import java.util.HashMap;
import java.util.Map;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.server.AbstractService;

/**
 * Created by tustisos on 12/10/13.
 */
public class SearchStatusService extends AbstractService {
    public SearchStatusService(BayeuxServer bayeux, String name) {
        super(bayeux, name);
        addService("/service/status", "processStatus");
    }

    public void processStatus(final ServerSession remote, Message message) {
        Map<String, Object> input = message.getDataAsMap();

        Map<String, Object> output = new HashMap<String, Object>();

        output.put("test", "Hello");

        remote.deliver(getServerSession(), "/status", output, null);
    }
}
