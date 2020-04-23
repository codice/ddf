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
package ddf.security.impl;

import ddf.security.SubjectIdentity;
import ddf.security.SubjectUtils;
import ddf.security.principal.GuestPrincipal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.codice.ddf.security.claims.guest.GuestClaimsConfig;

public class SubjectIdentityImpl implements SubjectIdentity {

  private String identityAttribute;
  private GuestClaimsConfig guestClaimsConfig;

  /**
   * Get a subject's unique identifier. 1. If the configured unique identifier if present 2. Email
   * address if present 3. Username not, user name is returned.
   *
   * @param subject
   * @return subject unique identifier
   */
  @Override
  public String getUniqueIdentifier(Subject subject) {
    SortedSet<String> subjectAttribute = getSubjectAttribute(subject);
    Map<String, List<String>> guestClaims = getGuestClaims();

    // If the user is not guest, the guest claims should NOT be used to identify the user.
    // Remove any values granted by the guest claims handler from the identity attribute before
    // selecting the identifier.
    if (subject != null
        && !isGuest(subject)
        && guestClaims != null
        && guestClaims.containsKey(identityAttribute)) {
      subjectAttribute.removeAll(guestClaims.get(identityAttribute));
    }

    if (!subjectAttribute.isEmpty()) {
      return subjectAttribute.first();
    }

    String identifier = SubjectUtils.getEmailAddress(subject);
    if (StringUtils.isNotBlank(identifier)) {
      return identifier;
    }

    return SubjectUtils.getName(subject);
  }

  private SortedSet<String> getSubjectAttribute(Subject subject) {
    Map<String, SortedSet<String>> attrs = SubjectUtils.getSubjectAttributes(subject);

    if (attrs.containsKey(identityAttribute)) {
      return attrs.get(identityAttribute);
    }

    return Collections.emptySortedSet();
  }

  public String getIdentityAttribute() {
    return identityAttribute;
  }

  public void setIdentityAttribute(String identityAttribute) {
    this.identityAttribute = identityAttribute;
  }

  private boolean isGuest(Subject subject) {
    PrincipalCollection collection = subject.getPrincipals();
    for (Object principal : collection.asList()) {
      if (principal instanceof GuestPrincipal
          || principal.toString().startsWith(GuestPrincipal.GUEST_NAME_PREFIX)) {
        return true;
      }
    }
    return false;
  }

  public Map<String, List<String>> getGuestClaims() {
    return this.guestClaimsConfig
        .getClaimsMap()
        .entrySet()
        .stream()
        .collect(Collectors.toMap(e -> e.getKey().toString(), Entry::getValue));
  }

  public void setGuestClaimsConfig(GuestClaimsConfig guestClaimsConfig) {
    this.guestClaimsConfig = guestClaimsConfig;
  }
}
