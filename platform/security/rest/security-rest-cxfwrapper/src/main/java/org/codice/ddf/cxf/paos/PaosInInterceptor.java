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

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.HttpUnsuccessfulResponseHandler;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import ddf.security.liberty.paos.impl.ResponseBuilder;
import ddf.security.liberty.paos.impl.ResponseImpl;
import ddf.security.samlp.impl.SamlProtocol;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPPart;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.util.DOM2Writer;
import org.codice.ddf.platform.util.TemporaryFileBackedOutputStream;
import org.codice.ddf.security.jaxrs.SamlSecurity;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.IDPEntry;
import org.opensaml.saml.saml2.core.IDPList;
import org.opensaml.saml.saml2.ecp.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class PaosInInterceptor extends AbstractPhaseInterceptor<Message> {

  public static final Logger LOGGER = LoggerFactory.getLogger(PaosInInterceptor.class);

  public static final String RELAY_STATE = "RelayState";

  public static final String REQUEST = "Request";

  public static final String RESPONSE = "Response";

  public static final String ASSERTION_CONSUMER_SERVICE_URL = "AssertionConsumerServiceURL";

  public static final String RESPONSE_CONSUMER_URL = "responseConsumerURL";

  public static final String URN_OASIS_NAMES_TC_SAML_2_0_PROFILES_SSO_ECP =
      "urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp";

  public static final String MESSAGE_ID = "messageID";

  public static final String ECP_RESPONSE = "ecp:Response";

  public static final String BASIC = "BASIC";

  public static final String SAML = "SAML";

  public static final String TEXT_XML = "text/xml";

  public static final String SOAP_ACTION = "SOAPAction";

  public static final String HTTP_WWW_OASIS_OPEN_ORG_COMMITTEES_SECURITY =
      "http://www.oasis-open.org/committees/security";

  public static final String URN_LIBERTY_PAOS_2003_08 = "urn:liberty:paos:2003-08";

  public static final String APPLICATION_VND_PAOS_XML = "application/vnd.paos+xml";

  private String soapMessage;

  private String soapfaultMessage;

  private String securityHeader;

  private String usernameToken;

  private SamlSecurity samlSecurity;

  public PaosInInterceptor(String phase, SamlSecurity samlSecurity) {
    super(phase);
    try (InputStream soapMessageStream =
            PaosInInterceptor.class.getResourceAsStream("/templates/soap.handlebars");
        InputStream soapfaultMessageStream =
            PaosInInterceptor.class.getResourceAsStream("/templates/soapfault.handlebars");
        InputStream securityHeaderStream =
            PaosInInterceptor.class.getResourceAsStream("/templates/security.handlebars");
        InputStream userTokenStream =
            PaosInInterceptor.class.getResourceAsStream("/templates/username.handlebars")) {
      soapMessage = IOUtils.toString(soapMessageStream);
      soapfaultMessage = IOUtils.toString(soapfaultMessageStream);
      securityHeader = IOUtils.toString(securityHeaderStream);
      usernameToken = IOUtils.toString(userTokenStream);
      this.samlSecurity = samlSecurity;
    } catch (IOException e) {
      LOGGER.info("Unable to load templates for PAOS");
    }
  }

  @Override
  public void handleMessage(Message message) throws Fault {
    List authHeader =
        (List)
            ((Map) message.getExchange().getOutMessage().get(Message.PROTOCOL_HEADERS))
                .get("Authorization");
    String authorization = null;
    if (authHeader != null && authHeader.size() > 0) {
      authorization = (String) authHeader.get(0);
    }
    InputStream content = message.getContent(InputStream.class);
    String contentType = (String) message.get(Message.CONTENT_TYPE);
    if (contentType == null || !contentType.contains(APPLICATION_VND_PAOS_XML)) {
      return;
    }
    try {
      SOAPPart soapMessage =
          SamlProtocol.parseSoapMessage(IOUtils.toString(content, Charset.forName("UTF-8")));
      Iterator iterator = soapMessage.getEnvelope().getHeader().examineAllHeaderElements();
      IDPEntry idpEntry = null;
      String relayState = "";
      String responseConsumerURL = "";
      String messageId = "";
      while (iterator.hasNext()) {
        Element soapHeaderElement = (SOAPHeaderElement) iterator.next();
        if (RELAY_STATE.equals(soapHeaderElement.getLocalName())) {
          relayState = DOM2Writer.nodeToString(soapHeaderElement);
        } else if (REQUEST.equals(soapHeaderElement.getLocalName())
            && soapHeaderElement
                .getNamespaceURI()
                .equals(URN_OASIS_NAMES_TC_SAML_2_0_PROFILES_SSO_ECP)) {
          try {
            soapHeaderElement = SamlProtocol.convertDomImplementation(soapHeaderElement);
            Request ecpRequest = (Request) OpenSAMLUtil.fromDom(soapHeaderElement);
            IDPList idpList = ecpRequest.getIDPList();
            if (idpList == null) {
              throw new Fault(
                  new AccessDeniedException(
                      "Unable to complete SAML ECP connection. Unable to determine IdP server."));
            }
            List<IDPEntry> idpEntrys = idpList.getIDPEntrys();
            if (idpEntrys == null || idpEntrys.size() == 0) {
              throw new Fault(
                  new AccessDeniedException(
                      "Unable to complete SAML ECP connection. Unable to determine IdP server."));
            }
            // choose the right entry, probably need to do something better than select the first
            // one
            // but the spec doesn't specify how this is supposed to be done
            idpEntry = idpEntrys.get(0);
          } catch (WSSecurityException e) {
            // TODO figure out IdP alternatively
            LOGGER.info(
                "Unable to determine IdP appropriately. ECP connection will fail. SP may be incorrectly configured. Contact the administrator for the remote system.");
          }
        } else if (REQUEST.equals(soapHeaderElement.getLocalName())
            && soapHeaderElement.getNamespaceURI().equals(URN_LIBERTY_PAOS_2003_08)) {
          responseConsumerURL = soapHeaderElement.getAttribute(RESPONSE_CONSUMER_URL);
          messageId = soapHeaderElement.getAttribute(MESSAGE_ID);
        }
      }
      if (idpEntry == null) {
        throw new Fault(
            new AccessDeniedException(
                "Unable to complete SAML ECP connection. Unable to determine IdP server."));
      }
      String token = createToken(authorization);
      checkAuthnRequest(soapMessage);
      Element authnRequestElement =
          SamlProtocol.getDomElement(soapMessage.getEnvelope().getBody().getFirstChild());
      String loc = idpEntry.getLoc();
      String soapRequest = buildSoapMessage(token, relayState, authnRequestElement, null);
      HttpResponseWrapper httpResponse = getHttpResponse(loc, soapRequest, null);
      InputStream httpResponseContent = httpResponse.content;
      SOAPPart idpSoapResponse =
          SamlProtocol.parseSoapMessage(
              IOUtils.toString(httpResponseContent, Charset.forName("UTF-8")));
      Iterator responseHeaderElements =
          idpSoapResponse.getEnvelope().getHeader().examineAllHeaderElements();
      String newRelayState = "";
      while (responseHeaderElements.hasNext()) {
        SOAPHeaderElement soapHeaderElement = (SOAPHeaderElement) responseHeaderElements.next();
        if (RESPONSE.equals(soapHeaderElement.getLocalName())) {
          String assertionConsumerServiceURL =
              soapHeaderElement.getAttribute(ASSERTION_CONSUMER_SERVICE_URL);
          if (!responseConsumerURL.equals(assertionConsumerServiceURL)) {
            String soapFault =
                buildSoapFault(
                    ECP_RESPONSE,
                    "The responseConsumerURL does not match the assertionConsumerServiceURL.");
            httpResponse = getHttpResponse(responseConsumerURL, soapFault, null);
            message.setContent(InputStream.class, httpResponse.content);
            return;
          }
        } else if (RELAY_STATE.equals(soapHeaderElement.getLocalName())) {
          newRelayState = DOM2Writer.nodeToString(soapHeaderElement);
          if (StringUtils.isNotEmpty(relayState) && !relayState.equals(newRelayState)) {
            LOGGER.debug("RelayState does not match between ECP request and response");
          }
          if (StringUtils.isNotEmpty(relayState)) {
            newRelayState = relayState;
          }
        }
      }
      checkSamlpResponse(idpSoapResponse);
      Element samlpResponseElement =
          SamlProtocol.getDomElement(idpSoapResponse.getEnvelope().getBody().getFirstChild());
      XMLObject paosResponse = null;
      if (StringUtils.isNotEmpty(messageId)) {
        paosResponse = getPaosResponse(messageId);
      }
      String soapResponse =
          buildSoapMessage(null, newRelayState, samlpResponseElement, paosResponse);
      httpResponse =
          getHttpResponse(responseConsumerURL, soapResponse, message.getExchange().getOutMessage());
      if (httpResponse.statusCode < 400) {
        httpResponseContent = httpResponse.content;
        message.setContent(InputStream.class, httpResponseContent);
        Map<String, List<String>> headers = new HashMap<>();
        message.put(Message.PROTOCOL_HEADERS, headers);
        httpResponse.headers.forEach(
            (entry) ->
                headers.put(
                    entry.getKey(),
                    // CXF Expects pairs of <String, List<String>>
                    entry.getValue() instanceof List
                        ? ((List<Object>) entry.getValue())
                            .stream()
                            .map(String::valueOf)
                            .collect(Collectors.toList())
                        : Lists.newArrayList(String.valueOf(entry.getValue()))));

      } else {
        throw new Fault(
            new AccessDeniedException("Unable to complete SAML ECP connection due to an error."));
      }

    } catch (IOException e) {
      LOGGER.debug("Error encountered while performing ECP handshake.", e);
    } catch (XMLStreamException | SOAPException e) {
      throw new Fault(
          new AccessDeniedException(
              "Unable to complete SAML ECP connection. The server's response was not in the correct format."));
    } catch (WSSecurityException e) {
      throw new Fault(
          new AccessDeniedException(
              "Unable to complete SAML ECP connection. Unable to send SOAP request messages."));
    }
  }

  private boolean isRedirectable(String method) {
    return "HEAD".equals(method) || "GET".equals(method) || "CONNECT".equals(method);
  }

  private String createToken(String authorization) throws IOException {
    String token = null;
    if (authorization != null) {
      if (StringUtils.startsWithIgnoreCase(authorization, BASIC)) {
        byte[] decode = Base64.getDecoder().decode(authorization.split("\\s")[1]);
        if (decode != null) {
          String userPass = new String(decode, StandardCharsets.UTF_8);
          String[] authComponents = userPass.split(":");
          if (authComponents.length == 2) {
            token = getUsernameToken(authComponents[0], authComponents[1]);
          } else if ((authComponents.length == 1) && (userPass.endsWith(":"))) {
            token = getUsernameToken(authComponents[0], "");
          }
        }
      } else if (StringUtils.startsWithIgnoreCase(authorization, SAML)) {
        token = samlSecurity.inflateBase64(authorization.split("\\s")[1]);
      }
    }
    return token;
  }

  @VisibleForTesting
  HttpResponseWrapper getHttpResponse(
      String responseConsumerURL, String soapResponse, Message message) throws IOException {
    // This used to use the ApacheHttpTransport which appeared to not work with 2 way TLS auth but
    // this one does
    HttpTransport httpTransport = new NetHttpTransport();
    HttpContent httpContent = new ByteArrayContent(TEXT_XML, soapResponse.getBytes("UTF-8"));
    HttpRequest httpRequest =
        httpTransport
            .createRequestFactory()
            .buildPostRequest(new GenericUrl(responseConsumerURL), httpContent);
    HttpUnsuccessfulResponseHandler httpUnsuccessfulResponseHandler =
        getHttpUnsuccessfulResponseHandler(message);
    httpRequest.setUnsuccessfulResponseHandler(httpUnsuccessfulResponseHandler);
    httpRequest.getHeaders().put(SOAP_ACTION, HTTP_WWW_OASIS_OPEN_ORG_COMMITTEES_SECURITY);
    // has 20 second timeout by default
    HttpResponse httpResponse = httpRequest.execute();
    HttpResponseWrapper httpResponseWrapper = new HttpResponseWrapper();
    httpResponseWrapper.statusCode = httpResponse.getStatusCode();
    httpResponseWrapper.content = httpResponse.getContent();
    httpResponseWrapper.headers = httpResponse.getHeaders().entrySet();
    return httpResponseWrapper;
  }

  @VisibleForTesting
  HttpUnsuccessfulResponseHandler getHttpUnsuccessfulResponseHandler(Message message) {
    return (request, response, supportsRetry) -> {
      String redirectLocation = response.getHeaders().getLocation();
      if (isRedirect(request, response, redirectLocation)) {
        String method = (String) message.get(Message.HTTP_REQUEST_METHOD);
        HttpContent content = null;
        if (!isRedirectable(method)) {
          try (TemporaryFileBackedOutputStream tfbos = new TemporaryFileBackedOutputStream()) {
            message.setContent(OutputStream.class, tfbos);
            BodyWriter bodyWriter = new BodyWriter();
            bodyWriter.handleMessage(message);
            ByteSource byteSource = tfbos.asByteSource();
            content =
                new InputStreamContent(
                        (String) message.get(Message.CONTENT_TYPE), byteSource.openStream())
                    .setLength(byteSource.size());
          }
        }

        // resolve the redirect location relative to the current location
        request.setUrl(new GenericUrl(request.getUrl().toURL(redirectLocation)));
        request.setRequestMethod(method);
        request.setContent(content);
        // remove Authorization and If-* headers
        request.getHeaders().setAuthorization((String) null);
        request.getHeaders().setIfMatch(null);
        request.getHeaders().setIfNoneMatch(null);
        request.getHeaders().setIfModifiedSince(null);
        request.getHeaders().setIfUnmodifiedSince(null);
        request.getHeaders().setIfRange(null);
        request
            .getHeaders()
            .setCookie((String) ((List) response.getHeaders().get("set-cookie")).get(0));

        Map<String, List<String>> headers =
            (Map<String, List<String>>) message.get(Message.PROTOCOL_HEADERS);
        headers.forEach((key, value) -> request.getHeaders().set(key, value));
        return true;
      }
      return false;
    };
  }

  @VisibleForTesting
  boolean isRedirect(HttpRequest request, HttpResponse response, String redirectLocation) {
    return request.getFollowRedirects()
        && HttpStatusCodes.isRedirect(response.getStatusCode())
        && redirectLocation != null;
  }

  @Override
  public void handleFault(Message message) {
    LOGGER.debug("PAOS interceptor fault method called.");
  }

  private String buildSoapMessage(
      String token, String relayState, Element body, XMLObject paosResponse)
      throws WSSecurityException {
    String updatedMessage = soapMessage.replace("{{XmlBody}}", DOM2Writer.nodeToString(body));
    if (token != null) {
      String updatedSecHdr = securityHeader.replace("{{token}}", token);
      updatedMessage = updatedMessage.replace("{{WSSecurity}}", updatedSecHdr);
    } else {
      updatedMessage = updatedMessage.replace("{{WSSecurity}}", "");
    }
    if (paosResponse != null) {
      updatedMessage =
          updatedMessage.replace("{{PAOSResponse}}", convertXmlObjectToString(paosResponse));
    } else {
      updatedMessage = updatedMessage.replace("{{PAOSResponse}}", "");
    }
    updatedMessage = updatedMessage.replace("{{ECPRelayState}}", relayState);
    return updatedMessage;
  }

  private String buildSoapFault(String faultcode, String faultstring) {
    String updatedFault = soapfaultMessage.replace("{{faultcode}}", faultcode);
    updatedFault = updatedFault.replace("{{faultstring}}", faultstring);
    return updatedFault;
  }

  private String convertXmlObjectToString(XMLObject xmlObject) throws WSSecurityException {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(PaosInInterceptor.class.getClassLoader());
    try {
      Document doc = DOMUtils.createDocument();
      doc.appendChild(doc.createElement("root"));

      Element requestElement = OpenSAMLUtil.toDom(xmlObject, null);

      return DOM2Writer.nodeToString(requestElement);
    } finally {
      Thread.currentThread().setContextClassLoader(contextClassLoader);
    }
  }

  private XMLObject getPaosResponse(String messageId) {
    ResponseBuilder responseBuilder = new ResponseBuilder();
    ResponseImpl response = responseBuilder.buildObject();
    response.setRefToMessageID(messageId);
    return response;
  }

  private void checkAuthnRequest(SOAPPart soapRequest) throws IOException {
    XMLObject authnXmlObj = null;
    try {
      Node node = soapRequest.getEnvelope().getBody().getFirstChild();
      authnXmlObj = SamlProtocol.getXmlObjectFromNode(node);
    } catch (WSSecurityException | SOAPException | XMLStreamException ex) {
      throw new IOException("Unable to convert AuthnRequest document to XMLObject.");
    }
    if (authnXmlObj == null) {
      throw new IOException("AuthnRequest object is not Found.");
    }
    if (!(authnXmlObj instanceof AuthnRequest)) {
      throw new IOException("SAMLRequest object is not AuthnRequest.");
    }
  }

  private void checkSamlpResponse(SOAPPart soapRequest) throws IOException {
    XMLObject responseXmlObj = null;
    try {
      Node node = soapRequest.getEnvelope().getBody().getFirstChild();
      responseXmlObj = SamlProtocol.getXmlObjectFromNode(node);
    } catch (WSSecurityException | SOAPException | XMLStreamException ex) {
      throw new IOException("Unable to convert Response document to XMLObject.");
    }
    if (responseXmlObj == null) {
      throw new IOException("Response object is not Found.");
    }
    if (!(responseXmlObj instanceof org.opensaml.saml.saml2.core.Response)) {
      throw new IOException("SAMLRequest object is not org.opensaml.saml.saml2.core.Response.");
    }
  }

  private String getUsernameToken(String username, String password) {
    String updatedToken = usernameToken.replace("{{username}}", username);
    updatedToken = updatedToken.replace("{{password}}", password);
    return updatedToken;
  }

  static class HttpResponseWrapper {
    int statusCode;

    InputStream content;

    Set<Entry<String, Object>> headers;
  }
}
