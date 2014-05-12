/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.plugin.groomer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateRequestImpl;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteRequestImpl;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.plugin.groomer.metacard.StandardMetacardGroomerPlugin;

/**
 * Tests the class {@link StandardMetacardGroomerPlugin}
 * 
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 */
public class TestMetacardGroomerPlugin {
    private static final String SAMPLE_ID = "12345678900987654321abcdeffedcba";

    private static final String DEFAULT_METADATA = "<sample>sample</sample>";

    public static final String DEFAULT_TITLE = "Flagstaff";

    public static final String DEFAULT_VERSION = "mockVersion";

    public static final String DEFAULT_TYPE = "simple";

    public static final String DEFAULT_LOCATION = "POINT (1 0)";

    public static final String DEFAULT_SOURCE_ID = "ddf";

    @Test
    public void testCreateWithNullRequest() throws PluginExecutionException,
        StopProcessingException {

        StandardMetacardGroomerPlugin plugin = new StandardMetacardGroomerPlugin();

        CreateRequest returnedRequest = plugin.process((CreateRequest) null);

        assertThat(returnedRequest, nullValue());

    }

    @Test
    public void testCreateWithNullRecords() throws PluginExecutionException,
        StopProcessingException {

        StandardMetacardGroomerPlugin plugin = new StandardMetacardGroomerPlugin();

        CreateRequest request = mock(CreateRequest.class);

        when(request.getMetacards()).thenReturn(null);

        CreateRequest returnedRequest = plugin.process(request);

        assertThat(returnedRequest, not(nullValue()));

        assertThat(returnedRequest.getMetacards(), nullValue());

    }

    @Test
    public void testCreateWithNoRecords() throws PluginExecutionException, StopProcessingException {

        StandardMetacardGroomerPlugin plugin = new StandardMetacardGroomerPlugin();

        CreateRequest request = mock(CreateRequest.class);

        when(request.getMetacards()).thenReturn(new ArrayList<Metacard>());

        CreateRequest returnedRequest = plugin.process(request);

        assertThat(returnedRequest, not(nullValue()));

        assertThat(returnedRequest.getMetacards(), not(nullValue()));

        assertThat(returnedRequest.getMetacards().size(), is(0));

    }

    @Test
    public void testCreateStandard() throws PluginExecutionException, StopProcessingException {

        Metacard inputMetacard = getStandardMetacard();

        Metacard outputMetacard = processCreate(inputMetacard);

        assertNotNull(outputMetacard.getId());
        assertEquals(DEFAULT_TITLE, outputMetacard.getTitle());
        assertEquals(DEFAULT_LOCATION, outputMetacard.getLocation());
        assertEquals(DEFAULT_TYPE, outputMetacard.getContentTypeName());
        assertEquals(DEFAULT_VERSION, outputMetacard.getContentTypeVersion());
        assertThat(outputMetacard.getMetadata(), is(DEFAULT_METADATA));
        assertThat(outputMetacard.getCreatedDate().getTime(), is(greaterThan(inputMetacard
                .getCreatedDate().getTime())));
        assertThat(outputMetacard.getModifiedDate().getTime(), is(greaterThan(inputMetacard
                .getModifiedDate().getTime())));
        assertEquals(inputMetacard.getEffectiveDate(), outputMetacard.getEffectiveDate());
        assertEquals(inputMetacard.getExpirationDate(), outputMetacard.getExpirationDate());
        assertTrue(Arrays.equals(inputMetacard.getThumbnail(), outputMetacard.getThumbnail()));
        assertEquals(inputMetacard.getLocation(), outputMetacard.getLocation());

    }

