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

import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.TextMessage;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sjms2.support.JmsTestSupport;
import org.junit.Assert;
import org.junit.Test;

public class InOutQueueProducerTest extends JmsTestSupport {

    private static final String TEST_DESTINATION_NAME = "in.out.queue.producer.test";

    public InOutQueueProducerTest() {
    }

    @Override
    protected boolean useJmx() {
        return false;
    }

    @Test
    public void testInOutQueueProducer() throws Exception {
        MessageConsumer mc = createQueueConsumer(TEST_DESTINATION_NAME + ".request");
        Assert.assertNotNull(mc);
        String requestText = "Hello World!";
        String responseText = "How are you";
        mc.setMessageListener(new MyMessageListener(requestText, responseText));
        Object responseObject = template.requestBody("direct:start", requestText);
        Assert.assertNotNull(responseObject);
        Assert.assertTrue(responseObject instanceof String);
        Assert.assertEquals(responseText, responseObject);
        mc.close();

    }

    @Test
    public void testInOutQueueProducerWithCorrelationId() throws Exception {
        MessageConsumer mc = createQueueConsumer(TEST_DESTINATION_NAME + ".request");
        Assert.assertNotNull(mc);
        String requestText = "Hello World!";
        String responseText = "How are you";
        mc.setMessageListener(new MyMessageListener(requestText, responseText));
        String correlationId = UUID.randomUUID()
                .toString()
                .replace("-", "");
        Exchange exchange = template.request("direct:start", new Processor() {

            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getOut()
                        .setBody(requestText);
                exchange.getOut()
                        .setHeader("JMSCorrelationID", correlationId);
            }
        });
        Assert.assertNotNull(exchange);
        Assert.assertTrue(exchange.getIn()
                .getBody() instanceof String);
        Assert.assertEquals(responseText,
                exchange.getIn()
                        .getBody());
        Assert.assertEquals(correlationId,
                exchange.getIn()
                        .getHeader("JMSCorrelationID", String.class));
        mc.close();

    }

    /*
     * @see org.apache.camel.test.junit4.CamelTestSupport#createRouteBuilder()
     * 
     * @return
     * 
     * @throws Exception
     */
    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:start").to("log:" + TEST_DESTINATION_NAME + ".in.log.1?showBody=true")
                        .inOut("sjms:queue:" + TEST_DESTINATION_NAME + ".request" + "?namedReplyTo="
                                + TEST_DESTINATION_NAME + ".response")
                        .to("log:" + TEST_DESTINATION_NAME + ".out.log.1?showBody=true");
            }
        };
    }

    protected class MyMessageListener implements MessageListener {
        private final String requestText;

        private final String responseText;

        public MyMessageListener(String request, String response) {
            requestText = request;
            responseText = response;
        }

        @Override
        public void onMessage(Message message) {
            try {
                TextMessage request = (TextMessage) message;
                Assert.assertNotNull(request);
                String text = request.getText();
                Assert.assertEquals(requestText, text);

                TextMessage response = getSession().createTextMessage();
                response.setText(responseText);
                response.setJMSCorrelationID(request.getJMSCorrelationID());
                MessageProducer mp = getSession().createProducer(message.getJMSReplyTo());
                mp.send(response);
                mp.close();
            } catch (JMSException e) {
                Assert.fail(e.getLocalizedMessage());
            }
        }
    }
}
