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
package org.codice.ddf.catalog.ui.query.utility;

import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryRequest;
import java.util.List;
import java.util.Set;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface CqlRequest {

  String getCacheId();

  void setCacheId(String cacheId);

  Set<String> getFacets();

  void setFacets(Set<String> facets);

  List<String> getSrcs();

  String getSrc();

  void setQueryType(String queryType);

  String getQueryType();

  void setBatchId(String batchId);

  String getBatchId();

  void setSpellcheck(boolean spellcheck);

  boolean getSpellcheck();

  void setPhonetics(boolean phonetics);

  boolean getPhonetics();

  void setSrc(String src);

  void setSrcs(List<String> srcs);

  long getTimeout();

  void setTimeout(long timeout);

  int getStart();

  void setStart(int start);

  int getCount();

  void setCount(int count);

  String getCql();

  void setCql(String cql);

  List<CqlRequest.Sort> getSorts();

  void setSorts(List<CqlRequest.Sort> sorts);

  boolean isNormalize();

  void setNormalize(boolean normalize);

  QueryRequest createQueryRequest(String localSource, FilterBuilder filterBuilder);

  String getSourceResponseString();

  String getSource();

  String getId();

  void setId(String id);

  boolean isExcludeUnnecessaryAttributes();

  void setExcludeUnnecessaryAttributes(boolean excludeUnnecessaryAttributes);

  public static class Sort {

    private String attribute;
    private String direction;

    public Sort(String attribute, String direction) {
      this.attribute = attribute;
      this.direction = direction;
    }

    public String getAttribute() {
      return attribute;
    }

    public void setAttribute(String attribute) {
      this.attribute = attribute;
    }

    public String getDirection() {
      return direction;
    }

    public void setDirection(String direction) {
      this.direction = direction;
    }
  }
}
