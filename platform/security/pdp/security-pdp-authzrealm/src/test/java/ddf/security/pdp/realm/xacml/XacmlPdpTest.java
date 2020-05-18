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
package ddf.security.pdp.realm.xacml;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import ddf.security.audit.SecurityLogger;
import ddf.security.pdp.realm.xacml.processor.PdpException;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;
import ddf.security.permission.impl.KeyValueCollectionPermissionImpl;
import ddf.security.permission.impl.KeyValuePermissionImpl;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributesType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.RequestType;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.codice.ddf.parser.xml.XmlParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class XacmlPdpTest {

  private static final String STRING_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#string";

  private static final String BOOLEAN_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#boolean";

  private static final String INTEGER_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#integer";

  private static final String DOUBLE_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#double";

  private static final String TIME_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#time";

  private static final String DATE_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#date";

  private static final String DATE_TIME_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#dateTime";

  private static final String URI_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#anyURI";

  private static final String RFC822_NAME_DATA_TYPE =
      "urn:oasis:names:tc:xacml:1.0:data-type:rfc822Name";

  private static final String IP_ADDRESS_DATA_TYPE =
      "urn:oasis:names:tc:xacml:2.0:data-type:ipAddress";

  private static final String X500_NAME_DATA_TYPE =
      "urn:oasis:names:tc:xacml:1.0:data-type:x500Name";

  private static final String ENVIRONMENT_CATEGORY =
      "urn:oasis:names:tc:xacml:3.0:attribute-category:environment";

  private static final String USER_NAME = "Test User";

  private static final String QUERY_ACTION = "query";

  private static final String SITE_NAME_ACTION = "LocalSiteName";

  private static final String RESOURCE_ACCESS = "RESOURCE_ACCESS";

  private static final String SUBJECT_ACCESS = "SUBJECT_ACCESS";

  private static final String ACCESS_TYPE_A = "A";

  private static final String ACCESS_TYPE_B = "B";

  private static final String ACCESS_TYPE_C = "C";

  // Subject info
  private static final String TEST_COUNTRY = "ATA";

  private static final String NAME_IDENTIFIER =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier";

  private static final String GIVEN_NAME =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname";

  private static final String COUNTRY = "http://www.opm.gov/feddata/CountryOfCitizenship";

  private static XacmlPdp testRealm;

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before()
  public void setup() throws PdpException, IOException {
    temporaryFolder.create();
    File policy = temporaryFolder.newFile("policy.xml");
    FileOutputStream policyOutStream = new FileOutputStream(policy);
    InputStream policyStream = XacmlPdp.class.getResourceAsStream("/policies/test-policy.xml");
    IOUtils.copy(policyStream, policyOutStream);
    testRealm =
        new XacmlPdp(
            new File(policy.getParent()).getAbsolutePath(),
            new XmlParser(),
            Arrays.asList(
                "item0=item0Val1",
                "item1=item1Val1,item1Val2",
                "item2=item2Val1,item2Val2,item2Val3"),
            mock(SecurityLogger.class));
  }

  @After
  public void destroy() {
    temporaryFolder.delete();
  }

  @Test(expected = PdpException.class)
  public void testBadSetupNull() throws PdpException {
    XacmlPdp xacmlPdp =
        new XacmlPdp(null, new XmlParser(), new ArrayList<>(), mock(SecurityLogger.class));
  }

  @Test(expected = PdpException.class)
  public void testBadSetupEmpty() throws PdpException {
    XacmlPdp xacmlPdp =
        new XacmlPdp("", new XmlParser(), new ArrayList<>(), mock(SecurityLogger.class));
  }

  @Test
  public void testActionGoodCountry() {
    RequestType request =
        testRealm.createXACMLRequest(
            USER_NAME,
            generateSubjectInfo(TEST_COUNTRY),
            new KeyValueCollectionPermissionImpl(QUERY_ACTION));

    assertThat(testRealm.isPermitted(request), equalTo(true));
  }

  @Test
  public void testActionBadCountry() {
    RequestType request =
        testRealm.createXACMLRequest(
            USER_NAME,
            generateSubjectInfo("CAN"),
            new KeyValueCollectionPermissionImpl(QUERY_ACTION));

    assertThat(testRealm.isPermitted(request), equalTo(false));
  }

  @Test
  public void testActionGoodSiteName() {
    SimpleAuthorizationInfo blankUserInfo = new SimpleAuthorizationInfo(new HashSet<String>());
    blankUserInfo.setObjectPermissions(new HashSet<Permission>());
    RequestType request =
        testRealm.createXACMLRequest(
            USER_NAME, blankUserInfo, new KeyValueCollectionPermissionImpl(SITE_NAME_ACTION));

    assertThat(testRealm.isPermitted(request), equalTo(true));
  }

  @Test
  public void testActionBadAction() {

    RequestType request =
        testRealm.createXACMLRequest(
            USER_NAME,
            generateSubjectInfo(TEST_COUNTRY),
            new KeyValueCollectionPermissionImpl("bad"));

    assertThat(testRealm.isPermitted(request), equalTo(false));
  }

  @Test
  public void testSameAccessRedaction() throws PdpException {

    HashMap<String, List<String>> security = new HashMap<String, List<String>>();
    security.put(RESOURCE_ACCESS, Arrays.asList(ACCESS_TYPE_A, ACCESS_TYPE_B));

    KeyValueCollectionPermission resourcePermissions =
        new KeyValueCollectionPermissionImpl(CollectionPermission.READ_ACTION, security);

    RequestType request =
        testRealm.createXACMLRequest(
            USER_NAME, generateSubjectInfo(TEST_COUNTRY), resourcePermissions);

    assertThat(testRealm.isPermitted(request), equalTo(true));
  }

  @Test
  public void testResourceIsPermitted() {

    HashMap<String, List<String>> security = new HashMap<String, List<String>>();
    security.put(RESOURCE_ACCESS, Arrays.asList(ACCESS_TYPE_A));

    KeyValueCollectionPermission resourcePermissions =
        new KeyValueCollectionPermissionImpl(CollectionPermission.READ_ACTION, security);
    RequestType request =
        testRealm.createXACMLRequest(
            USER_NAME, generateSubjectInfo(TEST_COUNTRY), resourcePermissions);

    assertThat(testRealm.isPermitted(request), equalTo(true));
  }

  @Test
  public void testResourceIsNotPermitted() {

    HashMap<String, List<String>> security = new HashMap<String, List<String>>();
    security.put(RESOURCE_ACCESS, Arrays.asList(ACCESS_TYPE_A, ACCESS_TYPE_B, ACCESS_TYPE_C));

    KeyValueCollectionPermission resourcePermissions =
        new KeyValueCollectionPermissionImpl(CollectionPermission.READ_ACTION, security);
    RequestType request =
        testRealm.createXACMLRequest(
            USER_NAME, generateSubjectInfo(TEST_COUNTRY), resourcePermissions);

    assertThat(testRealm.isPermitted(request), equalTo(false));
  }

  @Test
  public void testParseAttributeTypeBoolean() {
    assertThat(testRealm.getXacmlDataType("true"), is(BOOLEAN_DATA_TYPE));
  }

  @Test
  public void testParseAttributeTypeInteger() {
    assertThat(testRealm.getXacmlDataType("42"), is(INTEGER_DATA_TYPE));
  }

  @Test
  public void testParseAttributeTypeDouble() throws ParseException {
    String val = NumberFormat.getNumberInstance(Locale.getDefault()).format(42.42);
    assertThat(testRealm.getXacmlDataType(val), is(DOUBLE_DATA_TYPE));
  }

  @Test
  public void testParseAttributeTypeTime() {

    assertThat(testRealm.getXacmlDataType("09:00:52.545-05:00"), is(TIME_DATA_TYPE));
    assertThat(testRealm.getXacmlDataType("09:00:52.545"), is(TIME_DATA_TYPE));
    assertThat(testRealm.getXacmlDataType("09:00:52-05:00"), is(TIME_DATA_TYPE));
    assertThat(testRealm.getXacmlDataType("09:00:52"), is(TIME_DATA_TYPE));
  }

  @Test
  public void testParseAttributeTypeDate() {
    assertThat(testRealm.getXacmlDataType("1984-11-06"), is(DATE_DATA_TYPE));
    assertThat(testRealm.getXacmlDataType("1984-11-06-05:00"), is(DATE_DATA_TYPE));
  }

  @Test
  public void testParseAttributeTypeDateTime() {
    assertThat(testRealm.getXacmlDataType("2002-05-30T09:30:10Z"), is(DATE_TIME_DATA_TYPE));
    assertThat(testRealm.getXacmlDataType("2002-05-30T09:30:10"), is(DATE_TIME_DATA_TYPE));
    assertThat(testRealm.getXacmlDataType("2002-05-30T09:30:10-05:00"), is(DATE_TIME_DATA_TYPE));
    assertThat(testRealm.getXacmlDataType("2002-05-30T09:30:10.525"), is(DATE_TIME_DATA_TYPE));
  }

  @Test
  public void testParseAttributeTypeUri() {
    assertThat(testRealm.getXacmlDataType("http://www.codice.org"), is(URI_DATA_TYPE));
  }

  @Test
  public void testParseAttributeTypeString() {
    assertThat(testRealm.getXacmlDataType("Simple string with 1 integer."), is(STRING_DATA_TYPE));
  }

  @Test
  public void testParseAttributeTypeRfc822Name() {
    assertThat(testRealm.getXacmlDataType("dev@codice.com"), is(RFC822_NAME_DATA_TYPE));
  }

  @Test
  public void testParseAttributeTypeIpAddress() {
    assertThat(testRealm.getXacmlDataType("8.8.8.8"), is(IP_ADDRESS_DATA_TYPE));
  }

  @Test
  public void testParseAttributeTypeX500Name() {
    assertThat(
        testRealm.getXacmlDataType("c=UK,l=London,o=Tardis,o=police box,cn=john.smith"),
        is(X500_NAME_DATA_TYPE));
  }

  @Test
  public void testEnvironmentVariables() {
    RequestType request =
        testRealm.createXACMLRequest(
            USER_NAME,
            generateSubjectInfo(TEST_COUNTRY),
            new KeyValueCollectionPermissionImpl(QUERY_ACTION));

    List<AttributesType> attributes = request.getAttributes();

    AttributesType environmentAttributes = null;
    for (AttributesType attribute : attributes) {
      if (attribute.getCategory().equals(ENVIRONMENT_CATEGORY)) {
        environmentAttributes = attribute;
      }
    }

    assertNotNull(environmentAttributes);

    assertThat(environmentAttributes.getAttribute().get(0).getAttributeId(), is("item0"));
    assertThat(environmentAttributes.getAttribute().get(0).getAttributeValue().size(), is(1));
    assertThat(environmentAttributes.getAttribute().get(1).getAttributeId(), is("item1"));
    assertThat(environmentAttributes.getAttribute().get(1).getAttributeValue().size(), is(2));
    assertThat(environmentAttributes.getAttribute().get(2).getAttributeId(), is("item2"));
    assertThat(environmentAttributes.getAttribute().get(2).getAttributeValue().size(), is(3));
  }

  private AuthorizationInfo generateSubjectInfo(String country) {
    SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
    Set<Permission> permissions = new HashSet<Permission>();
    Set<String> roles = new HashSet<String>();

    // add roles
    roles.add("users");
    roles.add("admin");

    // add permissions
    KeyValuePermission citizenshipPermission = new KeyValuePermissionImpl(COUNTRY);
    citizenshipPermission.addValue(country);
    permissions.add(citizenshipPermission);

    KeyValuePermission typePermission = new KeyValuePermissionImpl(SUBJECT_ACCESS);
    typePermission.addValue(ACCESS_TYPE_A);
    typePermission.addValue(ACCESS_TYPE_B);

    KeyValuePermission nameIdentPermission = new KeyValuePermissionImpl(NAME_IDENTIFIER);
    nameIdentPermission.addValue("testuser1");

    KeyValuePermission givenNamePermission = new KeyValuePermissionImpl(GIVEN_NAME);
    givenNamePermission.addValue("Test User");

    permissions.add(typePermission);
    permissions.add(nameIdentPermission);
    permissions.add(givenNamePermission);

    info.setRoles(roles);
    info.setObjectPermissions(permissions);
    return info;
  }
}
