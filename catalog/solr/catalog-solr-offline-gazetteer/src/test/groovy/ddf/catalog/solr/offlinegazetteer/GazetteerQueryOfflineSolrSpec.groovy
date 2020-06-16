package ddf.catalog.solr.offlinegazetteer

import ddf.catalog.solr.offlinegazetteer.GazetteerQueryOfflineSolr
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
import org.codice.solr.client.solrj.SolrClient
import org.codice.solr.factory.SolrClientFactory
import org.locationtech.jts.io.WKTReader
import spock.lang.Specification

import java.util.stream.Stream

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
                                get("title_txt") >> ["title"]
                                get("ext.population_lng") >> [1337l]
                                get("location_geo") >> ["POINT (-98.86253 29.18968)"]
                                get("ext.feature-code_txt") >> ["PPL"]
                                get("location.country-code_txt") >> ["USA"]
                                get("ext.gazetteer-sort-value_int") >> [42i]
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
                                get("title_txt") >> ["title"]
                                get("ext.population_lng") >> [1337l]
                                get("location_geo") >> ["POINT (!!!!!INVALIDWKT"]
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
                                get("title_txt") >> ["title"]
                                get("ext.population_lng") >> [1337l]
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
            assert query.get("suggest.count") == "${maxResults}"
            assert query.requestHandler == "/suggest"
            Mock(QueryResponse) {
                getSuggesterResponse() >> Mock(SuggesterResponse) {
                    getSuggestions() >>
                            [(GazetteerQueryOfflineSolr.SUGGEST_PLACE_KEY): [Mock(Suggestion) {
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
            assert query.get("suggest.count") == "${GazetteerQueryOfflineSolr.MAX_RESULTS}"
            assert query.requestHandler == "/suggest"
            Mock(QueryResponse) {
                getSuggesterResponse() >> Mock(SuggesterResponse) {
                    getSuggestions() >>
                            [(GazetteerQueryOfflineSolr.SUGGEST_PLACE_KEY): [Mock(Suggestion) {
                                getPayload() >> "id"
                                getTerm() >> "title"

                            }]]
                }
            }
        }
        when:
        List<Suggestion> results = testedClass.getSuggestedNames("place", maxResults)

        then: "request goes through but the suggest.count was limited to $GazetteerQueryOfflineSolr.MAX_RESULTS"
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


            // TODO (RCZ) - I really don't like extracting the polygon from the solr query. its
            // risky logic and closely coupling to the implementation. But I also don't want to
            // open access and have a @VisibleForTesting..

            // A point about 42km away should fall well within searching for cities within 50miles
            WKTReader reader = new WKTReader()
            String WKTPolygon = extractIntersectsPolygon(query.getQuery())
            assert reader.read(WKTPolygon).contains(reader.read(pointAbout42kmAway))

            Mock(QueryResponse) {
                getResults() >> Mock(SolrDocumentList) {
                    stream() >> {
                        Stream.of(Mock(SolrDocument) {
                            get("title_txt") >> ["title"]
                            get("location_geo") >> [pointAbout42kmAway]
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
                            get("title_txt") >> ["title"]
                            get("location.country-code_txt") >> ["USA"]
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
