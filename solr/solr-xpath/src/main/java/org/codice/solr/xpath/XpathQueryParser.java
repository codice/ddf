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

import lux.Compiler;
import lux.Evaluator;
import lux.XdmResultSet;
import lux.functions.Search;
import lux.search.LuxSearcher;
import lux.solr.SolrIndexConfig;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.Query;
import org.apache.solr.common.SolrException;
import org.apache.solr.search.SolrQueryParser;
import org.apache.solr.search.SyntaxError;

/**
 * XPath query parser that will create Lucene and Post Filter queries to support XPath.
 */
public class XpathQueryParser extends SolrQueryParser {

    private final XpathQParser parser;

    public XpathQueryParser(XpathQParser parser, String defaultField) {
        super(parser, defaultField);
        this.parser = parser;
    }

    @Override
    protected Query getFieldQuery(String field, String queryText, boolean quoted)
        throws SyntaxError {

        if (field.equals("xpath")) {
            // post filter with Saxon
            return new XpathFilterQuery(queryText);
        } else if (field.equals("xpath_index")) {
            // pre filter with Lux index fields
            return getLuceneQuery(queryText);
        } else {
            // pass through any non-XPath related fields
            return super.getFieldQuery(field, queryText, quoted);
        }
    }

    /**
     * Converts XPath into a Lucene query that will pre-filter based on LuxDB path and attribute
     * index fields. Further post filtering is needed for XPath functionality that cannot evaluated
     * against a LuxDB index.
     * 
     * @param queryText
     *            XPath expression to convert into LuxDB index query
     * @return Lucene query to pre-filter using LuxDB index
     */
    private Query getLuceneQuery(final String queryText) {
        String xpath = queryText;

        SolrIndexConfig solrIndexConfig = SolrIndexConfig.registerIndexConfiguration(parser
                .getRequest().getCore());
        LuxSearcher searcher = new LuxSearcher(parser.getRequest().getSearcher());
        Compiler compiler = new Compiler(solrIndexConfig.getIndexConfig());
        Evaluator evaluator = new Evaluator(compiler, searcher, null);

        // get Lucene query for Lux indexes by overriding lux:search function
        LuceneSearch lsearch = new LuceneSearch();
        evaluator.getCompiler().getProcessor().registerExtensionFunction(lsearch);

        // Assume root is context node since evaluation does not have a context item
        if (StringUtils.startsWith(xpath, "./")) {
            xpath = StringUtils.removeStart(xpath, ".");
        } else if (!StringUtils.startsWith(xpath, "/")) {
            xpath = "/" + xpath;
        }

        XdmResultSet result = evaluator.evaluate(xpath);

        Query luxQuery = lsearch.getQuery();
        if (luxQuery == null || result.getErrors().size() > 0) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                    "Failed to rewrite XPath into Query: " + queryText + "\nErrors: "
                            + result.getErrors().toString());
        }

        return luxQuery;
    }

    /**
     * Lux search function that is overridden to store the Lucene query and return an empty sequence
     * iterator to bypass further Saxon evaluation. This extension function, like the evaluator, is
     * not thread safe.
     */
    public class LuceneSearch extends Search {

        private Query query;

        @Override
        public SequenceIterator<NodeInfo> iterate(Query searchQuery, Evaluator eval,
                String[] sortCriteria, int start) throws XPathException {
            query = searchQuery;

            return new SequenceIterator<NodeInfo>() {
                @Override
                public NodeInfo next() throws XPathException {
                    return null;
                }

                @Override
                public NodeInfo current() {
                    return null;
                }

                @Override
                public int position() {
                    return 0;
                }

                @Override
                public void close() {

                }

                @Override
                public SequenceIterator<NodeInfo> getAnother() throws XPathException {
                    return null;
                }

                @Override
                public int getProperties() {
                    return 0;
                }
            };
        }

        @Override
        public SequenceIterator<NodeInfo> iterateDistributed(String searchQuery,
                QueryParser queryParser, Evaluator eval, String[] sortCriteria, int start)
            throws XPathException {
            return super.iterateDistributed(searchQuery, queryParser, eval, sortCriteria, start);
        }

        public Query getQuery() {
            return query;
        }
    }

}
