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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ddf.catalog.data.impl.BasicTypes.VALIDATION_ERRORS;
import static ddf.catalog.data.impl.BasicTypes.VALIDATION_WARNINGS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.StopProcessingException;

public class MetacardValidityFilterPluginTest {

    private MetacardValidityFilterPlugin metacardValidityFilterPlugin;

    @Before
    public void setUp() {
        metacardValidityFilterPlugin = new MetacardValidityFilterPlugin();
    }

    @Test
    public void testSetAttributeMapping() {
        List<String> attributeMapping = Collections.singletonList("sample=test1,test2");
        metacardValidityFilterPlugin.setAttributeMap(attributeMapping);
        Map<String, List<String>> assertMap = metacardValidityFilterPlugin.getAttributeMap();
        assertThat(assertMap.size(), is(1));
        assertThat(assertMap.containsKey("sample"), is(true));
        assertThat(assertMap.get("sample")
                .contains("test1"), is(true));
        assertThat(assertMap.get("sample")
                .contains("test2"), is(true));

    }

    @Test
    public void testValidMetacards() {
        Result result = mock(Result.class);
        Metacard metacard = getValidMetacard();
        when(result.getMetacard()).thenReturn(metacard);

        try {
            PolicyResponse response = metacardValidityFilterPlugin.processPostQuery(result,
                    new HashMap<>());
            assertThat(response.itemPolicy()
                    .size(), is(0));
        } catch (StopProcessingException e) {
            fail();
        }
    }

    @Test
    public void testInvalidMetacards() {
        List<String> attributeMapping = Collections.singletonList("sample=test1,test2");
        metacardValidityFilterPlugin.setAttributeMap(attributeMapping);
        Result result = mock(Result.class);
        Metacard metacard = getInvalidMetacard();

        when(result.getMetacard()).thenReturn(metacard);

        try {
            PolicyResponse response = metacardValidityFilterPlugin.processPostQuery(result,
                    new HashMap<>());
            assertThat(response.itemPolicy()
                    .get("sample")
                    .contains("test1"), is(true));
        } catch (StopProcessingException e) {
            fail();
        }
    }

    @Test
    public void testNullMetacard() {
        List<String> attributeMapping = Collections.singletonList("sample=test1,test2");
        metacardValidityFilterPlugin.setAttributeMap(attributeMapping);
        Result result = mock(Result.class);

        when(result.getMetacard()).thenReturn(null);
        try {
            PolicyResponse response = metacardValidityFilterPlugin.processPostQuery(result,
                    new HashMap<>());
            assertThat(response.itemPolicy()
                    .isEmpty(), is(true));

        } catch (StopProcessingException e) {
            fail();
        }
    }

    @Test
    public void testNullResults() {
        List<String> attributeMapping = Collections.singletonList("sample=test1,test2");
        metacardValidityFilterPlugin.setAttributeMap(attributeMapping);

        try {
            PolicyResponse response = metacardValidityFilterPlugin.processPostQuery(null,
                    new HashMap<>());
            assertThat(response.itemPolicy()
                    .isEmpty(), is(true));

        } catch (StopProcessingException e) {
            fail();
        }
    }

    private MetacardImpl getValidMetacard() {
        return new MetacardImpl();

    }

    private MetacardImpl getInvalidMetacard() {
        MetacardImpl returnMetacard = new MetacardImpl();
        returnMetacard.setAttribute(new AttributeImpl(VALIDATION_ERRORS,
                Collections.singletonList("sample-validator")));
        returnMetacard.setAttribute(new AttributeImpl(VALIDATION_WARNINGS,
                Collections.singletonList("sample-validator")));
        return returnMetacard;
    }

    @Test
    public void testResetAttributeMappingEmptyList() {
        metacardValidityFilterPlugin.setAttributeMap(new ArrayList<String>());
        assertThat(metacardValidityFilterPlugin.getAttributeMap(),
                is(new HashMap<String, List<String>>()));
    }

    @Test
    public void testResetAttributeMappingEmptyString() {
        metacardValidityFilterPlugin.setAttributeMap(Arrays.asList(""));
        assertThat(metacardValidityFilterPlugin.getAttributeMap(),
                is(new HashMap<String, List<String>>()));
    }
}


