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
 **/
package ddf.catalog.impl.operations;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;

public class OverrideAttributesSupportTest {

    private static final String OVERRIDDEN_STRING = "Overridden description.";

    private static final short OVERRIDDEN_SHORT = 123;

    private static final float OVERRIDDEN_FLOAT = 123.456f;

    private static final double OVERRIDDEN_DOUBLE = 123.456;

    private static final DateTime OVERRIDDEN_DATE = new DateTime("2004-04-12T13:20:00");

    private static final boolean OVERRIDDEN_BOOLEAN = true;

    private static final String SHORT_ATTR = "shortAttr";

    private static final String INT_ATTR = "intAttr";

    private static final String LONG_ATTR = "longAttr";

    private static final String FLOAT_ATTR = "floatAttr";

    private static final String DOUBLE_ATTR = "doubleAttr";

    private static final String DATE_ATTR = "dateAttr";

    private static final String STRING_ATTR = "stringAttr";

    private static final String BOOLEAN_ATTR = "boolAttr";

    @Test
    public void testOverrideAttributesBasic() throws URISyntaxException {
        List<ContentItem> contentItems = new ArrayList<>();
        Map<String, Metacard> metacardMap = new HashMap<>();
        MetacardImpl overrideMetacard = new MetacardImpl();
        MetacardImpl metacard = new MetacardImpl(new MetacardTypeImpl("special",
                overrideMetacard.getMetacardType()
                        .getAttributeDescriptors()));
        metacard.setMetadata("original");
        metacard.setTitle("original");
        metacard.setId("original");
        metacard.setResourceURI(new URI("content:stuff"));

        overrideMetacard.setTitle("updated");
        overrideMetacard.setId("updated");
        overrideMetacard.setResourceURI(new URI("content:newstuff"));
        overrideMetacard.setMetadata("updated");
        metacardMap.put(metacard.getId(), metacard);
        contentItems.add(new ContentItemImpl("original", null, "txt/plain", overrideMetacard));

        OverrideAttributesSupport.overrideAttributes(contentItems, metacardMap);

        assertNotNull(metacardMap.get("original"));
        assertThat(metacardMap.get("original")
                .getMetadata(), is("updated"));
        assertThat(metacardMap.get("original")
                .getTitle(), is("updated"));
        assertThat(metacardMap.get("original")
                .getResourceURI()
                .toString(), is("content:newstuff"));
        assertThat(metacardMap.get("original")
                .getId(), is("original"));
        assertThat(metacardMap.get("original")
                .getMetacardType()
                .getName(), is("special"));
    }

    @Test
    public void testOverrideAttributesOther() throws URISyntaxException {
        List<ContentItem> contentItems = new ArrayList<>();
        Map<String, Metacard> metacardMap = new HashMap<>();
        MetacardImpl metacard = new MetacardImpl();
        metacard.setMetadata("original");
        metacard.setTitle("original");
        metacard.setId("original");
        metacard.setResourceURI(new URI("content:stuff"));
        MetacardImpl overrideMetacard = new MetacardImpl(new MetacardTypeImpl("other",
                metacard.getMetacardType()
                        .getAttributeDescriptors()));
        overrideMetacard.setTitle("updated");
        overrideMetacard.setId("updated");
        overrideMetacard.setMetadata("updated");
        overrideMetacard.setResourceURI(new URI("content:newstuff"));
        metacardMap.put(metacard.getId(), metacard);
        contentItems.add(new ContentItemImpl("original", null, "txt/plain", overrideMetacard));

        OverrideAttributesSupport.overrideAttributes(contentItems, metacardMap);

        assertNotNull(metacardMap.get("original"));
        assertThat(metacardMap.get("original")
                .getMetadata(), is("updated"));
        assertThat(metacardMap.get("original")
                .getTitle(), is("updated"));
        assertThat(metacardMap.get("original")
                .getResourceURI()
                .toString(), is("content:newstuff"));
        assertThat(metacardMap.get("original")
                .getId(), is("original"));
        assertThat(metacardMap.get("original")
                .getMetacardType()
                .getName(), is("other"));
    }

