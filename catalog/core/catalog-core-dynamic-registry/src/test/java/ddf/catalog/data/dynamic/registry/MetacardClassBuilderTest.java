/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.data.dynamic.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import ddf.catalog.data.dynamic.api.MetacardPropertyDescriptor;
import ddf.catalog.data.dynamic.impl.MetacardPropertyDescriptorImpl;

public class MetacardClassBuilderTest {

    @Test
    public void testAddType() throws Exception {
        MetacardClassBuilder mcb = new MetacardClassBuilder();
        mcb.addType("type1");
        assertEquals(1,
                mcb.getTypes()
                        .size());
        mcb.addType("type2");
        mcb.addType("type3");
        assertEquals(3,
                mcb.getTypes()
                        .size());
        assertTrue(mcb.getTypes()
                .contains("type2"));
        assertTrue(mcb.getTypes()
                .contains("type1"));
        assertTrue(mcb.getTypes().contains("type3"));
    }

    @Test
    public void testAddAttribute() throws Exception {
        MetacardAttribute attr1, attr2, attr3, attr4;

        attr1 = buildMetacardAttribute("attr1", "String", true, false, false, true);
        attr2 = buildMetacardAttribute("attr2", "Double", false, true, true, false);
        attr3 = buildMetacardAttribute("attr3", "Integer", true, false, false, true);
        attr4 = buildMetacardAttribute("attr4", "Binary", false, true, true, false);

        MetacardClassBuilder mcb = new MetacardClassBuilder();

        mcb.addAttribute(attr1);

        assertEquals(1,
                mcb.getDescriptorImpls()
                        .size());
        assertEquals("attr1",
                mcb.getDescriptorImpls()
                        .get(0)
                        .getName());

        mcb.addAttribute(attr2);
        assertEquals(2,
                mcb.getDescriptorImpls()
                        .size());
        assertEquals("attr2", mcb.getDescriptorImpls().get(1).getName());
    }

    @Test
    public void testGetTypes() throws Exception {

    }

    @Test
    public void testGetDescriptorsAsArray() throws Exception {
        MetacardAttribute attr1, attr2, attr3, attr4;

        attr1 = buildMetacardAttribute("attr1", "String", true, false, false, true);
        attr2 = buildMetacardAttribute("attr2", "Double", false, true, true, false);
        attr3 = buildMetacardAttribute("attr3", "Integer", true, false, false, true);
        attr4 = buildMetacardAttribute("attr4", "Binary", false, true, true, false);

        MetacardClassBuilder mcb = new MetacardClassBuilder();

        MetacardPropertyDescriptor[] descriptors =  mcb.getDescriptorsAsArray();
        assertEquals(0, descriptors.length);

        mcb.addAttribute(attr1);
        mcb.addAttribute(attr2);
        mcb.addAttribute(attr3);
        mcb.addAttribute(attr4);

        descriptors = mcb.getDescriptorsAsArray();
        assertEquals(4, descriptors.length);
        assertTrue(descriptors[0].equals(attr1.getMetacardPropertyDescriptor()));
        assertTrue(descriptors[1].equals(attr2.getMetacardPropertyDescriptor()));
        assertTrue(descriptors[2].equals(attr3.getMetacardPropertyDescriptor()));
        assertTrue(descriptors[3].equals(attr4.getMetacardPropertyDescriptor()));

    }

    @Test
    public void testGetDescriptorImpls() throws Exception {
        MetacardAttribute attr1, attr2, attr3, attr4;

        attr1 = buildMetacardAttribute("attr1", "String", true, false, false, true);
        attr2 = buildMetacardAttribute("attr2", "Double", false, true, true, false);
        attr3 = buildMetacardAttribute("attr3", "Integer", true, false, false, true);
        attr4 = buildMetacardAttribute("attr4", "Binary", false, true, true, false);

        MetacardClassBuilder mcb = new MetacardClassBuilder();

        List<MetacardPropertyDescriptorImpl> descriptorList =  mcb.getDescriptorImpls();
        assertEquals(0, descriptorList.size());

        mcb.addAttribute(attr1);
        mcb.addAttribute(attr2);
        mcb.addAttribute(attr3);
        mcb.addAttribute(attr4);

        descriptorList = mcb.getDescriptorImpls();
        assertEquals(4, descriptorList.size());
        assertTrue(descriptorList.contains(attr1.getMetacardPropertyDescriptor()));
        assertTrue(descriptorList.contains(attr2.getMetacardPropertyDescriptor()));
        assertTrue(descriptorList.contains(attr3.getMetacardPropertyDescriptor()));
        assertTrue(descriptorList.contains(attr4.getMetacardPropertyDescriptor()));
    }

    @Test
    public void testGetDescriptors() throws Exception {
        MetacardAttribute attr1, attr2, attr3, attr4;

        attr1 = buildMetacardAttribute("attr1", "String", true, false, false, true);
        attr2 = buildMetacardAttribute("attr2", "Double", false, true, true, false);
        attr3 = buildMetacardAttribute("attr3", "Integer", true, false, false, true);
        attr4 = buildMetacardAttribute("attr4", "Binary", false, true, true, false);

        MetacardClassBuilder mcb = new MetacardClassBuilder();

        List<MetacardPropertyDescriptor> descriptorList =  mcb.getDescriptors();
        assertEquals(0, descriptorList.size());

        mcb.addAttribute(attr1);
        mcb.addAttribute(attr2);
        mcb.addAttribute(attr3);
        mcb.addAttribute(attr4);

        descriptorList = mcb.getDescriptors();
        assertEquals(4, descriptorList.size());
        assertTrue(descriptorList.contains(attr1.getMetacardPropertyDescriptor()));
        assertTrue(descriptorList.contains(attr2.getMetacardPropertyDescriptor()));
        assertTrue(descriptorList.contains(attr3.getMetacardPropertyDescriptor()));
        assertTrue(descriptorList.contains(attr4.getMetacardPropertyDescriptor()));
    }

    private MetacardAttribute buildMetacardAttribute(String name, String type,
            boolean indexed, boolean multiValued, boolean stored, boolean tokenized) {
        MetacardAttribute mca = new MetacardAttribute();
        mca.setIndexed(indexed);
        mca.setMultiValued(multiValued);
        mca.setName(name);
        mca.setStored(stored);
        mca.setTokenized(tokenized);
        mca.setType(type);

        return mca;
    }
}