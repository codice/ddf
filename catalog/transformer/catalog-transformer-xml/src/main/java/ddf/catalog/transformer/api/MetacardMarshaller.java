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
package ddf.catalog.transformer.api;

import ddf.catalog.data.Metacard;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import org.xmlpull.v1.XmlPullParserException;

/** MetacardMarshaller transforms a Metacard to a StringWriter. */
public interface MetacardMarshaller {

  /**
   * Transform a {@link Metacard} into an XML {@link String}.
   *
   * <p>Convenience method that always produces xml string with XML declaration prepended.
   *
   * @param metacard the Metacard instance to transform.
   * @return String
   * @throws XmlPullParserException
   * @throws IOException
   * @throws CatalogTransformerException
   */
  String marshal(Metacard metacard)
      throws XmlPullParserException, IOException, CatalogTransformerException;

  /**
   * Transform a {@link Metacard} into an XML {@link String}.
   *
   * <p>Use the map of arguments to configure the xml output. For example, to turn off the XML
   * declaration, do {@code arguments.put("OMIT_XML_DECLARATION", Boolean.TRUE)}.
   *
   * @param metacard the Metacard instance to transform.
   * @param arguments map of arguments.
   * @return String
   * @throws XmlPullParserException
   * @throws IOException
   * @throws CatalogTransformerException
   */
  String marshal(Metacard metacard, Map<String, Serializable> arguments)
      throws XmlPullParserException, IOException, CatalogTransformerException;
}
