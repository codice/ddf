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
package ddf.catalog.registry.policy;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.Callable;

import org.apache.shiro.subject.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.plugin.PolicyResponse;
import ddf.security.Subject;

public class RegistryPolicyPluginTest {
    private CatalogFramework mockFramework;

    @Before
    public void setup() {
        mockFramework = mock(CatalogFramework.class);
    }

    @Test
    public void testBlackListPostQuery() throws Exception {

        Metacard mcard = new MetacardImpl();
        mcard.setAttribute(new AttributeImpl(Metacard.CONTENT_TYPE, "registry.service"));
        mcard.setAttribute(new AttributeImpl(Metacard.ID, "1234567890abcdefg987654321"));

        RegistryPolicyPlugin rpp = createRegistryPlugin();

        rpp.setRegistryBypassPolicyStrings(Collections.singletonList("role=system-admin"));
        PolicyResponse response = rpp.processPostQuery(new ResultImpl(mcard), null);

        assertThat(response.operationPolicy()
                .size(), is(0));
        assertThat(response.itemPolicy()
                .size(), is(0));
        rpp.setRegistryEntryIds(Collections.singleton("1234567890abcdefg987654321"));

        response = rpp.processPostQuery(new ResultImpl(mcard), null);
        assertThat(response.itemPolicy(), equalTo(rpp.getBypassAccessPolicy()));
    }

    @Test
    public void testWhiteListPostQuery() throws Exception {

        Metacard mcard = new MetacardImpl();
        mcard.setAttribute(new AttributeImpl(Metacard.CONTENT_TYPE, "registry.service"));
        mcard.setAttribute(new AttributeImpl(Metacard.ID, "1234567890abcdefg987654321"));

        RegistryPolicyPlugin rpp = createRegistryPlugin();
        rpp.setWhiteList(true);
        rpp.setRegistryBypassPolicyStrings(Collections.singletonList("role=system-admin"));
        PolicyResponse response = rpp.processPostQuery(new ResultImpl(mcard), null);

        assertThat(response.operationPolicy()
                .size(), is(0));
        assertThat(response.itemPolicy(), equalTo(rpp.getBypassAccessPolicy()));

        rpp.setRegistryEntryIds(Collections.singleton("1234567890abcdefg987654321"));
        response = rpp.processPostQuery(new ResultImpl(mcard), null);
        assertThat(response.itemPolicy()
                .size(), is(0));
    }

    @Test
    public void testCudRegistryOperations() throws Exception {
        RegistryPolicyPlugin rpp = createRegistryPlugin();
        rpp.setRegistryBypassPolicyStrings(Collections.singletonList("role=system-admin"));
        rpp.setWriteAccessPolicyStrings(Collections.singletonList("role=guest"));

        Metacard mcard = new MetacardImpl();
        mcard.setAttribute(new AttributeImpl(Metacard.CONTENT_TYPE, "registry.service"));
        mcard.setAttribute(new AttributeImpl(Metacard.ID, "1234567890abcdefg987654321"));

        when(mockFramework.query(any(QueryRequest.class))).thenReturn(new QueryResponseImpl(null,
                Collections.singletonList(new ResultImpl(mcard)),
                1));

        PolicyResponse response = rpp.processPreCreate(mcard, null);
        assertThat(response.operationPolicy(), equalTo(rpp.getWriteAccessPolicy()));
        response = rpp.processPreUpdate(mcard, null);
        assertThat(response.operationPolicy(), equalTo(rpp.getWriteAccessPolicy()));
        response = rpp.processPreDelete(Metacard.ID,
                Collections.singletonList("1234567890abcdefg987654321"),
                null);
        assertThat(response.operationPolicy(), equalTo(rpp.getWriteAccessPolicy()));

        rpp = createRegistryPlugin(true);
        rpp.setRegistryBypassPolicyStrings(Collections.singletonList("role=system-admin"));
        rpp.setWriteAccessPolicyStrings(Collections.singletonList("role=guest"));

        response = rpp.processPreDelete(Metacard.ID,
                Collections.singletonList("1234567890abcdefg987654321"),
                null);
        assertThat(response.operationPolicy()
                .size(), is(0));
    }