    @Test
    public void testCreateNoDates() throws PluginExecutionException, StopProcessingException {

        Date snapshotOfNow = new Date();
        Metacard inputMetacard = getStandardMetacard();
        inputMetacard.setAttribute(new AttributeImpl(Metacard.CREATED, null));
        inputMetacard.setAttribute(new AttributeImpl(Metacard.MODIFIED, null));
        inputMetacard.setAttribute(new AttributeImpl(Metacard.EXPIRATION, null));
        inputMetacard.setAttribute(new AttributeImpl(Metacard.EFFECTIVE, null));

        Metacard outputMetacard = processCreate(inputMetacard);

        assertNotNull(outputMetacard.getId());
        assertEquals(DEFAULT_TITLE, outputMetacard.getTitle());
        assertEquals(DEFAULT_LOCATION, outputMetacard.getLocation());
        assertEquals(DEFAULT_TYPE, outputMetacard.getContentTypeName());
        assertEquals(DEFAULT_VERSION, outputMetacard.getContentTypeVersion());
        assertThat(outputMetacard.getMetadata(), is(DEFAULT_METADATA));

        // CREATED
        assertThat(inputMetacard.getCreatedDate(), is(nullValue()));
        assertThat(outputMetacard.getCreatedDate().getTime(),
                is(greaterThanOrEqualTo(snapshotOfNow.getTime())));

        // MODIFIED
        assertThat(inputMetacard.getModifiedDate(), is(nullValue()));
        assertThat(outputMetacard.getModifiedDate().getTime(),
                is(greaterThanOrEqualTo(snapshotOfNow.getTime())));

        // EFFECTIVE
        assertEquals(inputMetacard.getEffectiveDate(), outputMetacard.getEffectiveDate());

        // EXPIRATION
        assertEquals(inputMetacard.getExpirationDate(), outputMetacard.getExpirationDate());

        assertTrue(Arrays.equals(inputMetacard.getThumbnail(), outputMetacard.getThumbnail()));
        assertEquals(inputMetacard.getLocation(), outputMetacard.getLocation());

    }

    @Test
    public void testCreateNoTitleEmptyString() throws PluginExecutionException,
        StopProcessingException, ParseException {

        Metacard inputMetacard = getStandardMetacard();
        inputMetacard.setAttribute(new AttributeImpl(Metacard.TITLE, ""));

        Metacard outputMetacard = processCreate(inputMetacard);

        assertNotNull(outputMetacard.getId());

        assertThat(outputMetacard.getTitle(), is(""));
        assertEquals(DEFAULT_LOCATION, outputMetacard.getLocation());
        assertEquals(DEFAULT_TYPE, outputMetacard.getContentTypeName());
        assertEquals(DEFAULT_VERSION, outputMetacard.getContentTypeVersion());
        assertThat(outputMetacard.getMetadata(), is(DEFAULT_METADATA));
        assertThat(outputMetacard.getCreatedDate().getTime(), is(greaterThan(inputMetacard
                .getCreatedDate().getTime())));
        assertThat(outputMetacard.getModifiedDate().getTime(), is(greaterThan(inputMetacard
                .getModifiedDate().getTime())));
        assertEquals(inputMetacard.getEffectiveDate(), outputMetacard.getEffectiveDate());
        assertEquals(inputMetacard.getExpirationDate(), outputMetacard.getExpirationDate());
        assertTrue(Arrays.equals(inputMetacard.getThumbnail(), outputMetacard.getThumbnail()));
        assertEquals(inputMetacard.getLocation(), outputMetacard.getLocation());

    }

    @Test
    public void testCreateNoTitleNull() throws PluginExecutionException, StopProcessingException,
        ParseException {

        Metacard inputMetacard = getStandardMetacard();
        inputMetacard.setAttribute(new AttributeImpl(Metacard.TITLE, null));

        Metacard outputMetacard = processCreate(inputMetacard);

        assertNotNull(outputMetacard.getId());

        assertThat(outputMetacard.getTitle(), is(nullValue()));
        assertEquals(DEFAULT_LOCATION, outputMetacard.getLocation());
        assertEquals(DEFAULT_TYPE, outputMetacard.getContentTypeName());
        assertEquals(DEFAULT_VERSION, outputMetacard.getContentTypeVersion());
        assertThat(outputMetacard.getMetadata(), is(DEFAULT_METADATA));
        assertThat(outputMetacard.getCreatedDate().getTime(), is(greaterThan(inputMetacard
                .getCreatedDate().getTime())));
        assertThat(outputMetacard.getModifiedDate().getTime(), is(greaterThan(inputMetacard
                .getModifiedDate().getTime())));
        assertEquals(inputMetacard.getEffectiveDate(), outputMetacard.getEffectiveDate());
        assertEquals(inputMetacard.getExpirationDate(), outputMetacard.getExpirationDate());
        assertTrue(Arrays.equals(inputMetacard.getThumbnail(), outputMetacard.getThumbnail()));
        assertEquals(inputMetacard.getLocation(), outputMetacard.getLocation());

    }

