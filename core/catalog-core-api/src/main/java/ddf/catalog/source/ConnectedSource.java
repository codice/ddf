/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.source;

import ddf.catalog.operation.Query;
import ddf.catalog.util.Maskable;

/**
 * Provides an implementation of {@link Source} that represents a remote
 * {@Source} that is always included in every local and enterprise
 * {@link Query} processed. However, {@link ConnectedSource} implementations can
 * never be queried individually by clients, meaning a client cannot query a
 * {@link ConnectedSource} by name. 
 * 
 * @see Source
 * @see Query
 */
public interface ConnectedSource extends RemoteSource, Maskable {

}
