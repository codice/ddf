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
package ddf.catalog.resource;

import java.io.InputStream;

import javax.activation.MimeType;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * BinaryContentImpl is the default implementation of {@link BinaryContent}
 * 
 * @see InputTransformer
 * @see QueryResponseTransformer
 * 
 * @author LMCO
 */
@Deprecated
public class BinaryContentImpl extends ddf.catalog.data.BinaryContentImpl implements BinaryContent {

    public BinaryContentImpl(InputStream inputStream, MimeType mimeType) {
        super(inputStream, mimeType);
    }

    public BinaryContentImpl(InputStream inputStream) {
        super(inputStream);
    }

}
