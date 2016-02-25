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
package ddf.catalog.nato.stanag4559.source;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.core.Response;

import org.codice.ddf.cxf.SecureCxfClientFactory;
import org.codice.ddf.spatial.ogc.catalog.common.AvailabilityTask;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.omg.CORBA.IntHolder;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

import ddf.catalog.data.Metacard;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.nato.stanag4559.common.GIAS.AttributeInformation;
import ddf.catalog.nato.stanag4559.common.GIAS.AttributeType;
import ddf.catalog.nato.stanag4559.common.GIAS.CatalogMgr;
import ddf.catalog.nato.stanag4559.common.GIAS.DataModelMgr;
import ddf.catalog.nato.stanag4559.common.GIAS.Domain;
import ddf.catalog.nato.stanag4559.common.GIAS.HitCountRequest;
import ddf.catalog.nato.stanag4559.common.GIAS.Polarity;
import ddf.catalog.nato.stanag4559.common.GIAS.Query;
import ddf.catalog.nato.stanag4559.common.GIAS.RequirementMode;
import ddf.catalog.nato.stanag4559.common.GIAS.SortAttribute;
import ddf.catalog.nato.stanag4559.common.GIAS.SubmitQueryRequest;
import ddf.catalog.nato.stanag4559.common.GIAS.View;
import ddf.catalog.nato.stanag4559.common.Stanag4559;
import ddf.catalog.nato.stanag4559.common.Stanag4559Constants;
import ddf.catalog.nato.stanag4559.common.UCO.DAG;
import ddf.catalog.nato.stanag4559.common.UCO.DAGListHolder;
import ddf.catalog.nato.stanag4559.common.UCO.NameValue;
import ddf.catalog.nato.stanag4559.common.UCO.State;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.UnsupportedQueryException;

public class TestStanag4559Source {

    private static final String ID = "mySTANAG";

    private static final String IOR_URL = "http://localhost:20002/data/ior.txt";

    private static final Integer POLL_INTERVAL = 1;

    private static final Integer MAX_HIT_COUNT = 250;

    private static final String GMTI = "GMTI";

    private static final String GMTI_EQ_FILTER = "(type =  'GMTI')";

    private static final String GMTI_LIKE_FILTER = "(GMTI like '%')";

    private static final String RELEVANCE = "RELEVANCE";

    private static final long LONG = 12l;

    private AvailabilityTask mockAvailabilityTask = mock(AvailabilityTask.class);

    private CatalogMgr catalogMgr = mock(CatalogMgr.class);

    private final GeotoolsFilterBuilder builder = new GeotoolsFilterBuilder();

    private Stanag4559Source source;

    private AttributeInformation[] attributeInformations = new AttributeInformation[0];

    private HashMap<String, List<AttributeInformation>> attributeInformationMap =
            getAttributeInformationMap();

    @Before
    public void setUp() throws Exception {
        source = buildSource();
    }

    @Test
    public void testInitialContentList() {
        source.getContentTypes();
        assertThat(source.getContentTypes(), is(Stanag4559Constants.setContentStrings()));
    }

    @Test
    public void testIsAvailable() {
        source.setupAvailabilityPoll();
        assertThat(source.isAvailable(), is(true));
    }

    @Test
    public void testQuerySupportedAscendingSorting() throws Exception {
        QueryImpl propertyIsLikeQuery =
                new QueryImpl(builder.attribute(Stanag4559FilterFactory.METADATA_CONTENT_TYPE)
                        .is()
                        .equalTo()
                        .text(GMTI));

        SortBy sortBy = new SortByImpl(Metacard.MODIFIED, SortOrder.ASCENDING);
        propertyIsLikeQuery.setSortBy(sortBy);

        source.query(new QueryRequestImpl(propertyIsLikeQuery));
        ArgumentCaptor<SortAttribute[]> argumentCaptor =
                ArgumentCaptor.forClass(SortAttribute[].class);
        verify(catalogMgr).submit_query(any(Query.class),
                any(String[].class),
                argumentCaptor.capture(),
                any(NameValue[].class));

        assertThat(argumentCaptor.getValue()[0].attribute_name, is(Stanag4559Source.DATE_MODFIIED));
        assertThat(argumentCaptor.getValue()[0].sort_polarity, is(Polarity.ASCENDING));
    }

