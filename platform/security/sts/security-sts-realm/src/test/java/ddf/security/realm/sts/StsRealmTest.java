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
package ddf.security.realm.sts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.codice.ddf.security.handler.api.SAMLAuthenticationToken;
import org.codice.ddf.security.handler.api.STSAuthenticationToken;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class StsRealmTest {

  @BeforeClass
  public static void setupClass() throws Exception {
    OpenSAMLUtil.initSamlEngine();
  }

  private static Document readXml(InputStream is)
      throws SAXException, IOException, ParserConfigurationException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    dbf.setValidating(false);
    dbf.setIgnoringComments(false);
    dbf.setIgnoringElementContentWhitespace(true);
    dbf.setNamespaceAware(true);

    DocumentBuilder db = dbf.newDocumentBuilder();
    db.setEntityResolver(new DOMUtils.NullResolver());
    return db.parse(is);
  }

  @Test
  public void testSupports() {
    StsRealm realm = new StsRealm();
    AuthenticationToken authenticationToken = mock(SAMLAuthenticationToken.class);
    when(authenticationToken.getCredentials()).thenReturn("creds");
    boolean supports = realm.supports(authenticationToken);
    assertTrue(supports);

    authenticationToken = mock(SAMLAuthenticationToken.class);
    when(authenticationToken.getCredentials()).thenReturn(null);
    supports = realm.supports(authenticationToken);
    assertFalse(supports);

    authenticationToken = mock(STSAuthenticationToken.class);
    when(authenticationToken.getCredentials()).thenReturn("creds");
    supports = realm.supports(authenticationToken);
    assertTrue(supports);

    authenticationToken = mock(STSAuthenticationToken.class);
    when(authenticationToken.getCredentials()).thenReturn(null);
    supports = realm.supports(authenticationToken);
    assertFalse(supports);

    supports = realm.supports(null);
    assertFalse(supports);
  }

  @Ignore
  @Test
  public void testDoGetAuthenticationInfoSAML()
      throws ParserConfigurationException, SAXException, IOException {
    StsRealm realm =
        new StsRealm() {
          protected SecurityToken renewSecurityToken(SecurityToken securityToken) {
            return securityToken;
          }

          protected STSClient configureStsClient() {
            return null;
          }
        };
    Element issuedAssertion = this.readDocument("/saml.xml").getDocumentElement();
    String assertionId = issuedAssertion.getAttributeNodeNS(null, "ID").getNodeValue();
    SecurityToken token = new SecurityToken(assertionId, issuedAssertion, null);
    AuthenticationToken authenticationToken = mock(SAMLAuthenticationToken.class);
    when(authenticationToken.getCredentials()).thenReturn(token);

    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(authenticationToken);
    assertNotNull(authenticationInfo.getCredentials());
    assertNotNull(authenticationInfo.getPrincipals());
  }

  @Ignore
  @Test
  public void testDoGetAuthenticationInfoBase()
      throws ParserConfigurationException, SAXException, IOException {
    Element issuedAssertion = this.readDocument("/saml.xml").getDocumentElement();
    String assertionId = issuedAssertion.getAttributeNodeNS(null, "ID").getNodeValue();
    final SecurityToken token = new SecurityToken(assertionId, issuedAssertion, null);
    StsRealm realm =
        new StsRealm() {
          protected SecurityToken requestSecurityToken(Object obj) {
            return token;
          }

          protected STSClient configureStsClient() {
            return null;
          }
        };

    STSAuthenticationToken authenticationToken = mock(STSAuthenticationToken.class);
    when(authenticationToken.getCredentialsAsString()).thenReturn("creds");

    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(authenticationToken);

    assertNotNull(authenticationInfo.getCredentials());
    assertNotNull(authenticationInfo.getPrincipals());
  }

  @Test
  public void testCreateClaimsElement() {
    StsRealm stsRealm = new StsRealm();
    stsRealm.setClaims(Arrays.asList("claim1", "claim2", "claim3"));
    ContextPolicyManager contextPolicyManager = mock(ContextPolicyManager.class);
    ContextPolicy policy1 = mock(ContextPolicy.class);
    ContextPolicy policy2 = mock(ContextPolicy.class);
    when(policy1.getAllowedAttributeNames()).thenReturn(Arrays.asList("claim4", "claim5"));
    when(policy2.getAllowedAttributeNames()).thenReturn(Arrays.asList("claim6", "claim7"));
    when(contextPolicyManager.getAllContextPolicies()).thenReturn(Arrays.asList(policy1, policy2));
    stsRealm.setContextPolicyManager(contextPolicyManager);

    Element claimsElement = stsRealm.createClaimsElement();
    assertNotNull(claimsElement);
    NodeList childNodes = claimsElement.getChildNodes();
    assertEquals("claim1", childNodes.item(0).getAttributes().item(1).getTextContent());
    assertEquals("claim2", childNodes.item(1).getAttributes().item(1).getTextContent());
    assertEquals("claim3", childNodes.item(2).getAttributes().item(1).getTextContent());
    assertEquals("claim4", childNodes.item(3).getAttributes().item(1).getTextContent());
    assertEquals("claim5", childNodes.item(4).getAttributes().item(1).getTextContent());
    assertEquals("claim6", childNodes.item(5).getAttributes().item(1).getTextContent());
    assertEquals("claim7", childNodes.item(6).getAttributes().item(1).getTextContent());
  }

  @Test
  public void testAddClaimsAsString() {
    StsRealm stsRealm = new StsRealm();
    assertEquals(0, stsRealm.getClaims().size());
    stsRealm.setClaims("claim1,claim2,claim3");
    assertEquals(3, stsRealm.getClaims().size());
  }

  private Document readDocument(String name)
      throws SAXException, IOException, ParserConfigurationException {
    InputStream inStream = getClass().getResourceAsStream(name);
    return readXml(inStream);
  }
}
