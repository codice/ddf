/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.transformer.xml.streaming;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import java.util.List;
import java.util.Set;
import org.xml.sax.ContentHandler;

/**
 * Interface used to handle sax events fired from {@link
 * org.codice.ddf.transformer.xml.streaming.lib.SaxEventHandlerDelegate} and {@link
 * org.codice.ddf.transformer.xml.streaming.lib.XMLInputTransformer}. At the end of parsing, it will
 * have a list of {@link Attribute}s that can be used to populate a metacard. A specific
 * implementation can be used to handle specific XML elements and metacards. @see
 * ddf.catalog.transformer.generic.xml.impl.XMLSaxEventHandlerImpl {@inheritDoc}
 *
 * <p><b> This code is experimental. While this class is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface SaxEventHandler extends ContentHandler {

  /**
   * @return a list of attributes that has been constructed during the parsing of an XML document.
   */
  List<Attribute> getAttributes();

  /**
   * Get all the possible attribute types that can be returned by this factory's handler
   *
   * @return a set of attribute descriptors that can be returned by this handler
   */
  Set<AttributeDescriptor> getSupportedAttributeDescriptors();
}
