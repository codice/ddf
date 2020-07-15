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


import ddf.catalog.Constants
import ddf.catalog.data.Metacard
import ddf.catalog.data.impl.AttributeImpl
import ddf.catalog.data.types.Core
import ddf.catalog.data.types.Location
import ddf.catalog.operation.CreateResponse
import ddf.catalog.operation.DeleteResponse
import ddf.catalog.operation.QueryRequest
import ddf.catalog.operation.Update
import ddf.catalog.operation.UpdateResponse
import ddf.catalog.plugin.PluginExecutionException
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.common.SolrInputDocument
import org.apache.solr.common.params.SolrParams
import org.codice.ddf.spatial.geocoding.GeoEntryAttributes
import org.codice.solr.client.solrj.SolrClient
import org.codice.solr.factory.SolrClientFactory
import spock.lang.Specification

import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.STANDALONE_GAZETTEER_CORE_NAME
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.SUGGEST_BUILD_KEY
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.SUGGEST_DICT
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.SUGGEST_DICT_KEY

class OfflineGazetteerPluginSpec extends Specification {
    OfflineGazetteerPlugin testedPlugin

    SolrClientFactory solrClientFactory
    SolrClient solrClient

    static List<Metacard> resourceMetacards
    static List<Metacard> gazetteerMetacards
    static Metacard fullGazetteerMetacard

    void setupSpec() {

    }

    void setup() {
        solrClient = Mock(SolrClient)
        solrClientFactory = Mock(SolrClientFactory) {
            newClient(STANDALONE_GAZETTEER_CORE_NAME) >> solrClient
        }
        testedPlugin = new OfflineGazetteerPlugin(solrClientFactory)

        // testing data
        resourceMetacards = [
                Mock(Metacard) {
                    getTitle() >> "resource metacard mock 1"
                    getAttribute(Core.TITLE) >> new AttributeImpl(Core.TITLE,
                                                                  "resource metacard mock 1")
                    getTags() >> ["resource"]
                    getAttribute(Metacard.TAGS) >> new AttributeImpl(Metacard.TAGS, ["resource"])
                    getId() >> "id1"
                    getAttribute(Core.ID) >> new AttributeImpl(Core.ID, "id1")
                },
                Mock(Metacard) {
                    getTitle() >> "resource metacard mock 2"
                    getAttribute(Core.TITLE) >> new AttributeImpl(Core.TITLE,
                                                                  "resource metacard mock 2")
                    getTags() >> ["resource"]
                    getAttribute(Metacard.TAGS) >> new AttributeImpl(Metacard.TAGS, ["resource"])
                    getId() >> "id2"
                    getAttribute(Core.ID) >> new AttributeImpl(Core.ID, "id2")
                }]


        gazetteerMetacards = [
                fullGazetteerMetacard = Mock(Metacard) {
                    getTags() >> ["gazetteer"]
                    getAttribute(Metacard.TAGS) >> new AttributeImpl(Metacard.TAGS, ["gazetteer"])
                    getAttribute(Metacard.DESCRIPTION) >> new AttributeImpl(Metacard.DESCRIPTION,
                                                                            "mock gazetteer metacard desc")
                    getAttribute(GeoEntryAttributes.
                            FEATURE_CODE_ATTRIBUTE_NAME) >> new AttributeImpl(GeoEntryAttributes.
                            FEATURE_CODE_ATTRIBUTE_NAME, "PPL")
                    getTitle() >> "gazetteer metacard mock 1"
                    getAttribute(Core.TITLE) >> new AttributeImpl(Core.TITLE,
                                                                  "gazetteer metacard mock 1")
                    getId() >> "id3"
                    getAttribute(Core.ID) >> new AttributeImpl(Core.ID, "id3")
                    getAttribute(Location.COUNTRY_CODE) >> new AttributeImpl(Location.COUNTRY_CODE,
                                                                             "FRA")
                    getAttribute(Core.LOCATION) >> new AttributeImpl(Core.LOCATION,
                                                                     "POINT (3.999 48.36502)")
                    getAttribute(GeoEntryAttributes.POPULATION_ATTRIBUTE_NAME) >> new AttributeImpl(
                            GeoEntryAttributes.POPULATION_ATTRIBUTE_NAME,
                            2724)
                    getAttribute(GeoEntryAttributes.GAZETTEER_SORT_VALUE) >> new AttributeImpl(
                            GeoEntryAttributes.GAZETTEER_SORT_VALUE, 10)
                },
                Mock(Metacard) {
                    getTags() >> ["gazetteer"]
                    getAttribute(Metacard.TAGS) >> new AttributeImpl(Metacard.TAGS, ["gazetteer"])
                    getAttribute(Metacard.DESCRIPTION) >> new AttributeImpl(Metacard.DESCRIPTION,
                                                                            "mock gazetteer metacard desc2")
                    getAttribute(GeoEntryAttributes.
                            FEATURE_CODE_ATTRIBUTE_NAME) >> new AttributeImpl(Metacard.DESCRIPTION,
                                                                              "PPL")
                    getTitle() >> "gazetteer metacard mock 2"
                    getAttribute(Core.TITLE) >> new AttributeImpl(Metacard.DESCRIPTION,
                                                                  "gazetteer metacard mock 2")
                    getId() >> "id4"
                    getAttribute(Core.ID) >> new AttributeImpl(Metacard.DESCRIPTION, "id4")
                    getAttribute(Location.COUNTRY_CODE) >> new AttributeImpl(Metacard.DESCRIPTION,
                                                                             "USA")
                    getAttribute(Core.LOCATION) >> new AttributeImpl(Metacard.DESCRIPTION,
                                                                     "POINT (-87.70058 34.27482)")
                    getAttribute(GeoEntryAttributes.POPULATION_ATTRIBUTE_NAME) >> new AttributeImpl(
                            Metacard.DESCRIPTION,
                            1047)
                    getAttribute(GeoEntryAttributes.GAZETTEER_SORT_VALUE) >> null
                }]
    }

