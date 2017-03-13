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
package org.codice.ddf.broker.config;

import java.util.Map;

import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.codice.ddf.configuration.PropertyResolver;

import ddf.security.encryption.EncryptionService;

public class ActiveMQJMSClientFactoryWrapper {

    EncryptionService encryptionService;

    private String url;

    private String name = "common-broker-config";

    private String username;

    private char[] password = {'a', 'd', 'm', 'i', 'n'};

    private long retryInterval = 1000L;

    private double retryIntervalMultiplier = 1.0;

    private int reconnectAttempts = -1;

    public ActiveMQJMSClientFactoryWrapper(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    public ActiveMQConnectionFactory createConnectionFactory() throws Exception {
        ActiveMQConnectionFactory connectionFactory = ActiveMQJMSClient.createConnectionFactory(
                PropertyResolver.resolveProperties(url),
                name)
                .setUser(username)
                .setPassword(encryptionService.decryptValue(new String(password)));
        connectionFactory.setRetryInterval(retryInterval);
        connectionFactory.setRetryIntervalMultiplier(retryIntervalMultiplier);
        connectionFactory.setReconnectAttempts(reconnectAttempts);

        return connectionFactory;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return new String(password);
    }

    public void setPassword(String password) {
        this.password = password.toCharArray();
    }

    public long getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(long retryInterval) {
        this.retryInterval = retryInterval;
    }

    public double getRetryIntervalMultiplier() {
        return retryIntervalMultiplier;
    }

    public void setRetryIntervalMultiplier(double retryIntervalMultiplier) {
        this.retryIntervalMultiplier = retryIntervalMultiplier;
    }

    public int getReconnectAttempts() {
        return reconnectAttempts;
    }

    public void setReconnectAttempts(int reconnectAttempts) {
        this.reconnectAttempts = reconnectAttempts;
    }

    public void update(Map<String, Object> properties) throws Exception {
        password = ((String) properties.get("password")).toCharArray();
        username = (String) properties.get("username");
        url = (String) properties.get("url");
        //test if the url is good
        createConnectionFactory();
    }

}
