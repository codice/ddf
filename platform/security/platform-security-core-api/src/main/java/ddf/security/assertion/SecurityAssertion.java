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
package ddf.security.assertion;

import java.io.Serializable;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * This class serves as a wrapper for a CXF SecurityToken
 *
 * @author tustisos
 */
public interface SecurityAssertion extends Serializable {

  int IDP_AUTH_WEIGHT = 0;

  int LOCAL_AUTH_WEIGHT = 5;

  int NO_AUTH_WEIGHT = 10;

  /**
   * Returns the Principal contained within the SecurityToken
   *
   * @return Principal
   */
  Principal getPrincipal();

  /**
   * Returns the name of the entity that issued the SecurityToken
   *
   * @return String - token issuer
   */
  String getIssuer();

  /**
   * Returns the list of attribute statements contained in the SecurityToken
   *
   * @return List<AttributeStatement>
   */
  List<AttributeStatement> getAttributeStatements();

  /**
   * Returns the list of authn statements contained in the SecurityToken
   *
   * @return List<AuthnStatement>
   */
  List<AuthenticationStatement> getAuthnStatements();

  /**
   * Returns the list of subject confirmations contained in the SecurityToken
   *
   * @return List<String>
   */
  List<String> getSubjectConfirmations();

  /**
   * Returns primary principal and all attributes as principals
   *
   * @return List<Principal>
   */
  Set<Principal> getPrincipals();

  /**
   * Returns the token type URI for this assertion
   *
   * @return either http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0 or
   *     http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV1.1
   */
  String getTokenType();

  /**
   * Returns the underlying token that this object wraps
   *
   * @return SecurityToken
   */
  Object getToken();

  /**
   * Returns the earliest date that the assertion is valid
   *
   * @return Date
   */
  Date getNotBefore();

  /**
   * Returns the date that the assertion is invalid
   *
   * @return Date
   */
  Date getNotOnOrAfter();

  /**
   * Returns a String representation of this Assertion
   *
   * @return String
   */
  String toString();

  /**
   * Returns the weight associated with the type of authentication used to produce this assertion
   *
   * @return int
   */
  int getWeight();

  /**
   * Returns true if checked while within the time bounds defined by NotBefore and NotOnOrAfter
   *
   * @return boolean
   */
  boolean isPresentlyValid();
}
