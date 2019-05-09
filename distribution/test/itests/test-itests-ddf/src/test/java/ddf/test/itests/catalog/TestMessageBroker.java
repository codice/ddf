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
package ddf.test.itests.catalog;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.path.json.JsonPath.with;
import static org.codice.ddf.itests.common.WaitCondition.expect;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.ConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.sjms2.Sjms2Component;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.commons.io.FileUtils;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.itests.common.WaitCondition;
import org.codice.ddf.test.common.annotations.AfterExam;
import org.codice.ddf.test.common.annotations.BeforeExam;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class TestMessageBroker extends AbstractIntegrationTest {

  private static final int TIMEOUT_IN_SECONDS = 60;

  private static final String EXAMPLE_TEST_ROUTE = "sdk.example";

  private static final String SJMS_EXAMPLE_TEST_QUEUE =
      String.format("sjms2:%s", EXAMPLE_TEST_ROUTE);

  private static final String SJMS_EXAMPLE_TEST_TOPIC =
      String.format("sjms2:topic:%s", EXAMPLE_TEST_ROUTE);

  private static final String MOCK_EXAMPLE_TEST_ROUTE =
      String.format("mock:%s", EXAMPLE_TEST_ROUTE);

  private static final String UNDELIVERED_TEST_QUEUE = "undelivered.test";

  private static final String SJMS_UNDELIVERED_TEST_QUEUE =
      String.format("sjms2:%s?transacted=true", UNDELIVERED_TEST_QUEUE);

  private static final String MOCK_UNDELIVERED_TEST_ENDPOINT =
      String.format("mock:%s.end", UNDELIVERED_TEST_QUEUE);

  private static final String DLQ_ADDRESS_NAME = "DLQ";

  private static final String DLQ_QUEUE_NAME = "DLQ";

  private static final String UNDELIVERED_MESSAGES_MBEAN_URL =
      "/admin/jolokia/exec/org.codice.ddf.broker.ui.UndeliveredMessages:service=UndeliveredMessages/";

  private static final DynamicPort AMQP_PORT = new DynamicPort(6);

  private static final DynamicPort ARTEMIS_PORT = new DynamicPort(7);

  private static final DynamicPort OPENWIRE_PORT = new DynamicPort(8);

  private static final DynamicUrl GET_UNDELIVERED_MESSAGES_PATH =
      new DynamicUrl(
          DynamicUrl.SECURE_ROOT,
          HTTPS_PORT,
          UNDELIVERED_MESSAGES_MBEAN_URL
              + "getMessages/"
              + DLQ_ADDRESS_NAME
              + "/"
              + DLQ_QUEUE_NAME
              + "/");

  private static final DynamicUrl DELETE_UNDELIVERED_MESSAGES_PATH =
      new DynamicUrl(
          DynamicUrl.SECURE_ROOT,
          HTTPS_PORT,
          UNDELIVERED_MESSAGES_MBEAN_URL
              + "deleteMessages/"
              + DLQ_ADDRESS_NAME
              + "/"
              + DLQ_QUEUE_NAME
              + "/");

  private static final DynamicUrl RESEND_UNDELIVERED_MESSAGES_PATH =
      new DynamicUrl(
          DynamicUrl.SECURE_ROOT,
          HTTPS_PORT,
          UNDELIVERED_MESSAGES_MBEAN_URL
              + "resendMessages/"
              + DLQ_ADDRESS_NAME
              + "/"
              + DLQ_QUEUE_NAME
              + "/");

  private static final String ADMIN_USERNAME = "admin";

  private static final String ADMIN_PASSWORD = "admin";

  private static final AtomicBoolean BLOW_UP = new AtomicBoolean(true);

  private static CamelContext camelContext;

  private String messageId;

  @BeforeExam
  public void beforeExam() throws Exception {
    waitForSystemReady();
    getSecurityPolicy().configureRestForBasic();
    waitForSystemReady();
    System.setProperty("artemis.amqp.port", AMQP_PORT.getPort());
    System.setProperty("artemis.multiprotocol.port", ARTEMIS_PORT.getPort());
    System.setProperty("artemis.openwire.port", OPENWIRE_PORT.getPort());

    getServiceManager()
        .startFeature(true, "broker-app", "broker-undelivered-messages-ui", "broker-route-manager");
    setupCamelContext();
    FileUtils.copyInputStreamToFile(
        AbstractIntegrationTest.getFileContentAsStream(
            "sdk-example-route.xml", AbstractIntegrationTest.class),
        Paths.get(ddfHome, "etc", "routes").resolve("sdk-example-route.xml").toFile());
  }

  @AfterExam
  public void afterExam() throws Exception {
    getServiceManager()
        .stopFeature(true, "broker-app", "broker-undelivered-messages-ui", "broker-route-manager");
  }

  @Test
  public void testDynamicRouting() throws Exception {
    MockEndpoint endpoint = camelContext.getEndpoint(MOCK_EXAMPLE_TEST_ROUTE, MockEndpoint.class);
    endpoint.expectedBodiesReceived("test Message");
    sendMessage(SJMS_EXAMPLE_TEST_QUEUE, "test Message");
    endpoint.assertIsSatisfied(2000L);
  }

  @Test
  public void testUndeliveredMessagesRetry() throws Exception {
    MockEndpoint endpoint =
        camelContext.getEndpoint(MOCK_UNDELIVERED_TEST_ENDPOINT, MockEndpoint.class);
    endpoint.expectedBodiesReceived("retry");

    BLOW_UP.set(true);
    sendMessage(SJMS_UNDELIVERED_TEST_QUEUE, "retry");

    String initialDlqMessageId = verifyDlqIsNotEmpty(TIMEOUT_IN_SECONDS, 10);
    endpoint.assertIsNotSatisfied();

    BLOW_UP.set(false);
    resendMessageFromDlq(initialDlqMessageId);

    verifyDlqIsEmpty(TIMEOUT_IN_SECONDS, 10);
    endpoint.assertIsSatisfied();
  }

  @Test
  public void testUndeliveredMessagesDelete() throws Exception {
    MockEndpoint endpoint =
        camelContext.getEndpoint(MOCK_UNDELIVERED_TEST_ENDPOINT, MockEndpoint.class);
    endpoint.expectedBodiesReceived("delete");

    BLOW_UP.set(true);
    sendMessage(SJMS_UNDELIVERED_TEST_QUEUE, "delete");

    String initialDlqMessageId = verifyDlqIsNotEmpty(TIMEOUT_IN_SECONDS, 10);
    endpoint.assertIsNotSatisfied();

    BLOW_UP.set(false);
    deleteMessageFromDlq(initialDlqMessageId);

    verifyDlqIsEmpty(TIMEOUT_IN_SECONDS, 10);
    endpoint.assertIsNotSatisfied();
  }

  private String verifyDlqIsNotEmpty(long timeout, long pollingInterval) {
    WaitCondition waitCondition =
        expect("The Dead Letter Queue is not empty and contains an undelivered message.")
            .within(timeout, TimeUnit.SECONDS)
            .checkEvery(pollingInterval, TimeUnit.SECONDS)
            .until(
                () -> {
                  // Get undelivered messages from the DLQ
                  List<Object> responseList = getMessagesFromDlq();
                  if (!responseList.isEmpty()) {
                    messageId = (String) ((Map) responseList.get(0)).get("messageID");
                    return true;
                  }
                  return false;
                });
    if (waitCondition.lastResult()) {
      return messageId;
    }
    return "";
  }

  private boolean verifyDlqIsEmpty(long timeout, long pollingInterval) {
    WaitCondition waitCondition =
        expect("The Dead Letter Queue is empty.")
            .within(timeout, TimeUnit.SECONDS)
            .checkEvery(pollingInterval, TimeUnit.SECONDS)
            .until(
                () -> {
                  // Retrieve Jolokia response from the UndeliveredMessages MBean
                  List<Object> responseList = getMessagesFromDlq();
                  return responseList.isEmpty();
                });
    return waitCondition.lastResult();
  }

  private List<Object> getMessagesFromDlq() {
    String getUndeliveredMessagesResponse =
        performMbeanOperation(GET_UNDELIVERED_MESSAGES_PATH.getUrl());
    // Validate the undelivered message from the Jolokia response
    validateJolokiaGetMessagesResponse(getUndeliveredMessagesResponse);
    return with(getUndeliveredMessagesResponse).get("value");
  }

  private void deleteMessageFromDlq(String messageId) {
    int value =
        with(performMbeanOperation(
                DELETE_UNDELIVERED_MESSAGES_PATH.getUrl() + "[\"" + messageId + "\"]"))
            .get("value");
    if (value != 1) {
      fail("Failed to delete message in the DLQ");
    }
  }

  private void resendMessageFromDlq(String messageId) {
    int value =
        with(performMbeanOperation(
                RESEND_UNDELIVERED_MESSAGES_PATH.getUrl() + "[\"" + messageId + "\"]"))
            .get("value");
    if (value != 1) {
      fail("Failed to resend message in the DLQ");
    }
  }

  private String performMbeanOperation(String operationUrl) {
    // Retrieve Jolokia response from the UndeliveredMessages MBean
    return given()
        .auth()
        .preemptive()
        .basic(ADMIN_USERNAME, ADMIN_PASSWORD)
        .header("X-Requested-With", "XMLHttpRequest")
        .header("Origin", operationUrl)
        .when()
        .get(operationUrl)
        .asString();
  }

  private void validateJolokiaGetMessagesResponse(String jolokiaGetMessagesResponse) {
    assertThat(with(jolokiaGetMessagesResponse).get("status"), is(200));
    if (!((List) with(jolokiaGetMessagesResponse).get("value")).isEmpty()) {
      assertThat(
          extractMessage(jolokiaGetMessagesResponse, 0).get("address"), is(DLQ_ADDRESS_NAME));
    }
  }

  private Map extractMessage(String jolokiaGetMessagesResponse, int index) {
    return (Map) ((List) with(jolokiaGetMessagesResponse).get("value")).get(index);
  }

  private void sendMessage(String topicName, String message) {
    camelContext.createProducerTemplate().sendBody(topicName, message);
  }

  private void setupCamelContext() throws Exception {
    camelContext = new DefaultCamelContext();
    ConnectionFactory factory = getServiceManager().getService(ConnectionFactory.class);
    Sjms2Component sjms2 = new Sjms2Component();
    sjms2.setConnectionFactory(factory);
    camelContext.addComponent("sjms2", sjms2);
    camelContext.addRoutes(
        new RouteBuilder() {
          @Override
          public void configure() throws Exception {
            from(SJMS_EXAMPLE_TEST_TOPIC).to(MOCK_EXAMPLE_TEST_ROUTE);
            from(SJMS_UNDELIVERED_TEST_QUEUE)
                .process(
                    exchange -> {
                      if (BLOW_UP.get()) {
                        throw new Exception("Boom!!!");
                      }
                    })
                .to(MOCK_UNDELIVERED_TEST_ENDPOINT);
          }
        });

    camelContext.start();
  }
}