    @Test
    public void testQuerySupportedDescendingSorting() throws Exception {
        QueryImpl propertyIsLikeQuery =
                new QueryImpl(builder.attribute(Stanag4559FilterFactory.METADATA_CONTENT_TYPE)
                        .is()
                        .equalTo()
                        .text(GMTI));

        SortBy sortBy = new SortByImpl(Metacard.MODIFIED, SortOrder.DESCENDING);
        propertyIsLikeQuery.setSortBy(sortBy);

        source.query(new QueryRequestImpl(propertyIsLikeQuery));
        ArgumentCaptor<SortAttribute[]> argumentCaptor =
                ArgumentCaptor.forClass(SortAttribute[].class);
        verify(catalogMgr).submit_query(any(Query.class),
                any(String[].class),
                argumentCaptor.capture(),
                any(NameValue[].class));

        assertThat(argumentCaptor.getValue()[0].attribute_name, is(Stanag4559Source.DATE_MODFIIED));
        assertThat(argumentCaptor.getValue()[0].sort_polarity, is(Polarity.DESCENDING));
    }

    @Test
    public void testQueryUnsupportedSorting() throws Exception {
        QueryImpl propertyIsLikeQuery =
                new QueryImpl(builder.attribute(Stanag4559FilterFactory.METADATA_CONTENT_TYPE)
                        .is()
                        .equalTo()
                        .text(GMTI));

        SortBy sortBy = new SortByImpl(RELEVANCE, SortOrder.DESCENDING);
        propertyIsLikeQuery.setSortBy(sortBy);

        source.query(new QueryRequestImpl(propertyIsLikeQuery));
        ArgumentCaptor<SortAttribute[]> argumentCaptor =
                ArgumentCaptor.forClass(SortAttribute[].class);
        verify(catalogMgr).submit_query(any(Query.class),
                any(String[].class),
                argumentCaptor.capture(),
                any(NameValue[].class));

        assertThat(argumentCaptor.getValue().length, is(0));
    }

    @Test
    public void testQuerSortingNullSortableAttributes() throws Exception {

        source.setSortableAttributes(null);
        QueryImpl propertyIsLikeQuery =
                new QueryImpl(builder.attribute(Stanag4559FilterFactory.METADATA_CONTENT_TYPE)
                        .is()
                        .equalTo()
                        .text(GMTI));

        SortBy sortBy = new SortByImpl(RELEVANCE, SortOrder.DESCENDING);
        propertyIsLikeQuery.setSortBy(sortBy);

        source.query(new QueryRequestImpl(propertyIsLikeQuery));
        ArgumentCaptor<SortAttribute[]> argumentCaptor =
                ArgumentCaptor.forClass(SortAttribute[].class);
        verify(catalogMgr).submit_query(any(Query.class),
                any(String[].class),
                argumentCaptor.capture(),
                any(NameValue[].class));

        assertThat(argumentCaptor.getValue().length, is(0));
    }

    @Test
    public void testQuerySortingNullSortBy() throws Exception {

        QueryImpl propertyIsLikeQuery =
                new QueryImpl(builder.attribute(Stanag4559FilterFactory.METADATA_CONTENT_TYPE)
                        .is()
                        .equalTo()
                        .text(GMTI));

        SortBy sortBy = null;
        propertyIsLikeQuery.setSortBy(sortBy);

        source.query(new QueryRequestImpl(propertyIsLikeQuery));
        ArgumentCaptor<SortAttribute[]> argumentCaptor =
                ArgumentCaptor.forClass(SortAttribute[].class);
        verify(catalogMgr).submit_query(any(Query.class),
                any(String[].class),
                argumentCaptor.capture(),
                any(NameValue[].class));

        assertThat(argumentCaptor.getValue().length, is(0));
    }

    @Test
    public void testQueryResponseHitCount() throws Exception {
        QueryImpl propertyIsLikeQuery = new QueryImpl(builder.attribute(Metacard.ANY_TEXT)
                .is()
                .like()
                .text("*"));
        SourceResponse sourceResponse = source.query(new QueryRequestImpl(propertyIsLikeQuery));
        assertThat(sourceResponse.getHits(), is(LONG));
    }

    @Test
    public void testQueryByContentType() throws Exception {
        QueryImpl propertyIsLikeQuery =
                new QueryImpl(builder.attribute(Stanag4559FilterFactory.METADATA_CONTENT_TYPE)
                        .is()
                        .equalTo()
                        .text(GMTI));
        SourceResponse sourceResponse = source.query(new QueryRequestImpl(propertyIsLikeQuery));
        ArgumentCaptor<Query> argumentCaptor = ArgumentCaptor.forClass(Query.class);
        verify(catalogMgr).submit_query(argumentCaptor.capture(), any(String[].class), any(
                SortAttribute[].class), any(NameValue[].class));
        assertThat(sourceResponse.getHits(), is(LONG));
        assertThat(argumentCaptor.getValue().bqs_query, is(GMTI_EQ_FILTER));
    }