    @Test
    public void testCreateNoContentTypeNull() throws PluginExecutionException,
        StopProcessingException, ParseException {

        Metacard inputMetacard = getStandardMetacard();
        inputMetacard.setAttribute(new AttributeImpl(Metacard.CONTENT_TYPE, null));
        inputMetacard.setAttribute(new AttributeImpl(Metacard.CONTENT_TYPE_VERSION, null));

        Metacard outputMetacard = processCreate(inputMetacard);

        assertNotNull(outputMetacard.getId());
        assertEquals(DEFAULT_TITLE, outputMetacard.getTitle());
        assertEquals(DEFAULT_LOCATION, outputMetacard.getLocation());
        assertThat(outputMetacard.getContentTypeName(), is(nullValue()));
        assertThat(outputMetacard.getContentTypeVersion(), is(nullValue()));
        assertThat(outputMetacard.getMetadata(), is(DEFAULT_METADATA));
        assertThat(outputMetacard.getCreatedDate().getTime(), is(greaterThan(inputMetacard
                .getCreatedDate().getTime())));
        assertThat(outputMetacard.getModifiedDate().getTime(), is(greaterThan(inputMetacard
                .getModifiedDate().getTime())));
        assertEquals(inputMetacard.getEffectiveDate(), outputMetacard.getEffectiveDate());
        assertEquals(inputMetacard.getExpirationDate(), outputMetacard.getExpirationDate());
        assertTrue(Arrays.equals(inputMetacard.getThumbnail(), outputMetacard.getThumbnail()));
        assertEquals(inputMetacard.getLocation(), outputMetacard.getLocation());

    }

    @Test
    public void testDelete() throws PluginExecutionException, StopProcessingException,
        ParseException {

        DeleteRequest inputRequest = new DeleteRequestImpl(SAMPLE_ID);

        StandardMetacardGroomerPlugin plugin = new StandardMetacardGroomerPlugin();

        DeleteRequest returnedRequest = plugin.process(inputRequest);

        assertNotNull(returnedRequest);

        assertThat(returnedRequest, equalTo(inputRequest));

    }

    @Test
    public void testUpdateStandard() throws PluginExecutionException, StopProcessingException {

        Metacard inputMetacard = getStandardMetacard();
        inputMetacard.setAttribute(new AttributeImpl(Metacard.ID, SAMPLE_ID));

        Metacard outputMetacard = processUpdate(inputMetacard);

        assertThat(inputMetacard.getId(), is(outputMetacard.getId()));
        assertEquals(DEFAULT_TITLE, outputMetacard.getTitle());
        assertEquals(DEFAULT_LOCATION, outputMetacard.getLocation());
        assertEquals(DEFAULT_TYPE, outputMetacard.getContentTypeName());
        assertEquals(DEFAULT_VERSION, outputMetacard.getContentTypeVersion());
        assertThat(outputMetacard.getMetadata(), is(DEFAULT_METADATA));
        assertThat(outputMetacard.getCreatedDate().getTime(), is(inputMetacard.getCreatedDate()
                .getTime()));
        assertThat(outputMetacard.getModifiedDate().getTime(), is(greaterThan(inputMetacard
                .getModifiedDate().getTime())));
        assertEquals(inputMetacard.getEffectiveDate(), outputMetacard.getEffectiveDate());
        assertEquals(inputMetacard.getExpirationDate(), outputMetacard.getExpirationDate());
        assertTrue(Arrays.equals(inputMetacard.getThumbnail(), outputMetacard.getThumbnail()));
        assertEquals(inputMetacard.getLocation(), outputMetacard.getLocation());

    }

