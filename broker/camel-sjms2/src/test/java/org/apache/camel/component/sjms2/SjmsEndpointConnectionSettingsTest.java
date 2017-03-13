/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.sjms2;

import java.util.Random;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.component.sjms.jms.ConnectionFactoryResource;
import org.apache.camel.component.sjms.jms.ConnectionResource;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Assert;
import org.junit.Test;

public class SjmsEndpointConnectionSettingsTest extends CamelTestSupport {
    private final ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
            "vm://broker?broker.persistent=false&broker.useJmx=false");

    private final ConnectionResource connectionResource = new ConnectionFactoryResource(2,
            connectionFactory);

    @Test
    public void testConnectionFactory() {
        Endpoint endpoint = context.getEndpoint("sjms2:queue:test?connectionFactory=activemq");
        Assert.assertNotNull(endpoint);
        Assert.assertTrue(endpoint instanceof Sjms2Endpoint);
        Sjms2Endpoint qe = (Sjms2Endpoint) endpoint;
        Assert.assertEquals(connectionFactory, qe.getConnectionFactory());
    }

    @Test
    public void testConnectionResource() {
        Endpoint endpoint = context.getEndpoint("sjms2:queue:test?connectionResource=connresource");
        Assert.assertNotNull(endpoint);
        Assert.assertTrue(endpoint instanceof Sjms2Endpoint);
        Sjms2Endpoint qe = (Sjms2Endpoint) endpoint;
        Assert.assertEquals(connectionResource, qe.getConnectionResource());
    }

    @Test
    public void testConnectionCount() {
        Random random = new Random();
        int poolSize = random.nextInt(100);
        Endpoint endpoint = context.getEndpoint("sjms2:queue:test?connectionCount=" + poolSize);
        Assert.assertNotNull(endpoint);
        Assert.assertTrue(endpoint instanceof Sjms2Endpoint);
        Sjms2Endpoint qe = (Sjms2Endpoint) endpoint;
        Assert.assertEquals(poolSize, qe.getConnectionCount());
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        SimpleRegistry registry = new SimpleRegistry();
        registry.put("activemq", connectionFactory);
        registry.put("connresource", connectionResource);
        return new DefaultCamelContext(registry);
    }
}
