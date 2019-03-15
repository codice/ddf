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
package org.codice.ddf.security.servlet.whoami;

import static org.apache.commons.lang.Validate.notEmpty;
import static org.apache.commons.lang.Validate.notNull;

import ddf.security.SubjectUtils;
import ddf.security.assertion.Attribute;
import ddf.security.assertion.AttributeStatement;
import ddf.security.assertion.AuthenticationStatement;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.principal.GuestPrincipal;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.shiro.subject.Subject;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

public class WhoAmI {

  private static final PeriodFormatter TIME_FORMATTER =
      new PeriodFormatterBuilder()
          .appendDays()
          .appendSuffix(" day", " days")
          .appendSeparator(" ")
          .appendHours()
          .appendSuffix(" hour", " hours")
          .appendSeparator(" ")
          .appendMinutes()
          .appendSuffix(" minute", " minutes")
          .appendSeparator(" ")
          .appendSeconds()
          .appendSuffix(" second", " seconds")
          .toFormatter();

  final List<WhoAmISubject> whoAmISubjects;

  public WhoAmI(final Subject subject) {
    notNull(subject);

    Collection<SecurityAssertion> assertions = extractSecurityAssertions(subject);
    notEmpty(assertions);

    whoAmISubjects =
        assertions.stream().map(a -> getWhoAmI(a, subject)).collect(Collectors.toList());
  }

  private Collection<SecurityAssertion> extractSecurityAssertions(Subject subject) {
    if ((null == subject)
        || (null == subject.getPrincipals())
        || subject.getPrincipals().byType(SecurityAssertion.class).isEmpty()) {
      return Collections.emptyList();
    }

    return subject.getPrincipals().byType(SecurityAssertion.class);
  }

  private WhoAmISubject getWhoAmI(SecurityAssertion assertion, Subject subject) {
    return new WhoAmISubject(assertion, subject);
  }

  public static class WhoAmISubject {
    private final String name;

    private final String principalName;

    private final String displayName;

    private final String email;

    private final Date notOnOrAfter;

    private final Date notBefore;

    private final String expiresIn;

    private final String issuer;

    private final String tokenType;

    private final boolean isGuest;

    private final List<String> authnContextClasses;

    private final Map<String, SortedSet<String>> claims;

    public WhoAmISubject(SecurityAssertion assertion, Subject subject) {
      name = SubjectUtils.getName(subject);
      principalName = assertion.getPrincipal().getName();
      displayName = SubjectUtils.getName(subject, null, true);
      email = SubjectUtils.getEmailAddress(subject);
      claims = Collections.unmodifiableMap(getAttributes(assertion));

      issuer = assertion.getIssuer();
      tokenType = assertion.getTokenType();
      isGuest = assertion.getPrincipal() instanceof GuestPrincipal;

      notOnOrAfter = assertion.getNotOnOrAfter();
      notBefore = assertion.getNotBefore();
      expiresIn = durationUntilNow(notOnOrAfter);

      authnContextClasses = extractAuthnContextClasses(assertion.getAuthnStatements());
    }

    private Map<String, SortedSet<String>> getAttributes(SecurityAssertion assertion) {
      return Stream.of(assertion)
          .map(SecurityAssertion::getAttributeStatements)
          .flatMap(Collection::stream)
          .map(AttributeStatement::getAttributes)
          .flatMap(Collection::stream)
          .collect(
              Collectors.toMap(
                  Attribute::getName,
                  WhoAmISubject::getAttributeValues,
                  (acc, val) -> {
                    acc.addAll(val);
                    return acc;
                  }));
    }

    private static SortedSet<String> getAttributeValues(Attribute attribute) {
      return new TreeSet<>(attribute.getValues());
    }

    private String durationUntilNow(Date expiration) {
      if (expiration == null) {
        return null;
      }

      return TIME_FORMATTER.print(
          new Period(expiration.getTime() - DateTime.now().getMillis()).normalizedStandard());
    }

    private List<String> extractAuthnContextClasses(List<AuthenticationStatement> authnStatements) {
      return Collections.unmodifiableList(
          authnStatements
              .stream()
              .filter(Objects::nonNull)
              .map(AuthenticationStatement::getAuthnContextClassRef)
              .collect(Collectors.toList()));
    }

    public String getName() {
      return name;
    }

    public String getDisplayName() {
      return displayName;
    }

    public String getEmail() {
      return email;
    }

    public Date getNotOnOrAfter() {
      return notOnOrAfter;
    }

    public Date getNotBefore() {
      return notBefore;
    }

    public String getExpiresIn() {
      return expiresIn;
    }

    public String getIssuer() {
      return issuer;
    }

    public boolean isGuest() {
      return isGuest;
    }

    public List<String> getAuthnContextClasses() {
      return authnContextClasses;
    }

    public Map<String, SortedSet<String>> getClaims() {
      return claims;
    }
  }
}