    @Test
    public void testUpdateNoId() throws PluginExecutionException, StopProcessingException {

        Metacard inputMetacard = getStandardMetacard();

        Metacard outputMetacard = processUpdate(inputMetacard);

        assertThat(inputMetacard.getId(), is(nullValue()));
        assertThat(outputMetacard.getId(), is(SAMPLE_ID));
        assertEquals(DEFAULT_TITLE, outputMetacard.getTitle());
        assertEquals(DEFAULT_LOCATION, outputMetacard.getLocation());
        assertEquals(DEFAULT_TYPE, outputMetacard.getContentTypeName());
        assertEquals(DEFAULT_VERSION, outputMetacard.getContentTypeVersion());
        assertThat(outputMetacard.getMetadata(), is(DEFAULT_METADATA));
        assertThat(outputMetacard.getCreatedDate().getTime(), is(inputMetacard.getCreatedDate()
                .getTime()));
        assertThat(outputMetacard.getModifiedDate().getTime(), is(greaterThan(inputMetacard
                .getModifiedDate().getTime())));
        assertEquals(inputMetacard.getEffectiveDate(), outputMetacard.getEffectiveDate());
        assertEquals(inputMetacard.getExpirationDate(), outputMetacard.getExpirationDate());
        assertTrue(Arrays.equals(inputMetacard.getThumbnail(), outputMetacard.getThumbnail()));
        assertEquals(inputMetacard.getLocation(), outputMetacard.getLocation());

    }

    @Test
    public void testUpdateByAlternativeIdentifier() throws PluginExecutionException,
        StopProcessingException, URISyntaxException {

        Metacard inputMetacard = getStandardMetacard();

        URI[] uris = new URI[1];
        uris[0] = new URI(SAMPLE_ID);
        UpdateRequestImpl inputRequest = new UpdateRequestImpl(uris,
                Arrays.asList(copy(inputMetacard)));

        StandardMetacardGroomerPlugin plugin = new StandardMetacardGroomerPlugin();

        UpdateRequest returnedRequest = plugin.process(inputRequest);

        assertNotNull(returnedRequest);

        assertThat(returnedRequest.getUpdates().size(), is(1));

        Metacard outputMetacard = returnedRequest.getUpdates().get(0).getValue();

        assertThat(inputMetacard.getId(), is(outputMetacard.getId()));
        assertEquals(DEFAULT_TITLE, outputMetacard.getTitle());
        assertEquals(DEFAULT_LOCATION, outputMetacard.getLocation());
        assertEquals(DEFAULT_TYPE, outputMetacard.getContentTypeName());
        assertEquals(DEFAULT_VERSION, outputMetacard.getContentTypeVersion());
        assertThat(outputMetacard.getMetadata(), is(DEFAULT_METADATA));
        assertThat(outputMetacard.getCreatedDate().getTime(), is(inputMetacard.getCreatedDate()
                .getTime()));
        assertThat(outputMetacard.getModifiedDate().getTime(), is(greaterThan(inputMetacard
                .getModifiedDate().getTime())));
        assertEquals(inputMetacard.getEffectiveDate(), outputMetacard.getEffectiveDate());
        assertEquals(inputMetacard.getExpirationDate(), outputMetacard.getExpirationDate());
        assertTrue(Arrays.equals(inputMetacard.getThumbnail(), outputMetacard.getThumbnail()));
        assertEquals(inputMetacard.getLocation(), outputMetacard.getLocation());

    }