    @Test
    public void testQueryAnyTextWildcardRepl() throws Exception {
        QueryImpl propertyIsLikeQuery = new QueryImpl(builder.attribute(Metacard.ANY_TEXT)
                .is()
                .like()
                .text("*"));
        SourceResponse sourceResponse = source.query(new QueryRequestImpl(propertyIsLikeQuery));
        ArgumentCaptor<Query> argumentCaptor = ArgumentCaptor.forClass(Query.class);
        verify(catalogMgr).submit_query(argumentCaptor.capture(), any(String[].class), any(
                SortAttribute[].class), any(NameValue[].class));
        assertThat(sourceResponse.getHits(), is(LONG));
        assertThat(argumentCaptor.getValue().bqs_query, is(GMTI_LIKE_FILTER));
    }

    @Test
    public void testQueryAnyText() throws Exception {
        QueryImpl propertyIsLikeQuery = new QueryImpl(builder.attribute(Metacard.ANY_TEXT)
                .is()
                .like()
                .text("%"));
        SourceResponse sourceResponse = source.query(new QueryRequestImpl(propertyIsLikeQuery));
        ArgumentCaptor<Query> argumentCaptor = ArgumentCaptor.forClass(Query.class);
        verify(catalogMgr).submit_query(argumentCaptor.capture(), any(String[].class), any(
                SortAttribute[].class), any(NameValue[].class));
        assertThat(sourceResponse.getHits(), is(LONG));
        assertThat(argumentCaptor.getValue().bqs_query, is(GMTI_LIKE_FILTER));
    }

    @Test(expected = UnsupportedQueryException.class)
    public void testBadQuery() throws Exception {
        QueryImpl propertyIsLikeQuery = new QueryImpl(builder.attribute(Metacard.ANY_TEXT)
                .is()
                .overlapping()
                .last(LONG));
        source.query(new QueryRequestImpl(propertyIsLikeQuery));
    }

    @Test
    public void testRefreshWithNullConfiguration() throws Exception {
        Stanag4559Source source = buildSource();
        HashMap<String, Object> configuration = null;
        assertConfiguration(source);
        source.refresh(configuration);
        assertConfiguration(source);
    }

    @Test
    public void testRefreshWithEmptyConfiguration() throws Exception {
        Stanag4559Source source = buildSource();
        HashMap<String, Object> configuration = new HashMap<>();
        assertConfiguration(source);
        source.refresh(configuration);
        assertConfiguration(source);
    }

    @Test
    public void testRefresh() throws Exception {
        Stanag4559Source source = buildSource();
        HashMap<String, Object> configuration = new HashMap<>();

        configuration.put(Stanag4559Source.USERNAME, GMTI);
        configuration.put(Stanag4559Source.PASSWORD, GMTI);
        configuration.put(Stanag4559Source.KEY, GMTI);
        configuration.put(Stanag4559Source.IOR_URL, GMTI);
        configuration.put(Stanag4559Source.POLL_INTERVAL, 0);
        configuration.put(Stanag4559Source.MAX_HIT_COUNT, 0);
        configuration.put(Stanag4559Source.ID, GMTI);

        source.refresh(configuration);
        assertChangedConfiguration(source);
    }

    private Stanag4559Source buildSource() throws Exception {
        Stanag4559Source source;
        Stanag4559 stanag4559 = mock(Stanag4559.class);
        Response clientResponse = mock(Response.class);
        when(clientResponse.getEntity()).thenReturn("");
        InputStream mockInputStream = mock(InputStream.class);
        when(stanag4559.getIorFile()).thenReturn(mockInputStream);
        SecureCxfClientFactory factory = getMockFactory(stanag4559);

        HashMap<String, String[]> resultAttributes = new HashMap<>();
        HashMap<String, List<String>> sortableAttributes = generateMockSortableAttributes();

        source = Mockito.spy(new Stanag4559Source(factory,
                resultAttributes,
                sortableAttributes,
                new Stanag4559FilterDelegate(attributeInformationMap,
                        Stanag4559Constants.NSIL_ALL_VIEW)));
        source.setIorUrl(IOR_URL);
        source.setUsername(Stanag4559Source.USERNAME);
        source.setPassword(Stanag4559Source.PASSWORD);
        source.setKey(Stanag4559Source.KEY);
        source.setMaxHitCount(MAX_HIT_COUNT);
        source.setId(ID);
        source.setPollInterval(POLL_INTERVAL);
        source.setDataModelMgr(getMockDataModelMgr());
        source.setCatalogMgr(getMockCatalogMgr());
        source.setFilterAdapter(new GeotoolsFilterAdapterImpl());

        // Suppress CORBA communications to test refresh
        doNothing().when(source)
                .init();

        when(mockAvailabilityTask.isAvailable()).thenReturn(true);
        source.setAvailabilityTask(mockAvailabilityTask);
        return source;
    }