    @Test
    public void testOverrideMetacardIgnoreType() throws URISyntaxException {
        MetacardImpl metacard = new MetacardImpl();
        metacard.setMetadata("original");
        metacard.setTitle("original");
        metacard.setId("original");
        metacard.setResourceURI(new URI("content:stuff"));
        MetacardImpl overrideMetacard = new MetacardImpl(new MetacardTypeImpl("other",
                metacard.getMetacardType()
                        .getAttributeDescriptors()));
        overrideMetacard.setTitle("updated");
        overrideMetacard.setId("updated");
        overrideMetacard.setMetadata("updated");
        overrideMetacard.setResourceURI(new URI("content:newstuff"));

        Metacard updatedMetacard = OverrideAttributesSupport.overrideMetacard(metacard,
                overrideMetacard,
                true,
                false);

        assertThat(updatedMetacard.getMetadata(), is("updated"));
        assertThat(updatedMetacard.getTitle(), is("updated"));
        assertThat(updatedMetacard.getResourceURI()
                .toString(), is("content:newstuff"));
        assertThat(updatedMetacard.getId(), is("original"));
        assertThat(updatedMetacard.getMetacardType()
                .getName(), is("ddf.metacard"));
    }

    @Test
    public void testOverrideMetacard() throws URISyntaxException {
        MetacardImpl metacard = new MetacardImpl();
        metacard.setMetadata("original");
        metacard.setTitle("original");
        metacard.setId("original");
        metacard.setResourceURI(new URI("content:stuff"));
        MetacardImpl overrideMetacard = new MetacardImpl(new MetacardTypeImpl("other",
                metacard.getMetacardType()
                        .getAttributeDescriptors()));
        overrideMetacard.setTitle("updated");
        overrideMetacard.setId("updated");
        overrideMetacard.setMetadata("updated");
        overrideMetacard.setResourceURI(new URI("content:newstuff"));

        Metacard updatedMetacard = OverrideAttributesSupport.overrideMetacard(metacard,
                overrideMetacard,
                false,
                false);

        assertThat(updatedMetacard.getMetadata(), is("updated"));
        assertThat(updatedMetacard.getTitle(), is("updated"));
        assertThat(updatedMetacard.getResourceURI()
                .toString(), is("content:newstuff"));
        assertThat(updatedMetacard.getId(), is("original"));
        assertThat(updatedMetacard.getMetacardType()
                .getName(), is("other"));
    }

    @Test
    public void testNoOverrideMetacard() throws URISyntaxException {
        List<ContentItem> contentItems = new ArrayList<>();
        Map<String, Metacard> metacardMap = new HashMap<>();
        MetacardImpl metacard = new MetacardImpl();
        metacard.setMetadata("original");
        metacard.setTitle("original");
        metacard.setId("original");
        metacard.setResourceURI(new URI("content:stuff"));
        metacardMap.put(metacard.getId(), metacard);
        contentItems.add(new ContentItemImpl("original", null, "txt/plain", null));

        OverrideAttributesSupport.overrideAttributes(contentItems, metacardMap);

        assertNotNull(metacardMap.get("original"));
        assertThat(metacardMap.get("original")
                .getMetadata(), is("original"));
        assertThat(metacardMap.get("original")
                .getTitle(), is("original"));
        assertThat(metacardMap.get("original")
                .getResourceURI()
                .toString(), is("content:stuff"));
        assertThat(metacardMap.get("original")
                .getId(), is("original"));
    }

    @Test
    public void testNoContentItems() throws URISyntaxException {
        List<ContentItem> contentItems = new ArrayList<>();
        Map<String, Metacard> metacardMap = new HashMap<>();
        MetacardImpl metacard = new MetacardImpl();
        metacard.setMetadata("original");
        metacard.setTitle("original");
        metacard.setId("original");
        metacard.setResourceURI(new URI("content:stuff"));
        metacardMap.put(metacard.getId(), metacard);

        OverrideAttributesSupport.overrideAttributes(contentItems, metacardMap);

        assertNotNull(metacardMap.get("original"));
        assertThat(metacardMap.get("original")
                .getMetadata(), is("original"));
        assertThat(metacardMap.get("original")
                .getTitle(), is("original"));
        assertThat(metacardMap.get("original")
                .getResourceURI()
                .toString(), is("content:stuff"));
        assertThat(metacardMap.get("original")
                .getId(), is("original"));
    }

