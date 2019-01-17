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
package org.codice.ddf.dominion.commons.options;

/** Class used to defined known claims. */
public class Claims {
  /** Claim for an email address. */
  public static final String EMAIL =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress";

  /** Claim for a role. */
  public static final String ROLE = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";

  /** Claim for a unique identifier. */
  public static final String UID =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier";

  /** Claim for a surname. */
  public static final String SURNAME =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname";

  /** Claim for a given name. */
  public static final String GIVEN_NAME =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname";

  private Claims() {}
}
