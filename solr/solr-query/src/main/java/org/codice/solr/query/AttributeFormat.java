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
package org.codice.solr.query;

import java.io.Serializable;
import java.util.Date;


public enum AttributeFormat {

    /**
     * {@link Attribute}s of this {@link AttributeFormat} <b>must</b> have a value that is a
     * Java String and treated as plain text.
     */
    STRING,

    /**
     * {@link Attribute}s of this {@link AttributeFormat} <b>must</b> have a value that is a
     * Java {@link Boolean}.
     */
    BOOLEAN,

    /**
     * {@link Attribute}s of this {@link AttributeFormat} <b>must</b> have a value that is a
     * Java {@link Date}.
     */
    DATE,

    /**
     * {@link Attribute}s of this {@link AttributeFormat} <b>must</b> have a value that is a
     * Java {@link Short}.
     */
    SHORT,

    /**
     * {@link Attribute}s of this {@link AttributeFormat} <b>must</b> have a value that is a
     * Java {@link Integer}.
     */
    INTEGER,

    /**
     * {@link Attribute}s of this {@link AttributeFormat} <b>must</b> have a value that is a
     * Java {@link Long}.
     */
    LONG,

    /**
     * {@link Attribute}s of this {@link AttributeFormat} <b>must</b> have a value that is a
     * Java {@link Float}.
     */
    FLOAT,

    /**
     * {@link Attribute}s of this {@link AttributeFormat} <b>must</b> have a value that is a
     * Java {@link Double}.
     */
    DOUBLE,

    /**
     * {@link Attribute}s of this {@link AttributeFormat} <b>must</b> have a value that is a
     * WKT-formatted Java {@link String}.
     */
    GEOMETRY,

    /**
     * {@link Attribute}s of this {@link AttributeFormat} <b>must</b> have a value that is a
     * Java <code>byte[]</code> and {@link AttributeType#getBinding()} should return
     * <code>Class&lt;Array&gt;</code> of <code>byte</code>.
     */
    BINARY,

    /**
     * {@link Attribute}s of this {@link AttributeFormat} <b>must</b> have a value that is a
     * XML-formatted Java {@link String}.
     */
    XML,

    /**
     * {@link Attribute}s of this {@link AttributeFormat} <b>must</b> have a value that
     * implements the {@link Serializable} interface. Additional processing (
     * {@link AttributeDescriptor#isIndexed() indexing},
     * {@link AttributeDescriptor#isTokenized() tokenizing}) will not be performed even if
     * indicated by the {@link AttributeDescriptor}. <br>
     * 
     * <p>
     * <b>NOTE:</b> In order for classes to be deserialized by a {@link Source}, that class must
     * exist on the classpath of that {@link Source}. This may require additional parameters to
     * be set such as DynamicImport-Package in order for the Source to accommodate OBJECT
     * format.
     * </p>
     * 
     * @see Serializable
     */
    OBJECT

}
