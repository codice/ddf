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
package ddf.catalog.cache;

import ddf.catalog.data.Metacard;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.IOException;
import java.util.List;
import org.apache.solr.client.solrj.SolrServerException;
import org.opengis.filter.Filter;

public interface SolrCacheMBean {

  String OBJECT_NAME = "ddf.catalog.cache.solr.impl.SolrCache:service=cache-manager";

  void removeAll() throws IOException, SolrServerException;

  void removeById(String[] ids) throws IOException, SolrServerException;

  List<Metacard> query(Filter filter) throws UnsupportedQueryException;
}
