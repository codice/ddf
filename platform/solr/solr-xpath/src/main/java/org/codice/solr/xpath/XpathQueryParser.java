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
package org.codice.solr.xpath;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.Query;
import org.apache.solr.search.SolrQueryParser;
import org.apache.solr.search.SyntaxError;

/** XPath query parser that will create Lucene and Post Filter queries to support XPath. */
public class XpathQueryParser extends SolrQueryParser {

  private final XpathQParser parser;

  public XpathQueryParser(XpathQParser parser, String defaultField) {
    super(parser, defaultField);
    this.parser = parser;
  }

  @Override
  protected Query getFieldQuery(String field, String queryText, int slop) throws SyntaxError {

    if (field.equals("xpath")) {
      // post-filter with Saxon
      return new XpathFilterQuery(queryText);
    } else if (field.equals("xpath_index")) {
      // pre-filter with xpath index
      return getLuceneQuery(queryText);
    } else {
      // pass-through any non-XPath related fields
      return super.getFieldQuery(field, queryText, slop);
    }
  }

  /**
   * Converts XPath into a Lucene query that will pre-filter based on xpath path and attribute index
   * fields. Further post filtering is needed for XPath functionality that cannot evaluated against
   * xpath index.
   *
   * @param queryText XPath expression to convert into lucene path and attribute index query
   * @return Lucene query to pre-filter using xpath index
   */
  private Query getLuceneQuery(final String queryText) {
    String xpath = queryText;

    // Assume root is context node since evaluation does not have a context item
    if (StringUtils.startsWith(xpath, "./")) {
      xpath = StringUtils.removeStart(xpath, ".");
    } else if (!StringUtils.startsWith(xpath, "/")) {
      xpath = "/" + xpath;
    }

    return null; // TODO DDF-1882 add lucene xpath pre-filtering
  }
}
