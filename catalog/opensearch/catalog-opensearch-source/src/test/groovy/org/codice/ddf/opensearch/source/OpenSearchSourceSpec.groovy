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
package org.codice.ddf.opensearch.source

import ddf.catalog.data.Metacard
import ddf.catalog.data.Result
import ddf.catalog.filter.FilterBuilder
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder
import ddf.catalog.operation.SourceResponse
import ddf.catalog.operation.impl.QueryImpl
import ddf.catalog.operation.impl.QueryRequestImpl
import ddf.catalog.source.UnsupportedQueryException
import ddf.catalog.transform.InputTransformer
import ddf.security.SecurityConstants
import ddf.security.Subject
import ddf.security.encryption.EncryptionService
import org.apache.cxf.jaxrs.client.WebClient
import org.codice.ddf.cxf.client.ClientFactoryFactory
import org.codice.ddf.cxf.client.SecureCxfClientFactory
import org.codice.ddf.cxf.client.impl.ClientFactoryFactoryImpl
import org.codice.ddf.opensearch.OpenSearchConstants
import org.opengis.filter.Filter
import org.opengis.filter.PropertyIsLike
import org.opengis.filter.spatial.Contains
import org.opengis.filter.spatial.DWithin
import org.opengis.filter.spatial.Intersects
import org.opengis.filter.temporal.During
import org.osgi.framework.Bundle
import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceReference
import spock.lang.Specification
import spock.lang.Unroll

import javax.ws.rs.core.Response
import java.nio.charset.StandardCharsets
import static org.mockito.Matchers.any
import static org.mockito.Matchers.anyBoolean
import static org.mockito.Matchers.anyInt
import static org.mockito.Matchers.anyString
import static org.mockito.Mockito.when

class OpenSearchSourceSpec extends Specification {

    private static final FilterBuilder filterBuilder = new GeotoolsFilterBuilder()

    private static final PropertyIsLike PROPERTY_IS_LIKE_FILTER = (PropertyIsLike) filterBuilder.attribute("this attribute name is ignored").is().like().text("someSearchPhrase")
    private static final DWithin D_WITHIN_FILTER = (DWithin) filterBuilder.attribute(OpenSearchConstants.SUPPORTED_SPATIAL_SEARCH_TERM).is().withinBuffer().wkt("POINT(1.0 2.0)", 5)
    private static final Contains CONTAINS_FILTER = (Contains) filterBuilder.attribute(OpenSearchConstants.SUPPORTED_SPATIAL_SEARCH_TERM).containing().wkt("POLYGON ((1.1 1.1, 1.1 2.1, 2.1 2.1, 2.1 1.1, 1.1 1.1))")
    private static final Intersects INTERSECTS_FILTER = (Intersects) filterBuilder.attribute(OpenSearchConstants.SUPPORTED_SPATIAL_SEARCH_TERM).intersecting().wkt("POLYGON ((10.2 10.2, 10.2 20.2, 20.2 20.2, 20.2 10.2, 10.2 10.2))")
    private static final During DURING_FILTER = (During) filterBuilder.attribute(OpenSearchConstants.SUPPORTED_TEMPORAL_SEARCH_TERM).during().dates(new Date(10000), new Date(10005))
    private static final Intersects GEOMETRY_COLLECTION = (Intersects) filterBuilder.attribute(OpenSearchConstants.SUPPORTED_SPATIAL_SEARCH_TERM).intersecting().wkt("GEOMETRYCOLLECTION(POINT(4 6),LINESTRING(4 6,7 10))");

    private static final DWithin NOT_SUPPORTED_D_WITHIN_FILTER = (DWithin) filterBuilder.attribute("this attribute name is not supported for spatial filters").is().withinBuffer().wkt("POINT(1.0 2.0)", 5)
    private static final Contains NOT_SUPPORTED_CONTAINS_FILTER = (Contains) filterBuilder.attribute("this attribute name is not supported for spatial filters").containing().wkt("POLYGON ((1.1 1.1, 1.1 2.1, 2.1 2.1, 2.1 1.1, 1.1 1.1))")
    private static final Intersects NOT_SUPPORTED_INTERSECTS_FILTER = (Intersects) filterBuilder.attribute("this attribute name is not supported for spatial filters").intersecting().wkt("POLYGON ((1.1 1.1, 1.1 2.1, 2.1 2.1, 2.1 1.1, 1.1 1.1))")
    private static final During NOT_SUPPORTED_DURING_FILTER = (During) filterBuilder.attribute("this attribute name is not supported for temporal filters").during().dates(new Date(10000), new Date(10005))

