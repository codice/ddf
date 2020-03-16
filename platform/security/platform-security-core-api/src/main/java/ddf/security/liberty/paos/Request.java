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

public interface Request {

  String PAOS_NS = "urn:liberty:paos:2003-08";

  String PAOS_PREFIX = "paos";

  String DEFAULT_ELEMENT_LOCAL_NAME = "Request";

  QName DEFAULT_ELEMENT_NAME = new QName(PAOS_NS, DEFAULT_ELEMENT_LOCAL_NAME, PAOS_PREFIX);

  String TYPE_LOCAL_NAME = "RequestType";

  QName TYPE_NAME = new QName(PAOS_NS, TYPE_LOCAL_NAME, PAOS_PREFIX);

  String RESPONSE_CONSUMER_URL_ATTRIB_NAME = "responseConsumerURL";

  String SERVICE_ATTRIB_NAME = "service";

  String MESSAGE_ID_ATTRIB_NAME = "messageID";

  String ECP_SERVICE = "urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp";

  /**
   * Get the responseConsumerURL attribute value.
   *
   * @return String
   */
  String getResponseConsumerURL();

  /**
   * Set the responseConsumerURL attribute value.
   *
   * @param newResponseConsumerURL the new responseConsumerURL attribute value
   */
  void setResponseConsumerURL(String newResponseConsumerURL);

  /**
   * Get the service attribute value.
   *
   * @return String
   */
  String getService();

  /**
   * Set the service attribute value.
   *
   * @param newService the new service attribute value
   */
  void setService(String newService);

  /**
   * Get the messageID attribute value.
   *
   * @return String
   */
  String getMessageID();

  /**
   * Set the messageID attribute value.
   *
   * @param newMessageID the new messageID attribute value
   */
  void setMessageID(String newMessageID);
}
