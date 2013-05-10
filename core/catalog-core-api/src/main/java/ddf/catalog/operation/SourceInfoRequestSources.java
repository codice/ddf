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
package ddf.catalog.operation;

import java.util.Set;

/**
 * The SourceInfoRequestSources should be used to obtain {@link Source} information
 * about specific sources identified by id.
 */
public class SourceInfoRequestSources extends SourceInfoRequestLocal {

    protected Set<String> ids = null;
	
	/**
	 * Instantiates a new SourceInfoRequestSources.
	 *
	 * @param includeContentTypes - true to include contentTypes in the response, otherwise fails
	 * @param ids - the source ids that are requested for information.
	 */
	public SourceInfoRequestSources( boolean includeContentTypes, Set<String> ids ){
        super( includeContentTypes );
        this.ids = ids;
    }

    /* (non-Javadoc)
     * @see ddf.catalog.operation.SourceInfoRequestLocal#getSourceIds()
     */
    @Override
    public Set<String> getSourceIds() {
        return ids;
    }

}
