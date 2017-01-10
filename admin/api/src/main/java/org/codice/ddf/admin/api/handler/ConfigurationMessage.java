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

package org.codice.ddf.admin.api.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigurationMessage {

    private MessageType type;
    private String subType;
    private String message;

    private String configId;
    private List<Exception> exceptions;

    public ConfigurationMessage(MessageType type, Exception... exceptions) {
        this.type = type;
        this.exceptions = new ArrayList<>();
        this.exceptions.addAll(Arrays.asList(exceptions));
    }

    // TODO: tbatie - 1/3/17 - Pet peeve, let's change the message signature around here, messageType should go first
    public ConfigurationMessage(String message, MessageType type, Exception... exceptions) {
        this.message = message;
        this.type = type;
        this.exceptions = new ArrayList<>();
        this.exceptions.addAll(Arrays.asList(exceptions));
    }

    public ConfigurationMessage(MessageType type, String subType, String message) {
        this.message = message;
        this.type = type;
        this.subType = subType;
    }

    // TODO: tbatie - 1/3/17 - Why do we have methods that just wrap the constructors
    public static ConfigurationMessage buildMessage(MessageType type) {
        return new ConfigurationMessage(type);
    }

    public static ConfigurationMessage buildMessage(MessageType type, String message) {
        return new ConfigurationMessage(message, type);
    }

    public void addException(Exception e) {
        this.exceptions.add(e);
    }

    public String subType() {
        return subType;
    }

    public String configId() {
        return configId;
    }

    public String getMessage() {
        return message;
    }

    public MessageType getType() {
        return type;
    }

    //
    // Builder Methods
    //
    public ConfigurationMessage configId(String configId) {
        this.configId = configId;
        return this;
    }

    public ConfigurationMessage exception(Exception e) {
        exceptions.add(e);
        return this;
    }

    public enum MessageType {
        SUCCESS, WARNING, FAILURE, NO_TEST_FOUND, NO_PROBE_FOUND, NO_PERSIST_FOUND, REQUIRED_FIELDS
    }
}
