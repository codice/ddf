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

/**
 * A {@link ConfigurationMessage} encapsulates the results of an operation performed on a {@link org.codice.ddf.admin.api.config.Configuration}.
 * {@link ConfigurationMessage}s are wrapped by {@link org.codice.ddf.admin.api.handler.report.Report}s to relay messages about the {@link org.codice.ddf.admin.api.handler.method.ConfigurationHandlerMethod}s results.
 */
public class ConfigurationMessage {

    // Default error message subtypes
    public static final String NO_METHOD_FOUND = "NO_METHOD_FOUND";
    public static final String MISSING_REQUIRED_FIELD = "MISSING_REQUIRED_FIELD";
    public static final String INVALID_FIELD = "INVALID_FIELD";
    public static final String FAILED_PERSIST = "FAILED_PERSIST";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private MessageType type;
    private String subType;
    private String message;
    private String configFieldId;

    /**
     * Creates a new {@link ConfigurationMessage}. A {@link ConfigurationMessage} is structured such that
     * it has a high level description of the problem of {@param #type} {@link MessageType} and a more detailed description
     * of {@param subType}. For instance, if the {@param #type}  was {@link MessageType#FAILURE}, the {@param subType}
     * could be {@link #MISSING_REQUIRED_FIELD}
     *
     * @param type a high level description message
     * @param subType a more specific type of the message
     * @param message a message indicating additional details about this {@link ConfigurationMessage}
     * @param configFieldId the id of the {@link org.codice.ddf.admin.api.config.Configuration} field being referenced in the {@link ConfigurationMessage}
     */
    public ConfigurationMessage(MessageType type, String subType, String message, String configFieldId) {
        this.type = type;
        this.subType = subType;
        this.message = message;
        this.configFieldId = configFieldId;
    }

    public ConfigurationMessage(MessageType type, String subType, String message) {
        this(type, subType, message, null);
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

    public String configFieldId() {
        return configFieldId;
    }

    // Setters
    public ConfigurationMessage configFieldId(String configFieldId) {
        this.configFieldId = configFieldId;
        return this;
    }

    //Builders
    public static ConfigurationMessage createInvalidFieldMsg(String description, String configFieldId) {
        return new ConfigurationMessage(FAILURE, INVALID_FIELD, description, configFieldId);
    }

    public static ConfigurationMessage createMissingRequiredFieldMsg(String configFieldId) {
        return new ConfigurationMessage(FAILURE, MISSING_REQUIRED_FIELD, "Missing required field \"" + configFieldId + "\".", configFieldId);
    }
}
