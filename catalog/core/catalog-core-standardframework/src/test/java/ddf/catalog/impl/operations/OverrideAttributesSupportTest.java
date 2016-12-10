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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;

public class OverrideAttributesSupportTest {

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
                .toString(), is("content:stuff"));
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
                .toString(), is("content:stuff"));
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
                overrideMetacard, true, false);

        assertThat(updatedMetacard.getMetadata(), is("updated"));
        assertThat(updatedMetacard.getTitle(), is("updated"));
        assertThat(updatedMetacard.getResourceURI()
                .toString(), is("content:stuff"));
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
                overrideMetacard, false, false);

        assertThat(updatedMetacard.getMetadata(), is("updated"));
        assertThat(updatedMetacard.getTitle(), is("updated"));
        assertThat(updatedMetacard.getResourceURI()
                .toString(), is("content:stuff"));
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
}
