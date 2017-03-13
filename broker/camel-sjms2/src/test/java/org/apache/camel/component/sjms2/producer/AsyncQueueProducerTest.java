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
package org.apache.camel.component.sjms2.producer;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sjms2.Sjms2Component;
import org.apache.camel.component.sjms2.support.MyAsyncComponent;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Assert;
import org.junit.Test;

/**
 * @version
 */
public class AsyncQueueProducerTest extends CamelTestSupport {

    private static String beforeThreadName;

    private static String afterThreadName;

    private static String sedaThreadName;

    private static String route = "";

    @Test
    public void testAsyncJmsProducerEndpoint() throws Exception {
        getMockEndpoint("mock:before").expectedBodiesReceived("Hello Camel");
        getMockEndpoint("mock:after").expectedBodiesReceived("Bye Camel");
        getMockEndpoint("mock:result").expectedBodiesReceived("Bye Camel");

        template.sendBody("direct:start", "Hello Camel");
        // we should run before the async processor that sets B
        route += "A";

        assertMockEndpointsSatisfied();

        Assert.assertFalse("Should use different threads",
                beforeThreadName.equalsIgnoreCase(afterThreadName));
        Assert.assertFalse("Should use different threads",
                beforeThreadName.equalsIgnoreCase(sedaThreadName));
        Assert.assertFalse("Should use different threads",
                afterThreadName.equalsIgnoreCase(sedaThreadName));

        Assert.assertEquals("AB", route);
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = super.createCamelContext();

        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                "vm://broker?broker.persistent=false&broker.useJmx=false");
        Sjms2Component component = new Sjms2Component();
        component.setConnectionFactory(connectionFactory);
        camelContext.addComponent("sjms", component);

        return camelContext;
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                context.addComponent("async", new MyAsyncComponent());

                from("direct:start").to("mock:before")
                        .to("log:before")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                beforeThreadName = Thread.currentThread()
                                        .getName();
                            }
                        })
                        .to("async:bye:camel")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                afterThreadName = Thread.currentThread()
                                        .getName();
                            }
                        })
                        .to("sjms:queue:foo?synchronous=false");

                from("sjms:queue:foo?synchronous=false").to("mock:after")
                        .to("log:after")
                        .delay(1000)
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                route += "B";
                                sedaThreadName = Thread.currentThread()
                                        .getName();
                            }
                        })
                        .to("mock:result");
            }
        };
    }
}
