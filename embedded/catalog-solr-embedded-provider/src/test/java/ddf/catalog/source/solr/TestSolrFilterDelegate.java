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
package ddf.catalog.source.solr;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.when;

import org.apache.solr.client.solrj.SolrQuery;
import org.junit.Test;

import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.Metacard;

public class TestSolrFilterDelegate {

    private DynamicSchemaResolver mockResolver = mock(DynamicSchemaResolver.class);

    private SolrFilterDelegate toTest = new SolrFilterDelegate(mockResolver);

    @Test(expected = UnsupportedOperationException.class)
    public void intersectsWithNullWkt() {
        // given null WKT and a valid property name
        stub(mockResolver.getField("testProperty", AttributeFormat.GEOMETRY, false)).toReturn(
                "testProperty_geohash_index");
        // when the delegate intersects
        toTest.intersects("testProperty", null);
        // then the operation is unsupported
    }

    @Test(expected = UnsupportedOperationException.class)
    public void intersectsWithNullPropertyName() {
        // given null property name
        // when the delegate intersects
        toTest.intersects(null, "wkt");
        // then the operation is unsupported
    }

    @Test
    public void intersectsWithInvalidJtsWkt() {
        // given a geospatial property
        stub(mockResolver.getField("testProperty", AttributeFormat.GEOMETRY, false)).toReturn(
                "testProperty_geohash_index");

        // when the delegate intersects on WKT not handled by JTS
        SolrQuery query = toTest.intersects("testProperty", "invalid JTS wkt");

        // then return a valid Solr query using the given WKT
        assertThat(query.getQuery(),
                is("testProperty_geohash_index:\"Intersects(invalid JTS wkt)\""));
    }

    @Test
    public void reservedSpecialCharactersIsEqual() {
        // given a text property
        stub(mockResolver.getField("testProperty", AttributeFormat.STRING, true)).toReturn(
                "testProperty_txt_index");

        // when searching for exact reserved characters
        SolrQuery equalQuery = toTest.propertyIsEqualTo("testProperty",
                "+ - && || ! ( ) { } [ ] ^ \" ~ :", true);

        // then return escaped special characters in the query
        assertThat(
                equalQuery.getQuery(),
                is("testProperty_txt_index:\"\\+ \\- \\&& \\|| \\! \\( \\) \\{ \\} \\[ \\] \\^ \\\" \\~ \\:\""));
    }

    @Test
    public void reservedSpecialCharactersIsLike() {
        // given a tokenized text property
        stub(mockResolver.getField("testProperty", AttributeFormat.STRING, false)).toReturn(
                "testProperty_txt_index");
        stub(mockResolver.getCaseSensitiveField("testProperty_txt_index")).toReturn(
                "testProperty_txt_index_tokenized");

        // when searching for like reserved characters
        SolrQuery likeQuery = toTest.propertyIsLike("testProperty",
                "+ - && || ! ( ) { } [ ] ^ \" ~ : \\*?", true);

        // then return escaped special characters in the query
        assertThat(
                likeQuery.getQuery(),
                is("testProperty_txt_index_tokenized:(\\+ \\- \\&& \\|| \\! \\( \\) \\{ \\} \\[ \\] \\^ \\\" \\~ \\: \\*?)"));
    }

    /*OMIT
    @Test
    public void testPropertyIsEqualTo_AnyText_CaseSensitive() {
        String expectedQuery = "any_text:\"mySearchPhrase\"";
        String searchPhrase = "mySearchPhrase";
        boolean isCaseSensitive = true;
        SolrQuery equalToQuery = toTest.propertyIsEqualTo(Metacard.ANY_TEXT, searchPhrase,
                isCaseSensitive);
        assertThat(equalToQuery.getQuery(), is(expectedQuery));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyIsEqualTo_AnyText_CaseInsensitive() {
        String searchPhrase = "mySearchPhrase";
        boolean isCaseSensitive = false;
        toTest.propertyIsEqualTo(Metacard.ANY_TEXT, searchPhrase, isCaseSensitive);
    }

    @Test
    public void testPropertyIsFuzzy_AnyText() {
        String expectedQuery = "+any_text:mysearchphrase~ ";
        String searchPhrase = "mySearchPhrase";
        SolrQuery fuzzyQuery = toTest.propertyIsFuzzy(Metacard.ANY_TEXT, searchPhrase);
        assertThat(fuzzyQuery.getQuery(), is(expectedQuery));
    }

    @Test
    public void testPropertyIsLike_AnyText_CaseInsensitive() {
        String expectedQuery = "any_text:\"mySearchPhrase\"";
        String searchPhrase = "mySearchPhrase";
        boolean isCaseSensitive = false;
        SolrQuery isLikeQuery = toTest.propertyIsLike(Metacard.ANY_TEXT, searchPhrase,
                isCaseSensitive);
        assertThat(isLikeQuery.getQuery(), is(expectedQuery));
    }

    @Test
    public void testPropertyIsLike_AnyText_CaseSensitive() {
        String expectedQuery = "any_text_has_case:\"mySearchPhrase\"";
        String searchPhrase = "mySearchPhrase";
        boolean isCaseSensitive = true;
        when(mockResolver.getCaseSensitiveField("any_text")).thenReturn(
                "any_text" + SchemaFields.HAS_CASE);
        SolrQuery isLikeQuery = toTest.propertyIsLike(Metacard.ANY_TEXT, searchPhrase,
                isCaseSensitive);
        assertThat(isLikeQuery.getQuery(), is(expectedQuery));
    }
    END OMIT*/

}
