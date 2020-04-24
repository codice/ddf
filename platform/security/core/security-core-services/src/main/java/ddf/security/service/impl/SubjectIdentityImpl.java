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
package ddf.security.service.impl;

import ddf.security.SubjectIdentity;
import ddf.security.SubjectOperations;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.subject.Subject;

public class SubjectIdentityImpl implements SubjectIdentity {

  private final SubjectOperations subjectOperations;

  private String identityAttribute;

  public SubjectIdentityImpl(SubjectOperations subjectOperations) {
    this.subjectOperations = subjectOperations;
  }

  /**
   * Get a subject's unique identifier. 1. If the configured unique identifier if present 2. Email
   * address if present 3. Username not, user name is returned.
   *
   * @param subject
   * @return subject unique identifier
   */
  @Override
  public String getUniqueIdentifier(Subject subject) {
    Optional<String> owner = getSubjectAttribute(subject).stream().findFirst();
    if (owner.isPresent()) {
      return owner.get();
    }

    String identifier = subjectOperations.getEmailAddress(subject);
    if (StringUtils.isNotBlank(identifier)) {
      return identifier;
    }

    return subjectOperations.getName(subject);
  }

  private SortedSet<String> getSubjectAttribute(Subject subject) {
    Map<String, SortedSet<String>> attrs = subjectOperations.getSubjectAttributes(subject);

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
}
