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
package ddf.security.liberty.paos;

import javax.xml.namespace.QName;

public interface Response {

  String PAOS_NS = "urn:liberty:paos:2003-08";

  String PAOS_PREFIX = "paos";

  String DEFAULT_ELEMENT_LOCAL_NAME = "Response";

  QName DEFAULT_ELEMENT_NAME = new QName(PAOS_NS, DEFAULT_ELEMENT_LOCAL_NAME, PAOS_PREFIX);

  String TYPE_LOCAL_NAME = "ResponseType";

  QName TYPE_NAME = new QName(PAOS_NS, TYPE_LOCAL_NAME, PAOS_PREFIX);

  String REF_TO_MESSAGE_ID_ATTRIB_NAME = "refToMessageID";

  /**
   * Get the refToMessageID attribute value.
   *
   * @return String
   */
  String getRefToMessageID();

  /**
   * Set the refToMessageID attribute value.
   *
   * @param newRefToMessageID the new refToMessageID attribute value
   */
  void setRefToMessageID(String newRefToMessageID);
}
