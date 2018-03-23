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

import java.io.InputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import net.opengis.filter.v_2_0.FilterType;

/** Provide functions for hydrating Filter XML into Filter JAXB objects. */
public class FilterReader {
  private final JAXBContext context;

  public FilterReader() throws JAXBException {
    this.context = JAXBContext.newInstance(FilterType.class);
  }

  public JAXBElement<FilterType> unmarshalFilter(InputStream inputStream) throws JAXBException {
    return unmarshal(inputStream, FilterType.class);
  }

  @SuppressWarnings("unchecked")
  private <T> JAXBElement<T> unmarshal(InputStream inputStream, Class<T> tClass)
      throws JAXBException {
    Unmarshaller unmarshaller = context.createUnmarshaller();
    Object result = unmarshaller.unmarshal(inputStream);
    if (result instanceof JAXBElement) {
      JAXBElement element = (JAXBElement) result;
      if (tClass.isInstance(element.getValue())) {
        return (JAXBElement<T>) element;
      }
    }
    return null;
  }
}
