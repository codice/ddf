package org.codice.ui.admin.wizard.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigurationTestMessage<T> {

    private MessageType type;

    private String message;

    private T subtype;

    private List<Exception> exceptions;

    public ConfigurationTestMessage(MessageType type, Exception... exceptions){
        this.type = type;
        this.exceptions = new ArrayList<>();
        this.exceptions.addAll(Arrays.asList(exceptions));
    }

    public ConfigurationTestMessage(String message, MessageType type, Exception... exceptions){
        this.message = message;
        this.type = type;
        this.exceptions = new ArrayList<>();
        this.exceptions.addAll(Arrays.asList(exceptions));
    }

    public void addException(Exception e) {
        this.exceptions.add(e);
    }

    public String getMessage() {
        return message;
    }

    public MessageType getType() {
        return type;
    }

    public T getSubtype() {
        return subtype;
    }

    public enum MessageType {
        SUCCESS, INFO, FAILURE, NO_TEST_FOUND
    }


    //
    // Builder Methods
    //

    public static ConfigurationTestMessage buildMessage(MessageType type){
        return new ConfigurationTestMessage(type);
    }

    public static ConfigurationTestMessage buildMessage(MessageType type, String message){
        return new ConfigurationTestMessage(message, type);
    }

    public ConfigurationTestMessage subType(T subtype) {
        this.subtype = subtype;
        return this;
    }

    public ConfigurationTestMessage exception(Exception e) {
        exceptions.add(e);
        return this;
    }
}
