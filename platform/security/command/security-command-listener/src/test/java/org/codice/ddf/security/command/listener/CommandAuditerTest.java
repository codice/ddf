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
 */
package org.codice.ddf.security.command.listener;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.osgi.service.event.Event;

public class CommandAuditerTest {

    @Test
    public void testHandleEvent() {
        final String[] passedCommand = new String[1];
        CommandAuditer commandAuditer = new CommandAuditer() {
            void writeAudit(String command) {
                passedCommand[0] = command;
            }
        };
        Map<String, Object> props = new HashMap<>();
        props.put("command", "command I'm trying to run");
        Event event = new Event("org/apache/karaf/shell/console/EXECUTING", props);
        commandAuditer.handleEvent(event);
        assertThat(passedCommand[0], is("command I'm trying to run"));
    }

    @Test
    public void testHandleBadEvent() {
        final String[] passedCommand = new String[1];
        CommandAuditer commandAuditer = new CommandAuditer() {
            void writeAudit(String command) {
                passedCommand[0] = command;
            }
        };
        Map<String, Object> props = new HashMap<>();
        props.put("command", "command I'm trying to run");
        Event event = new Event("not/the/event/you/are/looking/for", props);
        commandAuditer.handleEvent(event);
        assertNull(passedCommand[0]);
    }

    @Test
    public void testHandleEventEmptyProps() {
        final String[] passedCommand = new String[1];
        CommandAuditer commandAuditer = new CommandAuditer() {
            void writeAudit(String command) {
                passedCommand[0] = command;
            }
        };
        Event event = new Event("org/apache/karaf/shell/console/EXECUTING", new HashMap<>());
        commandAuditer.handleEvent(event);
        assertNull(passedCommand[0]);
    }
}