    private SecureCxfClientFactory getMockFactory(Stanag4559 client) {
        SecureCxfClientFactory factory = mock(SecureCxfClientFactory.class);
        doReturn(client).when(factory)
                .getClient();
        return factory;
    }

    private CatalogMgr getMockCatalogMgr() throws Exception {
        SubmitQueryRequest submitQueryRequest = mock(SubmitQueryRequest.class);
        HitCountRequest hitCountRequest = mock(HitCountRequest.class);

        doReturn(State.COMPLETED).when(hitCountRequest)
                .complete(any(IntHolder.class));

        when(hitCountRequest.complete(any(IntHolder.class))).thenAnswer((InvocationOnMock invocationOnMock) -> {
            IntHolder intHolder = (IntHolder) invocationOnMock.getArguments()[0];
            intHolder.value = 12;
            return State.COMPLETED;
        });

        when(submitQueryRequest.complete_DAG_results(any(DAGListHolder.class))).thenAnswer((InvocationOnMock invocationOnMock) -> {
            DAGListHolder dagListHolder = (DAGListHolder) invocationOnMock.getArguments()[0];
            dagListHolder.value = getMockDAGArray();
            return State.COMPLETED;
        });

        doReturn(submitQueryRequest).when(catalogMgr)
                .submit_query(any(Query.class),
                        any(String[].class),
                        any(SortAttribute[].class),
                        any(NameValue[].class));
        doReturn(hitCountRequest).when(catalogMgr)
                .hit_count(any(Query.class), any(NameValue[].class));
        return catalogMgr;
    }

    private DataModelMgr getMockDataModelMgr() throws Exception {
        DataModelMgr dataModelMgr = mock(DataModelMgr.class);
        View[] views = new View[0];
        doReturn(attributeInformations).when(dataModelMgr)
                .get_attributes(anyString(), any(NameValue[].class));
        doReturn(attributeInformations).when(dataModelMgr)
                .get_queryable_attributes(anyString(), any(NameValue[].class));
        doReturn(views).when(dataModelMgr)
                .get_view_names(any(NameValue[].class));

        return dataModelMgr;
    }

    private DAG[] getMockDAGArray() {
        return new DAG[0];
    }

    private HashMap<String, List<AttributeInformation>> getAttributeInformationMap() {
        HashMap<String, List<AttributeInformation>> map = new HashMap<>();
        List<AttributeInformation> list = new ArrayList<>();
        Domain domain = new Domain();
        domain.t(200);
        AttributeInformation attributeInformation = new AttributeInformation(GMTI,
                AttributeType.TEXT,
                domain,
                Stanag4559FilterDelegate.EMPTY_STRING,
                Stanag4559FilterDelegate.EMPTY_STRING,
                RequirementMode.OPTIONAL,
                Stanag4559FilterDelegate.EMPTY_STRING,
                false,
                true);
        list.add(attributeInformation);
        map.put(Stanag4559Constants.NSIL_ALL_VIEW, list);
        return map;
    }

    private HashMap<String, List<String>> generateMockSortableAttributes() {
        HashMap<String, List<String>> sortableAttributes = new HashMap<>();
        sortableAttributes.put(Stanag4559Constants.NSIL_ALL_VIEW,
                Arrays.asList(Stanag4559Source.DATE_CREATED, Stanag4559Source.DATE_MODFIIED));
        return sortableAttributes;
    }

    private void assertConfiguration(Stanag4559Source source) {
        assertThat(source.getId(), is(ID));
        assertThat(source.getUsername(), is(Stanag4559Source.USERNAME));
        assertThat(source.getPassword(), is(Stanag4559Source.PASSWORD));
        assertThat(source.getKey(), is(Stanag4559Source.KEY));
        assertThat(source.getPollInterval(), is(POLL_INTERVAL));
        assertThat(source.getMaxHitCount(), is(MAX_HIT_COUNT));
        assertThat(source.getIorUrl(), is(IOR_URL));
    }

    private void assertChangedConfiguration(Stanag4559Source source) {
        assertThat(source.getId(), is(GMTI));
        assertThat(source.getUsername(), is(GMTI));
        assertThat(source.getPassword(), is(GMTI));
        assertThat(source.getKey(), is(GMTI));
        assertThat(source.getPollInterval(), is(0));
        assertThat(source.getMaxHitCount(), is(0));
        assertThat(source.getIorUrl(), is(GMTI));
    }
}
