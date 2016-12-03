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

package org.codice.ui.admin.wizard.api;

import static org.codice.ui.admin.wizard.api.ConfigurationMessage.MessageType.SUCCESS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestReport {

    List<ConfigurationMessage> messages;

    public TestReport() {
        this(new ArrayList<>());
    }

    public TestReport(ConfigurationMessage... messages) {
        this(new ArrayList<>());
        Arrays.stream(messages)
                .forEach(msg -> this.messages.add(msg));
    }

    public TestReport(List<ConfigurationMessage> messages) {
        this.messages = messages;
    }

    public List<ConfigurationMessage> getMessages() {
        return messages;
    }

    public TestReport addMessage(ConfigurationMessage result) {
        this.messages.add(result);
        return this;
    }

    public void addMessages(List<ConfigurationMessage> messages) {
        this.messages.addAll(messages);
    }

    public boolean containsUnsuccessfulMessages() {
        return messages.stream()
                .filter(msg -> msg.getType() != SUCCESS)
                .findFirst()
                .isPresent();
    }
}
