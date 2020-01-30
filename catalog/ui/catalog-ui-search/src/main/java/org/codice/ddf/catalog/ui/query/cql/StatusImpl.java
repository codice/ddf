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
package org.codice.ddf.catalog.ui.query.cql;

import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.QueryResponse;
import java.util.Set;
import org.codice.ddf.catalog.ui.query.utility.Status;

public class StatusImpl implements Status {

  private final long hits;

  private final long count;

  private final long elapsed;

  private final String id;

  private final boolean successful;

  public StatusImpl(QueryResponse response, String source, long elapsedTime) {
    elapsed = elapsedTime;
    id = source;

    count = response.getResults().size();
    hits = response.getHits();
    successful = isSuccessful(response.getProcessingDetails());
  }

  private boolean isSuccessful(final Set<ProcessingDetails> details) {
    for (ProcessingDetails detail : details) {
      if (detail.hasException()) {
        return false;
      }
    }
    return true;
  }

  public long getHits() {
    return hits;
  }

  public long getElapsed() {
    return elapsed;
  }

  public String getId() {
    return id;
  }

  public long getCount() {
    return count;
  }

  public boolean getSuccessful() {
    return successful;
  }
}