    private OpenSearchSource source

    private WebClient webClient = Mock(WebClient)

    private Map<String, Serializable> queryRequestProperties

    def setup() {
        final Bundle bundle = Mock(Bundle) {
            getBundleContext() >> Mock(BundleContext) {

                final ServiceReference<InputTransformer> inputTransformerServiceReference = Mock(ServiceReference) {
                    getBundle() >> Mock(Bundle)
                }
                getServiceReferences(InputTransformer.class, "(schema=urn:catalog:metacard)") >> Mock(Collection) {
                    iterator() >> Mock(Iterator) {
                        next() >> inputTransformerServiceReference
                    }
                }
                getService(inputTransformerServiceReference) >> Mock(InputTransformer) {
                    transform(_ as InputStream, _ as String) >> Mock(Metacard)
                }
            }
        }

        final Subject subject = Mock(Subject)

        final SecureCxfClientFactory factory = Mock(SecureCxfClientFactory) {
            getWebClientForSubject(subject) >> webClient
        }

        source = new OpenSearchSource(new GeotoolsFilterAdapterImpl(), new OpenSearchParserImpl(), new OpenSearchFilterVisitor(), Mock(EncryptionService), new ClientFactoryFactoryImpl()) {

            @Override
            protected Bundle getBundle() {
                return bundle
            }

            @Override
            protected SecureCxfClientFactory createClientFactory(
                    String url, String username, String password) {
                return factory
            }
        }

        source.setEndpointUrl("http://localhost:8181/services/catalog/query")
        source.setDistanceTolerance(1)
        source.setNumMultiPointRadiusVertices(8)
        source.init()
        source.setParameters(["q", "src", "mr", "start", "count", "mt", "dn", "lat", "lon", "radius", "bbox", "geometry", "polygon", "dtstart", "dtend", "dateName", "filter", "sort"])

        queryRequestProperties = [:]
        queryRequestProperties.put(SecurityConstants.SECURITY_SUBJECT, subject)
    }

