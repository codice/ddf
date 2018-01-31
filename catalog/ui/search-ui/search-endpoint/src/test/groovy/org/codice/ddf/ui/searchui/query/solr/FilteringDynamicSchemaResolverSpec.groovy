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
package org.codice.ddf.ui.searchui.query.solr

import ddf.catalog.data.AttributeType
import ddf.catalog.data.Metacard
import ddf.catalog.data.impl.MetacardImpl
import ddf.catalog.filter.FilterAdapter
import ddf.catalog.operation.Query
import ddf.catalog.operation.QueryRequest
import ddf.catalog.source.UnsupportedQueryException
import ddf.catalog.source.solr.SchemaFields
import ddf.catalog.source.solr.SolrFilterDelegate
import ddf.catalog.source.solr.SolrFilterDelegateFactory
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrInputDocument
import org.apache.solr.common.util.NamedList
import org.apache.solr.common.util.SimpleOrderedMap
import org.opengis.filter.sort.SortBy
import spock.lang.Specification

import static ddf.catalog.source.solr.SchemaFields.METACARD_TYPE_FIELD_NAME

class FilteringDynamicSchemaResolverSpec extends Specification {

    public static final String ID_TXT_FIELD = "$Metacard.ID$SchemaFields.TEXT_SUFFIX"

    public static final String SOURCE_ID_TXT_FIELD = "source-id$SchemaFields.TEXT_SUFFIX"

    public static final String TITLE_TXT_FIELD = "$Metacard.TITLE$SchemaFields.TEXT_SUFFIX"

    public static final String METADATA_TXT_FIELD = "$Metacard.METADATA$SchemaFields.TEXT_SUFFIX"

    public static final String METADATA_TXT_TOKENIZED_FIELD = "$METADATA_TXT_FIELD$SchemaFields.TOKENIZED"

    public static final String METADATA_TXT_WS_FIELD = "$METADATA_TXT_FIELD$SchemaFields.WHITESPACE_TEXT_SUFFIX"

    public static final String METADATA_TXT_WS_HAS_CASE_FIELD = "$METADATA_TXT_WS_FIELD$SchemaFields.HAS_CASE"

    def adapter = Mock(FilterAdapter)

    def delegateFactory = Mock(SolrFilterDelegateFactory) {
        newInstance(_) >> Mock(SolrFilterDelegate)
    }

    def request = Mock(QueryRequest) {
        getQuery() >> Mock(Query) {
            getSortBy() >> Mock(SortBy)
        }
    }

    def resolver = new FilteringDynamicSchemaResolver(adapter, delegateFactory, request)

    MetacardImpl metacard = new MetacardImpl() {{
        setId("id")
        setTitle("title")
        setLocation("POINT(0 0)")
        setMetadata("<foo>bar</foo>")
    }};

    SolrInputDocument solrDoc = new SolrInputDocument()

    def "New resolver checks filter"() {
        when:
        new FilteringDynamicSchemaResolver(adapter, delegateFactory, request)

        then:
        1 * adapter.adapt(*_)
    }

    def "Throw exception if invalid query"() {
        setup:
        adapter.adapt(_,_) >> { throw new UnsupportedQueryException() }

        when:
        new FilteringDynamicSchemaResolver(adapter, delegateFactory, request)

        then:
        thrown IllegalArgumentException
    }

    def "Do not remove required attributes"() {
        when:
        resolver.addFields(metacard, solrDoc)

        then:
        solrDoc.getFieldNames().size() == 3
        solrDoc.getFieldNames().containsAll([ID_TXT_FIELD, METACARD_TYPE_FIELD_NAME, SOURCE_ID_TXT_FIELD])
    }

    def "Do not filter used anonymous fields that are known"() {
        setup:
        SolrClient server = Mock(SolrClient) {
            query(_) >> {
                Mock(QueryResponse) {
                    getResponse() >> {
                        Mock(NamedList) {
                            get(_) >> {
                                SimpleOrderedMap fields = new SimpleOrderedMap();
                                fields.add(TITLE_TXT_FIELD, null)
                                fields
                            }
                        }
                    }
                }
            }
        }

        when:
        resolver.addFieldsFromClient(server)
        resolver.getAnonymousField("title")
        resolver.addFields(metacard, solrDoc)

        then:
        solrDoc.getFieldNames().size() == 4
        solrDoc.getFieldNames().contains(TITLE_TXT_FIELD)
    }

    def "Filter used anonymous fields that are unknown"() {
        when:
        resolver.getAnonymousField("title")
        resolver.addFields(metacard, solrDoc)

        then:
        solrDoc.getFieldNames().size() == 3
        !solrDoc.getFieldNames().contains(TITLE_TXT_FIELD)
    }

    def "Do not filter used case sensitive fields"() {
        when:
        resolver.getCaseSensitiveField(METADATA_TXT_WS_FIELD)
        resolver.addFields(metacard, solrDoc)

        then:
        solrDoc.getFieldNames().contains(METADATA_TXT_TOKENIZED_FIELD)
    }

    def "Do not filter used whitespace tokenized fields"() {
        when:
        resolver.getWhitespaceTokenizedField(METADATA_TXT_FIELD)
        resolver.addFields(metacard, solrDoc)

        then:
        solrDoc.getFieldNames().contains(METADATA_TXT_TOKENIZED_FIELD)
    }

    def "Do not filter used fields"() {
        when:
        resolver.getField("title", AttributeType.AttributeFormat.STRING, true)
        resolver.addFields(metacard, solrDoc)

        then:
        solrDoc.getFieldNames().size() == 4
        solrDoc.getFieldNames().contains(TITLE_TXT_FIELD)
    }

    def "Filter fields that are not in the metacard type"() {
        when:
        resolver.getField("foo", AttributeType.AttributeFormat.STRING, true)
        resolver.addFields(metacard, solrDoc)

        then:
        solrDoc.getFieldNames().size() == 3
        !solrDoc.getFieldNames().contains("foo" + SchemaFields.TEXT_SUFFIX)
    }

}
