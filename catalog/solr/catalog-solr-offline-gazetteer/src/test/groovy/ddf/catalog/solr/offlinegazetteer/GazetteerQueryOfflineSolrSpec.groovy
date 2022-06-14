/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.solr.offlinegazetteer

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.SolrRequest
import org.apache.solr.client.solrj.SolrRequest.METHOD
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.client.solrj.response.SuggesterResponse
import org.apache.solr.client.solrj.response.Suggestion
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList
import org.codice.ddf.spatial.geocoding.GeoEntry
import org.codice.ddf.spatial.geocoding.GeoEntryQueryException
import org.codice.ddf.spatial.geocoding.context.NearbyLocation
import org.apache.solr.client.solrj.SolrClient
import org.codice.solr.factory.SolrClientFactory
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import spock.lang.Specification

import java.util.stream.Stream

import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.COUNTRY_CODE
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.FEATURE_CODE
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.GAZETTEER_REQUEST_HANDLER
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.LOCATION
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.NAME
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.POPULATION
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.SORT_VALUE
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.SUGGEST_COUNT_KEY
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.SUGGEST_DICT

@RunWith(JUnitPlatform.class)
class GazetteerQueryOfflineSolrSpec extends Specification {
    GazetteerQueryOfflineSolr testedClass
    SolrClientFactory solrClientFactory
    SolrClient solrClient

    void setup() {
        solrClient = Mock(SolrClient)
        solrClientFactory = Mock(SolrClientFactory) {
            newClient(_) >> solrClient
        }
        testedClass = new GazetteerQueryOfflineSolr(solrClientFactory)
    }


    def "Test normal query"() {
        setup:
        int numResults = 10
        1 * solrClient.query({ SolrQuery it -> it.getRows() == numResults }, *_) >>
                Mock(QueryResponse) {
                    getResults() >> Mock(SolrDocumentList) {
                        stream() >> {
                            Stream.of(Mock(SolrDocument) {
                                get(NAME) >> ["title"]
                                get(POPULATION) >> [1337l]
                                get(LOCATION) >> ["POINT (-98.86253 29.18968)"]
                                get(FEATURE_CODE) >> ["PPL"]
                                get(COUNTRY_CODE) >> ["USA"]
                                get(SORT_VALUE) >> [42i]
                            })
                        }
                    }
                }

        when:
        List<GeoEntry> results = testedClass.query("sample", numResults)

        then:
        results.size() == 1
        with(results.first()) {
            name == "title"
            population == 1337
            29.18 <= latitude && latitude <= 29.19
            -98.9 <= longitude && longitude <= -98.8
            featureCode == "PPL"
            countryCode == "USA"
        }

    }

    def "Test query max results"() {
        setup:
        int numResults = 101
        1 * solrClient.
                query(*_) >> {
            SolrQuery params, SolrRequest.METHOD method ->
                assert params.getRows() == GazetteerQueryOfflineSolr.MAX_RESULTS
                Mock(QueryResponse) {
                    getResults() >> Mock(SolrDocumentList) {
                        stream() >> { Stream.empty() }
                    }
                }
        }

        when:
        List<GeoEntry> results = testedClass.query("sample", numResults)

        then:
        notThrown(Exception)

    }

    def "Test invalid wkt "() {
        setup:
        int numResults = 10
        1 * solrClient.query({ SolrQuery it -> it.getRows() == numResults }, *_) >>
                Mock(QueryResponse) {
                    getResults() >> Mock(SolrDocumentList) {
                        stream() >> {
                            Stream.of(Mock(SolrDocument) {
                                get(NAME) >> ["title"]
                                get(POPULATION) >> [1337l]
                                get(LOCATION) >> ["POINT (!!!!!INVALIDWKT"]
                            })
                        }
                    }
                }

        when:
        List<GeoEntry> results = testedClass.query("sample", numResults)

        then:
        results.size() == 1
        with(results.first()) {
            name == "title"
            population == 1337
            latitude == null
            longitude == null
        }

    }

    def "Test query solrclient exception"() {
        setup:
        int numResults = 101
        1 * solrClient.query(*_) >> { throw new SolrServerException("solr exception") }

        when:
        List<GeoEntry> results = testedClass.query("sample", numResults)

        then:
        GeoEntryQueryException e = thrown()
    }

    def "test queryById normal"() {
        setup:
        1 * solrClient.query(*_) >>
                Mock(QueryResponse) {
                    getResults() >> Mock(SolrDocumentList) {
                        stream() >> {
                            Stream.of(Mock(SolrDocument) {
                                get(NAME) >> ["title"]
                                get(POPULATION) >> [1337l]
                            })
                        }
                    }
                }
        when:
        GeoEntry result = testedClass.queryById("test")

        then:
        result.name == "title"
    }

    def "test queryById exception"() {
        setup:
        1 * solrClient.query(*_) >> { throw new SolrServerException("solr exception") }
        when:
        GeoEntry result = testedClass.queryById("test")

        then:
        GeoEntryQueryException e = thrown()
    }

