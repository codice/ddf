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

import java.util.List;

import org.xml.sax.ContentHandler;

import ddf.catalog.data.Attribute;

/**
 * Interface used to handle sax events fired from {@link org.codice.ddf.transformer.xml.streaming.lib.SaxEventHandlerDelegate} and {@link org.codice.ddf.transformer.xml.streaming.lib.XMLInputTransformer}.
 * At the end of parsing, it will have a list of {@link Attribute}s that can be used to populate a metacard.
 * A specific implementation can be used to handle specific XML elements and metacards. @see ddf.catalog.transformer.generic.xml.impl.XMLSaxEventHandlerImpl
 * {@inheritDoc}
 */
public interface SaxEventHandler extends ContentHandler {

    /**
     * @return a list of attributes that has been constructed during the parsing of an XML document.
     */
    List<Attribute> getAttributes();

}
