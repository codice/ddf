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
import ddf.security.assertion.Attribute;
import ddf.security.assertion.AttributeStatement;
import ddf.security.assertion.SecurityAssertion;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.junit.Before;
import org.junit.Test;
import org.opensaml.core.xml.schema.XSString;

public class SubjectIdentityTest {

  private static final String TEST_NAME = "test01";

  private SubjectIdentityImpl subjectIdentity;

  @Before
  public void setUp() {
    subjectIdentity = new SubjectIdentityImpl();
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

  private Subject getSubjectWithAttributes(Map<String, List<String>> attributes) {

    Subject subject = mock(Subject.class);
    PrincipalCollection pc = mock(PrincipalCollection.class);
    SecurityAssertion assertion = mock(SecurityAssertion.class);
    AttributeStatement as = mock(AttributeStatement.class);

    List<Attribute> attrs =
        attributes.entrySet().stream().map(this::getAttribute).collect(Collectors.toList());

    doReturn(pc).when(subject).getPrincipals();
    doReturn(Collections.singletonList(assertion)).when(pc).byType(SecurityAssertion.class);
    doReturn(ImmutableList.of(assertion)).when(pc).byType(SecurityAssertion.class);
    doReturn(Collections.singletonList(as)).when(assertion).getAttributeStatements();
    doReturn(attrs).when(as).getAttributes();

    return subject;
  }

  private Attribute getAttribute(Map.Entry<String, List<String>> attribute) {
    Attribute attr = mock(Attribute.class);

    doReturn(attribute.getKey()).when(attr).getName();

    doReturn(attribute.getValue()).when(attr).getValues();

    return attr;
  }

  private XSString getXSString(String str) {
    XSString xstr = mock(XSString.class);
    doReturn(str).when(xstr).getValue();
    return xstr;
  }
}