    @Test
    public void testUpdateEmptyFields() throws PluginExecutionException, StopProcessingException,
        ParseException {

        Date snapshotOfNow = new Date();
        Metacard inputMetacard = getStandardMetacard();
        inputMetacard.setAttribute(new AttributeImpl(Metacard.TITLE, null));
        inputMetacard.setAttribute(new AttributeImpl(Metacard.GEOGRAPHY, null));
        inputMetacard.setAttribute(new AttributeImpl(Metacard.CREATED, null));
        inputMetacard.setAttribute(new AttributeImpl(Metacard.MODIFIED, null));
        inputMetacard.setAttribute(new AttributeImpl(Metacard.EFFECTIVE, null));
        inputMetacard.setAttribute(new AttributeImpl(Metacard.THUMBNAIL, null));
        inputMetacard.setAttribute(new AttributeImpl(Metacard.CONTENT_TYPE, null));
        inputMetacard.setAttribute(new AttributeImpl(Metacard.CONTENT_TYPE_VERSION, null));

        Metacard outputMetacard = processUpdate(inputMetacard);

        assertThat(outputMetacard.getId(), is(SAMPLE_ID));

        assertThat(outputMetacard.getTitle(), is(nullValue()));

        assertThat(outputMetacard.getLocation(), is(nullValue()));
        assertThat(outputMetacard.getMetadata(), is(DEFAULT_METADATA));

        assertThat(outputMetacard.getCreatedDate(), is(notNullValue()));
        assertThat(outputMetacard.getCreatedDate().getTime(),
                is(greaterThanOrEqualTo(snapshotOfNow.getTime())));

        assertThat(outputMetacard.getModifiedDate(), is(notNullValue()));
        assertThat(outputMetacard.getModifiedDate().getTime(),
                is(greaterThanOrEqualTo(snapshotOfNow.getTime())));

        assertEquals(inputMetacard.getEffectiveDate(), outputMetacard.getEffectiveDate());
        assertThat(outputMetacard.getEffectiveDate(), is(nullValue()));

        assertEquals(inputMetacard.getExpirationDate(), outputMetacard.getExpirationDate());

        assertThat(outputMetacard.getThumbnail(), is(nullValue()));

        assertThat(outputMetacard.getContentTypeName(), is(nullValue()));
        assertThat(outputMetacard.getContentTypeVersion(), is(nullValue()));

    }

    @Test
    public void testUpdateWithNullRequest() throws PluginExecutionException,
        StopProcessingException {

        StandardMetacardGroomerPlugin plugin = new StandardMetacardGroomerPlugin();

        UpdateRequest returnedRequest = plugin.process((UpdateRequest) null);

        assertThat(returnedRequest, nullValue());

    }

    @Test
    public void testUpdateWithNullRecords() throws PluginExecutionException,
        StopProcessingException {

        StandardMetacardGroomerPlugin plugin = new StandardMetacardGroomerPlugin();

        UpdateRequest request = mock(UpdateRequest.class);

        when(request.getUpdates()).thenReturn(null);

        UpdateRequest returnedRequest = plugin.process(request);

        assertThat(returnedRequest, not(nullValue()));

        assertThat(returnedRequest.getUpdates(), nullValue());

    }

    @Test
    public void testUpdateWithNoRecords() throws PluginExecutionException, StopProcessingException {

        StandardMetacardGroomerPlugin plugin = new StandardMetacardGroomerPlugin();

        UpdateRequest request = mock(UpdateRequest.class);

        when(request.getUpdates()).thenReturn(new ArrayList<Entry<Serializable, Metacard>>());

        UpdateRequest returnedRequest = plugin.process(request);

        assertThat(returnedRequest, not(nullValue()));

        assertThat(returnedRequest.getUpdates(), not(nullValue()));

        assertThat(returnedRequest.getUpdates().size(), is(0));

    }

    @Test
    public void testUpdateSingleUpdateNull() throws PluginExecutionException,
        StopProcessingException {

        StandardMetacardGroomerPlugin plugin = new StandardMetacardGroomerPlugin();

        UpdateRequest request = mock(UpdateRequest.class);

        List<Entry<Serializable, Metacard>> updates = new ArrayList<Entry<Serializable, Metacard>>();
        updates.add((Entry<Serializable, Metacard>) null);

        when(request.getUpdates()).thenReturn(updates);

        UpdateRequest returnedRequest = plugin.process(request);

        assertThat(returnedRequest, is(request));

    }

