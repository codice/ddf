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

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;

public class XpathQParserPlugin extends QParserPlugin {

    @Override
    public void init(NamedList args) {

    }

    @Override
    public QParser createParser(String qstr, SolrParams localParams, SolrParams params,
            SolrQueryRequest req) {
        // TODO use local params to pass prefix to namespace mappings used in XPath
        return new XpathQParser(qstr, localParams, params, req);
    }

    @Override
    public String getSource() {
        return "https://github.com/codice/ddf-solr";
    }

    @Override
    public String getName() {
        return "xpath";
    }
}
