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
package ddf.security.assertion.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import ddf.security.assertion.AuthenticationStatement;
import ddf.security.assertion.saml.impl.SecurityAssertionSaml;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class SecurityAssertionSamlTest {

  public static final String SAML_CONDITION_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

  public static final String BEFORE = "2013-04-23T23:39:54.788Z";

  public static final String AFTER = "2113-04-24T00:09:54.788Z";

  private static final String ISSUER = "tokenissuer";

  private static final String PRINCIPAL =
      "CN=client,OU=I4CE,O=Lockheed Martin,L=Goodyear,ST=Arizona,C=US";

  private static final int NUM_ATTRIBUTES = 1;

  private static final int NUM_NAUTH = 1;

  private static final int NUM_AUTHZ = 0;

  private static final String SESSION_INDEX = "42";

  public static Document readXml(InputStream is)
      throws SAXException, IOException, ParserConfigurationException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    dbf.setValidating(false);
    dbf.setIgnoringComments(false);
    dbf.setIgnoringElementContentWhitespace(true);
    dbf.setNamespaceAware(true);
    // dbf.setCoalescing(true);
    // dbf.setExpandEntityReferences(true);

    DocumentBuilder db = dbf.newDocumentBuilder();
    db.setEntityResolver(new DOMUtils.NullResolver());

    // db.setErrorHandler( new MyErrorHandler());

    return db.parse(is);
  }

  @Test
  public void testEmptyAssertion() {
    SecurityAssertionSaml assertion = new SecurityAssertionSaml();
    assertNull(assertion.getIssuer());
    assertEquals(0, assertion.getAttributeStatements().size());
    assertEquals(0, assertion.getAuthnStatements().size());
    assertNull(assertion.getPrincipal());
    assertNull(assertion.getNotBefore());
    assertNull(assertion.getNotOnOrAfter());
    assertTrue(assertion.isPresentlyValid());
  }

  @Test
  public void testSampleAssertion() throws Exception {
    Element issuedAssertion = this.readDocument("/saml.xml").getDocumentElement();
    String assertionId = issuedAssertion.getAttributeNodeNS(null, "ID").getNodeValue();
    SecurityToken token = new SecurityToken(assertionId, issuedAssertion, null);
    SecurityAssertionSaml assertion = new SecurityAssertionSaml(token);
    assertNotNull(assertion.getToken());
    assertEquals(token, assertion.getToken());
    assertEquals(ISSUER, assertion.getIssuer());
    assertEquals(PRINCIPAL, assertion.getPrincipal().getName());
    assertEquals(PRINCIPAL, assertion.getPrincipal().toString());
    assertEquals(NUM_ATTRIBUTES, assertion.getAttributeStatements().size());
    List<AuthenticationStatement> authnStatements = assertion.getAuthnStatements();
    assertEquals(NUM_NAUTH, authnStatements.size());
    assertEquals(
        (long) NUM_NAUTH,
        authnStatements.stream().map(AuthenticationStatement::getSessionIndex).count());
    Optional<String> sessionIndex =
        authnStatements.stream().map(AuthenticationStatement::getSessionIndex).findFirst();
    assertTrue(sessionIndex.isPresent());
    assertEquals(SESSION_INDEX, sessionIndex.get());
    assertEquals(
        DatatypeConverter.parseDateTime(BEFORE).getTimeInMillis(),
        assertion.getNotBefore().getTime());
    assertEquals(
        DatatypeConverter.parseDateTime(AFTER).getTimeInMillis(),
        assertion.getNotOnOrAfter().getTime());
    // we don't currently parse these
    //        assertEquals(NUM_AUTHZ, assertion.getAuthzDecisionStatements().size());
    assertNotNull(assertion.toString());
    assertTrue(assertion.isPresentlyValid());
  }

  @Test
  public void testIsPresentlyValidWithNullBounds() throws Exception {
    Element issuedAssertion = this.readDocument("/saml.xml").getDocumentElement();

    // Remove Time Bounds
    issuedAssertion
        .getElementsByTagName("saml2:Conditions")
        .item(0)
        .getAttributes()
        .removeNamedItem("NotBefore");
    issuedAssertion
        .getElementsByTagName("saml2:Conditions")
        .item(0)
        .getAttributes()
        .removeNamedItem("NotOnOrAfter");

    SecurityAssertionSaml assertion = getSecurityAssertion(issuedAssertion);

    assertTrue(assertion.isPresentlyValid());
  }

  @Test
  public void testIsPresentlyValidWithNullNotBefore() throws Exception {
    Element issuedAssertion = this.readDocument("/saml.xml").getDocumentElement();

    // Remove NotBefore
    issuedAssertion
        .getElementsByTagName("saml2:Conditions")
        .item(0)
        .getAttributes()
        .removeNamedItem("NotBefore");

    SecurityAssertionSaml assertion = getSecurityAssertion(issuedAssertion);

    assertTrue(assertion.isPresentlyValid());
  }

  @Test
  public void testIsPresentlyValidWithNullNotOnOrAfter() throws Exception {
    Element issuedAssertion = this.readDocument("/saml.xml").getDocumentElement();

    // Remove NotOnOrAfter
    issuedAssertion
        .getElementsByTagName("saml2:Conditions")
        .item(0)
        .getAttributes()
        .removeNamedItem("NotOnOrAfter");

    SecurityAssertionSaml assertion = getSecurityAssertion(issuedAssertion);

    assertTrue(assertion.isPresentlyValid());
  }

  @Test
  public void testIsPresentlyValidBeforeNotBefore() throws Exception {
    Element issuedAssertion = this.readDocument("/saml.xml").getDocumentElement();

    // Change the NotBefore Date on the SAML Assertion to be after "now"
    issuedAssertion
        .getElementsByTagName("saml2:Conditions")
        .item(0)
        .getAttributes()
        .getNamedItem("NotBefore")
        .setNodeValue(getNowWithOffset(1));

    SecurityAssertionSaml assertion = getSecurityAssertion(issuedAssertion);

    assertFalse(assertion.isPresentlyValid());
  }

  @Test
  public void testIsPresentlyValidAfterNotOnOrAfter() throws Exception {
    Element issuedAssertion = this.readDocument("/saml.xml").getDocumentElement();

    // Change the NotOnOrAfter Date on the SAML Assertion to be before "now"
    issuedAssertion
        .getElementsByTagName("saml2:Conditions")
        .item(0)
        .getAttributes()
        .getNamedItem("NotOnOrAfter")
        .setNodeValue(getNowWithOffset(-1));

    SecurityAssertionSaml assertion = getSecurityAssertion(issuedAssertion);

    assertFalse(assertion.isPresentlyValid());
  }

  /**
   * Reads a classpath resource into a Document.
   *
   * @param name the name of the classpath resource
   */
  protected Document readDocument(String name)
      throws SAXException, IOException, ParserConfigurationException {
    InputStream inStream = getClass().getResourceAsStream(name);
    return readXml(inStream);
  }

  private SecurityAssertionSaml getSecurityAssertion(Element issuedAssertion) {
    String assertionId = issuedAssertion.getAttributeNodeNS(null, "ID").getNodeValue();
    SecurityToken token = new SecurityToken(assertionId, issuedAssertion, null);
    return new SecurityAssertionSaml(token);
  }

  private String getNowWithOffset(int offset) {
    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    calendar.add(Calendar.SECOND, offset);
    Date offsetNow = calendar.getTime();
    DateFormat dateFormat = new SimpleDateFormat(SAML_CONDITION_DATE_FORMAT);
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    return dateFormat.format(offsetNow);
  }
}
