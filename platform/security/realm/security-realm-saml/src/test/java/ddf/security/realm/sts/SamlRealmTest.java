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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.SAMLAuthenticationToken;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class SamlRealmTest {

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
    SamlRealm realm = new SamlRealm();
    org.apache.shiro.authc.AuthenticationToken authenticationToken =
        mock(SAMLAuthenticationToken.class);
    when(authenticationToken.getCredentials()).thenReturn("creds");
    boolean supports = realm.supports(authenticationToken);
    assertTrue(supports);

    authenticationToken = mock(SAMLAuthenticationToken.class);
    when(authenticationToken.getCredentials()).thenReturn(null);
    supports = realm.supports(authenticationToken);
    assertFalse(supports);

    authenticationToken = mock(SAMLAuthenticationToken.class);
    when(authenticationToken.getCredentials()).thenReturn("creds");
    supports = realm.supports(authenticationToken);
    assertTrue(supports);

    authenticationToken = mock(SAMLAuthenticationToken.class);
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
    SamlRealm realm = new SamlRealm();
    Element issuedAssertion = this.readDocument("/saml.xml").getDocumentElement();
    String assertionId = issuedAssertion.getAttributeNodeNS(null, "ID").getNodeValue();
    SecurityToken token = new SecurityToken(assertionId, issuedAssertion, null);
    org.apache.shiro.authc.AuthenticationToken authenticationToken =
        mock(SAMLAuthenticationToken.class);
    when(authenticationToken.getCredentials()).thenReturn(token);

    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(authenticationToken);
    assertNotNull(authenticationInfo.getCredentials());
    assertNotNull(authenticationInfo.getPrincipals());
  }

  @Ignore
  @Test
  public void testDoGetAuthenticationInfoBase() {
    SamlRealm realm = new SamlRealm();

    BaseAuthenticationToken authenticationToken = mock(BaseAuthenticationToken.class);
    when(authenticationToken.getCredentialsAsString()).thenReturn("creds");

    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(authenticationToken);

    assertNotNull(authenticationInfo.getCredentials());
    assertNotNull(authenticationInfo.getPrincipals());
  }

  private Document readDocument(String name)
      throws SAXException, IOException, ParserConfigurationException {
    InputStream inStream = getClass().getResourceAsStream(name);
    return readXml(inStream);
  }
}
