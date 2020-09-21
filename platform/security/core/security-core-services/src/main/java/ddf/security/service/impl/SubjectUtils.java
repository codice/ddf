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

import ddf.security.SubjectOperations;
import ddf.security.assertion.Attribute;
import ddf.security.assertion.AttributeStatement;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.principal.impl.GuestPrincipal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.x500.X500Principal;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility class used to perform operations on Subjects. */
public final class SubjectUtils implements SubjectOperations {

  private static final Logger LOGGER = LoggerFactory.getLogger(SubjectUtils.class);

  /**
   * Converts the given principal name to a formatted display name.
   *
   * @param principal
   * @param defaultName
   * @return
   */
  private String getDisplayName(Principal principal, String defaultName) {

    String displayName = defaultName;

    if (principal instanceof GuestPrincipal) {
      displayName = GUEST_DISPLAY_NAME;
    } else if (principal instanceof X500Principal) {
      displayName = getCommonName((X500Principal) principal);
    } else {
      LOGGER.debug(
          "No display name format identified for given principal. Returning principal name ",
          defaultName);
    }

    return displayName;
  }

  /**
   * Retrieves the user name from a given subject.
   *
   * @param subject Subject to get the user name from.
   * @return String representation of the user name if available or null if no user name could be
   *     found.
   */
  @Override
  public String getName(org.apache.shiro.subject.Subject subject) {
    return getName(subject, null, false);
  }

  /**
   * Retrieves the user name from a given subject.
   *
   * @param subject Subject to get the user name from.
   * @return String representation of the user name if available or null if no user name could be
   *     found.
   */
  @Override
  public String getName(Subject subject, String defaultName) {
    return getName(subject, defaultName, false);
  }

  /**
   * Retrieves the user name from a given subject.
   *
   * @param subject Subject to get the user name from.
   * @param defaultName Name to send back if no user name was found.
   * @param returnDisplayName return formatted user name for displaying
   * @return String representation of the user name if available or defaultName if no user name
   *     could be found or incoming subject was null.
   */
  @Override
  public String getName(Subject subject, String defaultName, boolean returnDisplayName) {
    String name = defaultName;
    if (subject != null) {
      PrincipalCollection principals = subject.getPrincipals();
      if (principals != null) {
        Collection<SecurityAssertion> assertions = principals.byType(SecurityAssertion.class);
        if (!assertions.isEmpty()) {
          List<SecurityAssertion> assertionList = new ArrayList<>(assertions);
          assertionList.sort(new SecurityAssertionComparator());
          for (SecurityAssertion assertion : assertionList) {
            Principal principal = assertion.getPrincipal();
            if (principal instanceof KerberosPrincipal) {
              StringTokenizer st = new StringTokenizer(principal.getName(), "@");
              st = new StringTokenizer(st.nextToken(), "/");
              name = st.nextToken();
            } else {
              name = principal.getName();
            }

            if (returnDisplayName) {
              name = getDisplayName(principal, name);
            }

            if (StringUtils.isNotEmpty(name)) {
              break;
            }
          }
        } else {
          // send back the primary principal as a string
          name = principals.getPrimaryPrincipal().toString();
        }
      } else {
        LOGGER.debug(
            "No principals located in the incoming subject, cannot look up user name. Using default name of {}.",
            defaultName);
      }
    } else {
      LOGGER.debug(
          "Incoming subject was null, cannot look up user name. Using default name of {}.",
          defaultName);
    }

    LOGGER.debug("Sending back name {}.", name);
    return name;
  }

  private String getExtendedCertAttribute(
      X500Principal principal, ASN1ObjectIdentifier identifier) {
    RDN[] rdNs = new X500Name(principal.getName()).getRDNs(identifier);
    if (rdNs != null && rdNs.length > 0) {
      AttributeTypeAndValue attributeTypeAndValue = rdNs[0].getFirst();
      if (attributeTypeAndValue != null) {
        return attributeTypeAndValue.getValue().toString();
      }
    }
    return null;
  }

  @Override
  public String getCommonName(X500Principal principal) {
    return getExtendedCertAttribute(principal, BCStyle.CN);
  }

  @Override
  public String getEmailAddress(X500Principal principal) {
    return getExtendedCertAttribute(principal, BCStyle.EmailAddress);
  }

  @Override
  public String getCountry(X500Principal principal) {
    return getExtendedCertAttribute(principal, BCStyle.C);
  }

  @Override
  public String filterDN(X500Principal principal, Predicate<RDN> predicate) {
    RDN[] rdns =
        Arrays.stream(new X500Name(principal.getName()).getRDNs())
            .filter(predicate)
            .toArray(RDN[]::new);

    return new X500Name(rdns).toString();
  }

