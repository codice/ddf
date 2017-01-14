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

import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigurationMessage {

    // Default error message subtypes
    public static final String NO_METHOD_FOUND = "NO_METHOD_FOUND";
    public static final String MISSING_REQUIRED_FIELD = "MISSING_REQUIRED_FIELD";
    public static final String INVALID_FIELD = "INVALID_FIELD";
    public static final String FAILED_PERSIST = "FAILED_PERSIST";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    // Default success messages subtypes
    public static final String SUCCESSFUL_PERSIST = "SUCCESSFUL_PERSIST";

    private MessageType type;
    private String subType;
    private String message;
    private String configId;
    private List<Exception> exceptions;

    public ConfigurationMessage(MessageType type, String subType, String message, String configId,
            Exception... exceptions) {
        this.type = type;
        this.subType = subType;
        this.message = message;
        this.configId = configId;
        this.exceptions = new ArrayList<>();
        if (exceptions != null) {
            this.exceptions.addAll(Arrays.asList(exceptions));
        }
    }

    public ConfigurationMessage(MessageType type, String subType, String message, String configId) {
        this(type, subType, message, configId, null);
    }

    public ConfigurationMessage(MessageType type, String subType, String message,
            Exception... exceptions) {
        this(type, subType, message, null, exceptions);
    }

    public ConfigurationMessage(MessageType type, String subType, String message) {
        this(type, subType, message, null, null);
    }

    public enum MessageType {
        SUCCESS, WARNING, FAILURE
    }

    public static ConfigurationMessage buildMessage(MessageType type, String subtype, String message) {
        return new ConfigurationMessage(type, subtype, message);
    }

    // Getters
    public MessageType type() {
        return type;
    }

    public String subtype() {
        return subType;
    }

    public String message() {
        return message;
    }

    public String configId() {
        return configId;
    }

    public List<Exception> exceptions() {
        return exceptions;
    }

    // Setters
    public ConfigurationMessage configId(String configId) {
        this.configId = configId;
        return this;
    }

    //Builders
    public static ConfigurationMessage createInvalidFieldMsg(String description, String configId) {
        return new ConfigurationMessage(FAILURE, INVALID_FIELD, description, configId);
    }

    public static ConfigurationMessage createMissingRequiredFieldMsg(String configId) {
        return new ConfigurationMessage(FAILURE, MISSING_REQUIRED_FIELD, "Missing required field: " + configId, configId);
    }

    public static ConfigurationMessage createInternalErrorMsg(String description, Exception... e) {
        return new ConfigurationMessage(FAILURE, INTERNAL_ERROR, description, e);
    }

    public static ConfigurationMessage createInternalErrorMsg(String description, String configId,
            Exception... e) {
        return new ConfigurationMessage(FAILURE, INTERNAL_ERROR, description, configId, e);
    }

}
