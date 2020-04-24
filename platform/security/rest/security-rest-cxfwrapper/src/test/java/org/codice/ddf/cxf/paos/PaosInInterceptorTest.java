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
package org.codice.ddf.cxf.paos;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpUnsuccessfulResponseHandler;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ddf.security.liberty.paos.Request;
import ddf.security.liberty.paos.Response;
import ddf.security.liberty.paos.impl.RequestBuilder;
import ddf.security.liberty.paos.impl.RequestMarshaller;
import ddf.security.liberty.paos.impl.RequestUnmarshaller;
import ddf.security.liberty.paos.impl.ResponseBuilder;
import ddf.security.liberty.paos.impl.ResponseMarshaller;
import ddf.security.liberty.paos.impl.ResponseUnmarshaller;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.Phase;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.codice.ddf.security.jaxrs.impl.SamlSecurity;
import org.junit.Before;
import org.junit.Test;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;

public class PaosInInterceptorTest {

  @Before
  public void setup() {
    OpenSAMLUtil.initSamlEngine();
    XMLObjectProviderRegistry xmlObjectProviderRegistry =
        ConfigurationService.get(XMLObjectProviderRegistry.class);
    xmlObjectProviderRegistry.registerObjectProvider(
        Request.DEFAULT_ELEMENT_NAME,
        new RequestBuilder(),
        new RequestMarshaller(),
        new RequestUnmarshaller());
    xmlObjectProviderRegistry.registerObjectProvider(
        Response.DEFAULT_ELEMENT_NAME,
        new ResponseBuilder(),
        new ResponseMarshaller(),
        new ResponseUnmarshaller());
  }

  @Test
  public void handleMessagePaosResponseBasicGood() throws IOException {
    Message message = new MessageImpl();
    message.setContent(
        InputStream.class,
        PaosInInterceptorTest.class.getClassLoader().getResource("ecprequest.xml").openStream());
    final String testHeaderKey = "X-Test-Header";
    final String correctHeaderToBeForwarded = "correct header that needs to be forwarded";
    final String listOfIntsHeaderKey = "X-Test-IntList-Header";
    final List<Object> listOfIntsHeader = ImmutableList.of(1, 2, 3);

    message.put(Message.CONTENT_TYPE, "application/vnd.paos+xml");
    HashMap<String, List<String>> messageHeaders = new HashMap<>();
    messageHeaders.put(testHeaderKey, ImmutableList.of("original, incorrect header value"));
    message.put(Message.PROTOCOL_HEADERS, messageHeaders);

    Message outMessage = new MessageImpl();
    HashMap<String, List> protocolHeaders = new HashMap<>();
    outMessage.put(Message.PROTOCOL_HEADERS, protocolHeaders);
    outMessage.put(Message.HTTP_REQUEST_METHOD, "GET");
    protocolHeaders.put("Authorization", Collections.singletonList("BASIC dGVzdDp0ZXN0"));
    ExchangeImpl exchange = new ExchangeImpl();
    exchange.setOutMessage(outMessage);
    message.setExchange(exchange);

    PaosInInterceptor paosInInterceptor =
        new PaosInInterceptor(Phase.RECEIVE, new SamlSecurity()) {
          HttpResponseWrapper getHttpResponse(
              String responseConsumerURL, String soapResponse, Message message) throws IOException {
            HttpResponseWrapper httpResponseWrapper = new HttpResponseWrapper();
            if (responseConsumerURL.equals("https://sp.example.org/PAOSConsumer")) {
              httpResponseWrapper.statusCode = 200;
              httpResponseWrapper.content = new ByteArrayInputStream("actual content".getBytes());
              httpResponseWrapper.headers =
                  ImmutableMap.of(
                          testHeaderKey,
                          (Object) ImmutableList.of(correctHeaderToBeForwarded),
                          listOfIntsHeaderKey,
                          listOfIntsHeader)
                      .entrySet();
            } else if (responseConsumerURL.equals("https://idp.example.org/saml2/sso")) {
              httpResponseWrapper.statusCode = 200;
              httpResponseWrapper.content =
                  PaosInInterceptorTest.class
                      .getClassLoader()
                      .getResource("idpresponse.xml")
                      .openStream();
            }
            return httpResponseWrapper;
          }
        };
    paosInInterceptor.handleMessage(message);
    assertThat(IOUtils.toString(message.getContent(InputStream.class)), is("actual content"));
    Map<String, List<String>> headers = (Map) message.get(Message.PROTOCOL_HEADERS);
    assertThat(headers.get(testHeaderKey), hasItem(correctHeaderToBeForwarded));
    assertThat(headers.get(listOfIntsHeaderKey), hasItems("1", "2", "3"));
  }

