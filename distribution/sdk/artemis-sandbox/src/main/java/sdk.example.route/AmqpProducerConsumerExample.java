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
 **/
package sdk.example.route;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.amqp.AMQPComponent;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.main.Main;
import org.apache.qpid.jms.JmsConnectionFactory;

/**
 * This class is to demonstrate how to create a simple Camel producer and consumer in Java that uses the Artemis Message Broker
 * as the broker for its queues and topics.
 *
 * The associated resource, "sdk-example-route.xml" can be placed in the <DDF_HOME>/etc/routes directory of a running DDF
 * to create the routes. This class can be run from the AmqpProducerConsumerExample#main method and will exercise the route
 * that was deployed in the "sdk-example-route.xml".
 *
 * You should see incrementing PINGs and ACKs in the system log.
 */
public class AmqpProducerConsumerExample extends RouteBuilder {

    public static void main(String... args) throws Exception {
        //normally this would run inside of some container and be converted to a camel blueprint.xml
        // but to simplify testing of a route we can use the dsl camel language and run it from a camel main
        Main main = new Main();
        main.addRouteBuilder(new AmqpProducerConsumerExample());
        System.setProperty("javax.net.ssl.keyStore", AmqpProducerConsumerExample.class.getResource(
                "/serverKeystore.jks")
                .toURI()
                .getPath());
        System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
        System.setProperty("javax.net.ssl.trustStore", AmqpProducerConsumerExample.class.getResource(
                "/serverTruststore.jks")
                .toURI()
                .getPath());
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
        main.run(args);
    }

    @Override
    public void configure() throws Exception {
        onException(Throwable.class).logStackTrace(true);

        createCamelContext();
        CamelContext camelContext = getContext();
        from("timer:simple").process(new Processor() {
            private int count;

            @Override
            public void process(Exchange exchange) {
                count++;
                exchange.getIn()
                        .setBody("Message " + count);

                System.out.println(count + " PING");
            }
        })
                .to("amqp:sdk.example");

        from("amqp:topic:sdk.example").process(exchange -> System.out.println(
                "ACK " + exchange.getIn()
                        .getBody()))
                .stop();

    }

    private void createCamelContext() throws Exception {

        CamelContext camelContext = getContext();
        JmsConnectionFactory connectionFactory = new JmsConnectionFactory("admin",
                "admin",
                "amqps://localhost:5671");
        JmsComponent jms = JmsComponent.jmsComponent(connectionFactory);
        AMQPComponent amqp = new AMQPComponent(connectionFactory);
        camelContext.addComponent("jms", jms);
        camelContext.addComponent("amqp", amqp);
    }
}