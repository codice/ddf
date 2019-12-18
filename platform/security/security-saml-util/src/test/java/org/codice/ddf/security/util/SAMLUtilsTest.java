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
package org.codice.ddf.security.util;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.security.service.SecurityServiceException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.codice.ddf.platform.util.XMLUtils;
import org.junit.Test;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class SAMLUtilsTest {

  private static final SAMLUtils SAML_UTILS = SAMLUtils.getInstance();

  private static final String SAML_ASSERTION =
      "<saml2:Assertion ID=\"_30a958fd-9283-4169-a65c-f79a33cec296\" IssueInstant=\"2017-12-21T22:41:17.713Z\" Version=\"2.0\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"saml2:AssertionType\"><saml2:Issuer>localhost</saml2:Issuer><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><ds:SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\"/><ds:Reference URI=\"#_30a958fd-9283-4169-a65c-f79a33cec296\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"><ec:InclusiveNamespaces PrefixList=\"xsd\" xmlns:ec=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transform></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/><ds:DigestValue>IVlNpScFYdqhdrg1UplqBicy/JDbxb0ZK/V5KrxcIKw=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>LS6AHvoJZT47zZyEhAFjzbRXWZwSExZ57uESc+hXBjabZ3vWVYvnTh0n0xAD8fgOK2TMHPP0zOPkRk411XEFeC1/5upMDI3x9uRCj2C+Ht0oNqLVNGoPOHd4x3XOnoT/SCHxgBBdPiKfJ1sWDspcvjhsKhaIBoNResVrhdusNFY=</ds:SignatureValue><ds:KeyInfo><ds:X509Data><ds:X509Certificate>MIIC8DCCAlmgAwIBAgIJAIzc4FYrIp9pMA0GCSqGSIb3DQEBCwUAMIGEMQswCQYDVQQGEwJVUzEL\n"
          + "MAkGA1UECBMCQVoxDDAKBgNVBAoTA0RERjEMMAoGA1UECxMDRGV2MRkwFwYDVQQDExBEREYgRGVt\n"
          + "byBSb290IENBMTEwLwYJKoZIhvcNAQkBFiJlbWFpbEFkZHJlc3M9ZGRmcm9vdGNhQGV4YW1wbGUu\n"
          + "b3JnMCAXDTE1MTIxMTE1NDMyM1oYDzIxMTUxMTE3MTU0MzIzWjBwMQswCQYDVQQGEwJVUzELMAkG\n"
          + "A1UECBMCQVoxDDAKBgNVBAoTA0RERjEMMAoGA1UECxMDRGV2MRIwEAYDVQQDEwlsb2NhbGhvc3Qx\n"
          + "JDAiBgkqhkiG9w0BCQEWFWxvY2FsaG9zdEBleGFtcGxlLm9yZzCBnzANBgkqhkiG9w0BAQEFAAOB\n"
          + "jQAwgYkCgYEAx4LI1lsJNmmEdB8HmDwWuAGrVFjNXuKRXD+lUaTPyDHeXcD32zxa0DiZEB5vqfS9\n"
          + "NH3I0E56Rbidg6IQ6r/9hOL9+sjWTPRBsQfWzZwjmcUG61psPc9gbFRK5qltz4BLv4+SWvRMMjgx\n"
          + "HM8+SROnjCU5FD9roJ9Ww2v+ZWAvYJ8CAwEAAaN7MHkwCQYDVR0TBAIwADAsBglghkgBhvhCAQ0E\n"
          + "HxYdT3BlblNTTCBHZW5lcmF0ZWQgQ2VydGlmaWNhdGUwHQYDVR0OBBYEFID3lAgzIEAdGx3RHizs\n"
          + "LcGt4WuwMB8GA1UdIwQYMBaAFOFUx5ffCsK/qV94XjsLK+RIF73GMA0GCSqGSIb3DQEBCwUAA4GB\n"
          + "ACWWsi4WusO5/u1O91obGn8ctFnxVlogBQ/tDZ+neQDxy8YB2J28tztELrRHkaGiCPT4CCKdy0hx\n"
          + "/bG/jSM1ypJnPKrPVrCkYL3Y68pzxvrFNq5NqAFCcBOCNsDNfvCSZ/XHvFyGHIuso5wNVxJyvTdh\n"
          + "Q+vWbnpiX8qr6vTx2Wgw</ds:X509Certificate></ds:X509Data></ds:KeyInfo></ds:Signature><saml2:Subject><saml2:NameID Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:persistent\" NameQualifier=\"http://cxf.apache.org/sts\">admin</saml2:NameID><saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\"/></saml2:Subject><saml2:Conditions NotBefore=\"2017-12-21T22:41:17.714Z\" NotOnOrAfter=\"2017-12-21T23:11:17.714Z\"><saml2:AudienceRestriction><saml2:Audience>https://localhost:8993/services/SecurityTokenService?wsdl</saml2:Audience></saml2:AudienceRestriction></saml2:Conditions><saml2:AuthnStatement AuthnInstant=\"2017-12-21T22:41:17.713Z\" SessionIndex=\"135416475\"><saml2:AuthnContext><saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport</saml2:AuthnContextClassRef></saml2:AuthnContext></saml2:AuthnStatement><saml2:AttributeStatement><saml2:Attribute Name=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue xsi:type=\"xsd:string\">admin@localhost.local</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue xsi:type=\"xsd:string\">guest</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue xsi:type=\"xsd:string\">guest</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue xsi:type=\"xsd:string\">guest</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue xsi:type=\"xsd:string\">group</saml2:AttributeValue><saml2:AttributeValue xsi:type=\"xsd:string\">admin</saml2:AttributeValue><saml2:AttributeValue xsi:type=\"xsd:string\">manager</saml2:AttributeValue><saml2:AttributeValue xsi:type=\"xsd:string\">viewer</saml2:AttributeValue><saml2:AttributeValue xsi:type=\"xsd:string\">system-admin</saml2:AttributeValue><saml2:AttributeValue xsi:type=\"xsd:string\">systembundles</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue xsi:type=\"xsd:string\">admin</saml2:AttributeValue></saml2:Attribute></saml2:AttributeStatement></saml2:Assertion>";

  private static final String BAD_SAML_ASSERTION =
      "<saml2:Assertion ID=\"_30a958fd-9283-4169-a65c-f79a33cec296\" IssueInstant=\"2017-12-21T22:41:17.713Z\" Version=\"2.0\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"saml2:AssertionType\"><saml2:Issuer>localhost</saml2:Issuer><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><ds:SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\"/><ds:Reference URI=\"#_30a958fd-9283-4169-a65c-f79a33cec296\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"><ec:InclusiveNamespaces PrefixList=\"xsd\" xmlns:ec=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transform></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/><ds:DigestValue>IVlNpScFYdqhdrg1UplqBicy/JDbxb0ZK/V5KrxcIKw=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>LS6AHvoJZT47zZyEhAFjzbRXWZwSExZ57uESc+hXBjabZ3vWVYvnTh0n0xAD8fgOK2TMHPP0zOPkRk411XEFeC1/5upMDI3x9uRCj2C+Ht0oNqLVNGoPOHd4x3XOnoT/SCHxgBBdPiKfJ1sWDspcvjhsKhaIBoNResVrhdusNFY=</ds:SignatureValue><ds:KeyInfo><ds:X509Data><ds:X509Certificate>MIIC8DCCAlmgAwIBAgIJAIzc4FYrIp9pMA0GCSqGSIb3DQEBCwUAMIGEMQswCQYDVQQGEwJVUzEL\n"
          + "MAkGA1UECBMCQVoxDDAKBgNVBAoTA0RERjEMMAoGA1UECxMDRGV2MRkwFwYDVQQDExBEREYgRGVt\n"
          + "byBSb290IENBMTEwLwYJKoZIhvcNAQkBFiJlbWFpbEFkZHJlc3M9ZGRmcm9vdGNhQGV4YW1wbGUu\n"
          + "b3JnMCAXDTE1MTIxMTE1NDMyM1oYDzIxMTUxMTE3MTU0MzIzWjBwMQswCQYDVQQGEwJVUzELMAkG\n"
          + "A1UECBMCQVoxDDAKBgNVBAoTA0RERjEMMAoGA1UECxMDRGV2MRIwEAYDVQQDEwlsb2NhbGhvc3Qx\n"
          + "JDAiBgkqhkiG9w0BCQEWFWxvY2FsaG9zdEBleGFtcGxlLm9yZzCBnzANBgkqhkiG9w0BAQEFAAOB\n"
          + "jQAwgYkCgYEAx4LI1lsJNmmEdB8HmDwWuAGrVFjNXuKRXD+lUaTPyDHeXcD32zxa0DiZEB5vqfS9\n"
          + "NH3I0E56Rbidg6IQ6r/9hOL9+sjWTPRBsQfWzZwjmcUG61psPc9gbFRK5qltz4BLv4+SWvRMMjgx\n"
          + "HM8+SROnjCU5FD9roJ9Ww2v+ZWAvYJ8CAwEAAaN7MHkwCQYDVR0TBAIwADAsBglghkgBhvhCAQ0E\n"
          + "HxYdT3BlblNTTCBHZW5lcmF0ZWQgQ2VydGlmaWNhdGUwHQYDVR0OBBYEFID3lAgzIEAdGx3RHizs\n"
          + "LcGt4WuwMB8GA1UdIwQYMBaAFOFUx5ffCsK/qV94XjsLK+RIF73GMA0GCSqGSIb3DQEBCwUAA4GB\n"
          + "ACWWsi4WusO5/u1O91obGn8ctFnxVlogBQ/tDZ+neQDxy8YB2J28tztELrRHkaGiCPT4CCKdy0hx\n"
          + "/bG/jSM1ypJnPKrPVrCkYL3Y68pzxvrFNq5NqAFCcBOCNsDNfvCSZ/XHvFyGHIuso5wNVxJyvTdh\n"
          + "Q+vWbnpiX8qr6vTx2Wgw</ds:X509Certificate></ds:X509Data></ds:KeyInfo></ds:Signature><saml2:Subject><saml2:NameID Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:persistent\" NameQualifier=\"http://cxf.apache.org/sts\">admin</saml2:NameID><saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\"/></saml2:Subject><saml2:Conditions NotBefore=\"2017-12-21T22:41:17.714Z\" NotOnOrAfter=\"2017-12-21T23:11:17.714Z\"><saml2:AudienceRestriction><saml2:Audience>https://localhost:8993/services/SecurityTokenService?wsdl</saml2:Audience></saml2:AudienceRestriction></saml2:Conditions><saml2:AuthnStatement AuthnInstant=\"2017-12-21T22:41:17.713Z\" SessionIndex=\"135416475\"><saml2:AuthnContext><saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport</saml2:AuthnContextClassRef></saml2:AuthnContext></saml2:AuthnStatement><saml2:AttributeStatement><saml2:Attribute Name=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue xsi:type=\"xsd:string\">admin@localhost.local</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue xsi:type=\"xsd:string\">guest</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue xsi:type=\"xsd:string\">guest</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue xsi:type=\"xsd:string\">guest</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue xsi:type=\"xsd:string\">group</saml2:AttributeValue><saml2:AttributeValue xsi:type=\"xsd:string\">admin</saml2:AttributeValue><saml2:AttributeValue xsi:type=\"xsd:string\">manager</saml2:AttributeValue><saml2:AttributeValue xsi:type=\"xsd:string\">viewer</saml2:AttributeValue><saml2:AttributeValue xsi:type=\"xsd:string\">system-admin</saml2:AttributeValue><saml2:AttributeValue xsi:type=\"xsd:string\">systembundles</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue xsi:type=\"xsd:string\">admin</saml2:AttributeValue></saml2:Attribute></saml2:AttributeStatement></saml2:Assertion>";

  @Test
  public void testSAMLAssertionParse() throws SecurityServiceException {
    SAMLUtils spySamlUtils = Mockito.spy(SAMLUtils.getInstance());

    Element securityToken = spySamlUtils.getSecurityTokenFromSAMLAssertion(SAML_ASSERTION);
    verify(spySamlUtils, times(0)).parseAssertionWithoutNamespace(SAML_ASSERTION);
    assertThat(securityToken, is(instanceOf(Element.class)));
  }

  @Test
  public void testSAMLAssertionParseFail() throws SecurityServiceException {
    SAMLUtils spySamlUtils = Mockito.spy(SAMLUtils.getInstance());

    Element securityToken = spySamlUtils.getSecurityTokenFromSAMLAssertion(BAD_SAML_ASSERTION);
    verify(spySamlUtils, times(1)).parseAssertionWithoutNamespace(BAD_SAML_ASSERTION);
    assertThat(securityToken, is(instanceOf(Element.class)));
  }

  @Test
  public void testRemoveSignature() throws IOException, SAXException, ParserConfigurationException {
    DocumentBuilderFactory dbf = XMLUtils.getInstance().getSecureDocumentBuilderFactory();
    dbf.setNamespaceAware(true);
    DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
    Document doc = documentBuilder.parse(new ByteArrayInputStream(SAML_ASSERTION.getBytes()));
    String returnSubject = SAML_UTILS.getSubjectAsStringNoSignature(doc.getDocumentElement());
    assertThat(returnSubject, not(containsString("Signature")));
  }

  @Test
  public void testRemoveSignatureAlreadyRemoved()
      throws IOException, SAXException, ParserConfigurationException {
    Element mockElement = mock(Element.class, RETURNS_DEEP_STUBS);
    doNothing().when(mockElement).normalize();
    when(mockElement.getElementsByTagNameNS("*", "Signature").item(0)).thenReturn(null);
    String returnSubject = SAML_UTILS.getSubjectAsStringNoSignature(mockElement);
    verify(mockElement, times(0)).removeChild(Mockito.any(Node.class));
    assertThat(returnSubject, not(containsString("Signature")));
  }

  @Test
  public void testGetSubjectAsStringNoSignatureNullInput() {
    String returnSubject = SAML_UTILS.getSubjectAsStringNoSignature(null);
    assertThat(returnSubject, is(nullValue()));
  }
}
