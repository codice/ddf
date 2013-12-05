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

import java.util.List;

/**
 * The SourceProcessingDetailsImpl class represents a default implementation of
 * a {@link SourceProcessingDetails} to provide {@link Source} warnings.
 * 
 * @deprecated As of release 2.3.0, replaced by
 *             ddf.catalog.operation.impl.SourceProcessingDetailsImpl
 * 
 */
@Deprecated
public class SourceProcessingDetailsImpl implements SourceProcessingDetails {

    protected List<String> warnings = null;

    /**
     * Instantiates a new SourceProcessingDetailsImpl.
     */
    public SourceProcessingDetailsImpl() {
    }

    /**
     * Instantiates a new SourceProcessingDetailsImpl.
     * 
     * @param warnings
     *            the warnings associated with the {@link Source}
     */
    public SourceProcessingDetailsImpl(List<String> warnings) {
        this.warnings = warnings;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.operation.SourceProcessingDetails#getWarnings()
     */
    @Override
    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * Sets the warnings associated with the {@link Source}.
     * 
     * @param warnings
     *            the new warnings associated with the {@link Source}
     */
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

}
