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

import org.apache.lucene.search.IndexSearcher;
import org.apache.solr.search.DelegatingCollector;
import org.apache.solr.search.ExtendedQueryBase;
import org.apache.solr.search.PostFilter;

public class XpathFilterQuery extends ExtendedQueryBase implements PostFilter {

    public static final int POST_FILTER_COST = 100;

    private final String xpath;

    public XpathFilterQuery(String xpath) {
        super();
        super.setCost(POST_FILTER_COST);
        super.setCache(false);
        super.setCacheSep(false);

        this.xpath = xpath;
    }

    @Override
    public DelegatingCollector getFilterCollector(IndexSearcher searcher) {
        return new XpathFilterCollector(xpath);
    }

    @Override
    public void setCost(int cost) {
        if (cost > POST_FILTER_COST) {
            super.setCost(cost);
        }
    }

    @Override
    public boolean getCache() {
        return false;
    }

    @Override
    public String toString(String field) {
        return "XpathFilterQuery[" + xpath + "]";
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        XpathFilterQuery other = (XpathFilterQuery) obj;
        return xpath.equals(other.xpath);
    }

}
