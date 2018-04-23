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
package org.codice.ddf.catalog.ui.forms.filter;

import java.io.StringWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import net.opengis.filter.v_2_0.FilterType;

public class FilterWriter {
  private final JAXBContext context;

  public FilterWriter() throws JAXBException {
    this.context = JAXBContext.newInstance(FilterType.class);
  }

  public String marshal(JAXBElement element) throws JAXBException {
    StringWriter writer = new StringWriter();
    Marshaller marshaller = context.createMarshaller();
    marshaller.marshal(element, writer);
    return writer.toString();
  }
}
