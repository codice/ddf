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
 *
 * <p>
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ddf.security.sts;

import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.cxf.ws.security.trust.STSUtils;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.token.X509Security;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.apache.wss4j.dom.processor.Processor;
import org.apache.wss4j.dom.processor.SAMLTokenProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Some unit tests for the CXF STSClient Issue Binding. */
public class StsIssueTest {

  private static final String SAML2_TOKEN_TYPE =
      "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0";

  // Might need later.
  // private static final String PUBLIC_KEY_KEYTYPE =
  // "http://docs.oasis-open.org/ws-sx/ws-trust/200512/PublicKey";

  private static final String BEARER_KEYTYPE =
      "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Bearer";

  private static final String CAS_ID = "#CAS";

  private static final String IDENTITY_URI = "http://schemas.xmlsoap.org/ws/2005/05/identity";

  private static final String WST = "wst";

  private static final String IC = "ic";

  private static final String DIALECT = "Dialect";

  private static final String CLAIMS = "Claims";

  private static final String CLAIM_TYPE = "ClaimType";

  private static final String URI = "Uri";

  private static final Logger LOGGER = LoggerFactory.getLogger(StsIssueTest.class);

  /** Test the Username Token */
  public void testBearerUsernameTokenSaml2(StsPortTypes portType) throws Exception {
    SpringBusFactory bf = new SpringBusFactory();
    URL busFile = StsIssueTest.class.getResource("/cxf-client.xml");

    Bus bus = bf.createBus(busFile.toString());
    SpringBusFactory.setDefaultBus(bus);
    SpringBusFactory.setThreadDefaultBus(bus);

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.newDocument();

    // Create a Username Token
    UsernameToken oboToken = new UsernameToken(false, doc, WSConstants.PASSWORD_TEXT);
    oboToken.setName("pangerer");
    oboToken.setPassword("password");

    // Build the Claims object
    W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
    writer.writeStartElement(WST, CLAIMS, STSUtils.WST_NS_05_12);
    writer.writeNamespace(WST, STSUtils.WST_NS_05_12);
    writer.writeNamespace(IC, IDENTITY_URI);
    writer.writeAttribute(DIALECT, IDENTITY_URI);

    // Add the Role claim
    writer.writeStartElement(IC, CLAIM_TYPE, IDENTITY_URI);
    // writer.writeAttribute("Uri",
    // "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role");
    writer.writeAttribute(URI, "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/uid");
    writer.writeEndElement();

    Element claims = writer.getDocument().getDocumentElement();

    // Get a token
    SecurityToken token =
        requestSecurityToken(
            SAML2_TOKEN_TYPE,
            BEARER_KEYTYPE,
            oboToken.getElement(),
            bus,
            StsAddresses.valueOf(portType.toString()).toString(),
            WsdlLocations.valueOf(portType.toString()).toString(),
            EndPoints.valueOf(portType.toString()).toString(),
            claims);

    if (token != null) {
      validateSecurityToken(token);
    }
    bus.shutdown(true);
  }

  /** Test the Web SSO Token */
  public void testBearerWebSsoTokenSaml2(StsPortTypes portType) throws Exception {
    SpringBusFactory bf = new SpringBusFactory();
    URL busFile = StsIssueTest.class.getResource("/cxf-client.xml");

    Bus bus = bf.createBus(busFile.toString());
    SpringBusFactory.setDefaultBus(bus);
    SpringBusFactory.setThreadDefaultBus(bus);

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.newDocument();

    // Create a Username Token
    UsernameToken oboToken = new UsernameToken(false, doc, WSConstants.PASSWORD_TEXT);

    // Workout the details of how to fill out the username token
    // ID - the Key that tells the validator its an SSO token
    // Name - the SSO ticket
    oboToken.setID(CAS_ID);
    oboToken.setName("ST-098ASDF13245WERT");

    // Build the Claims object
    W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
    writer.writeStartElement(WST, CLAIMS, STSUtils.WST_NS_05_12);
    writer.writeNamespace(WST, STSUtils.WST_NS_05_12);
    writer.writeNamespace(IC, IDENTITY_URI);
    writer.writeAttribute(DIALECT, IDENTITY_URI);

    // Add the Role claim
    writer.writeStartElement(IC, CLAIM_TYPE, IDENTITY_URI);
    // writer.writeAttribute("Uri",
    // "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role");
    writer.writeAttribute(URI, "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/uid");
    writer.writeEndElement();

    Element claims = writer.getDocument().getDocumentElement();

    // Get a token
    SecurityToken token =
        requestSecurityToken(
            SAML2_TOKEN_TYPE,
            BEARER_KEYTYPE,
            oboToken.getElement(),
            bus,
            StsAddresses.valueOf(portType.toString()).toString(),
            WsdlLocations.valueOf(portType.toString()).toString(),
            EndPoints.valueOf(portType.toString()).toString(),
            claims);

    if (token != null) {
      validateSecurityToken(token);
    }
    bus.shutdown(true);
  }

