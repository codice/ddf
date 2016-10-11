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
package org.codice.ddf.catalog.plugin.metacard;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.Serializable;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;

/**
 * Assert that {@link MetacardCondition}s behave properly, and maintain the component separation so
 * that they can be mocked out in other tests.
 */
@RunWith(MockitoJUnitRunner.class)
public class MetacardConditionTest {

    private static final Map<String, Serializable> CRITERIA = ImmutableMap.of(

            "name", "bob jones",

            "age", 34,

            "occupation", "retired");

    private MetacardCondition metacardCondition;

    @Before
    public void setup() throws Exception {
        metacardCondition = new MetacardCondition();
    }

    @Test
    public void testAutoTrimmingBehavior() throws Exception {
        metacardCondition.setCriteriaKey("   key  ");
        metacardCondition.setExpectedValue(" value");
        assertThat(metacardCondition.getCriteriaKey(), is("key"));
        assertThat(metacardCondition.getExpectedValue(), is("value"));
    }

    @Test
    public void testMetacardConditionCriteriaWithoutKey() throws Exception {
        metacardCondition.setCriteriaKey("address");
        metacardCondition.setExpectedValue("555 Riverside Road");
        assertThat(metacardCondition.applies(CRITERIA), is(false));
    }

    @Test
    public void testMetacardConditionEqualityFailure() throws Exception {
        metacardCondition.setCriteriaKey("name");
        metacardCondition.setExpectedValue("bob saget");
        assertThat(metacardCondition.applies(CRITERIA), is(false));
    }

    @Test
    public void testMetacardConditionSuccessful() throws Exception {
        metacardCondition.setCriteriaKey("name");
        metacardCondition.setExpectedValue("bob jones");
        assertThat(metacardCondition.applies(CRITERIA), is(true));
    }
}
