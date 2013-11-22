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

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import ddf.catalog.data.Metacard;

/**
 * CreateRequestImpl represents a {@link CreateRequest} and supports passing a {@link Map} of
 * properties for create operations.
 * 
 * @deprecated Use ddf.catalog.operation.impl.CreateRequestImpl
 */
@Deprecated
public class CreateRequestImpl extends OperationImpl implements CreateRequest {

    /** The metacards to be created */
    protected List<Metacard> metacards;

    /**
     * Instantiates a new CreateRequestImpl with a single {@link Metacard}.
     * 
     * @param metacard
     *            the metacard
     */
    public CreateRequestImpl(Metacard metacard) {
        this(Arrays.asList(metacard), null);
    }

    /**
     * Instantiates a new CreateRequestImpl with a {@link List} of {@link Metacard}.
     * 
     * @param metacards
     *            the metacards
     */
    public CreateRequestImpl(List<Metacard> metacards) {
        this(metacards, null);
    }

    /**
     * Instantiates a new CreateRequestImpl with a {@link List} of {@link Metacard}. and a
     * {@link Map} of properties.
     * 
     * @param metacards
     *            the metacards
     * @param properties
     *            the properties
     */
    public CreateRequestImpl(List<Metacard> metacards, Map<String, Serializable> properties) {
        super(properties);
        this.metacards = metacards;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.operation.CreateRequest#getMetacards()
     */
    @Override
    public List<Metacard> getMetacards() {
        return metacards;
    }

}