    @Test
    public void testReadRegistryOperations() throws Exception {
        RegistryPolicyPlugin rpp = createRegistryPlugin();
        rpp.setRegistryBypassPolicyStrings(Collections.singletonList("role=system-admin"));
        rpp.setReadAccessPolicyStrings(Collections.singletonList("role=guest"));

        Metacard mcard = new MetacardImpl();
        mcard.setAttribute(new AttributeImpl(Metacard.CONTENT_TYPE, "registry.service"));
        mcard.setAttribute(new AttributeImpl(Metacard.ID, "1234567890abcdefg987654321"));

        PolicyResponse response = rpp.processPostQuery(new ResultImpl(mcard), null);
        assertThat(response.itemPolicy(), equalTo(rpp.getReadAccessPolicy()));
    }

    @Test
    public void testRemoteCudOperations() throws Exception {
        RegistryPolicyPlugin rpp = createRegistryPlugin();
        rpp.setRegistryBypassPolicyStrings(Collections.singletonList("role=system-admin"));
        rpp.setWriteAccessPolicyStrings(Collections.singletonList("role=guest"));

        Metacard mcard = new MetacardImpl();
        mcard.setAttribute(new AttributeImpl(Metacard.CONTENT_TYPE, "registry.service"));
        mcard.setAttribute(new AttributeImpl(Metacard.ID, "1234567890abcdefg987654321"));

        when(mockFramework.query(any(QueryRequest.class))).thenReturn(new QueryResponseImpl(null,
                Collections.singletonList(new ResultImpl(mcard)),
                1));

        HashMap<String, Serializable> props = new HashMap<>();
        props.put("local-destination", false);
        PolicyResponse response = rpp.processPreCreate(mcard, props);
        assertThat(response.operationPolicy()
                .size(), is(0));
        response = rpp.processPreUpdate(mcard, props);
        assertThat(response.operationPolicy()
                .size(), is(0));
        response = rpp.processPreDelete(Metacard.ID,
                Collections.singletonList("1234567890abcdefg987654321"),
                props);
        assertThat(response.operationPolicy()
                .size(), is(0));
    }

    @Test
    public void testNonRegistryMcardTypes() throws Exception {
        RegistryPolicyPlugin rpp = createRegistryPlugin();
        rpp.setRegistryBypassPolicyStrings(Collections.singletonList("role=system-admin"));

        Metacard mcard = new MetacardImpl();
        mcard.setAttribute(new AttributeImpl(Metacard.CONTENT_TYPE, "some.type"));
        mcard.setAttribute(new AttributeImpl(Metacard.ID, "1234567890abcdefg987654321"));
        when(mockFramework.query(any(QueryRequest.class))).thenReturn(new QueryResponseImpl(null,
                new ArrayList<>(),
                0));

        PolicyResponse response = rpp.processPostQuery(new ResultImpl(mcard), null);
        assertThat(response.itemPolicy()
                .isEmpty(), is(true));
        response = rpp.processPreCreate(mcard, null);
        assertThat(response.operationPolicy()
                .isEmpty(), is(true));
        response = rpp.processPreUpdate(mcard, null);
        assertThat(response.operationPolicy()
                .isEmpty(), is(true));
        response = rpp.processPreDelete(Metacard.ID,
                Collections.singletonList("1234567890abcdefg987654321"),
                null);
        assertThat(response.operationPolicy()
                .isEmpty(), is(true));
        FilterBuilder filterBuilder = new GeotoolsFilterBuilder();
        Filter filter = filterBuilder.attribute(Metacard.CONTENT_TYPE)
                .is()
                .like()
                .text("some.type");
        response = rpp.processPreQuery(new QueryImpl(filter), null);
        //TODO: uncomment when vinas stuff gets in
        //                assertThat(response.operationPolicy()
        //                        .isEmpty(), is(true));

        Metacard mcard2 = new MetacardImpl();
        mcard2.setAttribute(new AttributeImpl(Metacard.ID, "abcdefghijklmnop1234567890"));

        response = rpp.processPostQuery(new ResultImpl(mcard2), null);
        assertThat(response.itemPolicy()
                .isEmpty(), is(true));
    }

