/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.security.pdp.realm.xacml;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

import ddf.security.pdp.realm.xacml.processor.PdpException;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributesType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.RequestType;

public class XacmlPdpTest {
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

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before()
    public void setup() throws PdpException, IOException {
        temporaryFolder.create();
        File policy = temporaryFolder.newFile("policy.xml");
        FileOutputStream policyOutStream = new FileOutputStream(policy);
        InputStream policyStream = XacmlPdp.class.getResourceAsStream("/policies/test-policy.xml");
        IOUtils.copy(policyStream, policyOutStream);
        testRealm = new XacmlPdp(new File(policy.getParent()).getAbsolutePath(),
                new XmlParser(),
                Arrays.asList("item0=item0Val1",
                        "item1=item1Val1,item1Val2",
                        "item2=item2Val1,item2Val2,item2Val3"));
    }

    @After
    public void destroy() {
        temporaryFolder.delete();
    }

    @Test(expected = PdpException.class)
    public void testBadSetupNull() throws PdpException {
        XacmlPdp xacmlPdp = new XacmlPdp(null, new XmlParser(), new ArrayList<>());
    }

    @Test(expected = PdpException.class)
    public void testBadSetupEmpty() throws PdpException {
        XacmlPdp xacmlPdp = new XacmlPdp("", new XmlParser(), new ArrayList<>());
    }

    @Test
    public void testActionGoodCountry() {
        RequestType request = testRealm.createXACMLRequest(USER_NAME,
                generateSubjectInfo(TEST_COUNTRY),
                new KeyValueCollectionPermission(QUERY_ACTION));

        assertTrue(testRealm.isPermitted(request));
    }

    @Test
    public void testActionBadCountry() {
        RequestType request = testRealm.createXACMLRequest(USER_NAME,
                generateSubjectInfo("CAN"),
                new KeyValueCollectionPermission(QUERY_ACTION));

        assertFalse(testRealm.isPermitted(request));
    }

    @Test
    public void testActionGoodSiteName() {
        SimpleAuthorizationInfo blankUserInfo = new SimpleAuthorizationInfo(new HashSet<String>());
        blankUserInfo.setObjectPermissions(new HashSet<Permission>());
        RequestType request = testRealm.createXACMLRequest(USER_NAME,
                blankUserInfo,
                new KeyValueCollectionPermission(SITE_NAME_ACTION));

        assertTrue(testRealm.isPermitted(request));
    }

    @Test
    public void testActionBadAction() {

        RequestType request = testRealm.createXACMLRequest(USER_NAME,
                generateSubjectInfo(TEST_COUNTRY),
                new KeyValueCollectionPermission("bad"));

        assertFalse(testRealm.isPermitted(request));
    }

    @Test
    public void testSameAccessRedaction() throws PdpException {

        HashMap<String, List<String>> security = new HashMap<String, List<String>>();
        security.put(RESOURCE_ACCESS, Arrays.asList(ACCESS_TYPE_A, ACCESS_TYPE_B));

        KeyValueCollectionPermission resourcePermissions = new KeyValueCollectionPermission(
                CollectionPermission.READ_ACTION,
                security);

        RequestType request = testRealm.createXACMLRequest(USER_NAME,
                generateSubjectInfo(TEST_COUNTRY),
                resourcePermissions);

        assertTrue(testRealm.isPermitted(request));

    }

    @Test
    public void testResourceIsPermitted() {

        HashMap<String, List<String>> security = new HashMap<String, List<String>>();
        security.put(RESOURCE_ACCESS, Arrays.asList(ACCESS_TYPE_A));

        KeyValueCollectionPermission resourcePermissions = new KeyValueCollectionPermission(
                CollectionPermission.READ_ACTION,
                security);
        RequestType request = testRealm.createXACMLRequest(USER_NAME,
                generateSubjectInfo(TEST_COUNTRY),
                resourcePermissions);

        assertTrue(testRealm.isPermitted(request));

    }

    @Test
    public void testResourceIsNotPermitted() {

        HashMap<String, List<String>> security = new HashMap<String, List<String>>();
        security.put(RESOURCE_ACCESS, Arrays.asList(ACCESS_TYPE_A, ACCESS_TYPE_B, ACCESS_TYPE_C));

        KeyValueCollectionPermission resourcePermissions = new KeyValueCollectionPermission(
                CollectionPermission.READ_ACTION,
                security);
        RequestType request = testRealm.createXACMLRequest(USER_NAME,
                generateSubjectInfo(TEST_COUNTRY),
                resourcePermissions);

        assertFalse(testRealm.isPermitted(request));

    }

    @Test
    public void testParseAttributeTypeBoolean() {
        assertTrue(XACMLConstants.BOOLEAN_DATA_TYPE.equals(testRealm.getXacmlDataType("true")));
    }

    @Test
    public void testParseAttributeTypeInteger() {
        assertTrue(XACMLConstants.INTEGER_DATA_TYPE.equals(testRealm.getXacmlDataType("42")));
    }

    @Test
    public void testParseAttributeTypeDouble() {

        assertTrue(XACMLConstants.DOUBLE_DATA_TYPE.equals(testRealm.getXacmlDataType("42.42")));
    }