  /** Test the User PKI Token */
  public void testBearerPkiTokenSaml2(StsPortTypes portType) throws Exception {
    SpringBusFactory bf = new SpringBusFactory();
    URL busFile = StsIssueTest.class.getResource("/cxf-client.xml");

    Bus bus = bf.createBus(busFile.toString());
    SpringBusFactory.setDefaultBus(bus);
    SpringBusFactory.setThreadDefaultBus(bus);

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.newDocument();

    // Build the Claims object
    W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
    writer.writeStartElement(WST, CLAIMS, STSUtils.WST_NS_05_12);
    writer.writeNamespace(WST, STSUtils.WST_NS_05_12);
    writer.writeNamespace(IC, IDENTITY_URI);
    writer.writeAttribute(DIALECT, IDENTITY_URI);

    // Add the Role claim
    writer.writeStartElement(IC, CLAIM_TYPE, IDENTITY_URI);
    writer.writeAttribute("URI", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role");
    writer.writeEndElement();

    Element claims = writer.getDocument().getDocumentElement();

    // Alerternatively we can use a certificate to request a SAML
    X509Security oboToken = new X509Security(doc);
    Crypto crypto = CryptoFactory.getInstance("clientKeystore.properties");
    CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
    cryptoType.setAlias("client");
    X509Certificate[] certs = crypto.getX509Certificates(cryptoType);
    if (null != certs) {
      oboToken.setX509Certificate(certs[0]);

      // Get a token
      SecurityToken token =
          requestSecurityToken(
              SAML2_TOKEN_TYPE,
              BEARER_KEYTYPE,
              oboToken.getElement(),
              bus,
              StsAddresses.valueOf(portType.toString()).toString(),
              WsdlLocations.valueOf(portType.toString()).toString(),
              EndPoints.valueOf(portType.toString()).toString(),
              claims);
      if (token != null) {
        validateSecurityToken(token);
      }
    }

    bus.shutdown(true);
  }

  private void validateSecurityToken(SecurityToken token) {
    assert (SAML2_TOKEN_TYPE.equals(token.getTokenType()));
    assert (token.getToken() != null);

    // Process the token
    List<WSSecurityEngineResult> results;
    try {
      results = processToken(token);

      assert (results != null && results.size() == 1);
      SamlAssertionWrapper assertion =
          (SamlAssertionWrapper) results.get(0).get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
      assert (assertion != null);
      assert (assertion.getSaml1() == null && assertion.getSaml2() != null);
      assert (assertion.isSigned());

      List<String> methods = assertion.getConfirmationMethods();
      String confirmMethod = null;
      if (methods != null && methods.size() > 0) {
        confirmMethod = methods.get(0);
      }
      assert (confirmMethod != null);
    } catch (WSSecurityException e) {
      LOGGER.info("Error validating the SecurityToken.", e);
    }
  }

  private SecurityToken requestSecurityToken(
      String tokenType,
      String keyType,
      Element supportingToken,
      Bus bus,
      String endpointAddress,
      String wsdlLocation,
      String endpointName,
      Element claims) {
    STSClient stsClient = new STSClient(bus);

    stsClient.setWsdlLocation(wsdlLocation);
    stsClient.setEndpointName(endpointName);
    stsClient.setServiceName(
        "{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");

    Map<String, Object> properties = new HashMap<String, Object>();

    // XXX - Not sure how this is used - doesn't show up in the payload. -
    // Should this be "client"?
    // properties.put(SecurityConstants.USERNAME, "kcwire");
    // properties
    // .put(SecurityConstants.CALLBACK_HANDLER,
    // "org.apache.cxf.ws.security.trust.delegation.WSSUsernameCallbackHandler");
    properties.put(SecurityConstants.IS_BSP_COMPLIANT, "false");

    // Not sure if we will ever do this kind of Public key
    // if (PUBLIC_KEY_KEYTYPE.equals(keyType)) {
    // properties.put(SecurityConstants.STS_TOKEN_USERNAME, "tokenissuer");
    // properties.put(SecurityConstants.STS_TOKEN_PROPERTIES,
    // "clientKeystore.properties");
    // stsClient.setUseCertificateForConfirmationKeyInfo(true);
    // }
    if (supportingToken != null) {
      stsClient.setOnBehalfOf(supportingToken);
    }

    stsClient.setClaims(claims);
    stsClient.setProperties(properties);
    stsClient.setTokenType(tokenType);
    stsClient.setKeyType(keyType);

    SecurityToken token = null;
    try {
      token = stsClient.requestSecurityToken(endpointAddress);
    } catch (Exception e) {
      LOGGER.info("Error requesting the SecurityToken.", e);
    }
    return token;
  }

  /** Method to validate the retrieved token. */
  private List<WSSecurityEngineResult> processToken(SecurityToken token)
      throws WSSecurityException {
    RequestData requestData = new RequestData();
    WSSConfig wssConfig = WSSConfig.getNewInstance();
    requestData.setWssConfig(wssConfig);
    /*DDF-733
    CallbackHandler callbackHandler = new CommonCallbackHandler();
    requestData.setCallbackHandler(callbackHandler);
    */
    Crypto crypto = CryptoFactory.getInstance("serverKeystore.properties");
    requestData.setDecCrypto(crypto);
    requestData.setSigVerCrypto(crypto);

    Processor processor = new SAMLTokenProcessor();
    return processor.handleToken(
        token.getToken(), requestData, new WSDocInfo(token.getToken().getOwnerDocument()));
  }

  // Enum defining the Port Types
  public enum StsPortTypes {
    TRANSPORT,
    UT_ENCRYPTED,
    X509,
    UT;
  }

  // Enum defining the Wsdl Locations
  public enum WsdlLocations {
    TRANSPORT("https://localhost:8993/services/SecurityTokenService/Transport?wsdl"),
    UT_ENCRYPTED("https://localhost:8993/services/SecurityTokenService/UTEncrypted?wsdl"),
    X509("https://localhost:8993/services/SecurityTokenService/X509?wsdl"),
    UT("https://localhost:8993/services/SecurityTokenService/UT?wsdl");

    private String value;

    private WsdlLocations(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return this.value;
    }
  }

  // Enum defining the STS Endpoints
  public enum EndPoints {
    TRANSPORT("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}Transport_Port"),
    UT_ENCRYPTED("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}UTEncrypted_Port"),
    X509("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}X509_Port"),
    UT("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}UT_Port");

    private String value;

    private EndPoints(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return this.value;
    }
  }

  // Enum defining the STS Addresses
  public enum StsAddresses {
    TRANSPORT("http://localhost:8993/services/SecurityTokenServices/Transport"),
    UT_ENCRYPTED("https://localhost:8993/services/SecurityTokenServices/UTEncrypted"),
    X509("https://localhost:8993/services/SecurityTokenServices/X509"),
    UT("https://localhost:8993/services/SecurityTokenServices/UT");

    private String value;

    private StsAddresses(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return this.value;
    }
  }
}
