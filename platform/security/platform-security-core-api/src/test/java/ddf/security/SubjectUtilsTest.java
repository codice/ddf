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
package ddf.security;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import ddf.security.assertion.Attribute;
import ddf.security.assertion.AttributeStatement;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.principal.GuestPrincipal;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.x500.X500Principal;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.opensaml.core.xml.schema.XSString;

/** Tests out the SubjectUtils class */
public class SubjectUtilsTest {

  private static final String TEST_NAME = "test123";

  private static final String DEFAULT_NAME = "default";

  public static final String X500_DN = "CN=Foo,OU=Engineering,OU=Dev,O=DDF,ST=AZ,C=US";

  private Principal principal;

  private X500Principal dnPrincipal;

  @Before
  public void setup()
      throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
    try (InputStream systemKeyStream =
        SubjectUtilsTest.class.getResourceAsStream("/serverKeystore.jks")) {
      KeyStore keyStore = KeyStore.getInstance("jks");
      keyStore.load(systemKeyStream, "changeit".toCharArray());
      X509Certificate localhost = (X509Certificate) keyStore.getCertificate("localhost");
      principal = localhost.getSubjectDN();
    }

    Map<String, String> keywords = new HashMap<>();
    keywords.put("E", BCStyle.EmailAddress.getId());

