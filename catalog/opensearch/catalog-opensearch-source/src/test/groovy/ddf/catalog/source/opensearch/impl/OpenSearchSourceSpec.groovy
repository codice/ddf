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
package ddf.catalog.source.opensearch.impl

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
import org.codice.ddf.cxf.SecureCxfClientFactory
import org.opengis.filter.PropertyIsLike
import org.opengis.filter.spatial.DWithin
import org.opengis.filter.temporal.During
import org.osgi.framework.Bundle
import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceReference
import spock.lang.Specification

import javax.ws.rs.core.Response
import java.nio.charset.StandardCharsets

class OpenSearchSourceSpec extends Specification {

    private static FilterBuilder filterBuilder = new GeotoolsFilterBuilder()

    def testQueries() throws UnsupportedQueryException {
        given:
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
            getWebClientForSubject(subject) >> Mock(WebClient) {

                final Map<String, Object> queryParameters = [:]

                get() >> {
                    if (queryParameters == coorespondingQueryParameters) {
                        return Mock(Response) {
                            getStatus() >> Response.Status.OK.getStatusCode()
                            getEntity() >> new ByteArrayInputStream(OpenSearchSourceTest.SAMPLE_ATOM.getBytes(StandardCharsets.UTF_8))
                            getHeaderString(OpenSearchSource.HEADER_ACCEPT_RANGES) >> OpenSearchSource.BYTES
                        }
                    }
                }

                replaceQueryParam(_ as String, _ as Object) >> {
                    String parameter, Object value -> queryParameters.put(parameter, value)
                }
            }
        }

        final OpenSearchSource source = new OpenSearchSource(new GeotoolsFilterAdapterImpl(), new OpenSearchParserImpl(), new OpenSearchFilterVisitor(), Mock(EncryptionService)) {

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
        source.init()
        source.setParameters(["q", "src", "mr", "start", "count", "mt", "dn", "lat", "lon", "radius", "bbox", "polygon", "dtstart", "dtend", "dateName", "filter", "sort"])

        final Map<String, Serializable> properties = new HashMap<>()
        properties.put(SecurityConstants.SECURITY_SUBJECT, subject)
        final QueryRequestImpl queryRequest = new QueryRequestImpl(new QueryImpl(filter), properties)

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
                (PropertyIsLike) filterBuilder.attribute("this attribute name is ignored").is().like().text("someSearchPhrase"),
                (DWithin) filterBuilder.attribute(Metacard.ANY_GEO).is().withinBuffer().wkt("POINT(1.0 2.0)", 5),
                (During) filterBuilder.attribute(Metacard.MODIFIED).during().dates(new Date(10000), new Date(10005))
        ]
        coorespondingQueryParameters << [
                [start: ["1"], count: ["20"], mt: ["0"], q: ["someSearchPhrase"], src: [""]],
                [start: ["1"], count: ["20"], mt: ["0"], q: ["*"], lat: ["2.0"], lon: ["1.0"], radius: ["5.0"], src: [""]],
                [start: ["1"], count: ["20"], mt: ["0"], dtstart: ["1969-12-31T17:00:10.000-07:00"], dtend: ["1969-12-31T17:00:10.005-07:00"], src: [""]]
        ]
    }
}