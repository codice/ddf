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
package org.codice.ddf.catalog.ui.query.delegate;

import java.util.regex.Pattern;
import org.codice.util.tlmatcher.TimeLimitedMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchTerm {
  private static final Logger LOGGER = LoggerFactory.getLogger(SearchTerm.class);

  private final String term;

  private final Pattern pattern;

  private final String cqlTerm;

  public SearchTerm(String searchTerm) {
    term = searchTerm.toLowerCase();
    cqlTerm = searchTerm.replace("*", "%");

    if (searchTerm.contains("*")) {
      pattern = Pattern.compile("^" + searchTerm.replace("*", ".*"), Pattern.CASE_INSENSITIVE);
    } else {
      pattern = null;
    }
  }

  public String getCqlTerm() {
    return cqlTerm;
  }

  public String getTerm() {
    return term;
  }

  public boolean match(String other) {
    if ("*".equals(term)) {
      return true;
    }
    if (pattern != null) {
      try {
        return TimeLimitedMatcher.create(pattern, other).matches();
      } catch (TimeLimitedMatcher.TimeoutException e) {
        LOGGER.warn("Timeout performing RegEx match for {} against string {}", term, other);
        return false;
      }
    } else {
      return term.equals(other);
    }
  }
}
