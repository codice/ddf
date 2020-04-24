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
package ddf.security;

import ddf.security.assertion.SecurityAssertion;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import javax.security.auth.x500.X500Principal;
import org.apache.shiro.subject.Subject;
import org.bouncycastle.asn1.x500.RDN;

public interface SubjectOperations {
  String GUEST_DISPLAY_NAME = "Guest";

  String EMAIL_ADDRESS_CLAIM_URI =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress";

  String EMAIL_ADDRESS_CLAIM_ALTERNATE = "email";

  /** Street address */
  String STREET_ADDRESS_CLAIM_URI =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/streetaddress";

  /** Postal address */
  String POSTAL_CODE_CLAIM_URI = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/postalcode";

  /** City address */
  String LOCALITY_CODE_CLAIM_URI = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/locality";

  /** Country address */
  String COUNTRY_CLAIM_URI = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/country";

  /** Username */
  String NAME_IDENTIFIER_CLAIM_URI =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier";

  String NAME_IDENTIFIER_CLAIM_ALTERNATE = "name";

  /** Full name */
  String NAME_CLAIM_URI = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name";

  /** First name */
  String GIVEN_NAME_CLAIM_URI = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname";

  /** Last name */
  String SURNAME_CLAIM_URI = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname";

  /** Roles */
  String ROLE_CLAIM_URI = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";

  /** Mobile phone */
  String MOBILE_PHONE_CLAIM_URI =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/mobilephone";

  String getName(org.apache.shiro.subject.Subject subject);

  String getName(Subject subject, String defaultName);

  String getName(Subject subject, String defaultName, boolean returnDisplayName);

  String getCommonName(X500Principal principal);

  String getEmailAddress(X500Principal principal);

  String getCountry(X500Principal principal);

  String filterDN(X500Principal principal, Predicate<RDN> predicate);

  String getEmailAddress(Subject subject);

  List<String> getAttribute(@Nullable Subject subject, String key);

  Map<String, SortedSet<String>> getSubjectAttributes(Subject subject);

  String getType(Subject subject);

  boolean isGuest(Subject subject);

  Comparator<SecurityAssertion> getAssertionComparator();
}