  @Test(expected = Fault.class)
  public void handleMessagePaosResponseBasicBad() throws IOException {
    Message message = new MessageImpl();
    message.setContent(
        InputStream.class,
        PaosInInterceptorTest.class.getClassLoader().getResource("ecprequest.xml").openStream());
    message.put(Message.CONTENT_TYPE, "application/vnd.paos+xml");
    Message outMessage = new MessageImpl();
    HashMap<String, List> protocolHeaders = new HashMap<>();
    outMessage.put(Message.PROTOCOL_HEADERS, protocolHeaders);
    outMessage.put(Message.HTTP_REQUEST_METHOD, "GET");
    protocolHeaders.put("Authorization", Collections.singletonList("BASIC dGVzdDp0ZXN0"));
    ExchangeImpl exchange = new ExchangeImpl();
    exchange.setOutMessage(outMessage);
    message.setExchange(exchange);
    PaosInInterceptor paosInInterceptor =
        new PaosInInterceptor(Phase.RECEIVE, new SamlSecurity()) {
          HttpResponseWrapper getHttpResponse(
              String responseConsumerURL, String soapResponse, Message message) throws IOException {
            HttpResponseWrapper httpResponseWrapper = new HttpResponseWrapper();
            if (responseConsumerURL.equals("https://sp.example.org/PAOSConsumer")) {
              httpResponseWrapper.statusCode = 400;
              httpResponseWrapper.content = new ByteArrayInputStream("actual content".getBytes());
            } else if (responseConsumerURL.equals("https://idp.example.org/saml2/sso")) {
              httpResponseWrapper.statusCode = 200;
              httpResponseWrapper.content =
                  PaosInInterceptorTest.class
                      .getClassLoader()
                      .getResource("idpresponse.xml")
                      .openStream();
            }
            return httpResponseWrapper;
          }
        };
    paosInInterceptor.handleMessage(message);
  }

  @Test(expected = Fault.class)
  public void handleMessagePaosResponseBasicNoIdp() throws IOException {
    Message message = new MessageImpl();
    message.setContent(
        InputStream.class,
        PaosInInterceptorTest.class
            .getClassLoader()
            .getResource("ecprequest_noidp.xml")
            .openStream());
    message.put(Message.CONTENT_TYPE, "application/vnd.paos+xml");
    Message outMessage = new MessageImpl();
    HashMap<String, List> protocolHeaders = new HashMap<>();
    outMessage.put(Message.PROTOCOL_HEADERS, protocolHeaders);
    outMessage.put(Message.HTTP_REQUEST_METHOD, "GET");
    protocolHeaders.put("Authorization", Collections.singletonList("BASIC dGVzdDp0ZXN0"));
    ExchangeImpl exchange = new ExchangeImpl();
    exchange.setOutMessage(outMessage);
    message.setExchange(exchange);
    PaosInInterceptor paosInInterceptor = new PaosInInterceptor(Phase.RECEIVE, new SamlSecurity());
    paosInInterceptor.handleMessage(message);
  }

