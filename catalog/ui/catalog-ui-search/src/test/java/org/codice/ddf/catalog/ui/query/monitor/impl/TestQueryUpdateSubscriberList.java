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

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.codice.ddf.catalog.ui.query.monitor.api.QueryUpdateSubscriber;
import org.junit.Test;
import org.mockito.Mockito;

public class TestQueryUpdateSubscriberList {

    @Test
    public void testNotify() {
        QueryUpdateSubscriber childSubscriber = mock(QueryUpdateSubscriber.class);
        QueryUpdateSubscriberList queryUpdateSubscriberList = new QueryUpdateSubscriberList(
                Collections.singletonList(childSubscriber));
        Map<String, Pair<WorkspaceMetacardImpl, Long>> workspaceMetacardMap =
                Collections.emptyMap();
        queryUpdateSubscriberList.notify(workspaceMetacardMap);
        verify(childSubscriber).notify(workspaceMetacardMap);
    }

    /**
     * Test that if the first subscriber throws an exception that the second subscriber is
     * still called.
     */
    @Test
    public void testExceptions() {
        Map<String, Pair<WorkspaceMetacardImpl, Long>> workspaceMetacardMap =
                Collections.emptyMap();

        QueryUpdateSubscriber childSubscriber1 = mock(QueryUpdateSubscriber.class);
        QueryUpdateSubscriber childSubscriber2 = mock(QueryUpdateSubscriber.class);

        Mockito.doThrow(RuntimeException.class)
                .when(childSubscriber1)
                .notify(workspaceMetacardMap);

        QueryUpdateSubscriberList queryUpdateSubscriberList =
                new QueryUpdateSubscriberList(Arrays.asList(childSubscriber1, childSubscriber2));
        queryUpdateSubscriberList.notify(workspaceMetacardMap);

        verify(childSubscriber2).notify(workspaceMetacardMap);
    }

    @Test
    public void testToString() {
        assertThat(new QueryUpdateSubscriberList(Collections.emptyList()).toString(),
                notNullValue());
    }

}
