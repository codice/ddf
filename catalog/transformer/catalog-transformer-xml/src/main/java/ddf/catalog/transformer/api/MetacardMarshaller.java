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
package ddf.catalog.transformer.api;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import ddf.catalog.data.Metacard;
import ddf.catalog.transform.CatalogTransformerException;

/**
 * MetacardMarshaller transforms a Metacard to a StringWriter.
 */
public interface MetacardMarshaller {

    /**
     * Transform a {@link Metacard} into an XML {@link String}.
     *
     * @param metacard the Metacard instance to transform.
     * @return String
     * @throws XmlPullParserException
     * @throws IOException
     * @throws CatalogTransformerException
     */
    String marshal(Metacard metacard)
            throws XmlPullParserException, IOException, CatalogTransformerException;
}
