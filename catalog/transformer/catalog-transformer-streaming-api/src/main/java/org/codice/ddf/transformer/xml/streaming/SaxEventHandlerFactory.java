/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.transformer.xml.streaming;

import java.util.Set;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.util.Describable;

/**
 * A factory for {@link SaxEventHandler}
 * {@inheritDoc}
 */
public interface SaxEventHandlerFactory extends Describable {

    /**
     * Get a new handler of this factory's type
     *
     * @return a new handler to handle SAX events
     */
    SaxEventHandler getNewSaxEventHandler();

    /**
     * Get all the possible attribute types that can be returned by this factory's handler
     *
     * @return a set of attribute descriptors that can be returned by this handler
     */
    Set<AttributeDescriptor> getSupportedAttributeDescriptors();

}
