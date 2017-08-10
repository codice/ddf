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
package org.codice.ddf.catalog.plugin.configuration.mapping;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.security.expansion.impl.StraightExpansionImpl;

public class ConfigurationMappingPluginTest {

    ConfigurationMappingPlugin configurationMappingPlugin;

    StraightExpansionImpl straightExpansion;

    Metacard metacard1, metacard2, metacard3;

    private static final String ROLES = "roles";

    private static final String LOCATION = "location";

    private static final String EDUCATION = "education";

    private String[] rolesRuleA = new String[] {"VP-Sales", "VP-Sales VP Sales"};

    private String[] rolesRuleB = new String[] {"VP", "VP Manager"};

    private String[] rolesRuleC = new String[] {"Manager", "Manager Employee"};

    private String[] locationRule = new String[] {"AZ", "AZ Arizona"};

    private String[] educationRule = new String[] {"ASU", "Arizona State University"};

    @Before
    public void setUp() {

        configurationMappingPlugin = new ConfigurationMappingPlugin();

        // create expansion
        List<String[]> ruleList1 = Arrays.asList(rolesRuleA, rolesRuleB, rolesRuleC);
        List<String[]> ruleList2 = Arrays.<String[]>asList(locationRule);
        List<String[]> ruleList3 = Arrays.<String[]>asList(educationRule);

        straightExpansion = new StraightExpansionImpl();
        straightExpansion.addExpansionList(ROLES, ruleList1);
        straightExpansion.addExpansionList(LOCATION, ruleList2);
        straightExpansion.addExpansionList(EDUCATION, ruleList3);
        configurationMappingPlugin.setExpansionService(straightExpansion);

        // set up Attribute Objects
        Attribute attribute1a = new AttributeImpl(ROLES, rolesRuleA[0]);
        Attribute attribute1b = new AttributeImpl(ROLES, rolesRuleB[0]);
        Attribute attribute1c = new AttributeImpl(ROLES, rolesRuleC[0]);
        Attribute attribute2 = new AttributeImpl(LOCATION, locationRule[0]);
        Attribute attribute3 = new AttributeImpl(EDUCATION, educationRule[0]);

        // mock metacards
        metacard1 = new MetacardImpl();
        metacard2 = new MetacardImpl();
        metacard3 = new MetacardImpl();

        metacard1.setAttribute(attribute1a);
        metacard1.setAttribute(attribute2);
        metacard1.setAttribute(attribute3);

        metacard2.setAttribute(attribute1b);
        metacard3.setAttribute(attribute1c);
    }

    @Test
    public void testProcessMetacardMultipleAttributesWithExpansion() throws Exception {

        CreateRequest input = new CreateRequestImpl(Arrays.asList(metacard1));

        CreateRequest output = configurationMappingPlugin.process(input);

        assert (output.getMetacards()
                .get(0)
                .getAttribute(ROLES)
                .getValue()).equals(rolesRuleA[1]);
        assert (output.getMetacards()
                .get(0)
                .getAttribute(LOCATION)
                .getValue()).equals(locationRule[1]);
        assert (output.getMetacards()
                .get(0)
                .getAttribute(EDUCATION)
                .getValue()).equals(educationRule[1]);
    }

    @Test
    public void testProcessMetacardsWithExpansionSameAttribute() throws Exception {

        CreateRequest input = new CreateRequestImpl(Arrays.asList(metacard1, metacard2, metacard3));

        CreateRequest output = configurationMappingPlugin.process(input);

        assert (output.getMetacards()
                .get(0)
                .getAttribute(ROLES)
                .getValue()).equals(rolesRuleA[1]);
        assert (output.getMetacards()
                .get(1)
                .getAttribute(ROLES)
                .getValue()).equals(rolesRuleB[1]);
        assert (output.getMetacards()
                .get(2)
                .getAttribute(ROLES)
                .getValue()).equals(rolesRuleC[1]);
    }

    @Test
    public void testAttributeWithNoExpansion() throws Exception {

        String attrValue = "not accounted for";

        metacard1.setAttribute(new AttributeImpl("roles", attrValue));

        CreateRequest input = new CreateRequestImpl(Arrays.asList(metacard1));

        CreateRequest output = configurationMappingPlugin.process(input);

        assert (output.getMetacards()
                .get(0)
                .getAttribute(ROLES)
                .getValue()).equals(attrValue);
    }

    @Test
    public void testUpdateMetacardMultipleAttributesWithExpansion() throws Exception {

        UpdateRequest input = new UpdateRequestImpl(null, metacard1);

        UpdateRequest output = configurationMappingPlugin.process(input);

        assert (output.getUpdates()
                .get(0)
                .getValue()
                .getAttribute(ROLES)
                .getValue()).equals(rolesRuleA[1]);
        assert (output.getUpdates()
                .get(0)
                .getValue()
                .getAttribute(LOCATION)
                .getValue()).equals(locationRule[1]);
        assert (output.getUpdates()
                .get(0)
                .getValue()
                .getAttribute(EDUCATION)
                .getValue()).equals(educationRule[1]);
    }
}
