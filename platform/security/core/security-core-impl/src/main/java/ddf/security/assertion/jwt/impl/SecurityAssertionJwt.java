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
package ddf.security.assertion.jwt.impl;

import static org.pac4j.core.profile.jwt.JwtClaims.EXPIRATION_TIME;
import static org.pac4j.core.profile.jwt.JwtClaims.ISSUER;
import static org.pac4j.core.profile.jwt.JwtClaims.NOT_BEFORE;

import ddf.security.assertion.Attribute;
import ddf.security.assertion.AttributeStatement;
import ddf.security.assertion.AuthenticationStatement;
import ddf.security.assertion.SecurityAssertion;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.pac4j.oidc.profile.OidcProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityAssertionJwt implements SecurityAssertion {

  private static final Logger LOGGER = LoggerFactory.getLogger(SecurityAssertionJwt.class);

  public static final String JWT_TOKEN_TYPE = "jwt";

  private final List<String> usernameAttributeList;

  private final OidcProfile profile;

  private Map<String, Object> attributes;

  private List<AttributeStatement> attributeStatements;

  private List<AuthenticationStatement> authenticationStatements = new ArrayList<>();

  public SecurityAssertionJwt(OidcProfile profile, List<String> usernameAttributeList) {
    this.profile = profile;
    this.usernameAttributeList = usernameAttributeList;

    attributes = profile.getAttributes();
    attributeStatements = new ArrayList<>();
    AttributeStatement attributeStatement = new AttributeStatementJwt();
    attributeStatements.add(attributeStatement);

    for (Map.Entry<String, Object> entry : attributes.entrySet()) {
      Attribute attribute = new AttributeJwt();
      attribute.setName(entry.getKey());
      List<String> values = new ArrayList<>();
      if (entry.getValue() instanceof Collection) {
        Collection collection = (Collection) entry.getValue();
        for (Object next : collection) {
          values.add(String.valueOf(next));
        }
      } else {
        values.add(String.valueOf(entry.getValue()));
      }
      attribute.setValues(values);
      attributeStatement.addAttribute(attribute);
    }
  }

  @Override
  public Principal getPrincipal() {
    return () -> {
      try {
        return getMainPrincipalAsString();
      } catch (NoSuchElementException e) {
        return "unknown";
      }
    };
  }

  @Override
  public String getIssuer() {
    return (String) attributes.get(ISSUER);
  }

  @Override
  public List<AttributeStatement> getAttributeStatements() {
    return Collections.unmodifiableList(attributeStatements);
  }

  @Override
  public List<AuthenticationStatement> getAuthnStatements() {
    return Collections.unmodifiableList(authenticationStatements);
  }

  @Override
  public List<String> getSubjectConfirmations() {
    return new ArrayList<>();
  }

  @Override
  public Set<Principal> getPrincipals() {
    Set<Principal> principals = new HashSet<>();
    Principal primary = getPrincipal();
    principals.add(primary);
    principals.add(new RolePrincipal(primary.getName()));
    for (AttributeStatement attributeStatement : getAttributeStatements()) {
      for (Attribute attr : attributeStatement.getAttributes()) {
        if (StringUtils.containsIgnoreCase(attr.getName(), "role")) {
          for (final String attrValue : attr.getValues()) {
            principals.add(new RolePrincipal(attrValue));
          }
        }
      }
    }

    return principals;
  }

  @Override
  public String getTokenType() {
    return JWT_TOKEN_TYPE;
  }

  @Override
  public Object getToken() {
    return profile;
  }

  @Override
  public Date getNotBefore() {
    Date notBefore = (Date) attributes.get(NOT_BEFORE);
    return notBefore == null ? null : Date.from(notBefore.toInstant());
  }

  @Override
  public Date getNotOnOrAfter() {
    Date expiration = (Date) attributes.get(EXPIRATION_TIME);
    return expiration == null ? null : Date.from(expiration.toInstant());
  }

  @Override
  public int getWeight() {
    return SecurityAssertion.IDP_AUTH_WEIGHT;
  }

  @Override
  public boolean isPresentlyValid() {
    Date now = new Date();

    if (getNotBefore() != null && now.before(getNotBefore())) {
      LOGGER.debug("Security Assertion Time Bound Check Failed.");
      LOGGER.debug("\t Checked time of {} is before the NotBefore time of {}", now, getNotBefore());
      return false;
    }

    if (getNotOnOrAfter() != null
        && (now.equals(getNotOnOrAfter()) || now.after(getNotOnOrAfter()))) {
      LOGGER.debug("Security Assertion Time Bound Check Failed.");
      LOGGER.debug(
          "\t Checked time of {} is equal to or after the NotOnOrAfter time of {}",
          now,
          getNotOnOrAfter());
      return false;
    }

    return true;
  }

  private String getMainPrincipalAsString() throws NoSuchElementException {
    for (String claim : usernameAttributeList) {
      try {
        return (String) attributes.get(claim);
      } catch (NoSuchElementException e) {
        LOGGER.debug(
            "Could not find username claim [%s] in jwt claims [%s]",
            claim, String.join(",", attributes.keySet()), e);
      }
    }

    throw new NoSuchElementException(
        String.format(
            "Cannot find any username claims [%s] in jwt claims [%s]",
            String.join(",", usernameAttributeList), String.join(",", attributes.keySet())));
  }

  @Override
  public String toString() {
    return getPrincipal().getName();
  }
}
