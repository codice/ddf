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
package ddf.catalog.federation;

import java.util.Set;
/**
 * Interface describing an object that can be federated with other {@link Source}s
 *
 * @author ddf.isgs@lmco.com
 */
public interface Federatable{

	/**
	 * Returns the source ids that are associated with 
	 * the {@code Federatable} implementation.  The source ids will be the complete
	 * set of sources that should be acted upon. A set is used to 
	 * avoid duplicate ids.
	 * 
	 * @return set of source ids that should be acted upon.  
	 * Can be {@code null} or an empty list if {@link #isEnterprise()} method returns 
	 * {@code true} as that means all known sources should be acted upon (and the 
	 * complete list might not have been known at Object creation).
	 */
	public Set<String> getSourceIds();
	
	/**
	 * Specifies whether the {@code Federatable} implementation should be applied to
	 * the enterprise
	 * 
	 * @return {@code true} if the {@code Federatable} implementation should be applied
	 * to the entire enterprise (meaning all sources), otherwise {@code false} is returned.
	 * If {@code false} is returned then a list containing at least 1 source id should
	 * be returned by the {@link #getSourceIds()} method.
	 */
	public boolean isEnterprise();
	
}