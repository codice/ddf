/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.util.impl;

import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.source.SourceDescriptor;

/**
 * Comparator for the sourceId of 2 {@link SourceDescriptor} objects
 *
 */
public class SourceDescriptorComparator implements Comparator<SourceDescriptor> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceDescriptorComparator.class);

    /**
     * Uses the {@link String}#compareTo method on the lower-cased sourceId fields to sort
     */
    @Override
    public int compare(SourceDescriptor one, SourceDescriptor two) {
        if (one.getSourceId() != null && two.getSourceId() != null) {
            return one.getSourceId().toLowerCase().compareTo(two.getSourceId().toLowerCase());
        } else {
            LOGGER.warn("Error comparing results, at least one was null.  Returning 1: ");
            return 1;
        }
    }

}