    @Unroll
    def 'testQueries #filter'(Filter filter, Map<String, Object> expectedQueryParameters) throws UnsupportedQueryException {
        given:
        final Map<String, Object> webClientQueryParameters = [:]

        webClient.get() >> {
            assert webClientQueryParameters == expectedQueryParameters
            return Mock(Response) {
                getStatus() >> Response.Status.OK.getStatusCode()
                getEntity() >> new ByteArrayInputStream(OpenSearchSourceTest.SAMPLE_ATOM.getBytes(StandardCharsets.UTF_8))
                getHeaderString("Accept-Ranges") >> "bytes"
            }
        }
        webClient.replaceQueryParam(_ as String, _ as Object) >> {
            String parameter, Object value -> webClientQueryParameters.put(parameter, value)
        }

        final QueryRequestImpl queryRequest = new QueryRequestImpl(new QueryImpl(filter), queryRequestProperties)

        when:
        final SourceResponse response = source.query(queryRequest)

        then:
        response.getHits() == 1
        final List<Result> results = response.getResults()
        results.size() == 1
        final Result result = results.get(0)
        result.getMetacard() != null

        where:
        filter << [
                PROPERTY_IS_LIKE_FILTER,
                (PropertyIsLike) filterBuilder.attribute("this attribute name is ignored").is().like().text("some search phrase"), // the search phrase contains spaces
                DURING_FILTER,
                INTERSECTS_FILTER, // polygon filter
                D_WITHIN_FILTER, // point-radius filter
                CONTAINS_FILTER, // polygon filter

                //Multiple geometry filters
                filterBuilder.allOf(D_WITHIN_FILTER, filterBuilder.attribute(OpenSearchConstants.SUPPORTED_SPATIAL_SEARCH_TERM).is().withinBuffer().wkt("POINT(0.0 0.0)", 804672), PROPERTY_IS_LIKE_FILTER, DURING_FILTER), //multiple point radius filter
                filterBuilder.allOf(D_WITHIN_FILTER, INTERSECTS_FILTER, PROPERTY_IS_LIKE_FILTER, DURING_FILTER),  //point radius and polygon filters
                filterBuilder.allOf(INTERSECTS_FILTER, CONTAINS_FILTER, PROPERTY_IS_LIKE_FILTER, DURING_FILTER),  // multiple polygon filters
                filterBuilder.anyOf(GEOMETRY_COLLECTION, CONTAINS_FILTER, INTERSECTS_FILTER, D_WITHIN_FILTER),  // polygon, geometry, point radius filters

                /*not supported filters and supported filters*/
                filterBuilder.allOf(NOT_SUPPORTED_DURING_FILTER, PROPERTY_IS_LIKE_FILTER),
                filterBuilder.allOf(NOT_SUPPORTED_D_WITHIN_FILTER, PROPERTY_IS_LIKE_FILTER, DURING_FILTER),
                filterBuilder.allOf(NOT_SUPPORTED_CONTAINS_FILTER, PROPERTY_IS_LIKE_FILTER, DURING_FILTER),
                filterBuilder.allOf(NOT_SUPPORTED_INTERSECTS_FILTER, PROPERTY_IS_LIKE_FILTER, DURING_FILTER)
        ]
        expectedQueryParameters << [
                [start: ["1"], count: ["20"], mt: ["0"], q: ["someSearchPhrase"], src: [""]],
                [start: ["1"], count: ["20"], mt: ["0"], q: ["some search phrase"], src: [""]],
                [start: ["1"], count: ["20"], mt: ["0"], dtstart: ["1970-01-01T00:00:10.000Z"], dtend: ["1970-01-01T00:00:10.005Z"], src: [""]],
                [start: ["1"], count: ["20"], mt: ["0"], geometry:["POLYGON ((10.2 10.2, 10.2 20.2, 20.2 20.2, 20.2 10.2, 10.2 10.2))"] , src: [""]],
                [start: ["1"], count: ["20"], mt: ["0"], lat: ["2.0"], lon: ["1.0"], radius: ["5.0"], src: [""]],
                [start: ["1"], count: ["20"], mt: ["0"], geometry:["POLYGON ((1.1 1.1, 1.1 2.1, 2.1 2.1, 2.1 1.1, 1.1 1.1))"] , src: [""]],

                //multiple geometry filters
                [start: ["1"], count: ["20"], mt: ["0"], q: ["someSearchPhrase"], dtstart: ["1970-01-01T00:00:10.000Z"], dtend: ["1970-01-01T00:00:10.005Z"], geometry:["GEOMETRYCOLLECTION (POLYGON ((7.226139911476106 0, 5.11998429099165 -5.144155513526762, -0.0241711783546172 -7.269631875480294, -5.119666214239615 -5.0960072200722095, -7.178433793970663 0.0000000000000055, -5.119666214239607 5.0960072200722175, -0.0241711783545989 7.269631875480294, 5.119984290991666 5.144155513526745, 7.226139911476106 0)), POLYGON ((1.0000449333831427 2.0000000550958474, 1.0000000547731076 1.9999547917265539, 0.9999550666425021 1.9999999449027324, 0.9999999452500564 2.0000452082732347, 1.0000449333831427 2.0000000550958474)))"], src: [""]],
                [start: ["1"], count: ["20"], mt: ["0"], q: ["someSearchPhrase"], dtstart: ["1970-01-01T00:00:10.000Z"], dtend: ["1970-01-01T00:00:10.005Z"], geometry:["POLYGON ((10.2 10.2, 10.2 20.2, 20.2 20.2, 20.2 10.2, 10.2 10.2))"], lat:["2.0"], lon:["1.0"], radius:["5.0"], src: [""]],
                [start: ["1"], count: ["20"], mt: ["0"], q: ["someSearchPhrase"], dtstart: ["1970-01-01T00:00:10.000Z"], dtend: ["1970-01-01T00:00:10.005Z"], geometry:["GEOMETRYCOLLECTION (POLYGON ((10.2 10.2, 10.2 20.2, 20.2 20.2, 20.2 10.2, 10.2 10.2)), POLYGON ((1.1 1.1, 1.1 2.1, 2.1 2.1, 2.1 1.1, 1.1 1.1)))"], src: [""]],
                [start: ["1"], count: ["20"], mt: ["0"], geometry:["GEOMETRYCOLLECTION (POLYGON ((1.1 1.1, 1.1 2.1, 2.1 2.1, 2.1 1.1, 1.1 1.1)), POLYGON ((10.2 10.2, 10.2 20.2, 20.2 20.2, 20.2 10.2, 10.2 10.2)), GEOMETRYCOLLECTION (POINT (4 6), LINESTRING (4 6, 7 10)))"], lat: ["2.0"], lon: ["1.0"], radius: ["5.0"], src: [""]],

                /*not supported filters and supported filters*/
                [start: ["1"], count: ["20"], mt: ["0"], q: ["someSearchPhrase"], src: [""]],
                [start: ["1"], count: ["20"], mt: ["0"], q: ["someSearchPhrase"], dtstart: ["1970-01-01T00:00:10.000Z"], dtend: ["1970-01-01T00:00:10.005Z"], src: [""]],
                [start: ["1"], count: ["20"], mt: ["0"], q: ["someSearchPhrase"], dtstart: ["1970-01-01T00:00:10.000Z"], dtend: ["1970-01-01T00:00:10.005Z"], src: [""]],
                [start: ["1"], count: ["20"], mt: ["0"], q: ["someSearchPhrase"], dtstart: ["1970-01-01T00:00:10.000Z"], dtend: ["1970-01-01T00:00:10.005Z"], src: [""]]
        ]
    }