    def "Create processing no-ops when there are no gazetteer metacards"() {
        setup:
        CreateResponse createResponse = Mock(CreateResponse) {
            getCreatedMetacards() >> resourceMetacards
        }
        when: "called with no gazetteer metacards"
        CreateResponse result = testedPlugin.process(createResponse)

        then: "solr client is not called and the input is immediately returned"
        0 * solrClient.add(*_)
    }

    def "Create processing sends only gazetteer metacards to solr"() {
        setup:
        CreateResponse createResponse = Mock(CreateResponse) {
            getCreatedMetacards() >> resourceMetacards + gazetteerMetacards
        }

        when:
        CreateResponse result = testedPlugin.process(createResponse)

        then: "solr receives 2 items with the correct number of attributes"
        1 * solrClient.
                add(_, { it.size() == 2 && it.collect { it.keySet().size() }.containsAll([6, 7]) })
    }

    // This is testing implementation specifics that could change -- meaning this is brittle but to
    // document how it handles validation (anything goes).
    def "Create processing pulls needed fields from metacard"() {
        setup: "a metacard that manages to hit the plugin (has a gazetteer tag and an id) but nothing else"
        CreateResponse createResponse =
                Mock(CreateResponse) {
                    getCreatedMetacards() >> [
                            Mock(Metacard) {
                                getTags() >> ["gazetteer"]
                                getAttribute(Metacard.
                                        TAGS) >> new AttributeImpl(Metacard.TAGS,
                                                                   ["gazetteer"])
                                getId() >> "id3b"
                                getAttribute(Core.ID) >> new AttributeImpl(Core.ID,
                                                                           "id3b")
                            }]
                }

        when:
        CreateResponse result = testedPlugin.process(createResponse)

        then:
        1 * solrClient.add(_, _) >>
                { String collection, Collection<SolrInputDocument> docs ->
                    assert docs.size() == 1
                    docs.first().with {
                        assert keySet().size() == 1

                    }
                }
    }

    def "Create processing throws plugin exception when solr client throws exception"() {
        setup:
        CreateResponse createResponse = Mock(CreateResponse) {
            getCreatedMetacards() >> gazetteerMetacards
        }

        when:
        1 * solrClient.add(_, _) >> { throw new SolrServerException("exception") }
        CreateResponse result = testedPlugin.process(createResponse)

        then:
        PluginExecutionException e = thrown()
    }

    def "Delete processing no-ops when no gazetteer metacards present"() {
        setup:
        DeleteResponse deleteResponse = Mock(DeleteResponse) {
            getDeletedMetacards() >> resourceMetacards
        }
        when:
        DeleteResponse resp = testedPlugin.process(deleteResponse)

        then:
        0 * solrClient./delete.*/(*_)
    }