    @Test
    public void testDisabledRegistry() throws Exception {

        RegistryPolicyPlugin rpp = createRegistryPlugin();

        rpp.setRegistryBypassPolicyStrings(Collections.singletonList("role=system-admin"));
        rpp.setRegistryDisabled(true);

        Metacard mcard = new MetacardImpl();
        mcard.setAttribute(new AttributeImpl(Metacard.CONTENT_TYPE, "registry.service"));
        mcard.setAttribute(new AttributeImpl(Metacard.ID, "1234567890abcdefg987654321"));
        when(mockFramework.query(any(QueryRequest.class))).thenReturn(new QueryResponseImpl(null,
                Collections.singletonList(new ResultImpl(mcard)),
                1));

        PolicyResponse response = rpp.processPreCreate(mcard, null);
        assertThat(response.operationPolicy(), equalTo(rpp.getBypassAccessPolicy()));
        response = rpp.processPreUpdate(mcard, null);
        assertThat(response.operationPolicy(), equalTo(rpp.getBypassAccessPolicy()));
        response = rpp.processPreDelete(Metacard.ID,
                Collections.singletonList("1234567890abcdefg987654321"),
                null);
        assertThat(response.operationPolicy(), equalTo(rpp.getBypassAccessPolicy()));
        FilterBuilder filterBuilder = new GeotoolsFilterBuilder();
        Filter filter = filterBuilder.attribute(Metacard.CONTENT_TYPE)
                .is()
                .like()
                .text("registry.service");
        response = rpp.processPreQuery(new QueryImpl(filter), null);
        //TODO: uncomment when vinas stuff gets in
        //                assertThat(response.operationPolicy(), equalTo(rpp.getBypassAccessPolicy()));
    }

    @Test
    public void testNoRegistryBypassPermissions() throws Exception {
        Metacard mcard = new MetacardImpl();
        mcard.setAttribute(new AttributeImpl(Metacard.CONTENT_TYPE, "registry.service"));
        mcard.setAttribute(new AttributeImpl(Metacard.ID, "1234567890abcdefg987654321"));

        RegistryPolicyPlugin rpp = createRegistryPlugin();
        rpp.setRegistryBypassPolicyStrings(null);
        PolicyResponse response = rpp.processPostQuery(new ResultImpl(mcard), null);

        assertThat(response.itemPolicy()
                .isEmpty(), is(true));
    }

    private RegistryPolicyPlugin createRegistryPlugin() {
        return createRegistryPlugin(false);
    }

    private RegistryPolicyPlugin createRegistryPlugin(boolean badSubject) {
        FilterBuilder filterBuilder = new GeotoolsFilterBuilder();
        RegistryPolicyPlugin rpp;
        if (badSubject) {
            rpp = new RegistryPolicyPlugin(mockFramework, filterBuilder) {
                @Override
                protected Subject getSubject() {

                    Subject newSubject = mock(Subject.class);
                    when(newSubject.execute(any(Callable.class))).thenThrow(new ExecutionException(
                            new Throwable("Test exception")));
                    return newSubject;
                }
            };
        } else {
            rpp = new RegistryPolicyPlugin(mockFramework, filterBuilder) {
                @Override
                protected Subject getSubject() {
                    Subject newSubject = mock(Subject.class);
                    when(newSubject.execute(any(Callable.class))).thenAnswer((invocation) -> {
                        Callable<Boolean> callable =
                                (Callable<Boolean>) invocation.getArguments()[0];
                        return callable.call();
                    });
                    return newSubject;
                }
            };
        }
        return rpp;
    }
}
