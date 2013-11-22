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
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The DeleteRequestImpl class is used to delete a single or list of the {@link String} id or
 * {@link URI}.
 * @deprecated Use ddf.catalog.operation.impl.DeleteRequestImpl
 */
@Deprecated
public class DeleteRequestImpl extends OperationImpl implements DeleteRequest {

    /** The name of the attribute indicating id or URI */
    protected String name;

    /** Ids or URIs */
    protected List<Serializable> values;

    /**
     * Instantiates a new DeleteRequestImpl with a single {@link String} id to be deleted.
     * 
     * @param id
     *            - the id to be used for the delete operation
     */
    public DeleteRequestImpl(String id) {
        this(new String[] {id});
    }

    /**
     * Instantiates a new DeleteRequestImpl with an array of {@link String} id to be deleted.
     * 
     * @param id1
     *            - the id to be used for the delete operation
     */
    public DeleteRequestImpl(String[] ids) {
        this(Arrays.asList((Serializable[]) ids), DeleteRequest.DELETE_BY_ID, null);
    }

    /**
     * Instantiates a new DeleteRequestImpl to be deleted with an array of {@link String} ids and a
     * {@link Map} of properties
     * 
     * @param ids
     *            - {@link String} list of ids
     * @param properties
     *            - the properties associated with the operation
     */
    public DeleteRequestImpl(String[] ids, Map<String, Serializable> properties) {
        this(Arrays.asList((Serializable[]) ids), DeleteRequest.DELETE_BY_ID, properties);
    }

    /**
     * Instantiates a new DeleteRequestImpl with an single {@link URI}
     * 
     * @param uri
     *            - the {@link URI} to be used for the delete operation
     */
    public DeleteRequestImpl(URI uri) {
        this(new URI[] {uri});
    }

    /**
     * Instantiates a new DeleteRequestImpl with a {@link URI} array
     * 
     * @param uris
     *            - the list of {@link URI} to be used for the delete operation
     */
    public DeleteRequestImpl(URI[] uris) {
        this(Arrays.asList((Serializable[]) uris), DeleteRequest.DELETE_BY_PRODUCT_URI, null);
    }

    /**
     * Instantiates a new DeleteRequestImpl with a {@link URI} array with a {@link Map} of
     * properties
     * 
     * @param uris
     *            - the list of {@link URI} to be deleted
     * @param properties
     *            - the properties associated with the delete operation
     */
    public DeleteRequestImpl(URI[] uris, Map<String, Serializable> properties) {
        this(Arrays.asList((Serializable[]) uris), DeleteRequest.DELETE_BY_PRODUCT_URI, properties);
    }

    /**
     * Instantiates a new DeleteRequestImpl with a {@link List} of {@link Serializable} values. This
     * allows for custom delete operations aside from String id or URI.
     * 
     * @param values
     *            - the values to be used in the delete operation
     * @param attributeName
     *            - the attribute name associated with the values
     * @param properties
     *            the properties
     */
    public DeleteRequestImpl(List<Serializable> values, String attributeName,
            Map<String, Serializable> properties) {
        super(properties);
        this.name = attributeName;
        this.values = values;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.operation.DeleteRequest#getAttributeName()
     */
    @Override
    public String getAttributeName() {
        return name;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.operation.DeleteRequest#getAttributeValues()
     */
    @Override
    public List<Serializable> getAttributeValues() {
        return values;
    }

}
