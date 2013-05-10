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
package ddf.catalog.util;

/**
 * Used to specify an item whose ID is to be masked when it reports its
 * ID externally, e.g., in a query response. Refer to {@link ConnectedSource} 
 * for an example of a source that is Maskable.
 * 
 * For example, say DDF is configured to have a site name of "ddf" and a remote source 
 * as its catalog provider, and this source has a site name of source-123. 
 * Since the catalog provider and DDF are considered a single entity, the site name 
 * reported in query results from the source-123 provider will be "ddf" since the
 * remote catalog provider's ID is masked to be DDF's site name.
 * 
 * @see ConnectedSource
 * 
 * @author ddf.isgs@lmco.com
 * 
 */
public interface Maskable extends Describable {

	/**
	 * Instructs this source of the id to use when reporting an id externally. <b>When
	 * instructed, the provided ID <em>must</em> be used and <em>can not</em> be
	 * overridden.</b>
	 * 
	 * @param id the new id to use
	 */
	public void maskId(String id);

}