    def "test getSuggestedNames normal"() {
        setup:
        int maxResults = 10
        1 * solrClient.query(*_) >> { SolrQuery query ->
            assert query.get(SUGGEST_COUNT_KEY) == "${maxResults}"
            assert query.requestHandler == GAZETTEER_REQUEST_HANDLER
            Mock(QueryResponse) {
                getSuggesterResponse() >> Mock(SuggesterResponse) {
                    getSuggestions() >>
                            [(SUGGEST_DICT): [Mock(Suggestion) {
                                getPayload() >> "id"
                                getTerm() >> "title"

                            }]]
                }
            }
        }
        when:
        List<Suggestion> results = testedClass.getSuggestedNames("place", maxResults)

        then:
        results.size() == 1
    }

    def "test getSuggestedNames maxResults"() {
        setup: "request asking for an absurdly large number of results"
        int maxResults = 8675309
        1 * solrClient.query(*_) >> { SolrQuery query ->
            assert query.get(SUGGEST_COUNT_KEY) == "${GazetteerQueryOfflineSolr.MAX_RESULTS}"
            assert query.requestHandler == GAZETTEER_REQUEST_HANDLER
            Mock(QueryResponse) {
                getSuggesterResponse() >> Mock(SuggesterResponse) {
                    getSuggestions() >>
                            [(SUGGEST_DICT): [Mock(Suggestion) {
                                getPayload() >> "id"
                                getTerm() >> "title"

                            }]]
                }
            }
        }
        when:
        List<Suggestion> results = testedClass.getSuggestedNames("place", maxResults)

        then:
        "request goes through but the suggest.count was limited to $GazetteerQueryOfflineSolr.MAX_RESULTS"
        results.size() == 1
    }

    def "test getSuggestedNames solrclient exception"() {
        setup:
        int maxResults = 10
        1 * solrClient.query(*_) >> { throw new SolrServerException("exception") }

        when:
        List<Suggestion> results = testedClass.getSuggestedNames("place", maxResults)

        then:
        GeoEntryQueryException e = thrown()
    }

    def "getNearestCities"() {
        setup:
        String locationWkt = "POINT (-98.86253 29.18968)"
        String pointAbout42kmAway = "POINT (-98.496708 29.418376)"
        int radiusInKm = 50
        int maxResults = 10

        1 * solrClient.query(*_) >> { SolrQuery query, METHOD method ->
            assert METHOD.POST == method
            assert query.query == """location_geo_index:"Intersects( POLYGON\\ \\(\\(\\-98.41264292165667\\ 29.18968,\\ \\-98.41547171717704\\ 29.13930862896764,\\ \\-98.42392252999316\\ 29.089570707152163,\\ \\-98.4378890862306\\ 29.04109171777901,\\ \\-98.45719574834108\\ 28.99448131226772,\\ \\-98.4815997238466\\ 28.950325643511576,\\ \\-98.51079411859563\\ 28.909179994664907,\\ \\-98.54441179613524\\ 28.87156179613522,\\ \\-98.58202999466492\\ 28.83794411859562,\\ \\-98.62317564351159\\ 28.808749723846585,\\ \\-98.66733131226772\\ 28.784345748341078,\\ \\-98.71394171777902\\ 28.76503908623059,\\ \\-98.76242070715217\\ 28.75107252999315,\\ \\-98.81215862896765\\ 28.742621717177027,\\ \\-98.86253\\ 28.73979292165666,\\ \\-98.91290137103236\\ 28.742621717177027,\\ \\-98.96263929284784\\ 28.75107252999315,\\ \\-99.01111828222099\\ 28.76503908623059,\\ \\-99.0577286877323\\ 28.784345748341078,\\ \\-99.10188435648843\\ 28.808749723846585,\\ \\-99.14303000533509\\ 28.83794411859562,\\ \\-99.18064820386478\\ 28.87156179613522,\\ \\-99.21426588140439\\ 28.909179994664907,\\ \\-99.24346027615341\\ 28.950325643511576,\\ \\-99.26786425165893\\ 28.99448131226772,\\ \\-99.28717091376942\\ 29.04109171777901,\\ \\-99.30113747000685\\ 29.089570707152163,\\ \\-99.30958828282297\\ 29.13930862896764,\\ \\-99.31241707834334\\ 29.18968,\\ \\-99.30958828282297\\ 29.240051371032358,\\ \\-99.30113747000685\\ 29.289789292847836,\\ \\-99.28717091376942\\ 29.338268282220987,\\ \\-99.26786425165893\\ 29.38487868773228,\\ \\-99.24346027615341\\ 29.429034356488422,\\ \\-99.21426588140439\\ 29.47018000533509,\\ \\-99.18064820386478\\ 29.507798203864777,\\ \\-99.14303000533509\\ 29.54141588140438,\\ \\-99.10188435648843\\ 29.570610276153413,\\ \\-99.0577286877323\\ 29.59501425165892,\\ \\-99.01111828222099\\ 29.614320913769408,\\ \\-98.96263929284784\\ 29.62828747000685,\\ \\-98.91290137103236\\ 29.63673828282297,\\ \\-98.86253\\ 29.639567078343337,\\ \\-98.81215862896765\\ 29.63673828282297,\\ \\-98.76242070715217\\ 29.62828747000685,\\ \\-98.71394171777902\\ 29.614320913769408,\\ \\-98.66733131226772\\ 29.59501425165892,\\ \\-98.62317564351159\\ 29.570610276153413,\\ \\-98.58202999466492\\ 29.54141588140438,\\ \\-98.54441179613524\\ 29.507798203864777,\\ \\-98.51079411859563\\ 29.47018000533509,\\ \\-98.4815997238466\\ 29.429034356488422,\\ \\-98.45719574834108\\ 29.38487868773228,\\ \\-98.4378890862306\\ 29.338268282220987,\\ \\-98.42392252999316\\ 29.289789292847836,\\ \\-98.41547171717704\\ 29.240051371032358,\\ \\-98.41264292165667\\ 29.18968\\)\\) ) AND (feature-code_txt:PPL OR feature-code_txt:PPLA OR feature-code_txt:PPLA2 OR feature-code_txt:PPLA3 OR feature-code_txt:PPLA4 OR feature-code_txt:PPLC OR feature-code_txt:PPLCH OR feature-code_txt:PPLF OR feature-code_txt:PPLG OR feature-code_txt:PPLL OR feature-code_txt:PPLR OR feature-code_txt:PPLS OR feature-code_txt:PPLX)\""""

            return Mock(QueryResponse) {
                getResults() >> Mock(SolrDocumentList) {
                    stream() >> {
                        Stream.of(Mock(SolrDocument) {
                            get(NAME) >> ["title"]
                            get(LOCATION) >> [pointAbout42kmAway]
                        })
                    }
                }
            }
        }

        when:
        List<NearbyLocation> results = testedClass.
                getNearestCities(locationWkt, radiusInKm, maxResults)

        then:
        results.size() == 1
        with(results.first()) {
            cardinalDirection == "NE"
            40 <= it.distance && it.distance <= 50
            it.name == "title"
        }
    }

