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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

import java.util.HashMap;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ddf.security.encryption.EncryptionService;

@RunWith(MockitoJUnitRunner.class)
public class ActiveMQJMSClientFactoryWrapperTest {

    private ActiveMQJMSClientFactoryWrapper activeMQJMSClientFactoryWrapper;

    @Mock
    private EncryptionService encryptionService;

    @Before
    public void setUp() throws Exception {
        activeMQJMSClientFactoryWrapper = new ActiveMQJMSClientFactoryWrapper(encryptionService);
        activeMQJMSClientFactoryWrapper.setPassword("password");
        activeMQJMSClientFactoryWrapper.setName("name");
        activeMQJMSClientFactoryWrapper.setReconnectAttempts(-1);
        activeMQJMSClientFactoryWrapper.setRetryInterval(1000);
        activeMQJMSClientFactoryWrapper.setRetryIntervalMultiplier(0.0);
        activeMQJMSClientFactoryWrapper.setUsername("username");
        activeMQJMSClientFactoryWrapper.setUrl("tcp://localhost:12345");
    }

    @Test
    public void createConnectionFactory() throws Exception {

        ActiveMQConnectionFactory activeMQConnectionFactory =
                activeMQJMSClientFactoryWrapper.createConnectionFactory();
        assertThat(activeMQConnectionFactory, notNullValue());
    }

    @Test
    public void update() throws Exception {
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("password", "password");
        properties.put("username", "username");
        properties.put("url", "tcp://localhost:12345");
        activeMQJMSClientFactoryWrapper.update(properties);
        ActiveMQConnectionFactory activeMQConnectionFactory =
                activeMQJMSClientFactoryWrapper.createConnectionFactory();
        assertThat(activeMQConnectionFactory, notNullValue());
    }

}