    @Test
    public void testApplyAttributeOverridesToMetacardMap() throws Exception {
        Map<String, Metacard> metacardMap = createMetacardMap();
        Map<String, Serializable> attributeMap = createAttributeMap();

        OverrideAttributesSupport.applyAttributeOverridesToMetacardMap(attributeMap, metacardMap);

        metacardMap.values()
                .forEach(metacard -> assertAttributesOverridden(metacard));
    }

    private void assertAttributesOverridden(Metacard metacard) {
        assertThat(metacard.getAttribute(STRING_ATTR)
                .getValue(), is(OVERRIDDEN_STRING));
        assertThat(metacard.getAttribute(SHORT_ATTR)
                .getValue(), is(OVERRIDDEN_SHORT));
        assertThat(metacard.getAttribute(INT_ATTR)
                .getValue(), is(Integer.valueOf(OVERRIDDEN_SHORT)));
        assertThat(metacard.getAttribute(LONG_ATTR)
                .getValue(), is(Long.valueOf(OVERRIDDEN_SHORT)));
        assertThat(metacard.getAttribute(FLOAT_ATTR)
                .getValue(), is(OVERRIDDEN_FLOAT));
        assertThat(metacard.getAttribute(DOUBLE_ATTR)
                .getValue(), is(OVERRIDDEN_DOUBLE));
        assertThat(metacard.getAttribute(DATE_ATTR)
                .getValue(), is(OVERRIDDEN_DATE.toDate()));
        assertThat(metacard.getAttribute(BOOLEAN_ATTR)
                .getValue(), is(OVERRIDDEN_BOOLEAN));
    }

    private Map<String, Serializable> createAttributeMap() {
        return new ImmutableMap.Builder<String, Serializable>().put(STRING_ATTR, OVERRIDDEN_STRING)
                .put(SHORT_ATTR, OVERRIDDEN_SHORT)
                .put(INT_ATTR, OVERRIDDEN_SHORT)
                .put(LONG_ATTR, OVERRIDDEN_SHORT)
                .put(FLOAT_ATTR, OVERRIDDEN_FLOAT)
                .put(DOUBLE_ATTR, OVERRIDDEN_DOUBLE)
                .put(DATE_ATTR, OVERRIDDEN_DATE)
                .put(BOOLEAN_ATTR, OVERRIDDEN_BOOLEAN)
                .build();
    }

    private Map<String, Metacard> createMetacardMap() {
        MetacardType mockMetacardType = mock(MetacardType.class);
        doReturn(new AttributeDescriptorImpl(SHORT_ATTR,
                false /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.SHORT_TYPE)).when(mockMetacardType)
                .getAttributeDescriptor(SHORT_ATTR);
        doReturn(new AttributeDescriptorImpl(INT_ATTR,
                false /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.INTEGER_TYPE)).when(mockMetacardType)
                .getAttributeDescriptor(INT_ATTR);
        doReturn(new AttributeDescriptorImpl(LONG_ATTR,
                false /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.LONG_TYPE)).when(mockMetacardType)
                .getAttributeDescriptor(LONG_ATTR);
        doReturn(new AttributeDescriptorImpl(FLOAT_ATTR,
                false /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.FLOAT_TYPE)).when(mockMetacardType)
                .getAttributeDescriptor(FLOAT_ATTR);
        doReturn(new AttributeDescriptorImpl(DOUBLE_ATTR,
                false /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.DOUBLE_TYPE)).when(mockMetacardType)
                .getAttributeDescriptor(DOUBLE_ATTR);
        doReturn(new AttributeDescriptorImpl(DATE_ATTR,
                false /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.DATE_TYPE)).when(mockMetacardType)
                .getAttributeDescriptor(DATE_ATTR);
        doReturn(new AttributeDescriptorImpl(STRING_ATTR,
                false /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.STRING_TYPE)).when(mockMetacardType)
                .getAttributeDescriptor(STRING_ATTR);
        doReturn(new AttributeDescriptorImpl(BOOLEAN_ATTR,
                false /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.BOOLEAN_TYPE)).when(mockMetacardType)
                .getAttributeDescriptor(BOOLEAN_ATTR);

        Metacard metacard1 = new MetacardImpl(mockMetacardType);
        Metacard metacard2 = new MetacardImpl(mockMetacardType);

        return ImmutableMap.of("1234", metacard1, "0987", metacard2);
    }
}
