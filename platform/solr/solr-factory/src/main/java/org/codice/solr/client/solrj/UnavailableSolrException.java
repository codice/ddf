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
package org.codice.solr.client.solrj;

import javax.annotation.Nullable;
import org.apache.solr.common.SolrException;

/** Exception thrown when the Solr server or a core is not available. */
public class UnavailableSolrException extends SolrException {
  private static final long serialVersionUID = 6220554636663010299L;

  public UnavailableSolrException(String message, @Nullable Throwable cause) {
    super(ErrorCode.SERVICE_UNAVAILABLE, message, cause);
  }

  public UnavailableSolrException(String message) {
    super(ErrorCode.SERVICE_UNAVAILABLE, message);
  }

  public UnavailableSolrException(Throwable cause) {
    super(ErrorCode.SERVICE_UNAVAILABLE, cause.getMessage(), cause);
  }
}
