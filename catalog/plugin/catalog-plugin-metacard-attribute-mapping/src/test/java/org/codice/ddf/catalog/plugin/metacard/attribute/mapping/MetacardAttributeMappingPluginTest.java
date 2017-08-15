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
package org.codice.ddf.catalog.plugin.metacard.attribute.mapping;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.security.expansion.impl.StraightExpansionImpl;

public class MetacardAttributeMappingPluginTest {

    private MetacardAttributeMappingPlugin metacardAttributeMappingPlugin;

    private MetacardImpl metacard1, metacard2, metacard3;

    private static final String TITLE = "title";

    private static final String POINT_OF_CONTACT = "point-of-contact";

    private static final String LOCATION = "location";

    private String[] redColorExpansion = new String[] {"RD", "Red"};

    private String[] blueColorExpansion = new String[] {"BL", "Blue"};

    private String[] yellowColorExpansion = new String[] {"YL", "Yellow"};

    private String[] ddfSoftwareExpansion = new String[] {"DDF", "DistributedDataFramework"};

    private String[] usaLocationExpansion = new String[] {"USA", "UnitedStatesofAmerica"};

    @Before
    public void setUp() {
        metacardAttributeMappingPlugin = new MetacardAttributeMappingPlugin();
        List<String[]> ruleList1 = Arrays.asList(redColorExpansion,
                blueColorExpansion,
                yellowColorExpansion);
        List<String[]> ruleList2 = Arrays.<String[]>asList(ddfSoftwareExpansion);
        List<String[]> ruleList3 = Arrays.<String[]>asList(usaLocationExpansion);

        StraightExpansionImpl straightExpansion = new StraightExpansionImpl();

        //title:RD:Red
        //title:BL:Blue
        //title:YL:Yellow
        straightExpansion.addExpansionList(TITLE, ruleList1);

        //point-of-contact:DDF:Distributed Data Framework
        straightExpansion.addExpansionList(POINT_OF_CONTACT, ruleList2);

        //location:USA:United States of America
        straightExpansion.addExpansionList(LOCATION, ruleList3);
        metacardAttributeMappingPlugin.setExpansionService(straightExpansion);

        //Metacard 1:
        //title = RD
        //point-of-contact = DDF
        //location = USA
        metacard1 = new MetacardImpl();
        metacard1.setAttribute(TITLE, redColorExpansion[0]);
        metacard1.setAttribute(POINT_OF_CONTACT, ddfSoftwareExpansion[0]);
        metacard1.setAttribute(LOCATION, usaLocationExpansion[0]);

        //Metacard 2:
        //title = BL
        metacard2 = new MetacardImpl();
        metacard2.setAttribute(TITLE, blueColorExpansion[0]);

        //Metacard 3:
        //title = YL
        metacard3 = new MetacardImpl();
        metacard3.setAttribute(TITLE, yellowColorExpansion[0]);
    }

    /*
        Test that if there exists an expansion for a metacard attribute that it is expanded.
    */
    @Test
    public void testProcessMetacardMultipleAttributesWithExpansion() throws Exception {

        CreateRequest input = new CreateRequestImpl(Arrays.asList(metacard1));

        CreateRequest output = metacardAttributeMappingPlugin.process(input);

        assertThat(output.getMetacards()
                .get(0)
                .getAttribute(TITLE)
                .getValue(), is(redColorExpansion[1]));
        assertThat(output.getMetacards()
                .get(0)
                .getAttribute(POINT_OF_CONTACT)
                .getValue(), is(ddfSoftwareExpansion[1]));
        assertThat(output.getMetacards()
                .get(0)
                .getAttribute(LOCATION)
                .getValue(), is(usaLocationExpansion[1]));
    }

