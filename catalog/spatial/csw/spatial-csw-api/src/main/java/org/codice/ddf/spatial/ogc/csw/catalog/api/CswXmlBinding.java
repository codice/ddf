/*
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
package org.codice.ddf.spatial.ogc.csw.catalog.api;

import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;

public interface CswXmlBinding {
  javax.xml.bind.Unmarshaller createUnmarshaller() throws JAXBException;

  Object unmarshal(Source source) throws JAXBException;

  Object unmarshal(Reader reader) throws Exception;

  void marshallWriter(Object obj, Writer w) throws JAXBException;

  void marshal(Object obj, OutputStream os) throws JAXBException;

  Object unmarshal(XMLStreamReader xmlStreamReader) throws JAXBException;
}
