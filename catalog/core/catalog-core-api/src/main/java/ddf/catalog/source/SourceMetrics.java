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
package ddf.catalog.source;

/**
 * The interface {@link SourceMetrics} used by the {@link FederationStrategy} to update metrics on
 * individual {@link Source}s as queries and exceptions occur when the {@link Source} is accessed.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 *
 * @author rodgersh
 */
public interface SourceMetrics {

  public static final String METRICS_PREFIX = "ddf.catalog.source";

  public static final String QUERY_SCOPE = "query";

  public static final String REQUEST_TYPE = "request";

  public static final String RESPONSE_TYPE = "response";

  public static final String EXCEPTION_TYPE = "exception";
}
