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
package ddf.security.impl;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ddf.security.Subject;
import ddf.security.SubjectUtils;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.principal.GuestPrincipal;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.codice.ddf.security.claims.guest.GuestClaimsConfig;
import org.junit.Before;
import org.junit.Test;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;

public class SubjectIdentityTest {

  private static final String TEST_NAME = "test01";

  private SubjectIdentityImpl subjectIdentity;

  @Before
  public void setUp() {
    subjectIdentity = new SubjectIdentityImpl();
    subjectIdentity.setGuestClaimsConfig(getGuestClaimsConfig());
  }

  @Test
  public void testUniqueIdentifierWithAttribute() {
    final String identifier = "guest12345";
    Map<String, List<String>> attrs =
        ImmutableMap.of("identifier", Collections.singletonList(identifier));
    Subject subject = getSubjectWithAttributes(attrs);
    subjectIdentity.setIdentityAttribute("identifier");
    assertThat(subjectIdentity.getUniqueIdentifier(subject), is(identifier));
  }

  @Test
  public void testUniqueIdentifierWithEmail() {
    final String email = "guest@localhost";
    Map<String, List<String>> attrs =
        ImmutableMap.of(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI, Collections.singletonList(email));
    Subject subject = getSubjectWithAttributes(attrs);
    assertThat(subjectIdentity.getUniqueIdentifier(subject), is(email));
  }

  @Test
  public void testUniqueIdentifierNoEmail() {
    org.apache.shiro.subject.Subject subject;
    org.apache.shiro.mgt.SecurityManager secManager = new DefaultSecurityManager();
    PrincipalCollection principals = new SimplePrincipalCollection(TEST_NAME, "testrealm");
    subject =
        new Subject.Builder(secManager)
            .principals(principals)
            .session(new SimpleSession())
            .buildSubject();
    assertThat(TEST_NAME, is(subjectIdentity.getUniqueIdentifier(subject)));
  }

  @Test
  public void testGuestClaimsRemoval() {
    final String nameIdentifier = "user";
    Map<String, List<String>> attrs =
        ImmutableMap.of(
            SubjectUtils.NAME_IDENTIFIER_CLAIM_URI, Arrays.asList("guest", nameIdentifier));
    Subject subject = getSubjectWithAttributes(attrs);
    subjectIdentity.setIdentityAttribute(SubjectUtils.NAME_IDENTIFIER_CLAIM_URI);
    assertThat(subjectIdentity.getUniqueIdentifier(subject), is(nameIdentifier));
  }

  @Test
  public void testGuestClaimsRemovalAsGuestSubject() {
    Subject subject = getGuestSubject();
    subjectIdentity.setIdentityAttribute(SubjectUtils.NAME_IDENTIFIER_CLAIM_URI);
    assertThat(subjectIdentity.getUniqueIdentifier(subject), is("guest"));
  }

  private Subject getGuestSubject() {

    Subject subject = mock(Subject.class);
    PrincipalCollection pc = mock(PrincipalCollection.class);
    SecurityAssertion assertion = mock(SecurityAssertion.class);
    AttributeStatement as = mock(AttributeStatement.class);

    Map<String, List<String>> guestClaims = new HashMap<>();
    guestClaims.put(SubjectUtils.NAME_IDENTIFIER_CLAIM_URI, Collections.singletonList("guest"));
    guestClaims.put(SubjectUtils.ROLE_CLAIM_URI, Collections.singletonList("guest"));
    List<Attribute> attrs =
        guestClaims.entrySet().stream().map(this::getAttribute).collect(Collectors.toList());
    List<Object> principalList = Arrays.asList(mock(GuestPrincipal.class), assertion);

    doReturn(pc).when(subject).getPrincipals();
    doReturn(principalList).when(pc).asList();
    doReturn(assertion).when(pc).oneByType(SecurityAssertion.class);
    doReturn(ImmutableList.of(assertion)).when(pc).byType(SecurityAssertion.class);
    doReturn(Collections.singletonList(as)).when(assertion).getAttributeStatements();
    doReturn(attrs).when(as).getAttributes();

    return subject;
  }

  private Subject getSubjectWithAttributes(Map<String, List<String>> attributes) {

    Subject subject = mock(Subject.class);
    PrincipalCollection pc = mock(PrincipalCollection.class);
    SecurityAssertion assertion = mock(SecurityAssertion.class);
    AttributeStatement as = mock(AttributeStatement.class);

    List<Attribute> attrs =
        attributes.entrySet().stream().map(this::getAttribute).collect(Collectors.toList());

    doReturn(pc).when(subject).getPrincipals();
    doReturn(Collections.singletonList(assertion)).when(pc).asList();
    doReturn(assertion).when(pc).oneByType(SecurityAssertion.class);
    doReturn(ImmutableList.of(assertion)).when(pc).byType(SecurityAssertion.class);
    doReturn(Collections.singletonList(as)).when(assertion).getAttributeStatements();
    doReturn(attrs).when(as).getAttributes();

    return subject;
  }

  private GuestClaimsConfig getGuestClaimsConfig() {
    Map<URI, List<String>> guestClaims = new HashMap<>();
    guestClaims.put(
        URI.create(SubjectUtils.NAME_IDENTIFIER_CLAIM_URI), Collections.singletonList("guest"));
    guestClaims.put(URI.create(SubjectUtils.ROLE_CLAIM_URI), Collections.singletonList("guest"));

    GuestClaimsConfig guestClaimsConfig = mock(GuestClaimsConfig.class);
    doReturn(guestClaims).when(guestClaimsConfig).getClaimsMap();
    return guestClaimsConfig;
  }

  private Attribute getAttribute(Map.Entry<String, List<String>> attribute) {
    Attribute attr = mock(Attribute.class);

    doReturn(attribute.getKey()).when(attr).getName();

    doReturn(attribute.getValue().stream().map(this::getXSString).collect(Collectors.toList()))
        .when(attr)
        .getAttributeValues();

    return attr;
  }

  private XSString getXSString(String str) {
    XSString xstr = mock(XSString.class);
    doReturn(str).when(xstr).getValue();
    return xstr;
  }
}