  /**
   * Get a subject's email.
   *
   * @param subject
   * @return email or null if not found.
   */
  @Override
  @Nullable
  public String getEmailAddress(Subject subject) {
    List<String> values = getAttribute(subject, EMAIL_ADDRESS_CLAIM_URI);

    if (values.isEmpty()) {
      values = getAttribute(subject, EMAIL_ADDRESS_CLAIM_ALTERNATE);
      if (values.isEmpty()) {
        return null;
      }
    }

    return values.get(0);
  }

  /**
   * Get any attribute from a subject by key.
   *
   * @param subject
   * @param key
   * @return attribute values or an empty list if not found.
   */
  @Override
  public List<String> getAttribute(@Nullable Subject subject, String key) {
    Validate.notNull(key);

    if (subject == null) {
      LOGGER.debug("Incoming subject was null, cannot look up {}.", key);
      return Collections.emptyList();
    }

    PrincipalCollection principals = subject.getPrincipals();
    if (principals == null) {
      LOGGER.debug("No principals located in the incoming subject, cannot look up {}.", key);
      return Collections.emptyList();
    }

    Collection<SecurityAssertion> assertions = principals.byType(SecurityAssertion.class);
    if (assertions.isEmpty()) {
      LOGGER.debug("Could not find Security Assertion, cannot look up {}.", key);
      return Collections.emptyList();
    }

    List<SecurityAssertion> assertionList = new ArrayList<>(assertions);
    assertionList.sort(new SecurityAssertionComparator());

    return assertionList.stream()
        .map(SecurityAssertion::getAttributeStatements)
        .flatMap(List::stream)
        .flatMap(as -> as.getAttributes().stream())
        .filter(a -> a.getName().equals(key))
        .flatMap(a -> a.getValues().stream())
        .collect(Collectors.toList());
  }

  /**
   * Retrieves the security attributes for the given subject.
   *
   * @param subject the Subject to check
   * @return Map of attributes with the collected values for each
   */
  @Override
  public Map<String, SortedSet<String>> getSubjectAttributes(Subject subject) {
    if (subject == null) {
      return Collections.emptyMap();
    }

    return subject.getPrincipals().byType(SecurityAssertion.class).stream()
        .map(SecurityAssertion::getAttributeStatements)
        .flatMap(Collection::stream)
        .map(AttributeStatement::getAttributes)
        .flatMap(Collection::stream)
        .filter(SubjectUtils::attributeValuesAreNonNull)
        .collect(
            Collectors.toMap(
                Attribute::getName,
                SubjectUtils::getAttributeValues,
                (acc, val) -> {
                  acc.addAll(val);
                  return acc;
                }));
  }

  /**
   * Retrieves the type of the Security Assertion inside the given Subject.
   *
   * @param subject Subject to get the user name from.
   * @return String representation of the user name if available or defaultName if no user name
   *     could be found or incoming subject was null.
   */
  @Override
  public String getType(Subject subject) {
    if (subject == null) {
      LOGGER.debug("Incoming subject was null, cannot look up security assertion type.");
      return null;
    }

    PrincipalCollection principals = subject.getPrincipals();
    if (principals == null) {
      LOGGER.debug(
          "No principals located in the incoming subject, cannot look up security assertion type.");
      return null;
    }

    Collection<SecurityAssertion> assertions = principals.byType(SecurityAssertion.class);
    if (assertions == null || assertions.isEmpty()) {
      LOGGER.debug(
          "No principals located in the incoming subject, cannot look up security assertion type.");
      return null;
    }

    List<SecurityAssertion> assertionList = new ArrayList<>(assertions);
    assertionList.sort(new SecurityAssertionComparator());

    return assertionList.get(0).getTokenType();
  }

  @Override
  public boolean isGuest(Subject subject) {
    Collection<SecurityAssertion> securityAssertions =
        subject.getPrincipals().byType(SecurityAssertion.class);
    List<SecurityAssertion> assertionList = new ArrayList<>(securityAssertions);
    assertionList.sort(getAssertionComparator());
    return assertionList.get(0).getWeight() == SecurityAssertion.NO_AUTH_WEIGHT;
  }

  private static SortedSet<String> getAttributeValues(Attribute attribute) {
    return new TreeSet<>(attribute.getValues());
  }

  @Override
  public Comparator<SecurityAssertion> getAssertionComparator() {
    return new SecurityAssertionComparator();
  }

  private static class SecurityAssertionComparator implements Comparator<SecurityAssertion> {

    @Override
    public int compare(SecurityAssertion assertion1, SecurityAssertion assertion2) {
      return Integer.compare(assertion1.getWeight(), assertion2.getWeight());
    }
  }

  private static boolean attributeValuesAreNonNull(Attribute attribute) {
    return attribute.getValues().stream().noneMatch(Objects::isNull);
  }
}
