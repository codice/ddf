/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.operation;

import java.util.Set;

/**
 * The SourceInfoRequestLocal should be used to obtain local {@link Source}
 * information. This request may obtain information from the
 * {@link CatalogProvider}.
 * 
 * @deprecated As of release 2.3.0, replaced by
 *             ddf.catalog.operation.impl.SourceInfoRequestLocal
 * 
 */
@Deprecated
public class SourceInfoRequestLocal extends OperationImpl implements SourceInfoRequest {

    protected boolean includeContentTypes = false;

    /**
     * Instantiates a new SourceInfoRequestLocal,
     * 
     * @param includeContentTypes
     *            - true to include the content types, otherwise false
     */
    public SourceInfoRequestLocal(boolean includeContentTypes) {
        super(null);
        this.includeContentTypes = includeContentTypes;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.federation.Federatable#getSourceIds()
     */
    @Override
    public Set<String> getSourceIds() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.federation.Federatable#isEnterprise()
     */
    @Override
    public boolean isEnterprise() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.operation.SourceInfoRequest#includeContentTypes()
     */
    @Override
    public boolean includeContentTypes() {
        return includeContentTypes;
    }

}
