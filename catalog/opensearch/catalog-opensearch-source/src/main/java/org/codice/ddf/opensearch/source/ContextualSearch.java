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
package org.codice.ddf.opensearch.source;

import java.util.Map;

public class ContextualSearch {

  // A map of search phrases to be used to build the OpenSearch URL parameters
  // The default search phrase is "q"
  private Map<String, String> searchPhraseMap;

  private String selectors;

  private boolean isCaseSensitive;

  public ContextualSearch(
      String selectors, Map<String, String> searchPhraseMap, boolean isCaseSensitive) {
    this.selectors = selectors;
    this.searchPhraseMap = searchPhraseMap;
    this.isCaseSensitive = isCaseSensitive;
  }

  public Map<String, String> getSearchPhraseMap() {
    return searchPhraseMap;
  }

  public void setSearchPhraseMap(Map<String, String> searchPhraseMap) {
    this.searchPhraseMap = searchPhraseMap;
  }

  public String getSelectors() {
    return selectors;
  }

  public void setSelectors(String selectors) {
    this.selectors = selectors;
  }

  public boolean isCaseSensitive() {
    return isCaseSensitive;
  }

  public void setCaseSensitive(boolean isCaseSensitive) {
    this.isCaseSensitive = isCaseSensitive;
  }
}
