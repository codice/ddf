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

import javax.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQSslConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.jms.JmsConfiguration;
import org.apache.camel.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.connection.JmsTransactionManager;

public class OpenwireProducerConsumerExample extends RouteBuilder {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(OpenwireProducerConsumerExample.class);

  private final int period = 1000;

  public static void main(String... args) throws Exception {
    // normally this would run inside of some container and be converted to a camel blueprint.xml
    // but to simplify testing of a route we can use the dsl camel language and run it from a camel
    // main
    Main main = new Main();
    main.addRouteBuilder(new OpenwireProducerConsumerExample());
    main.run(args);
  }

  @Override
  public void configure() throws Exception {
    onException(Throwable.class).logStackTrace(true);

    createCamelContext();

    from("timer:simple?period=" + period)
        .process(new OpenwireProducerConsumerExample.MessageProducerProcessor("openwire"))
        .to("jms:openwire.example")
        .onException(Throwable.class)
        .maximumRedeliveries(2)
        .redeliveryDelay(1000);

    from("jms:openwire.example?consumer.bridgeErrorHandler=true")
        .to("stream:out")
        .onException(Throwable.class)
        .maximumRedeliveries(-1);
  }

  private void createCamelContext() throws Exception {

    CamelContext camelContext = getContext();
    ConnectionFactory jmsConnectFactory = createConnectionFactory();

    PooledConnectionFactory jmsPooledConnectionFactory = new PooledConnectionFactory();
    jmsPooledConnectionFactory.setConnectionFactory(jmsConnectFactory);
    jmsPooledConnectionFactory.setMaxConnections(2);

    JmsTransactionManager jmsTransactionManager = new JmsTransactionManager();
    jmsTransactionManager.setConnectionFactory(jmsConnectFactory);

    JmsConfiguration jmsConfiguration = new JmsConfiguration();
    jmsConfiguration.setConnectionFactory(jmsPooledConnectionFactory);
    jmsConfiguration.setTransacted(true);
    jmsConfiguration.setTransactionManager(jmsTransactionManager);
    jmsConfiguration.setCacheLevelName("CACHE_CONSUMER");

    JmsComponent jms = new JmsComponent();
    jms.setConfiguration(jmsConfiguration);
    camelContext.addComponent("jms", jms);
  }

  private ConnectionFactory createConnectionFactory() {
    ActiveMQConnectionFactory jmsConnectFactory = new ActiveMQConnectionFactory();
    jmsConnectFactory.setUserName("admin");
    jmsConnectFactory.setPassword("admin");
    jmsConnectFactory.setBrokerURL("failover://(tcp://localhost:61616,tcp://localhost:61617)");
    jmsConnectFactory.setWatchTopicAdvisories(false);
    return jmsConnectFactory;
  }

  private ConnectionFactory createSslConnectionFactory() throws Exception {
    ActiveMQSslConnectionFactory jmsConnectFactory = new ActiveMQSslConnectionFactory();
    jmsConnectFactory.setKeyStore(
        OpenwireProducerConsumerExample.class.getResource("/serverKeystore.jks").toURI().getPath());
    jmsConnectFactory.setKeyStoreKeyPassword("changeit");
    jmsConnectFactory.setKeyStorePassword("changeit");
    jmsConnectFactory.setTrustStore(
        OpenwireProducerConsumerExample.class
            .getResource("/serverTruststore.jks")
            .toURI()
            .getPath());
    jmsConnectFactory.setTrustStorePassword("changeit");
    jmsConnectFactory.setBrokerURL("failover://(ssl://localhost:61616,ssl://localhost:61617)");
    jmsConnectFactory.setWatchTopicAdvisories(false);
    return jmsConnectFactory;
  }

  public static class MessageProducerProcessor implements Processor {

    private static volatile long count;

    private final String name;

    public MessageProducerProcessor(String name) {
      this.name = name;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
      exchange.getOut().setBody(name + count++);
    }
  }
}
