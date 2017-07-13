/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.servlet.whoami;

import static org.apache.commons.lang.Validate.notNull;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.apache.shiro.subject.Subject;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnStatement;

import ddf.security.SubjectUtils;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.principal.GuestPrincipal;

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

    private final String name;

    private final String displayName;

    private final String email;

    private final Date notOnOrAfter;

    private final Date notBefore;

    private final String expiresIn;

    private final String issuer;

    private final boolean isGuest;

    private final List<String> authnContextClasses;

    private final Map<String, SortedSet<String>> claims;

    public WhoAmI(Subject subject) {
        notNull(subject);

        SecurityAssertion assertion = extractSecurityAssertion(subject);
        notNull(assertion);

        name = SubjectUtils.getName(subject);
        displayName = SubjectUtils.getName(subject, null, true);
        email = SubjectUtils.getEmailAddress(subject);
        claims = Collections.unmodifiableMap(SubjectUtils.getSubjectAttributes(subject));

        issuer = assertion.getIssuer();
        isGuest = assertion.getPrincipal() instanceof GuestPrincipal;

        notOnOrAfter = assertion.getNotOnOrAfter();
        notBefore = assertion.getNotBefore();
        expiresIn = durationUntilNow(notOnOrAfter);

        authnContextClasses = extractAuthnContextClasses(assertion.getAuthnStatements());
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

    public String getIssuer() {
        return issuer;
    }

    public Date getNotOnOrAfter() {
        return new Date(notOnOrAfter.getTime());
    }

    public Date getNotBefore() {
        return new Date(notBefore.getTime());
    }

    public String getExpiresIn() {
        return expiresIn;
    }

    public List<String> getAuthnContextClasses() {
        return authnContextClasses;
    }

    public boolean isGuest() {
        return isGuest;
    }

    public Map<String, SortedSet<String>> getClaims() {
        return claims;
    }

    private SecurityAssertion extractSecurityAssertion(Subject subject) {
        if ((null == subject) || (null == subject.getPrincipals()) || (null
                == subject.getPrincipals()
                .oneByType(SecurityAssertion.class))) {
            return null;
        }

        return subject.getPrincipals()
                .oneByType(SecurityAssertion.class);
    }

    private String durationUntilNow(Date expiration) {
        if (expiration == null) {
            return null;
        }

        return TIME_FORMATTER.print(new Period(expiration.getTime() - DateTime.now()
                .getMillis()).normalizedStandard());
    }

    private List<String> extractAuthnContextClasses(List<AuthnStatement> authnStatements) {
        return Collections.unmodifiableList(authnStatements.stream()
                .filter(Objects::nonNull)
                .map(AuthnStatement::getAuthnContext)
                .filter(Objects::nonNull)
                .map(AuthnContext::getAuthnContextClassRef)
                .filter(Objects::nonNull)
                .map(AuthnContextClassRef::getAuthnContextClassRef)
                .collect(Collectors.toList()));
    }

}
