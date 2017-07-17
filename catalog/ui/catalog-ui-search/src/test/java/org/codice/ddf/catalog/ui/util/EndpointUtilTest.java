package org.codice.ddf.catalog.ui.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.InjectableAttribute;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.AttributeBuilder;
import ddf.catalog.filter.ContextualExpressionBuilder;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

/**
 * Created by mweser on 7/17/17.
 */
public class EndpointUtilTest {

    EndpointUtil endpointUtil;

    Map<String, Result> expectedMap;

    List<MetacardType> metacardTypeList;

    MetacardType metacardTypeMock;

    CatalogFramework catalogFrameworkMock;

    FilterBuilder filterBuilderMock;

    List<InjectableAttribute> injectableAttributeList;

    InjectableAttribute injectableAttributeMock;

    AttributeRegistry attributeRegistryMock;

    Filter filterMock;

    AttributeBuilder attributeBuilderMock;

    ContextualExpressionBuilder contextualExpressionBuilderMock;

    QueryResponse responseMock;

    @Before
    public void setUp() throws Exception {

        // mocks
        metacardTypeList = new ArrayList<>();
        metacardTypeMock = mock(MetacardType.class);
        metacardTypeList.add(metacardTypeMock);

        catalogFrameworkMock = mock(CatalogFramework.class);

        filterBuilderMock = mock(FilterBuilder.class);

        injectableAttributeList = new ArrayList<>();
        injectableAttributeMock = mock(InjectableAttribute.class);
        injectableAttributeList.add(injectableAttributeMock);

        attributeRegistryMock = mock(AttributeRegistry.class);

        filterMock = mock(Filter.class);

        attributeBuilderMock = mock(AttributeBuilder.class);

        contextualExpressionBuilderMock = mock(ContextualExpressionBuilder.class);

        responseMock = mock(QueryResponse.class);

        // when
        when(filterBuilderMock.attribute(any())).thenReturn(attributeBuilderMock);
        when(attributeBuilderMock.is()).thenReturn(attributeBuilderMock);
        when(attributeBuilderMock.like()).thenReturn(contextualExpressionBuilderMock);
        when(contextualExpressionBuilderMock.text(anyString())).thenReturn(filterMock);
        when(catalogFrameworkMock.query(any(QueryRequestImpl.class))).thenReturn(responseMock);

        // constructor
        endpointUtil = new EndpointUtil(metacardTypeList,
                catalogFrameworkMock,
                filterBuilderMock,
                injectableAttributeList,
                attributeRegistryMock);

        // TODO: 7/17/17 Instantiate expectedMap here
    }

    @Test
    public void testGetMetacardsByFilterExpectAll()
            throws UnsupportedQueryException, SourceUnavailableException, FederationException {

        String tagFilter = "*";
        Map<String, Result> expected;

        expected = endpointUtil.getMetacardsByFilter(tagFilter);

        assertThat("Something expected", true);

        //verify

    }

}