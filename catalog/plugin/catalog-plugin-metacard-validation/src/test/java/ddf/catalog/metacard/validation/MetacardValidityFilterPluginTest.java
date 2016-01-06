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
package ddf.catalog.metacard.validation;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ddf.catalog.metacard.validation.MetacardValidityMarkerPlugin.VALIDATION_ERRORS;
import static ddf.catalog.metacard.validation.MetacardValidityMarkerPlugin.VALIDATION_WARNINGS;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.StopProcessingException;

public class MetacardValidityFilterPluginTest {

    private MetacardValidityFilterPlugin metacardValidityFilterPlugin;

    @Before
    public void setUp() {
        metacardValidityFilterPlugin = new MetacardValidityFilterPlugin();
    }

    @Test
    public void testSetAttributeMapping() {
        List<String> attributeMapping = Arrays.asList("sample=test1,test2");
        metacardValidityFilterPlugin.setAttributeMap(attributeMapping);
        Map<String, List<String>> assertMap = metacardValidityFilterPlugin.getAttributeMap();
        assertThat(assertMap.size(), is(1));
        assertThat(assertMap.containsKey("sample"), is(true));
        assertThat(assertMap.get("sample").contains("test1"), is(true));
        assertThat(assertMap.get("sample").contains("test2"), is(true));

    }

    @Test
    public void testValidMetacards() {
        Result result = mock(Result.class);
        Metacard metacard = getValidMetacard();
        metacard.setAttribute(new AttributeImpl(Metacard.SECURITY, getPopulatedSecurity()));

        when(result.getMetacard()).thenReturn(metacard);
        QueryResponse queryResponse = mock(QueryResponse.class);
        when(queryResponse.getResults()).thenReturn(Arrays.asList(result));
        try {
            metacardValidityFilterPlugin.process(queryResponse);
            assertThat(
                    queryResponse.getResults().get(0).getMetacard().getAttribute(Metacard.SECURITY)
                            .getValues().size(), is(1));
            assertThat(((HashMap) queryResponse.getResults().get(0).getMetacard()
                            .getAttribute(Metacard.SECURITY).getValues().get(0)).get("marking"),
                    is(not(nullValue())));
        } catch (PluginExecutionException | StopProcessingException e) {
            fail();
        }
    }

    @Test
    public void testValidMetacardsEmptySecurity() {
        Result result = mock(Result.class);
        Metacard metacard = getValidMetacard();

        when(result.getMetacard()).thenReturn(metacard);
        QueryResponse queryResponse = mock(QueryResponse.class);
        when(queryResponse.getResults()).thenReturn(Arrays.asList(result));
        try {
            metacardValidityFilterPlugin.process(queryResponse);
            assertThat(
                    queryResponse.getResults().get(0).getMetacard().getAttribute(Metacard.SECURITY),
                    is(nullValue()));
        } catch (PluginExecutionException | StopProcessingException e) {
            fail();
        }
    }

    @Test
    public void testInvalidMetacards() {
        List<String> attributeMapping = Arrays.asList("sample=test1,test2");
        metacardValidityFilterPlugin.setAttributeMap(attributeMapping);
        Result result = mock(Result.class);
        Metacard metacard = getInvalidMetacard();
        metacard.setAttribute(new AttributeImpl(Metacard.SECURITY, getPopulatedSecurity()));

        when(result.getMetacard()).thenReturn(metacard);
        QueryResponse queryResponse = mock(QueryResponse.class);
        when(queryResponse.getResults()).thenReturn(Arrays.asList(result));
        try {
            metacardValidityFilterPlugin.process(queryResponse);
            assertThat(((List) ((HashMap) queryResponse.getResults().get(0).getMetacard()
                    .getAttribute(Metacard.SECURITY).getValues().get(0)).get("sample"))
                    .contains("test1"), is(true));
            assertThat(((List) ((HashMap) queryResponse.getResults().get(0).getMetacard()
                            .getAttribute(Metacard.SECURITY).getValues().get(0)).get("marking")).isEmpty(),
                    is(false));
        } catch (PluginExecutionException | StopProcessingException e) {
            fail();
        }
    }

    @Test
    public void testInvalidMetacardsEmptySecurity() {
        List<String> attributeMapping = Arrays.asList("sample=test1,test2");
        metacardValidityFilterPlugin.setAttributeMap(attributeMapping);
        Result result = mock(Result.class);
        Metacard metacard = getInvalidMetacard();

        when(result.getMetacard()).thenReturn(metacard);
        QueryResponse queryResponse = mock(QueryResponse.class);
        when(queryResponse.getResults()).thenReturn(Arrays.asList(result));
        try {
            metacardValidityFilterPlugin.process(queryResponse);
            assertThat(((List) ((HashMap) queryResponse.getResults().get(0).getMetacard()
                    .getAttribute(Metacard.SECURITY).getValues().get(0)).get("sample"))
                    .contains("test1"), is(true));
            assertThat(((HashMap) queryResponse.getResults().get(0).getMetacard()
                            .getAttribute(Metacard.SECURITY).getValues().get(0)).get("marking"),
                    is(nullValue()));
        } catch (PluginExecutionException | StopProcessingException e) {
            fail();
        }
    }

    @Test
    public void testNullMetacard() {
        List<String> attributeMapping = Arrays.asList("sample=test1,test2");
        metacardValidityFilterPlugin.setAttributeMap(attributeMapping);
        Result result = mock(Result.class);

        when(result.getMetacard()).thenReturn(null);
        QueryResponse queryResponse = mock(QueryResponse.class);
        when(queryResponse.getResults()).thenReturn(Arrays.asList(result));
        try {
            QueryResponse returnedQueryResponse = metacardValidityFilterPlugin.process(queryResponse);
            assertThat(returnedQueryResponse.equals(queryResponse), is(true));

        } catch (PluginExecutionException | StopProcessingException e) {
            fail();
        }
    }

    @Test
    public void testNullResults() {
        List<String> attributeMapping = Arrays.asList("sample=test1,test2");
        metacardValidityFilterPlugin.setAttributeMap(attributeMapping);
        QueryResponse queryResponse = mock(QueryResponse.class);
        when(queryResponse.getResults()).thenReturn(null);
        try {
            QueryResponse returnedQueryResponse = metacardValidityFilterPlugin.process(queryResponse);
            assertThat(returnedQueryResponse.equals(queryResponse), is(true));

        } catch (PluginExecutionException | StopProcessingException e) {
            fail();
        }
    }


    private MetacardImpl getValidMetacard() {
        MetacardImpl returnMetacard = new MetacardImpl();
        return returnMetacard;
    }

    private MetacardImpl getInvalidMetacard() {
        MetacardImpl returnMetacard = new MetacardImpl();
        returnMetacard.setAttribute(
                new AttributeImpl(VALIDATION_ERRORS, Arrays.asList("sample-validator")));
        returnMetacard.setAttribute(
                new AttributeImpl(VALIDATION_WARNINGS, Arrays.asList("sample-validator")));
        return returnMetacard;
    }

    private HashMap getPopulatedSecurity() {
        HashMap<String, List<String>> returnMap = new HashMap();
        returnMap.put("marking", Arrays.asList("TS, U"));
        return returnMap;
    }
}


