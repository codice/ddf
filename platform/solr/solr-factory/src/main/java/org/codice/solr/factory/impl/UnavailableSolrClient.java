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
package org.codice.solr.factory.impl;

import org.apache.commons.lang.Validate;
import org.apache.solr.client.solrj.SolrClient;
import org.codice.solr.client.solrj.UnavailableSolrException;

/** A SolrClient that will fail all calls to API methods. */
public class UnavailableSolrClient extends SolrClientProxy {
  private final Throwable cause;

  /**
   * Instantiates an <code>UnvailableSolrClient</code> with the provided messages to return as part
   * of the exception thrown from all methods.
   *
   * @param cause the cause for being unavailable
   * @throws IllegalArgumentException if <code>cause</code> is <code>null</code>
   */
  public UnavailableSolrClient(Throwable cause) {
    Validate.notNull(cause, "invalid null cause");
    this.cause = cause;
  }

  @Override
  protected SolrClient getProxiedClient() {
    throw new UnavailableSolrException(cause);
  }

  @Override
  public void close() { // nothing to close
  }

  public Throwable getCause() {
    return cause;
  }

  @Override
  public String toString() {
    return "UnavailableSolrClient(" + cause + ")";
  }
}
