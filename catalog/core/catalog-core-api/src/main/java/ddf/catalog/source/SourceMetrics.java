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

/** Contains metric names related to {@link Source}s. */
public class SourceMetrics {

  private SourceMetrics() {}

  public static final String METRICS_PREFIX = "ddf.catalog.source";

  public static final String QUERY_SCOPE = "query";

  public static final String REQUEST_TYPE = "request";

  public static final String RESPONSE_TYPE = "response";

  public static final String EXCEPTION_TYPE = "exception";
}
