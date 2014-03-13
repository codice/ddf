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
package org.codice.solr.xpath;

import org.apache.lucene.search.Query;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SyntaxError;

public class XpathQParser extends QParser {

    private final SolrQueryRequest request;

    public XpathQParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
        super(qstr, localParams, params, req);
        request = req;
    }

    @Override
    public Query parse() throws SyntaxError {
        String qstr = getString();

        String defaultField = getParam(CommonParams.DF);
        if (defaultField == null) {
            defaultField = getReq().getSchema().getDefaultSearchFieldName();
        }
        XpathQueryParser queryParser = new XpathQueryParser(this, defaultField);

        queryParser.setDefaultOperator(QueryParsing.getQueryParserDefaultOperator(getReq()
                .getSchema(), getParam(QueryParsing.OP)));

        return queryParser.parse(qstr);
    }

    public SolrQueryRequest getRequest() {
        return request;
    }
}
