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

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Assert;
import org.junit.Test;

public class SimpleJmsComponentTest extends CamelTestSupport {

    @Test
    public void testHelloWorld() throws Exception {
        Sjms2Component component = context.getComponent("sjms", Sjms2Component.class);
        Assert.assertNotNull(component);
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() {
                ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                        "vm://broker?broker.persistent=false&broker.useJmx=false");
                Sjms2Component component = new Sjms2Component();
                component.setConnectionFactory(connectionFactory);
                getContext().addComponent("sjms", component);
            }
        };
    }
}