    @Test
    public void testParseAttributeTypeTime() {

        assertTrue(XACMLConstants.TIME_DATA_TYPE.equals(testRealm.getXacmlDataType(
                "09:00:52.545-05:00")));
        assertTrue(XACMLConstants.TIME_DATA_TYPE.equals(testRealm.getXacmlDataType("09:00:52.545")));
        assertTrue(XACMLConstants.TIME_DATA_TYPE.equals(testRealm.getXacmlDataType("09:00:52-05:00")));
        assertTrue(XACMLConstants.TIME_DATA_TYPE.equals(testRealm.getXacmlDataType("09:00:52")));

    }

    @Test
    public void testParseAttributeTypeDate() {
        assertTrue(XACMLConstants.DATE_DATA_TYPE.equals(testRealm.getXacmlDataType("1984-11-06")));
        assertTrue(XACMLConstants.DATE_DATA_TYPE.equals(testRealm.getXacmlDataType(
                "1984-11-06-05:00")));

    }

    @Test
    public void testParseAttributeTypeDateTime() {
        assertTrue(XACMLConstants.DATE_TIME_DATA_TYPE.equals(testRealm.getXacmlDataType(
                "2002-05-30T09:30:10Z")));
        assertTrue(XACMLConstants.DATE_TIME_DATA_TYPE.equals(testRealm.getXacmlDataType(
                "2002-05-30T09:30:10")));
        assertTrue(XACMLConstants.DATE_TIME_DATA_TYPE.equals(testRealm.getXacmlDataType(
                "2002-05-30T09:30:10-05:00")));
        assertTrue(XACMLConstants.DATE_TIME_DATA_TYPE.equals(testRealm.getXacmlDataType(
                "2002-05-30T09:30:10.525")));
    }

    @Test
    public void testParseAttributeTypeUri() {
        assertTrue(XACMLConstants.URI_DATA_TYPE.equals(testRealm.getXacmlDataType(
                "http://www.codice.org")));
    }

    @Test
    public void testParseAttributeTypeString() {
        assertTrue(XACMLConstants.STRING_DATA_TYPE.equals(testRealm.getXacmlDataType(
                "Simple string with 1 integer.")));
    }

    @Test
    public void testParseAttributeTypeRfc822Name() {
        assertTrue(XACMLConstants.RFC822_NAME_DATA_TYPE.equals(testRealm.getXacmlDataType(
                "dev@codice.com")));
    }

    @Test
    public void testParseAttributeTypeIpAddress() {
        assertTrue(XACMLConstants.IP_ADDRESS_DATA_TYPE.equals(testRealm.getXacmlDataType("8.8.8.8")));
    }

    @Test
    public void testParseAttributeTypeX500Name() {
        assertTrue(XACMLConstants.X500_NAME_DATA_TYPE.equals(testRealm.getXacmlDataType(
                "c=UK,l=London,o=Tardis,o=police box,cn=john.smith")));
    }

    @Test
    public void testEnvironmentVariables() {
        RequestType request = testRealm.createXACMLRequest(USER_NAME,
                generateSubjectInfo(TEST_COUNTRY),
                new KeyValueCollectionPermission(QUERY_ACTION));

        List<AttributesType> attributes = request.getAttributes();

        AttributesType environmentAttributes = null;
        for (AttributesType attribute : attributes) {
            if (attribute.getCategory()
                    .equals(XACMLConstants.ENVIRONMENT_CATEGORY)) {
                environmentAttributes = attribute;
            }
        }

        assertNotNull(environmentAttributes);

        assertThat(environmentAttributes.getAttribute()
                .get(0)
                .getAttributeId(), is("item0"));
        assertThat(environmentAttributes.getAttribute()
                .get(0)
                .getAttributeValue()
                .size(), is(1));
        assertThat(environmentAttributes.getAttribute()
                .get(1)
                .getAttributeId(), is("item1"));
        assertThat(environmentAttributes.getAttribute()
                .get(1)
                .getAttributeValue()
                .size(), is(2));
        assertThat(environmentAttributes.getAttribute()
                .get(2)
                .getAttributeId(), is("item2"));
        assertThat(environmentAttributes.getAttribute()
                .get(2)
                .getAttributeValue()
                .size(), is(3));
    }

    private AuthorizationInfo generateSubjectInfo(String country) {
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        Set<Permission> permissions = new HashSet<Permission>();
        Set<String> roles = new HashSet<String>();

        // add roles
        roles.add("users");
        roles.add("admin");

        // add permissions
        KeyValuePermission citizenshipPermission = new KeyValuePermission(COUNTRY);
        citizenshipPermission.addValue(country);
        permissions.add(citizenshipPermission);

        KeyValuePermission typePermission = new KeyValuePermission(SUBJECT_ACCESS);
        typePermission.addValue(ACCESS_TYPE_A);
        typePermission.addValue(ACCESS_TYPE_B);

        KeyValuePermission nameIdentPermission = new KeyValuePermission(NAME_IDENTIFIER);
        nameIdentPermission.addValue("testuser1");

        KeyValuePermission givenNamePermission = new KeyValuePermission(GIVEN_NAME);
        givenNamePermission.addValue("Test User");

        permissions.add(typePermission);
        permissions.add(nameIdentPermission);
        permissions.add(givenNamePermission);

        info.setRoles(roles);
        info.setObjectPermissions(permissions);
        return info;
    }

}
