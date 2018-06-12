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
package ddf.catalog.operation;

import ddf.catalog.data.Result;
import java.util.List;
import java.util.Set;

/**
 * Query status should come back in the properties.
 *
 * @author Michael Menousek
 */
public interface SourceResponse extends Response<QueryRequest> {

  /**
   * The total number of hits matching the associated {@link Query} for the associated {@link
   * ddf.catalog.source.Source}, -1 if unknown. This is typically more than the number of {@link
   * Result}s returned from {@link #getResults()}.
   *
   * @return long - total hits matching this {@link Query}, -1 if unknown.
   */
  public long getHits();

  /**
   * Get the {@link Result}s of the associated {@link QueryRequest}
   *
   * @return the results
   */
  public List<Result> getResults();

  /**
   * Get any specific details about the execution of the {@link QueryRequest} associated with this
   * {@link SourceResponse}. <b>Must not be null.</b>
   *
   * @return the processing details
   */
  public Set<? extends SourceProcessingDetails> getProcessingDetails();
}
