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
package org.codice.ddf.security.idp.binding.api;

/** Describes a SAML 2 Web SSO binding and associated pieces. */
public interface Binding {

  /**
   * Returns the request decoder appropriate for this binding.
   *
   * @return RequestDecoder
   */
  RequestDecoder decoder();

  /**
   * Returns the response creator appropriate for this binding.
   *
   * @return ResponseCreator
   */
  ResponseCreator creator();

  /**
   * Returns the request validator appropriate for this binding.
   *
   * @return Validator
   */
  Validator validator();
}
