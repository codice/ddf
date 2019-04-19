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
import ddf.security.principal.GuestPrincipal;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.x500.X500Principal;
import org.apache.commons.lang.Validate;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility class used to perform operations on Subjects. */
public final class SubjectUtils {

  public static final String GUEST_DISPLAY_NAME = "Guest";

  private static final Logger LOGGER = LoggerFactory.getLogger(SubjectUtils.class);

  public static final String EMAIL_ADDRESS_CLAIM_URI =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress";

  /** Street address */
  public static final String STREET_ADDRESS_CLAIM_URI =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/streetaddress";

  /** Postal address */
  public static final String POSTAL_CODE_CLAIM_URI =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/postalcode";

  /** City address */
  public static final String LOCALITY_CODE_CLAIM_URI =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/locality";

  /** Country address */
  public static final String COUNTRY_CLAIM_URI =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/country";

  /** Username */
  public static final String NAME_IDENTIFIER_CLAIM_URI =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier";

  /** Full name */
  public static final String NAME_CLAIM_URI =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name";

  /** First name */
  public static final String GIVEN_NAME_CLAIM_URI =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname";

  /** Last name */
  public static final String SURNAME_CLAIM_URI =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname";

  /** Roles */
  public static final String ROLE_CLAIM_URI =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";

  /** Mobile phone */
  public static final String MOBILE_PHONE_CLAIM_URI =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/mobilephone";

  private SubjectUtils() {}

  /**
   * Converts the given principal name to a formatted display name.
   *
   * @param principal
   * @param defaultName
   * @return
   */
  private static String getDisplayName(Principal principal, String defaultName) {

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
  public static String getName(org.apache.shiro.subject.Subject subject) {
    return getName(subject, null, false);
  }

  /**
   * Retrieves the user name from a given subject.
   *
   * @param subject Subject to get the user name from.
   * @return String representation of the user name if available or null if no user name could be
   *     found.
   */
  public static String getName(Subject subject, String defaultName) {
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
  public static String getName(Subject subject, String defaultName, boolean returnDisplayName) {
    String name = defaultName;
    if (subject != null) {
      PrincipalCollection principals = subject.getPrincipals();
      if (principals != null) {
        SecurityAssertion assertion = principals.oneByType(SecurityAssertion.class);
        if (assertion != null) {
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

  private static String getExtendedCertAttribute(
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

  public static String getCommonName(X500Principal principal) {
    return getExtendedCertAttribute(principal, BCStyle.CN);
  }

  public static String getEmailAddress(X500Principal principal) {
    return getExtendedCertAttribute(principal, BCStyle.EmailAddress);
  }

  public static String getCountry(X500Principal principal) {
    return getExtendedCertAttribute(principal, BCStyle.C);
  }

  public static String filterDN(X500Principal principal, Predicate<RDN> predicate) {
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
  @Nullable
  public static String getEmailAddress(Subject subject) {
    List<String> values = getAttribute(subject, EMAIL_ADDRESS_CLAIM_URI);

    if (values.isEmpty()) {
      return null;
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
  public static List<String> getAttribute(@Nullable Subject subject, String key) {
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

    SecurityAssertion assertion = principals.oneByType(SecurityAssertion.class);
    if (assertion == null) {
      LOGGER.debug("Could not find Security Assertion, cannot look up {}.", key);
      return Collections.emptyList();
    }

    return assertion
        .getAttributeStatements()
        .stream()
        .flatMap(as -> as.getAttributes().stream())
        .filter(a -> a.getName().equals(key))
        .flatMap(a -> a.getAttributeValues().stream())
        .filter(o -> o instanceof XSString)
        .map(o -> (XSString) o)
        .map(XSString::getValue)
        .collect(Collectors.toList());
  }

  /**
   * Retrieves the security attributes for the given subject.
   *
   * @param subject the Subject to check
   * @return Map of attributes with the collected values for each
   */
  public static Map<String, SortedSet<String>> getSubjectAttributes(Subject subject) {
    if (subject == null) {
      return Collections.emptyMap();
    }

    return subject
        .getPrincipals()
        .byType(SecurityAssertion.class)
        .stream()
        .map(SecurityAssertion::getAttributeStatements)
        .flatMap(Collection::stream)
        .map(AttributeStatement::getAttributes)
        .flatMap(Collection::stream)
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
  public static String getType(Subject subject) {
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

    SecurityAssertion assertion = principals.oneByType(SecurityAssertion.class);
    if (assertion == null) {
      LOGGER.debug(
          "No principals located in the incoming subject, cannot look up security assertion type.");
      return null;
    }

    return assertion.getTokenType();
  }

  private static SortedSet<String> getAttributeValues(
      org.opensaml.saml.saml2.core.Attribute attribute) {
    return attribute
        .getAttributeValues()
        .stream()
        .filter(XSString.class::isInstance)
        .map(XSString.class::cast)
        .map(XSString::getValue)
        .collect(Collectors.toCollection(TreeSet::new));
  }
}
