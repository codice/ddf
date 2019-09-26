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
package org.codice.ddf.platform.http.proxy;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import org.apache.camel.Exchange;
import org.apache.camel.http.common.HttpMessage;
import org.apache.commons.io.IOUtils;
import org.codice.proxy.http.HttpProxyService;
import org.codice.proxy.http.HttpProxyServiceImpl;
import org.junit.Test;

public class HttpProxyTest {

  @Test
  public void testStartingAndStoppingProxyWithDifferentConfigurations() throws Exception {
    HttpProxyServiceImpl httpProxyService = mock(HttpProxyServiceImpl.class);
    when(httpProxyService.start(eq("0.0.0.0:8181"), anyString(), eq(120000), eq(true), anyObject()))
        .thenReturn("endpointName");
    HttpProxy httpProxy =
        new HttpProxy(httpProxyService) {
          public Properties getProperties() {
            Properties properties = new Properties();
            InputStream propertiesStream =
                HttpProxy.class.getResourceAsStream("/etc/org.ops4j.pax.web.cfg");
            try {
              properties.load(propertiesStream);
            } catch (IOException e) {
              fail(e.getMessage());
            } finally {
              IOUtils.closeQuietly(propertiesStream);
            }
            return properties;
          }
        };
    httpProxy.startProxy();
    verify(httpProxyService, times(1))
        .start(eq("0.0.0.0:8181"), anyString(), eq(120000), eq(true), anyObject());

    httpProxy.stopProxy();
    verify(httpProxyService, times(1)).stop("endpointName");

    httpProxy.setHostname("blah");

    httpProxy.startProxy();
    verify(httpProxyService, times(1))
        .start(eq("0.0.0.0:8181"), eq("https://blah:8993"), eq(120000), eq(true), anyObject());

    httpProxy.startProxy();
    verify(httpProxyService, times(3)).stop("endpointName");

    httpProxy =
        new HttpProxy(httpProxyService) {
          public Properties getProperties() {
            Properties properties = new Properties();
            InputStream propertiesStream =
                HttpProxy.class.getResourceAsStream("/etc/org.ops4j.pax.web.cfg");
            try {
              properties.load(propertiesStream);
            } catch (IOException e) {
              fail(e.getMessage());
            } finally {
              IOUtils.closeQuietly(propertiesStream);
            }
            properties.put("org.osgi.service.http.enabled", "true");
            return properties;
          }
        };
    httpProxy.startProxy();
    verify(httpProxyService, times(3))
        .start(eq("0.0.0.0:8181"), anyString(), eq(120000), eq(true), anyObject());
  }

  @Test
  public void testStartingWithNoProperties() throws Exception {
    HttpProxyServiceImpl httpProxyService = mock(HttpProxyServiceImpl.class);
    when(httpProxyService.start(anyString(), anyString(), anyInt(), anyBoolean(), anyObject()))
        .thenReturn("endpointName");
    HttpProxy httpProxy =
        new HttpProxy(httpProxyService) {
          public Properties getProperties() {
            return new Properties();
          }
        };
    httpProxy.startProxy();
    verify(httpProxyService, times(0))
        .start(anyString(), anyString(), anyInt(), anyBoolean(), anyObject());
  }

  @Test
  public void testLoadProperties() throws IOException {
    HttpProxyService httpProxyService = mock(HttpProxyService.class);
    HttpProxy httpProxy = new HttpProxy(httpProxyService);
    Properties properties = httpProxy.getProperties();
    assertThat(0, is(properties.stringPropertyNames().size()));
  }

  @Test
  public void testPolicyRemoveBean() throws IOException {
    InputStream inpuStream = HttpProxyTest.class.getResourceAsStream("/test.wsdl");
    String wsdl = IOUtils.toString(inpuStream, StandardCharsets.UTF_8);
    HttpProxy.PolicyRemoveBean policyRemoveBean =
        new HttpProxy.PolicyRemoveBean("8181", "8993", "localhost", "/services");
    Exchange exchange = mock(Exchange.class);
    HttpMessage httpMessage = mock(HttpMessage.class);
    when(exchange.getIn()).thenReturn(httpMessage);
    when(exchange.getOut()).thenReturn(httpMessage);
    when(httpMessage.getHeader("CamelHttpQuery")).thenReturn("wsdl");
    when(httpMessage.getBody()).thenReturn(inpuStream);
    doCallRealMethod().when(httpMessage).setBody(any());
    doCallRealMethod().when(httpMessage).getBody();
    httpMessage.setBody(wsdl);

    policyRemoveBean.rewrite(exchange);

    assertFalse(httpMessage.getBody().toString().contains("https:"));
  }

  @Test
  public void testPolicyRemoveBeanGzipEncoding() throws IOException {
    InputStream inpuStream = HttpProxyTest.class.getResourceAsStream("/test.wsdl");
    String wsdl = IOUtils.toString(inpuStream, StandardCharsets.UTF_8);
    HttpProxy.PolicyRemoveBean policyRemoveBean =
        new HttpProxy.PolicyRemoveBean("8181", "8993", "localhost", "/services");
    Exchange exchange = mock(Exchange.class);
    HttpMessage httpMessage = mock(HttpMessage.class);
    when(exchange.getIn()).thenReturn(httpMessage);
    when(exchange.getOut()).thenReturn(httpMessage);
    when(httpMessage.getHeader("CamelHttpQuery")).thenReturn("wsdl");
    when(httpMessage.getBody()).thenReturn(inpuStream);
    when(httpMessage.getHeader(Exchange.CONTENT_ENCODING)).thenReturn("gzip");
    doCallRealMethod().when(httpMessage).setBody(any());
    doCallRealMethod().when(httpMessage).getBody();
    httpMessage.setBody(wsdl);

    policyRemoveBean.rewrite(exchange);

    assertTrue(httpMessage.getBody().toString().contains("https:"));
  }

  @Test
  public void testPolicyRemoveBeanNulls() throws IOException {
    InputStream inpuStream = HttpProxyTest.class.getResourceAsStream("/test.wsdl");
    String wsdl = IOUtils.toString(inpuStream, StandardCharsets.UTF_8);
    HttpProxy.PolicyRemoveBean policyRemoveBean =
        new HttpProxy.PolicyRemoveBean("8181", "8993", "localhost", "/services");
    Exchange exchange = mock(Exchange.class);
    HttpMessage httpMessage = mock(HttpMessage.class);
    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    when(exchange.getIn()).thenReturn(httpMessage);
    when(exchange.getOut()).thenReturn(httpMessage);

    policyRemoveBean.rewrite(exchange);

    when(httpMessage.getRequest()).thenReturn(httpServletRequest);

    policyRemoveBean.rewrite(exchange);

    when(httpMessage.getHeader("CamelHttpQuery")).thenReturn("wsdl");

    policyRemoveBean.rewrite(exchange);

    when(exchange.getOut()).thenReturn(httpMessage);
    doCallRealMethod().when(httpMessage).setBody(any());
    doCallRealMethod().when(httpMessage).getBody();
    httpMessage.setBody(wsdl);

    policyRemoveBean.rewrite(exchange);

    assertFalse(httpMessage.getBody().toString().contains("https:"));
  }
}
