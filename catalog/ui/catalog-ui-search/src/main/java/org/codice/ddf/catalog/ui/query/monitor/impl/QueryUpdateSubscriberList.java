/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.ui.query.monitor.impl;

import static org.apache.commons.lang3.Validate.notNull;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.codice.ddf.catalog.ui.query.monitor.api.QueryUpdateSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Execute a list of QueryUpdateSubscriber objects. Catches runtime exceptions thrown by the
 * subscriber and logs it as a warning, then continues executing the remaining subscribers.
 */
public class QueryUpdateSubscriberList implements QueryUpdateSubscriber {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryUpdateSubscriberList.class);

    private final List<QueryUpdateSubscriber> queryUpdateSubscriberList;

    /**
     * @param queryUpdateSubscriberList must be non-null
     */
    public QueryUpdateSubscriberList(List<QueryUpdateSubscriber> queryUpdateSubscriberList) {
        notNull(queryUpdateSubscriberList, "queryUpdateSubscriberList must be non-null");
        this.queryUpdateSubscriberList = queryUpdateSubscriberList;
    }

    @Override
    public void notify(Map<String, Pair<WorkspaceMetacardImpl, Long>> workspaceMetacardMap) {
        notNull(workspaceMetacardMap, "workspaceMetacardMap must be non-null");
        queryUpdateSubscriberList.forEach(subscriber -> {
            try {
                subscriber.notify(workspaceMetacardMap);
            } catch (RuntimeException e) {
                LOGGER.warn("QueryUpdateSubscriber failed to run: subscriber={}", subscriber, e);
            }
        });
    }

    @Override
    public String toString() {
        return "QueryUpdateSubscriberList{" +
                "list=" + queryUpdateSubscriberList +
                '}';
    }
}
