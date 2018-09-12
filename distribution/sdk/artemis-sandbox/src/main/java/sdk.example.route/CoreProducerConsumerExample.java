/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package sdk.example.route;

import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sjms.jms.ConnectionFactoryResource;
import org.apache.camel.component.sjms2.Sjms2Component;
import org.apache.camel.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoreProducerConsumerExample extends RouteBuilder {

  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(CoreProducerConsumerExample.class);

  private static final int PERIOD = 1000;

  public static void main(String... args) throws Exception {
    Main main = new Main();
    main.enableHangupSupport();
    main.addRouteBuilder(new CoreProducerConsumerExample());
    System.setProperty(
        "javax.net.ssl.keyStore",
        CoreProducerConsumerExample.class.getResource("/serverKeystore.jks").toURI().getPath());
    System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
    System.setProperty(
        "javax.net.ssl.trustStore",
        CoreProducerConsumerExample.class.getResource("/serverTruststore.jks").toURI().getPath());
    System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
    main.run(args);
  }

  @Override
  public void configure() throws Exception {
    createCamelContext();

    from("timer:simple?PERIOD=" + PERIOD)
        .process(new CoreProducerConsumerExample.MessageProducerProcessor("core"))
        .to("sjms2:core.example");

    from("sjms2:core.example?consumer.bridgeErrorHandler=true").to("stream:out");
  }

  private void createCamelContext() throws Exception {
    CamelContext camelContext = getContext();
    ActiveMQConnectionFactory factory =
        ActiveMQJMSClient.createConnectionFactory(
            "(tcp://0.0.0.0:5672,tcp://0.0.0.0:61617)?ha=true;sslEnabled=true;enabledCipherSuites=TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256;enabledProtocols=TLSv1.1,TLSv1.2",
            "name");

    factory.setRetryInterval(1000);
    factory.setRetryIntervalMultiplier(1.0);
    factory.setReconnectAttempts(-1);

    Sjms2Component sjms2 = new Sjms2Component();
    ConnectionFactoryResource connectionResource =
        new ConnectionFactoryResource(1, factory, "admin", "admin");
    sjms2.setConnectionResource(connectionResource);
    camelContext.addComponent("sjms2", sjms2);
  }

  public static class MessageProducerProcessor implements Processor {

    private static volatile long count;

    private final String name;

    public MessageProducerProcessor(String name) {
      this.name = name;
    }

    @SuppressWarnings("squid:S2696")
    @Override
    public void process(Exchange exchange) throws Exception {
      exchange.getOut().setBody(name + count++);
    }
  }
}
