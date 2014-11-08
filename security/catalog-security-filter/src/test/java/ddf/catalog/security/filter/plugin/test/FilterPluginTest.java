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
package ddf.catalog.security.filter.plugin.test;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.security.filter.plugin.FilterPlugin;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.permission.KeyValueCollectionPermission;
import junit.framework.Assert;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DelegatingSubject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.sort.SortBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.*;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 */
public class FilterPluginTest {
    private static final Logger logger = LoggerFactory.getLogger(FilterPluginTest.class);

    FilterPlugin plugin;

    QueryResponseImpl incomingResponse;

    @Before
    public void setup() {
        plugin = new FilterPlugin();
        QueryRequestImpl request = getSampleRequest();
        Map<String, Serializable> properties = new HashMap<String, Serializable>();

        AuthorizingRealm realm = mock(AuthorizingRealm.class);
        
        when(realm.getName()).thenReturn("mockRealm");
        when(realm.isPermitted(any(PrincipalCollection.class), any(Permission.class))).then(makeDecision());
         
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

        ResultImpl result1 = new ResultImpl(getMoreRolesMetacard());
        ResultImpl result2 = new ResultImpl(getMissingRolesMetacard());
        ResultImpl result3 = new ResultImpl(getExactRolesMetacard());
        ResultImpl result4 = new ResultImpl(getNoRolesMetacard());
        ResultImpl result5 = new ResultImpl(getNoSecurityAttributeMetacard());
        incomingResponse.addResult(result1, false);
        incomingResponse.addResult(result2, false);
        incomingResponse.addResult(result3, false);
        incomingResponse.addResult(result4, false);
        incomingResponse.addResult(result5, true);
    }
    
    public Answer<Boolean> makeDecision() {
        
        Map<String, List<String>> testRoleMap = new HashMap<String, List<String>>();
        List<String> testRoles = new ArrayList<String>();
        testRoles.add("A");
        testRoles.add("B");
        testRoleMap.put("Roles", testRoles);
        
        final KeyValueCollectionPermission testUserPermission = new KeyValueCollectionPermission(testRoleMap);
        
        return new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                Permission incomingPermission = (Permission)args[1];
                return testUserPermission.implies(incomingPermission);
            }
        };
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
    
    @Test(expected=StopProcessingException.class)
    public void testNoSubject() throws Exception{
        QueryResponseImpl response = new QueryResponseImpl(getSampleRequest());
        plugin.process(response);
        fail("Plugin should have thrown exception when no subject was sent in.");
    }
    
    @Test(expected=StopProcessingException.class)
    public void testNoRequestSubject() throws Exception{
        QueryResponseImpl response = new QueryResponseImpl(null);
        plugin.process(response);
        fail("Plugin should have thrown exception when no subject was sent in.");
    }


    public void verifyFilterResponse(QueryResponse response) {
        logger.info("Filtered with " + response.getResults().size() + " out of 5 original.");
        logger.info("Checking Results");
        Assert.assertEquals(4, response.getResults().size());
        logger.info("Filtering succeeded.");
    }

    public Metacard getMoreRolesMetacard() {
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
        security.put("Roles", Arrays.asList("A", "B", "CR", "WS"));
        metacard.setSecurity(security);
        return metacard;
    }

    public Metacard getMissingRolesMetacard() {
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
        security.put("Roles", Arrays.asList("A"));
        metacard.setSecurity(security);
        return metacard;
    }

    public Metacard getExactRolesMetacard() {
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
        security.put("Roles", Arrays.asList("A", "B"));
        metacard.setSecurity(security);
        return metacard;
    }
    
    public Metacard getNoRolesMetacard() {
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
        metacard.setSecurity(security);
        return metacard;
    }

    public Metacard getNoSecurityAttributeMetacard() {
        MetacardImpl metacard = new MetacardImpl();
        metacard.setResourceSize("100");
        try {
            metacard.setResourceURI(new URI("http://some.fancy.uri/goes/here"));
        } catch (URISyntaxException e) {
            logger.error("", e);
        }
        //Intentionally do not set the Metacard.SECURITY attribute
        metacard.setContentTypeName("Resource");
        metacard.setTitle("Metacard 1");
        metacard.setContentTypeVersion("1");
        metacard.setType(new MetacardTypeImpl(MetacardType.DEFAULT_METACARD_TYPE_NAME,
                new HashSet<AttributeDescriptor>()));

        return metacard;
    }




    private class MockSubject extends DelegatingSubject implements Subject {

        public MockSubject(SecurityManager manager, PrincipalCollection principals) {
            super(principals, true, null, new SimpleSession(UUID.randomUUID().toString()), manager);
        }

    }
      
    private QueryRequestImpl getSampleRequest() {
        return new QueryRequestImpl(new Query() {
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
    }
}