    dnPrincipal = new X500Principal(X500_DN, keywords);
  }

  @Test
  public void testGetName() {
    org.apache.shiro.subject.Subject subject;
    org.apache.shiro.mgt.SecurityManager secManager = new DefaultSecurityManager();
    PrincipalCollection principals = new SimplePrincipalCollection(TEST_NAME, "testrealm");
    subject =
        new Subject.Builder(secManager)
            .principals(principals)
            .session(new SimpleSession())
            .authenticated(true)
            .buildSubject();
    assertEquals(TEST_NAME, SubjectUtils.getName(subject));
  }

  @Test
  public void testGetDefaultName() {
    org.apache.shiro.subject.Subject subject;
    org.apache.shiro.mgt.SecurityManager secManager = new DefaultSecurityManager();
    PrincipalCollection principals = new SimplePrincipalCollection();
    subject =
        new Subject.Builder(secManager)
            .principals(principals)
            .session(new SimpleSession())
            .authenticated(true)
            .buildSubject();
    assertEquals(DEFAULT_NAME, SubjectUtils.getName(subject, DEFAULT_NAME));
    assertEquals(DEFAULT_NAME, SubjectUtils.getName(null, DEFAULT_NAME));
  }

  @Test
  public void testGuestDisplayName() {
    Subject subject = getSubjectWithPrincipal(new GuestPrincipal("127.0.0.1"));
    assertEquals(SubjectUtils.GUEST_DISPLAY_NAME, SubjectUtils.getName(subject, null, true));
  }

  @Test
  public void testKerberosDisplayName() {
    Subject subject = getSubjectWithPrincipal(new KerberosPrincipal("kerby/ddf.org@REALM"));
    assertEquals("kerby", SubjectUtils.getName(subject, null, true));
  }

  @Test
  public void testX509DisplayName() {
    Subject subject = getSubjectWithPrincipal(dnPrincipal);
    assertEquals("Foo", SubjectUtils.getName(subject, null, true));
  }

  @Test
  public void testX509Name() {
    Subject subject = getSubjectWithPrincipal(dnPrincipal);
    assertEquals(X500_DN, SubjectUtils.getName(subject));
  }

  @Test
  public void testX509Email() {
    Map<String, String> keywords = new HashMap<>();
    keywords.put("E", BCStyle.EmailAddress.getId());

    X500Principal x500EmailPrincipal = new X500Principal("E=foo@bar.baz," + X500_DN, keywords);

    assertEquals("foo@bar.baz", SubjectUtils.getEmailAddress(x500EmailPrincipal));
  }

  @Test
  public void testX509Country() {
    assertEquals("US", SubjectUtils.getCountry(dnPrincipal));
  }

  private Subject getSubjectWithPrincipal(Principal principal) {
    Subject subject = mock(Subject.class);
    PrincipalCollection pc = mock(PrincipalCollection.class);
    SecurityAssertion assertion = mock(SecurityAssertion.class);

    doReturn(pc).when(subject).getPrincipals();
    doReturn(Collections.singletonList(assertion)).when(pc).byType(SecurityAssertion.class);
    doReturn(ImmutableList.of(assertion)).when(pc).byType(SecurityAssertion.class);
    doReturn(principal).when(assertion).getPrincipal();

    return subject;
  }

  @Test
  public void testGetCommonName() {
    assertThat(SubjectUtils.getCommonName(new X500Principal(principal.getName())), is("localhost"));
  }

  @Test
  public void testGetCommonNameNull() {
    assertNull(SubjectUtils.getCommonName(new X500Principal("o=stuff")));
  }

  @Test
  public void testFilterDNKeepOne() {
    Predicate<RDN> predicate = rdn -> rdn.getTypesAndValues()[0].getType().equals(BCStyle.CN);
    String baseDN = SubjectUtils.filterDN(dnPrincipal, predicate);
    assertThat(baseDN, is("CN=Foo"));
  }

  @Test
  public void testFilterDNDropOne() {
    Predicate<RDN> predicate = rdn -> !rdn.getTypesAndValues()[0].getType().equals(BCStyle.CN);
    String baseDN = SubjectUtils.filterDN(dnPrincipal, predicate);
    assertThat(baseDN, is("OU=Engineering,OU=Dev,O=DDF,ST=AZ,C=US"));
  }

  @Test
  public void testFilterDNDropTwo() {
    Predicate<RDN> predicate =
        rdn ->
            !ImmutableSet.of(BCStyle.C, BCStyle.ST).contains(rdn.getTypesAndValues()[0].getType());
    String baseDN = SubjectUtils.filterDN(dnPrincipal, predicate);
    assertThat(baseDN, is("CN=Foo,OU=Engineering,OU=Dev,O=DDF"));
  }

  @Test
  public void testFilterDNDropMultivalue() {
    Predicate<RDN> predicate = rdn -> !rdn.getTypesAndValues()[0].getType().equals(BCStyle.OU);
    String baseDN = SubjectUtils.filterDN(dnPrincipal, predicate);
    assertThat(baseDN, is("CN=Foo,O=DDF,ST=AZ,C=US"));
  }

  @Test
  public void testFilterDNRemoveAll() {
    Predicate<RDN> predicate =
        rdn ->
            !ImmutableSet.of(BCStyle.OU, BCStyle.CN, BCStyle.O, BCStyle.ST, BCStyle.C)
                .contains(rdn.getTypesAndValues()[0].getType());
    String baseDN = SubjectUtils.filterDN(dnPrincipal, predicate);
    assertThat(baseDN, is(""));
  }

  private XSString getXSString(String str) {
    XSString xstr = mock(XSString.class);
    doReturn(str).when(xstr).getValue();
    return xstr;
  }

  private Attribute getAttribute(Map.Entry<String, List<String>> attribute) {
    Attribute mockAttribute = mock(Attribute.class);

    doReturn(attribute.getKey()).when(mockAttribute).getName();

    doReturn(attribute.getValue()).when(mockAttribute).getValues();

    return mockAttribute;
  }

  private Subject getSubjectWithAttributes(Map<String, List<String>> attributes) {

    Subject subject = mock(Subject.class);
    PrincipalCollection principalCollection = mock(PrincipalCollection.class);
    SecurityAssertion securityAssertion = mock(SecurityAssertion.class);
    AttributeStatement attributeStatement = mock(AttributeStatement.class);

    List<Attribute> attrs =
        attributes.entrySet().stream().map(this::getAttribute).collect(Collectors.toList());

    doReturn(principalCollection).when(subject).getPrincipals();
    doReturn(Collections.singletonList(securityAssertion))
        .when(principalCollection)
        .byType(SecurityAssertion.class);
    doReturn(ImmutableList.of(securityAssertion))
        .when(principalCollection)
        .byType(SecurityAssertion.class);
    doReturn(Collections.singletonList(attributeStatement))
        .when(securityAssertion)
        .getAttributeStatements();
    doReturn(attrs).when(attributeStatement).getAttributes();

    return subject;
  }

  @Test
  public void testGetAttribute() {
    final String key = "random";
    final List<String> values = Arrays.asList("one", "two", "three");
    Map<String, List<String>> attrs = ImmutableMap.of(key, values);
    Subject subject = getSubjectWithAttributes(attrs);
    assertThat(SubjectUtils.getAttribute(subject, key), is(values));
  }

  @Test
  public void testGetAttributeNotPresent() {
    Subject subject = getSubjectWithAttributes(Collections.emptyMap());
    assertThat(SubjectUtils.getAttribute(subject, "any"), is(Collections.emptyList()));
  }

  @Test
  public void testGetAttributeOnNullSubject() {
    assertThat(SubjectUtils.getAttribute(null, "any"), is(Collections.emptyList()));
  }

  @Test
  public void testGetAttributeNullPrincipal() {
    Subject s = mock(Subject.class);
    assertThat(SubjectUtils.getAttribute(s, "any"), is(Collections.emptyList()));
  }

  @Test
  public void testGetAttributeNullAssertion() {
    Subject s = mock(Subject.class);
    PrincipalCollection principals = mock(PrincipalCollection.class);
    doReturn(principals).when(s).getPrincipals();
    assertThat(SubjectUtils.getAttribute(s, "any"), is(Collections.emptyList()));
  }

  @Test
  public void testGetEmail() {
    final String email = "guest@localhost";
    Map<String, List<String>> attrs =
        ImmutableMap.of(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI, Collections.singletonList(email));
    Subject subject = getSubjectWithAttributes(attrs);
    assertThat(SubjectUtils.getEmailAddress(subject), is(email));
  }

  @Test
  public void testGetEmailNull() {
    Subject subject = getSubjectWithAttributes(Collections.emptyMap());
    assertNull(SubjectUtils.getEmailAddress(subject));
  }

  @Test
  public void testRetrieveAttributesNoSubject() throws Exception {
    Map<String, SortedSet<String>> subjectAttributes = SubjectUtils.getSubjectAttributes(null);
    assertThat(subjectAttributes.entrySet(), is(empty()));
  }

  @Test
  public void testRetrieveAttributes() throws Exception {
    final String key1 = "attr1";
    final List<String> values1 = Arrays.asList("one", "two", "three");
    final String key2 = "attr2";
    final List<String> values2 = Arrays.asList("four", "five");
    Map<String, List<String>> attrs = ImmutableMap.of(key1, values1, key2, values2);

    Subject subject = getSubjectWithAttributes(attrs);

    Map<String, SortedSet<String>> subjectAttributes = SubjectUtils.getSubjectAttributes(subject);
    assertThat(subjectAttributes.keySet(), hasItems("attr1", "attr2"));
    assertThat(subjectAttributes.get("attr1"), CoreMatchers.hasItems("one", "two", "three"));
    assertThat(subjectAttributes.get("attr2"), CoreMatchers.hasItems("four", "five"));
  }
}