    /*
        Test that all metacards that have an attribute that can be expanded get expanded
    */
    @Test
    public void testCreateMetacardsWithExpansionSameAttribute() throws Exception {
        CreateRequest input = new CreateRequestImpl(Arrays.asList(metacard1, metacard2, metacard3));
        CreateRequest output = metacardAttributeMappingPlugin.process(input);

        assertThat(output.getMetacards()
                .get(0)
                .getAttribute(TITLE)
                .getValue(), is(redColorExpansion[1]));
        assertThat(output.getMetacards()
                .get(1)
                .getAttribute(TITLE)
                .getValue(), is(blueColorExpansion[1]));
        assertThat(output.getMetacards()
                .get(2)
                .getAttribute(TITLE)
                .getValue(), is(yellowColorExpansion[1]));
    }

    /*
        Test that if there is no expansion available for an attribute that
        it is not modified by the plugin.
     */
    @Test
    public void testCreateMetacardWithAttributeWithNoExpansion() throws Exception {
        String attrValue = "rainbow";
        metacard1.setAttribute(new AttributeImpl(TITLE, attrValue));

        CreateRequest input = new CreateRequestImpl(Arrays.asList(metacard1));
        CreateRequest output = metacardAttributeMappingPlugin.process(input);

        assertThat(output.getMetacards()
                .get(0)
                .getAttribute(TITLE)
                .getValue(), is(attrValue));
    }

    /*
         Test that metacards unrelated to the configured ruleset will not
         be affected by configuration mapping
     */
    @Test
    public void testCreateMetacardNoMatchingAttributes() throws Exception {
        MetacardImpl metacard = new MetacardImpl();

        CreateRequest input = new CreateRequestImpl(Arrays.asList(metacard));
        CreateRequest output = metacardAttributeMappingPlugin.process(input);

        assertThat(output.getMetacards(), is(input.getMetacards()));
    }

    /*
        Test that if there exists an expansion for a metacard attribute that it is expanded when
         updating the metacard.
     */
    @Test
    public void testUpdateMetacardMultipleAttributesWithExpansion() throws Exception {
        UpdateRequest input = new UpdateRequestImpl(null, metacard1);
        UpdateRequest output = metacardAttributeMappingPlugin.process(input);

        assertThat(output.getUpdates()
                .get(0)
                .getValue()
                .getAttribute(TITLE)
                .getValue(), is(redColorExpansion[1]));
        assertThat(output.getUpdates()
                .get(0)
                .getValue()
                .getAttribute(POINT_OF_CONTACT)
                .getValue(), is(ddfSoftwareExpansion[1]));
        assertThat(output.getUpdates()
                .get(0)
                .getValue()
                .getAttribute(LOCATION)
                .getValue(), is(usaLocationExpansion[1]));
    }

    /*
        Test that if all values get expanded when an attribute is multivalued
     */
    @Test
    public void testMultiAttributeMetacardAllGetExpanded() throws Exception {
        MetacardImpl metacard = new MetacardImpl();
        ArrayList<String> colors = new ArrayList<>(Arrays.asList("RD", "BL", "YL"));
        metacard.setAttribute(TITLE, colors);

        CreateRequest input = new CreateRequestImpl(metacard);
        CreateRequest output = metacardAttributeMappingPlugin.process(input);
        assertThat(output.getMetacards()
                .get(0)
                .getAttribute(TITLE)
                .getValues(), containsInAnyOrder("Red", "Blue", "Yellow"));
    }

    /*
        Test that correct values get expanded when an attribute is multivalued
    */
    @Test
    public void testMultiAttributeMetacardSomeGetExpanded() throws Exception {
        MetacardImpl metacard = new MetacardImpl();
        ArrayList<String> colors = new ArrayList<>(Arrays.asList("RD", "Blue", "YL", "Rainbow"));
        metacard.setAttribute(TITLE, colors);

        CreateRequest input = new CreateRequestImpl(metacard);
        CreateRequest output = metacardAttributeMappingPlugin.process(input);
        assertThat(output.getMetacards()
                .get(0)
                .getAttribute(TITLE)
                .getValues(), containsInAnyOrder("Red", "Blue", "Yellow", "Rainbow"));
    }
}