  @Test
  public void handleMessagePaosResponseBasicBadAcsUrl() throws IOException {
    Message message = new MessageImpl();
    message.setContent(
        InputStream.class,
        PaosInInterceptorTest.class.getClassLoader().getResource("ecprequest.xml").openStream());
    message.put(Message.CONTENT_TYPE, "application/vnd.paos+xml");
    Message outMessage = new MessageImpl();
    HashMap<String, List> protocolHeaders = new HashMap<>();
    outMessage.put(Message.PROTOCOL_HEADERS, protocolHeaders);
    outMessage.put(Message.HTTP_REQUEST_METHOD, "GET");
    protocolHeaders.put("Authorization", Collections.singletonList("BASIC dGVzdDp0ZXN0"));
    ExchangeImpl exchange = new ExchangeImpl();
    exchange.setOutMessage(outMessage);
    message.setExchange(exchange);
    PaosInInterceptor paosInInterceptor =
        new PaosInInterceptor(Phase.RECEIVE, new SamlSecurity()) {
          HttpResponseWrapper getHttpResponse(
              String responseConsumerURL, String soapResponse, Message message) throws IOException {
            HttpResponseWrapper httpResponseWrapper = new HttpResponseWrapper();
            if (responseConsumerURL.equals("https://sp.example.org/PAOSConsumer")) {
              httpResponseWrapper.statusCode = 200;
              httpResponseWrapper.content = new ByteArrayInputStream("error content".getBytes());
            } else if (responseConsumerURL.equals("https://idp.example.org/saml2/sso")) {
              httpResponseWrapper.statusCode = 200;
              httpResponseWrapper.content =
                  new ByteArrayInputStream(
                      IOUtils.toString(
                              PaosInInterceptorTest.class
                                  .getClassLoader()
                                  .getResource("idpresponse.xml")
                                  .openStream())
                          .replace("https://sp.example.org/PAOSConsumer", "badurl")
                          .getBytes());
            }
            return httpResponseWrapper;
          }
        };
    paosInInterceptor.handleMessage(message);
    assertThat(IOUtils.toString(message.getContent(InputStream.class)), is("error content"));
  }

  @Test
  public void getHttpUnsuccessfulResponseHandlerHeaderTest() throws IOException {
    Message message = new MessageImpl();
    message.put(Message.HTTP_REQUEST_METHOD, "GET");

    HashMap<String, List> protocolHeaders = new HashMap<>();
    message.put(Message.PROTOCOL_HEADERS, protocolHeaders);
    protocolHeaders.put("X-Custom-Header", Collections.singletonList("Custom"));

    PaosInInterceptor paosInInterceptor =
        spy(new PaosInInterceptor(Phase.RECEIVE, new SamlSecurity()));
    doReturn(true)
        .when(paosInInterceptor)
        .isRedirect(any(HttpRequest.class), any(HttpResponse.class), any(String.class));

    GenericUrl url = new GenericUrl("https://localhost:8993/PAOSConsumer");
    HttpRequest request = new MockHttpTransport().createRequestFactory().buildGetRequest(url);
    request.getUrl().set("url", "https://localhost:8993/PAOSConsumer");

    // Using request.execute to create an HttpResponse since it's final and cannot be mocked
    HttpResponse response = request.execute();
    response.getHeaders().setLocation("https://localhost:8993/PAOSConsumer");
    response.getHeaders().set("set-cookie", Collections.singletonList("cookie"));

    HttpUnsuccessfulResponseHandler responseHandler =
        paosInInterceptor.getHttpUnsuccessfulResponseHandler(message);
    boolean returned = responseHandler.handleResponse(request, response, true);

    assertThat(returned, is(true));
    // HttpHeaders ignores header case
    assertThat(request.getHeaders().containsKey("x-custom-header"), is(true));
    assertThat(
        request.getHeaders().get("x-custom-header"), is(Collections.singletonList("Custom")));
  }
}