    def "Delete processing deletes all gazetteer metacards"() {
        setup:
        DeleteResponse deleteResponse = Mock(DeleteResponse) {
            getDeletedMetacards() >> resourceMetacards + gazetteerMetacards
        }

        when:
        DeleteResponse resp = testedPlugin.process(deleteResponse)

        then:
        1 * solrClient.deleteById(["id3", "id4"])
    }

    def "Delete processing throws plugin exception when solr client throws exception"() {
        setup:
        DeleteResponse deleteResponse = Mock(DeleteResponse) {
            getDeletedMetacards() >> gazetteerMetacards
        }

        when:
        1 * solrClient.deleteById(_) >> { throw new SolrServerException("exception") }
        DeleteResponse result = testedPlugin.process(deleteResponse)

        then:
        PluginExecutionException e = thrown()
    }

    def "Update no-ops when no gazetteer metacards are present"() {
        setup:
        UpdateResponse updateResponse = Mock(UpdateResponse) {
            getUpdatedMetacards() >> resourceMetacards.collect { metacard ->
                Mock(Update) {
                    getNewMetacard() >> metacard
                }
            }
        }

        when:
        UpdateResponse resp = testedPlugin.process(updateResponse)

        then:
        0 * solrClient.add(*_)
    }

    def "Update processes gazetteer metacards"() {
        setup:
        UpdateResponse updateResponse = Mock(UpdateResponse) {
            getUpdatedMetacards() >> (resourceMetacards + gazetteerMetacards).collect { metacard ->
                Mock(Update) {
                    getNewMetacard() >> metacard
                }
            }
        }

        when:
        UpdateResponse resp = testedPlugin.process(updateResponse)

        then:
        1 * solrClient.add(_, { it.size() == 2 })
    }

    def "Update throws plugin exception when solr throws exception"() {
        setup:
        UpdateResponse updateResponse = Mock(UpdateResponse) {
            getUpdatedMetacards() >> (resourceMetacards + gazetteerMetacards).collect { metacard ->
                Mock(Update) {
                    getNewMetacard() >> metacard
                }
            }
        }

        when:
        1 * solrClient.add(*_) >> { throw new SolrServerException("exception") }
        UpdateResponse resp = testedPlugin.process(updateResponse)

        then:
        PluginExecutionException e = thrown()
    }

    def "Prequery plugin rebuilds dictionary when suggestion build key is present"() {
        setup:
        QueryRequest queryRequest = Mock(QueryRequest) {
            getPropertyValue(Constants.SUGGESTION_BUILD_KEY) >> true
        }

        when:
        QueryRequest response = testedPlugin.process(queryRequest)

        then:
        1 * solrClient.query(*_) >> {
            args ->
                args.first().with { SolrParams it ->
                    assert it.get(SUGGEST_BUILD_KEY) == "true"
                    assert it.get(SUGGEST_DICT_KEY) == SUGGEST_DICT
                }

        }
    }

    def "prequery plugin no-ops if no suggestion build key is present"() {
        setup:
        QueryRequest queryRequest = Mock(QueryRequest)

        when:
        QueryRequest response = testedPlugin.process(queryRequest)

        then:
        0 * solrClient.query(*_)
    }

    def "Prequery plugin throws plugin exception when solr errors"() {
        setup:
        QueryRequest queryRequest = Mock(QueryRequest) {
            getPropertyValue(Constants.SUGGESTION_BUILD_KEY) >> true
        }

        when:
        1 * solrClient.query(*_) >> { throw new SolrServerException("exception") }
        QueryRequest response = testedPlugin.process(queryRequest)

        then:
        PluginExecutionException e = thrown()
    }

    def "All plugins are ephemeral and return the input (an identity transformation, with side effects)"() {
        // Note this test leaves the equality semantics to the objects being tested.. we will
        // assume that they have sufficiently defined that already
        expect: "the exact result as the input after processing"
        parameter == testedPlugin.process(parameter)

        where:
        parameter << [
                Mock(CreateResponse) {
                    getCreatedMetacards() >> gazetteerMetacards
                },
                Mock(UpdateResponse) {
                    getUpdatedMetacards() >> gazetteerMetacards.collect { metacard ->
                        Mock(Update) {
                            getNewMetacard() >> metacard
                        }
                    }
                },
                Mock(DeleteResponse) {
                    getDeletedMetacards() >> gazetteerMetacards
                },
                Mock(QueryRequest)
        ]
    }

}
