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
package ddf.mime;

import java.util.List;

import javax.activation.MimeType;

/**
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 */
public interface MimeTypeToTransformerMapper {

    public static final String MIME_TYPE_KEY = "mime-type";

    public static final String ID_KEY = "id";

    /**
     * 
     * @param clazz
     *            The Class type of the matching services
     * @param mimeType
     *            {@link MimeType} object
     * @return all OSGi Services that match the mimeType and clazz criteria. Passing a
     *         <code>null</code> {@link MimeType} will return all the matching services of the
     *         service type specified in the clazz argument. If no services are matched then an
     *         empty {@link List} is returned.
     */
    public <T> List<T> findMatches(Class<T> clazz, MimeType mimeType);

}
