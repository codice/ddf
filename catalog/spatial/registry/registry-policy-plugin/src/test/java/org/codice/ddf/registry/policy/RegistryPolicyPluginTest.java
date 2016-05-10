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
package org.codice.ddf.registry.policy;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;

import org.codice.ddf.registry.common.RegistryConstants;
import org.junit.Test;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.plugin.PolicyResponse;

public class RegistryPolicyPluginTest {

    @Test
    public void testBlackListPostQuery() throws Exception {

        Metacard mcard = new MetacardImpl();
        mcard.setAttribute(new AttributeImpl(Metacard.TAGS, RegistryConstants.REGISTRY_TAG));
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
        mcard.setAttribute(new AttributeImpl(Metacard.TAGS, RegistryConstants.REGISTRY_TAG));
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
        mcard.setAttribute(new AttributeImpl(Metacard.TAGS, RegistryConstants.REGISTRY_TAG));
        mcard.setAttribute(new AttributeImpl(Metacard.ID, "1234567890abcdefg987654321"));

        PolicyResponse response = rpp.processPreCreate(mcard, null);
        assertThat(response.operationPolicy(), equalTo(rpp.getWriteAccessPolicy()));
        response = rpp.processPreUpdate(mcard, null);
        assertThat(response.operationPolicy(), equalTo(rpp.getWriteAccessPolicy()));
        response = rpp.processPreDelete(Collections.singletonList(mcard), null);
        assertThat(response.operationPolicy(), equalTo(rpp.getWriteAccessPolicy()));

    }

    @Test
    public void testReadRegistryOperations() throws Exception {
        RegistryPolicyPlugin rpp = createRegistryPlugin();
        rpp.setRegistryBypassPolicyStrings(Collections.singletonList("role=system-admin"));
        rpp.setReadAccessPolicyStrings(Collections.singletonList("role=guest"));

        Metacard mcard = new MetacardImpl();
        mcard.setAttribute(new AttributeImpl(Metacard.TAGS, RegistryConstants.REGISTRY_TAG));
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
        mcard.setAttribute(new AttributeImpl(Metacard.TAGS, RegistryConstants.REGISTRY_TAG));
        mcard.setAttribute(new AttributeImpl(Metacard.ID, "1234567890abcdefg987654321"));

        HashMap<String, Serializable> props = new HashMap<>();
        props.put("local-destination", false);
        PolicyResponse response = rpp.processPreCreate(mcard, props);
        assertThat(response.operationPolicy()
                .size(), is(0));
        response = rpp.processPreUpdate(mcard, props);
        assertThat(response.operationPolicy()
                .size(), is(0));
        response = rpp.processPreDelete(Collections.singletonList(mcard), props);
        assertThat(response.operationPolicy()
                .size(), is(0));
    }

    @Test
    public void testNonRegistryMcardTypes() throws Exception {
        RegistryPolicyPlugin rpp = createRegistryPlugin();
        rpp.setRegistryBypassPolicyStrings(Collections.singletonList("role=system-admin"));

        Metacard mcard = new MetacardImpl();
        mcard.setAttribute(new AttributeImpl(Metacard.TAGS, "some.type"));
        mcard.setAttribute(new AttributeImpl(Metacard.ID, "1234567890abcdefg987654321"));

        PolicyResponse response = rpp.processPostQuery(new ResultImpl(mcard), null);
        assertThat(response.itemPolicy()
                .isEmpty(), is(true));
        response = rpp.processPreCreate(mcard, null);
        assertThat(response.operationPolicy()
                .isEmpty(), is(true));
        response = rpp.processPreUpdate(mcard, null);
        assertThat(response.operationPolicy()
                .isEmpty(), is(true));
        response = rpp.processPreDelete(Collections.singletonList(mcard), null);
        assertThat(response.operationPolicy()
                .isEmpty(), is(true));
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
        mcard.setAttribute(new AttributeImpl(Metacard.TAGS, RegistryConstants.REGISTRY_TAG));
        mcard.setAttribute(new AttributeImpl(Metacard.ID, "1234567890abcdefg987654321"));

        PolicyResponse response = rpp.processPreCreate(mcard, null);
        assertThat(response.operationPolicy(), equalTo(rpp.getBypassAccessPolicy()));
        response = rpp.processPreUpdate(mcard, null);
        assertThat(response.operationPolicy(), equalTo(rpp.getBypassAccessPolicy()));
        response = rpp.processPreDelete(Collections.singletonList(mcard), null);
        assertThat(response.operationPolicy(), equalTo(rpp.getBypassAccessPolicy()));
    }

    @Test
    public void testNoRegistryBypassPermissions() throws Exception {
        Metacard mcard = new MetacardImpl();
        mcard.setAttribute(new AttributeImpl(Metacard.TAGS, RegistryConstants.REGISTRY_TAG));
        mcard.setAttribute(new AttributeImpl(Metacard.ID, "1234567890abcdefg987654321"));

        RegistryPolicyPlugin rpp = createRegistryPlugin();
        rpp.setRegistryBypassPolicyStrings(null);
        PolicyResponse response = rpp.processPostQuery(new ResultImpl(mcard), null);

        assertThat(response.itemPolicy()
                .isEmpty(), is(true));
    }

    @Test
    public void testUnusedMethods() throws Exception {
        RegistryPolicyPlugin rpp = createRegistryPlugin();
        rpp.setRegistryBypassPolicyStrings(Collections.singletonList("role=system-admin"));
        rpp.setWriteAccessPolicyStrings(Collections.singletonList("role=guest"));
        rpp.setReadAccessPolicyStrings(Collections.singletonList("role=guest"));
        rpp.setRegistryEntryIds(Collections.singleton("1234567890abcdefg987654321"));

        assertThat(rpp.isRegistryDisabled(), is(false));
        assertThat(rpp.getBypassAccessPolicy()
                .get("role")
                .iterator()
                .next(), equalTo("system-admin"));
        assertThat(rpp.getWriteAccessPolicy()
                .get("role")
                .iterator()
                .next(), equalTo("guest"));
        assertThat(rpp.getReadAccessPolicy()
                .get("role")
                .iterator()
                .next(), equalTo("guest"));
        assertThat(rpp.getRegistryEntryIds()
                .contains("1234567890abcdefg987654321"), is(true));

        Metacard mcard = new MetacardImpl();
        mcard.setAttribute(new AttributeImpl(Metacard.TAGS, RegistryConstants.REGISTRY_TAG));
        mcard.setAttribute(new AttributeImpl(Metacard.ID, "1234567890abcdefg987654321"));

        assertThat(rpp.processPostDelete(mcard, null)
                .itemPolicy()
                .isEmpty(), is(true));
        assertThat(rpp.processPostDelete(mcard, null)
                .operationPolicy()
                .isEmpty(), is(true));

        assertThat(rpp.processPreQuery(null, null)
                .itemPolicy()
                .isEmpty(), is(true));
        assertThat(rpp.processPreQuery(null, null)
                .operationPolicy()
                .isEmpty(), is(true));

        assertThat(rpp.processPreResource(null)
                .itemPolicy()
                .isEmpty(), is(true));
        assertThat(rpp.processPreResource(null)
                .operationPolicy()
                .isEmpty(), is(true));

        assertThat(rpp.processPostResource(null, mcard)
                .itemPolicy()
                .isEmpty(), is(true));
        assertThat(rpp.processPostResource(null, mcard)
                .operationPolicy()
                .isEmpty(), is(true));

    }

    private RegistryPolicyPlugin createRegistryPlugin() {
        RegistryPolicyPlugin rpp = new RegistryPolicyPlugin();

        return rpp;
    }
}
