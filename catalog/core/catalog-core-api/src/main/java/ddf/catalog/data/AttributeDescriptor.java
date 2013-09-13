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
package ddf.catalog.data;

import java.io.Serializable;
import java.util.List;

import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.operation.Query;
import ddf.catalog.source.Source;

/**
 * AttributeDescriptor describes an {@link Attribute} and associates it with a {@link AttributeType}
 * .
 * 
 * @author ddf.isgs@lmco.com
 * 
 */
public interface AttributeDescriptor extends Serializable {

    /**
     * Returns the name of this {@link AttributeDescriptor}
     * 
     * @return {@link String} - the name of this {@link AttributeDescriptor}
     */
    public String getName();

    /**
     * Returns whether or not {@link Attribute}s described by this {@link AttributeDescriptor} are
     * multivalued, that is, {@link Attribute#getValue()} returns a {@link List}.
     * 
     * @return whether or not it is multivalued
     */
    public boolean isMultiValued();

    /**
     * Returns the {@link AttributeType} of {@link Attribute}s described by this
     * {@link AttributeDescriptor}.
     * 
     * @return {@link AttributeType} - the type of values of {@link Attribute}s described by this
     *         {@link AttributeDescriptor}
     */
    public AttributeType<?> getType();

    /**
     * Whether or not this {@link Attribute} should be indexed by {@link Source} implementations,
     * therefore part of {@link Query} evaluation. <br/>
     * Some {@link Attribute}s may only want to be stored, not indexed.
     * 
     * @see #isStored()
     * 
     * @return boolean - whether or not to index the associated {@link Attribute}
     */
    public boolean isIndexed();

    /**
     * If the {@link AttributeFormat} of this {@link Attribute} is {@link AttributeFormat#STRING},
     * returns whether or not {@link Source} implementations should tokenize it; removing stopwords,
     * etc.
     * 
     * <p>
     * <b> This method is only relevant to {@link Attribute}s with an {@link AttributeFormat} of
     * {@link AttributeFormat#STRING} </b>
     * </p>
     * 
     * Some {@link Attribute}s may only need to be stored, not indexed (or vice versa).
     * 
     * @see #isStored()
     * 
     * @return boolean - whether or not to tokenize values of associated {@link Attribute}s
     */
    public boolean isTokenized();

    /**
     * Returns whether or not {@link Source} implementations must store the value of this
     * {@link Attribute}. Some {@link Attribute}s may only need to be indexed, not stored (or vice
     * versa).
     * 
     * @see #isIndexed()
     * 
     * @return boolean - whether or not to store this {@link Attribute}
     */
    public boolean isStored();
}
