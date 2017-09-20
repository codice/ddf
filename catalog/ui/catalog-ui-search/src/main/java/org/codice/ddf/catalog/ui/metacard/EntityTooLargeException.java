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
package org.codice.ddf.catalog.ui.metacard;

/** Exception class for a HTTP 413 status code - Entity Too Large */
public class EntityTooLargeException extends RuntimeException {
  private String ip;

  private String userAgent;

  private String url;

  private int id;

  public EntityTooLargeException(String ip, String userAgent, String url, int id) {
    super();
    this.ip = ip;
    this.userAgent = userAgent;
    this.url = url;
    this.id = id;
  }

  @Override
  public String getMessage() {
    return String.format(
        "Client sent a request body that was too large. {ip='%s', userClient='%s', url='%s', id='0x%08X'",
        ip, userAgent, url, id);
  }

  public int getId() {
    return id;
  }

  public String getStringId() {
    return String.format("0x%08X", id);
  }
}
