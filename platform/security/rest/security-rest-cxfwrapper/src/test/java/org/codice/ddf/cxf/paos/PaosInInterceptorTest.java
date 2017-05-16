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
package org.codice.ddf.cxf.paos;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.Phase;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.junit.Before;
import org.junit.Test;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;

import ddf.security.liberty.paos.Request;
import ddf.security.liberty.paos.Response;
import ddf.security.liberty.paos.impl.RequestBuilder;
import ddf.security.liberty.paos.impl.RequestMarshaller;
import ddf.security.liberty.paos.impl.RequestUnmarshaller;
import ddf.security.liberty.paos.impl.ResponseBuilder;
import ddf.security.liberty.paos.impl.ResponseMarshaller;
import ddf.security.liberty.paos.impl.ResponseUnmarshaller;

public class PaosInInterceptorTest {

    @Before
    public void setup() {
        OpenSAMLUtil.initSamlEngine();
        XMLObjectProviderRegistry xmlObjectProviderRegistry = ConfigurationService.get(
                XMLObjectProviderRegistry.class);
        xmlObjectProviderRegistry.registerObjectProvider(Request.DEFAULT_ELEMENT_NAME,
                new RequestBuilder(),
                new RequestMarshaller(),
                new RequestUnmarshaller());
        xmlObjectProviderRegistry.registerObjectProvider(Response.DEFAULT_ELEMENT_NAME,
                new ResponseBuilder(),
                new ResponseMarshaller(),
                new ResponseUnmarshaller());
    }

    @Test
    public void handleMessagePaosResponseBasicGood() throws IOException {
        Message message = new MessageImpl();
        message.setContent(InputStream.class,
                PaosInInterceptorTest.class.getClassLoader()
                        .getResource("ecprequest.xml")
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
        PaosInInterceptor paosInInterceptor = new PaosInInterceptor(Phase.RECEIVE) {
            HttpResponseWrapper getHttpResponse(String responseConsumerURL, String soapResponse,
                    Message message) throws IOException {
                HttpResponseWrapper httpResponseWrapper = new HttpResponseWrapper();
                if (responseConsumerURL.equals("https://sp.example.org/PAOSConsumer")) {
                    httpResponseWrapper.statusCode = 200;
                    httpResponseWrapper.content =
                            new ByteArrayInputStream("actual content".getBytes());
                } else if (responseConsumerURL.equals("https://idp.example.org/saml2/sso")) {
                    httpResponseWrapper.statusCode = 200;
                    httpResponseWrapper.content = PaosInInterceptorTest.class.getClassLoader()
                            .getResource("idpresponse.xml")
                            .openStream();
                }
                return httpResponseWrapper;
            }
        };
        paosInInterceptor.handleMessage(message);
        assertThat(IOUtils.toString(message.getContent(InputStream.class)), is("actual content"));
    }

    @Test(expected = Fault.class)
    public void handleMessagePaosResponseBasicBad() throws IOException {
        Message message = new MessageImpl();
        message.setContent(InputStream.class,
                PaosInInterceptorTest.class.getClassLoader()
                        .getResource("ecprequest.xml")
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
        PaosInInterceptor paosInInterceptor = new PaosInInterceptor(Phase.RECEIVE) {
            HttpResponseWrapper getHttpResponse(String responseConsumerURL, String soapResponse,
                    Message message) throws IOException {
                HttpResponseWrapper httpResponseWrapper = new HttpResponseWrapper();
                if (responseConsumerURL.equals("https://sp.example.org/PAOSConsumer")) {
                    httpResponseWrapper.statusCode = 400;
                    httpResponseWrapper.content =
                            new ByteArrayInputStream("actual content".getBytes());
                } else if (responseConsumerURL.equals("https://idp.example.org/saml2/sso")) {
                    httpResponseWrapper.statusCode = 200;
                    httpResponseWrapper.content = PaosInInterceptorTest.class.getClassLoader()
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
        message.setContent(InputStream.class,
                PaosInInterceptorTest.class.getClassLoader()
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
        PaosInInterceptor paosInInterceptor = new PaosInInterceptor(Phase.RECEIVE);
        paosInInterceptor.handleMessage(message);
    }

    @Test
    public void handleMessagePaosResponseBasicBadAcsUrl() throws IOException {
        Message message = new MessageImpl();
        message.setContent(InputStream.class,
                PaosInInterceptorTest.class.getClassLoader()
                        .getResource("ecprequest.xml")
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
        PaosInInterceptor paosInInterceptor = new PaosInInterceptor(Phase.RECEIVE) {
            HttpResponseWrapper getHttpResponse(String responseConsumerURL, String soapResponse,
                    Message message) throws IOException {
                HttpResponseWrapper httpResponseWrapper = new HttpResponseWrapper();
                if (responseConsumerURL.equals("https://sp.example.org/PAOSConsumer")) {
                    httpResponseWrapper.statusCode = 200;
                    httpResponseWrapper.content =
                            new ByteArrayInputStream("error content".getBytes());
                } else if (responseConsumerURL.equals("https://idp.example.org/saml2/sso")) {
                    httpResponseWrapper.statusCode = 200;
                    httpResponseWrapper.content = new ByteArrayInputStream(IOUtils.toString(
                            PaosInInterceptorTest.class.getClassLoader()
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
}

