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
package ddf.security.pep.redaction.plugin.test;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardImpl;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.MetacardTypeImpl;
import ddf.catalog.data.ResultImpl;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequestImpl;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.QueryResponseImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.StopProcessingException;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.pdp.realm.SimpleAuthzRealm;
import ddf.security.pep.redaction.plugin.RedactionPlugin;
import ddf.security.permission.KeyValuePermission;
import junit.framework.Assert;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DelegatingSubject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.sort.SortBy;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * User: tustisos Date: 3/20/13 Time: 3:24 PM
 */
public class RedactionPluginTest {
    private static final Logger logger = Logger.getLogger(RedactionPluginTest.class);

    RedactionPlugin plugin;

    QueryResponseImpl incomingResponse;

    @BeforeClass()
    public static void setupLogging() {
        BasicConfigurator.configure();
        logger.setLevel(Level.TRACE);
    }

    @Before
    public void setup() {
        plugin = new RedactionPlugin();

        QueryRequestImpl request = new QueryRequestImpl(new Query() {
            @Override
            public int getStartIndex() {
                return 0;
            }

            @Override
            public int getPageSize() {
                return 10;
            }

            @Override
            public SortBy getSortBy() {
                return null;
            }

            @Override
            public boolean requestsTotalResultsCount() {
                return false;
            }

            @Override
            public long getTimeoutMillis() {
                return 0;
            }

            @Override
            public boolean evaluate(Object o) {
                return true;
            }

            @Override
            public Object accept(FilterVisitor filterVisitor, Object o) {
                return null;
            }
        });

        Map<String, Serializable> properties = new HashMap<String, Serializable>();

        Realm realm = new SimpleAuthzRealm();
        ((SimpleAuthzRealm) realm).setAuthorizationInfo(new AuthorizationInfo() {
            @Override
            public Collection<String> getRoles() {
                return null;
            }

            @Override
            public Collection<String> getStringPermissions() {
                return null;
            }

            @Override
            public Collection<Permission> getObjectPermissions() {
                Collection<Permission> permissions = new ArrayList<Permission>();
                KeyValuePermission keyValuePermission = new KeyValuePermission("FineAccessControls");
                keyValuePermission.addValue("A");
                keyValuePermission.addValue("B");
                KeyValuePermission keyValuePermission1 = new KeyValuePermission(
                        "CountryOfAffiliation");
                keyValuePermission1.addValue("GBR");
                permissions.add(keyValuePermission);
                permissions.add(keyValuePermission1);
                return permissions;
            }
        });
        Collection<org.apache.shiro.realm.Realm> realms = new ArrayList<org.apache.shiro.realm.Realm>();
        realms.add(realm);

        DefaultSecurityManager manager = new DefaultSecurityManager();
        manager.setRealms(realms);

        SimplePrincipalCollection principalCollection = new SimplePrincipalCollection(
                new Principal() {
                    @Override
                    public String getName() {
                        return "testuser";
                    }
                }, realm.getName());

        Subject subject = new MockSubject(manager, principalCollection);
        properties.put(SecurityConstants.SECURITY_SUBJECT, subject);
        request.setProperties(properties);

        incomingResponse = new QueryResponseImpl(request);

        ResultImpl result1 = new ResultImpl(getHighMetacard());
        ResultImpl result2 = new ResultImpl(getLowMetacard());
        ResultImpl result3 = new ResultImpl(getLowMetacardReleaseToOne());
        incomingResponse.addResult(result1, false);
        incomingResponse.addResult(result2, false);
        incomingResponse.addResult(result3, true);

        ((SimpleAuthzRealm) realm).setMatchAllMappings(Arrays.asList("FineAccessControls=rule"));
        ((SimpleAuthzRealm) realm).setMatchOneMappings(Arrays
                .asList("CountryOfAffiliation=country"));
    }

    @Test
    public void testPluginFilter() {

        try {
            QueryResponse response = plugin.process(incomingResponse);
            verifyFilterResponse(response);
        } catch (PluginExecutionException e) {
            logger.error("Error while processing the redaction plugin", e);
        } catch (StopProcessingException e) {
            logger.error("Stopped processing the redaction plugin", e);
        }
    }

    public void verifyFilterResponse(QueryResponse response) {
        System.out.println("Filtered with " + response.getResults().size() + " out of 3 original.");
        System.out.println("Checking Results");
        Assert.assertEquals(1, response.getResults().size());
        System.out.println("Filtering succeeded.");
    }

    public Metacard getHighMetacard() {
        MetacardImpl metacard = new MetacardImpl();
        metacard.setResourceSize("100");
        try {
            metacard.setResourceURI(new URI("http://some.fancy.uri/goes/here"));
        } catch (URISyntaxException e) {
            logger.error("", e);
        }
        metacard.setContentTypeName("Resource");
        metacard.setTitle("Metacard 1");
        metacard.setContentTypeVersion("1");
        metacard.setType(new MetacardTypeImpl(MetacardType.DEFAULT_METACARD_TYPE_NAME,
                new HashSet<AttributeDescriptor>()));
        HashMap<String, List<String>> security = new HashMap<String, List<String>>();
        security.put("rule", Arrays.asList("A", "B", "CR", "WS"));
        security.put("country", Arrays.asList("AUS", "CAN", "GBR"));
        metacard.setSecurity(security);
        return metacard;
    }

    public Metacard getLowMetacard() {
        MetacardImpl metacard = new MetacardImpl();
        metacard.setResourceSize("100");
        try {
            metacard.setResourceURI(new URI("http://some.fancy.uri/goes/here"));
        } catch (URISyntaxException e) {
            logger.error("", e);
        }
        metacard.setContentTypeName("Resource");
        metacard.setTitle("Metacard 1");
        metacard.setContentTypeVersion("1");
        metacard.setType(new MetacardTypeImpl(MetacardType.DEFAULT_METACARD_TYPE_NAME,
                new HashSet<AttributeDescriptor>()));

        HashMap<String, List<String>> security = new HashMap<String, List<String>>();
        security.put("rule", Arrays.asList("A", "B"));
        security.put("country", Arrays.asList("AUS", "CAN", "GBR"));
        metacard.setSecurity(security);
        return metacard;
    }

    public Metacard getLowMetacardReleaseToOne() {
        MetacardImpl metacard = new MetacardImpl();
        metacard.setResourceSize("100");
        try {
            metacard.setResourceURI(new URI("http://some.fancy.uri/goes/here"));
        } catch (URISyntaxException e) {
            logger.error("", e);
        }
        metacard.setContentTypeName("Resource");
        metacard.setTitle("Metacard 1");
        metacard.setContentTypeVersion("1");
        metacard.setType(new MetacardTypeImpl(MetacardType.DEFAULT_METACARD_TYPE_NAME,
                new HashSet<AttributeDescriptor>()));

        HashMap<String, List<String>> security = new HashMap<String, List<String>>();
        security.put("rule", Arrays.asList("A", "B"));
        security.put("country", Arrays.asList("AUS"));
        metacard.setSecurity(security);
        return metacard;
    }

    private class MockSubject extends DelegatingSubject implements Subject {

        public MockSubject(SecurityManager manager, PrincipalCollection principals) {
            super(principals, true, null, new SimpleSession(UUID.randomUUID().toString()), manager);
        }

    }
}