    String extractIntersectsPolygon(String query) {
        String[] arr = query.split(":", 2)
        assert arr.length == 2
        List<String> queryNodes = Arrays.asList(
                arr[1].
                        replace("\"", "").
                        split("AND|OR"))
        String intersectsQuery = queryNodes.find { it.contains("Intersects") }
        String WKTPolygon = intersectsQuery.
                replace("Intersects(", "").
                trim()[0..-2].
                replace("\\", "")

        WKTPolygon
    }

    def "getNearestCities wkt parse exception"() {
        when:
        testedClass.getNearestCities("POINT( !! INVALID WKT ", 50, 10)

        then:
        GeoEntryQueryException e = thrown()

    }

    def "getNearestCities solr query exception"() {
        setup:
        1 * solrClient.query(*_) >> { SolrQuery query, METHOD method ->
            throw new SolrServerException("Exception")
        }
        when:
        testedClass.getNearestCities("POINT (-98.86253 29.18968)", 50, 10)

        then:
        GeoEntryQueryException e = thrown()

    }

    def "getCountryCode normal"() {
        setup:
        1 * solrClient.query(*_) >> { SolrQuery query, METHOD method ->
            assert METHOD.POST == method
            assert query.rows == 1

            Mock(QueryResponse) {
                getResults() >> Mock(SolrDocumentList) {
                    stream() >> {
                        Stream.of(Mock(SolrDocument) {
                            get(NAME) >> ["title"]
                            get(COUNTRY_CODE) >> ["USA"]
                        })
                    }
                }
            }
        }

        when:
        Optional<String> result = testedClass.getCountryCode("POINT (-98.86253 29.18968)", 50)

        then:
        result.isPresent()
        result.get() == "USA"

    }

    def "getCountryCode with exception"() {
        setup:
        1 * solrClient.query(*_) >> { SolrQuery query, METHOD method ->
            throw new SolrServerException("exception")
        }

        when:
        Optional<String> result = testedClass.getCountryCode("POINT (-98.86253 29.18968)", 50)

        then:
        GeoEntryQueryException e = thrown()

    }

    def "getCountryCode no results"() {
        setup:
        1 * solrClient.query(*_) >> { SolrQuery query, METHOD method ->
            assert METHOD.POST == method
            assert query.rows == 1

            Mock(QueryResponse) {
                getResults() >> Mock(SolrDocumentList) {
                    stream() >> {
                        Stream.empty()
                    }
                }
            }
        }

        when:
        Optional<String> result = testedClass.getCountryCode("POINT (-98.86253 29.18968)", 50)

        then:
        !result.isPresent()

    }

    def "getCountryCode invalid wkt"() {
        when:
        Optional<String> result = testedClass.getCountryCode("POINT (!!INVALIDWKT !!", 50)

        then:
        GeoEntryQueryException e = thrown()

    }

}