    @Unroll
    def 'testBboxSpatialQueries #filter'(Filter filter, Map<String, Object> expectedQueryParameters) throws UnsupportedQueryException {
        given:
        final Map<String, Object> webClientQueryParameters = [:]

        webClient.get() >> {
            assert webClientQueryParameters == expectedQueryParameters
            return Mock(Response) {
                getStatus() >> Response.Status.OK.getStatusCode()
                getEntity() >> new ByteArrayInputStream(OpenSearchSourceTest.SAMPLE_ATOM.getBytes(StandardCharsets.UTF_8))
                getHeaderString("Accept-Ranges") >> "bytes"
            }
        }
        webClient.replaceQueryParam(_ as String, _ as Object) >> {
            String parameter, Object value -> webClientQueryParameters.put(parameter, value)
        }

        final QueryRequestImpl queryRequest = new QueryRequestImpl(new QueryImpl(filter), queryRequestProperties)

        source.setShouldConvertToBBox(true)

        when:
        final SourceResponse response = source.query(queryRequest)

        then:
        response.getHits() == 1
        final List<Result> results = response.getResults()
        results.size() == 1
        final Result result = results.get(0)
        result.getMetacard() != null

        where:
        filter << [
                D_WITHIN_FILTER,
                CONTAINS_FILTER,
                INTERSECTS_FILTER,
                filterBuilder.anyOf(D_WITHIN_FILTER, INTERSECTS_FILTER), // point-radius and polygon filters
                filterBuilder.anyOf(INTERSECTS_FILTER, CONTAINS_FILTER), // different polygon filters
                filterBuilder.anyOf(D_WITHIN_FILTER, filterBuilder.attribute(OpenSearchConstants.SUPPORTED_SPATIAL_SEARCH_TERM).is().withinBuffer().wkt("POINT(0.0 0.0)", 804672)) // different point-radius filters
        ]
        expectedQueryParameters << [
                [start: ["1"], count: ["20"], mt: ["0"], bbox: ["0.9999550570408705,1.999954933135129,1.0000449429591296,2.000045066864871"], src: [""]],
                [start: ["1"], count: ["20"], mt: ["0"], bbox: ["1.1,1.1,2.1,2.1"], src: [""]],
                [start: ["1"], count: ["20"], mt: ["0"], bbox: ["10.2,10.2,20.2,20.2"], src: [""]],
                [start: ["1"], count: ["20"], mt: ["0"], bbox: ["0.9999550570408705,1.999954933135129,20.2,20.2"], src: [""]],
                [start: ["1"], count: ["20"], mt: ["0"], bbox: ["1.1,1.1,20.2,20.2"], src: [""]],
                [start: ["1"], count: ["20"], mt: ["0"], bbox: ["-7.228491563030235,-7.25280885791844,7.228491563030235,7.25280885791844"], src: [""]]
        ]
    }

    @Unroll
    def 'testUnsupportedQuery #filter'(Filter filter) {
        given:
        final QueryRequestImpl queryRequest = new QueryRequestImpl(new QueryImpl(filter), queryRequestProperties)

        when:
        source.query(queryRequest)

        then:
        0 * webClient.get()
        thrown(UnsupportedQueryException)

        where:
        filter << [
                NOT_SUPPORTED_DURING_FILTER,
                NOT_SUPPORTED_D_WITHIN_FILTER,
                NOT_SUPPORTED_CONTAINS_FILTER,
                NOT_SUPPORTED_INTERSECTS_FILTER
        ]
    }
}