    @Test
    public void testUpdateSingleUpdateKeyNull() throws PluginExecutionException,
        StopProcessingException {

        StandardMetacardGroomerPlugin plugin = new StandardMetacardGroomerPlugin();

        UpdateRequest request = mock(UpdateRequest.class);

        List<Entry<Serializable, Metacard>> updates = new ArrayList<Entry<Serializable, Metacard>>();
        updates.add(new Entry<Serializable, Metacard>() {

            @Override
            public Metacard setValue(Metacard value) {
                return null;
            }

            @Override
            public Metacard getValue() {
                return null;
            }

            @Override
            public Serializable getKey() {
                return null;
            }
        });

        when(request.getUpdates()).thenReturn(updates);

        UpdateRequest returnedRequest = plugin.process(request);

        assertThat(returnedRequest, is(request));

    }

    @Test
    public void testUpdateSingleUpdateValueNull() throws PluginExecutionException,
        StopProcessingException {

        StandardMetacardGroomerPlugin plugin = new StandardMetacardGroomerPlugin();

        UpdateRequest request = mock(UpdateRequest.class);

        List<Entry<Serializable, Metacard>> updates = new ArrayList<Entry<Serializable, Metacard>>();
        updates.add(new Entry<Serializable, Metacard>() {

            @Override
            public Metacard setValue(Metacard value) {
                return null;
            }

            @Override
            public Metacard getValue() {
                return null;
            }

            @Override
            public Serializable getKey() {
                return SAMPLE_ID;
            }
        });

        when(request.getUpdates()).thenReturn(updates);

        UpdateRequest returnedRequest = plugin.process(request);

        assertThat(returnedRequest, is(request));

    }

    protected Metacard processCreate(Metacard inputMetacard) throws PluginExecutionException,
        StopProcessingException {
        CreateRequestImpl inputRequest = new CreateRequestImpl(copy(inputMetacard));

        StandardMetacardGroomerPlugin plugin = new StandardMetacardGroomerPlugin();

        CreateRequest returnedRequest = plugin.process(inputRequest);

        assertNotNull(returnedRequest);

        assertThat(returnedRequest.getMetacards().size(), is(1));

        return returnedRequest.getMetacards().get(0);
    }

    protected Metacard processUpdate(Metacard inputMetacard) throws PluginExecutionException,
        StopProcessingException {
        UpdateRequestImpl inputRequest = new UpdateRequestImpl(SAMPLE_ID, copy(inputMetacard));

        StandardMetacardGroomerPlugin plugin = new StandardMetacardGroomerPlugin();

        UpdateRequest returnedRequest = plugin.process(inputRequest);

        assertNotNull(returnedRequest);

        assertThat(returnedRequest.getUpdates().size(), is(1));

        return returnedRequest.getUpdates().get(0).getValue();
    }

    private Metacard copy(Metacard inputMetacard) {

        MetacardImpl newMetacard = new MetacardImpl();

        newMetacard.setSourceId(inputMetacard.getSourceId());
        newMetacard.setType(inputMetacard.getMetacardType());

        for (AttributeDescriptor ad : inputMetacard.getMetacardType().getAttributeDescriptors()) {

            newMetacard.setAttribute(inputMetacard.getAttribute(ad.getName()));
        }

        return newMetacard;
    }

    protected Metacard getStandardMetacard() {
        DateTime currentDate = new DateTime();
        Date defaultDate = currentDate.minusMinutes(1).toDate();

        MetacardImpl metacard = new MetacardImpl();
        metacard.setTitle(DEFAULT_TITLE);
        metacard.setCreatedDate(defaultDate);
        metacard.setEffectiveDate(defaultDate);
        metacard.setExpirationDate(defaultDate);
        metacard.setModifiedDate(defaultDate);
        metacard.setMetadata(DEFAULT_METADATA);
        metacard.setContentTypeName(DEFAULT_TYPE);
        metacard.setContentTypeVersion(DEFAULT_VERSION);
        metacard.setLocation(DEFAULT_LOCATION);
        byte[] defaultBytes = {-86};
        metacard.setThumbnail(defaultBytes);
        return metacard;
    }

}
