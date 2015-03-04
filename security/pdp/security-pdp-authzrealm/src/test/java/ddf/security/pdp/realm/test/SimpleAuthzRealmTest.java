/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.security.pdp.realm.test;

import ddf.security.pdp.realm.SimpleAuthzRealm;
import ddf.security.permission.ActionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;
import junit.framework.Assert;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.subject.PrincipalCollection;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

/**
 * User: tustisos Date: 3/20/13 Time: 9:35 AM
 */
public class SimpleAuthzRealmTest {
    SimpleAuthzRealm testRealm;

    List<Permission> permissionList;
    
    HashMap<String, List<String>> security;

    @Before
    public void setup() {
        testRealm = new SimpleAuthzRealm();

        String ruleClaim = "FineAccessControls";
        String countryClaim = "CountryOfAffiliation";

        // setup the subject permissions
        List<Permission> permissions = new ArrayList<Permission>();
        KeyValuePermission rulePermission = new KeyValuePermission(ruleClaim);
        rulePermission.addValue("A");
        rulePermission.addValue("B");
        permissions.add(rulePermission);
        KeyValuePermission countryPermission = new KeyValuePermission(countryClaim);
        countryPermission.addValue("AUS");
        permissions.add(countryPermission);

        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        authorizationInfo.addObjectPermission(rulePermission);
        authorizationInfo.addObjectPermission(countryPermission);
        authorizationInfo.addRole("admin");

        // setup the resource permissions
        permissionList = new ArrayList<Permission>();
        security = new HashMap<String, List<String>>();
        security.put("country", Arrays.asList("AUS", "CAN", "GBR"));
        security.put("rule", Arrays.asList("A", "B"));
        testRealm.setAuthorizationInfo(authorizationInfo);
        testRealm.setMatchOneMappings(Arrays.asList("CountryOfAffiliation=country"));
        testRealm.setMatchAllMappings(Arrays.asList("FineAccessControls=rule"));
    }

    @Test
    public void testIsPermitted() {
        permissionList.clear();
        KeyValueCollectionPermission kvcp = new KeyValueCollectionPermission(security);
        permissionList.add(kvcp);
        PrincipalCollection mockSubjectPrincipal = Mockito.mock(PrincipalCollection.class);

        boolean[] permittedArray = testRealm.isPermitted(mockSubjectPrincipal, permissionList);

        for (boolean permitted : permittedArray) {
            Assert.assertEquals(true, permitted);
        }
    }

    @Test
    public void testIsPermittedAllSingle() {
        permissionList.clear();
        KeyValuePermission kvp = new KeyValuePermission("rule", Arrays.asList("A", "B"));
        permissionList.add(kvp);
        PrincipalCollection mockSubjectPrincipal = Mockito.mock(PrincipalCollection.class);

        boolean[] permittedArray = testRealm.isPermitted(mockSubjectPrincipal, permissionList);

        for (boolean permitted : permittedArray) {
            Assert.assertEquals(true, permitted);
        }
    }

    @Test
    public void testIsPermittedOneSingle() {
        permissionList.clear();
        KeyValuePermission kvp = new KeyValuePermission("country", Arrays.asList("AUS", "CAN",
                "GBR"));
        permissionList.add(kvp);
        PrincipalCollection mockSubjectPrincipal = Mockito.mock(PrincipalCollection.class);

        boolean[] permittedArray = testRealm.isPermitted(mockSubjectPrincipal, permissionList);

        for (boolean permitted : permittedArray) {
            Assert.assertEquals(true, permitted);
        }
    }

    @Test
    public void testIsPermittedOneMultiple() {
        permissionList.clear();
        KeyValuePermission kvp = new KeyValuePermission("country", Arrays.asList("AUS", "CAN",
                "GBR"));
        permissionList.add(kvp);

        String ruleClaim = "FineAccessControls";
        String countryClaim = "CountryOfAffiliation";

        // create a new user here with multiple country permissions to test
        List<Permission> permissions = new ArrayList<Permission>();
        KeyValuePermission rulePermission = new KeyValuePermission(ruleClaim);
        rulePermission.addValue("A");
        rulePermission.addValue("B");
        permissions.add(rulePermission);
        KeyValuePermission countryPermission = new KeyValuePermission(countryClaim);
        countryPermission.addValue("USA");
        countryPermission.addValue("AUS");
        permissions.add(countryPermission);

        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        authorizationInfo.addObjectPermission(rulePermission);
        authorizationInfo.addObjectPermission(countryPermission);
        authorizationInfo.addRole("admin");

        testRealm.setAuthorizationInfo(authorizationInfo);

        PrincipalCollection mockSubjectPrincipal = Mockito.mock(PrincipalCollection.class);

        boolean[] permittedArray = testRealm.isPermitted(mockSubjectPrincipal, permissionList);

        for (boolean permitted : permittedArray) {
            Assert.assertEquals(true, permitted);
        }
    }

    @Test
    public void testIsActionPermitted() {
        permissionList.clear();
        ActionPermission actionPermission = new ActionPermission("action");
        testRealm.setOpenAccessActionList(Arrays.asList("action"));
        permissionList.add(actionPermission);
        PrincipalCollection mockSubjectPrincipal = Mockito.mock(PrincipalCollection.class);
        boolean[] permittedArray = testRealm.isPermitted(mockSubjectPrincipal, permissionList);
        for (boolean permitted : permittedArray) {
            Assert.assertEquals(true, permitted);
        }
    }

    @Test
    public void testIsRolePermitted() {
        permissionList.clear();
        ActionPermission actionPermission = new ActionPermission("action");
        testRealm.setOpenAccessActionList(Arrays.asList("otherAction"));
        testRealm.setAccessRoleList(Arrays.asList("admin"));
        permissionList.add(actionPermission);
        PrincipalCollection mockSubjectPrincipal = Mockito.mock(PrincipalCollection.class);
        boolean[] permittedArray = testRealm.isPermitted(mockSubjectPrincipal, permissionList);
        for (boolean permitted : permittedArray) {
            Assert.assertEquals(true, permitted);
        }
    }

    @Test
    public void testIsNotPermitted() {

        HashMap<String, List<String>> security = new HashMap<String, List<String>>();
        security.put("country", Arrays.asList("AUS", "CAN", "GBR"));
        security.put("country2", Arrays.asList("CAN", "GBR"));
        security.put("rule", Arrays.asList("A", "B"));
        security.put("rule2", Arrays.asList("A", "B", "C"));
        KeyValueCollectionPermission kvcp = new KeyValueCollectionPermission(security);
        permissionList.clear();
        permissionList.add(kvcp);
        PrincipalCollection mockSubjectPrincipal = Mockito.mock(PrincipalCollection.class);

        boolean[] permittedArray = testRealm.isPermitted(mockSubjectPrincipal, permissionList);

        for (boolean permitted : permittedArray) {
            Assert.assertEquals(false, permitted);
        }
    }
}
