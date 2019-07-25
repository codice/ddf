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
package ddf.security.assertion.saml.impl;

import ddf.security.SecurityConstants;
import ddf.security.assertion.Attribute;
import ddf.security.assertion.AttributeStatement;
import ddf.security.assertion.AuthenticationStatement;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.principal.GuestPrincipal;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.x500.X500Principal;
import javax.xml.bind.DatatypeConverter;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.joda.time.DateTime;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AttributeValue;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the SecurityAssertion interface. This class wraps a SecurityToken.
 *
 * @author tustisos
 */
public class SecurityAssertionSaml implements SecurityAssertion {

  /** Serial Version UID */
  private static final long serialVersionUID = 1L;

  /** Default Hash Value */
  private static final int DEFAULT_HASH = 127;

  /** Log4j Logger */
  private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConstants.SECURITY_LOGGER);

  public static final String SAML2_TOKEN_TYPE =
      "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0";

  /** Wrapped SecurityToken. */
  private SecurityToken securityToken;

  /** Principal associated with the security token */
  private Principal principal;

  private String name;

  private String nameIDFormat;

  /** Attributes associated with the username depending on the value of NameIDFormat */
  private ArrayList<String> usernameAttributeList;

  private String issuer;

  private transient List<AttributeStatement> attributeStatements;

  private transient List<AuthenticationStatement> authenticationStatements;

  private transient List<String> subjectConfirmations;

  private Date notBefore;

  private Date notOnOrAfter;

  private String tokenType;

  /** Uninitialized Constructor */
  public SecurityAssertionSaml() {
    init();
  }

  /**
   * Constructor without usernameAttributeList
   *
   * @param securityToken - token to wrap
   */
  public SecurityAssertionSaml(SecurityToken securityToken) {
    this(securityToken, new ArrayList<>());
  }

  /**
   * Default Constructor
   *
   * @param securityToken - token to wrap
   * @param usernameAttributeList - configurable list of attributes
   */
  public SecurityAssertionSaml(SecurityToken securityToken, List<String> usernameAttributeList) {
    init();
    this.securityToken = securityToken;
    if (usernameAttributeList == null) {
      this.usernameAttributeList = new ArrayList<>();
    } else {
      this.usernameAttributeList = new ArrayList<>(usernameAttributeList);
    }
    parseToken(securityToken);
    identifyNameIDFormat();
  }

  private void readObject(ObjectInputStream objectInputStream)
      throws IOException, ClassNotFoundException {
    objectInputStream.defaultReadObject();
    init();
  }

  private void init() {
    attributeStatements = new ArrayList<>();
    authenticationStatements = new ArrayList<>();
    usernameAttributeList = new ArrayList<>();
    subjectConfirmations = new ArrayList<>();
  }

  /**
   * Parses the SecurityToken by wrapping within an AssertionWrapper.
   *
   * @param securityToken SecurityToken
   */
  private void parseToken(SecurityToken securityToken) {
    XMLStreamReader xmlStreamReader = StaxUtils.createXMLStreamReader(securityToken.getToken());

    try {
      AttributeStatement attributeStatement = null;
      AuthenticationStatement authenticationStatement = null;
      Attribute attribute = null;
      int attrs = 0;
      while (xmlStreamReader.hasNext()) {
        int event = xmlStreamReader.next();
        switch (event) {
          case XMLStreamConstants.START_ELEMENT:
            {
              String localName = xmlStreamReader.getLocalName();
              switch (localName) {
                case NameID.DEFAULT_ELEMENT_LOCAL_NAME:
                  name = xmlStreamReader.getElementText();
                  for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++) {
                    if (xmlStreamReader
                        .getAttributeLocalName(i)
                        .equals(NameID.FORMAT_ATTRIB_NAME)) {
                      nameIDFormat = xmlStreamReader.getAttributeValue(i);
                      break;
                    }
                  }
                  break;
                case org.opensaml.saml.saml2.core.AttributeStatement.DEFAULT_ELEMENT_LOCAL_NAME:
                  attributeStatement = new AttributeStatementSaml();
                  attributeStatements.add(attributeStatement);
                  break;
                case AuthnStatement.DEFAULT_ELEMENT_LOCAL_NAME:
                  authenticationStatement = new AuthenticationStatementSaml();
                  authenticationStatements.add(authenticationStatement);
                  attrs = xmlStreamReader.getAttributeCount();
                  for (int i = 0; i < attrs; i++) {
                    String name = xmlStreamReader.getAttributeLocalName(i);
                    String value = xmlStreamReader.getAttributeValue(i);
                    if (AuthnStatement.AUTHN_INSTANT_ATTRIB_NAME.equals(name)) {
                      authenticationStatement.setAuthnInstant(DateTime.parse(value));
                    }
                    if (AuthnStatement.SESSION_INDEX_ATTRIB_NAME.equals(name)) {
                      authenticationStatement.setSessionIndex(value);
                    }
                  }
                  break;
                case AuthnContextClassRef.DEFAULT_ELEMENT_LOCAL_NAME:
                  if (authenticationStatement != null) {
                    String classValue = xmlStreamReader.getText();
                    classValue = classValue.trim();
                    authenticationStatement.setAuthnContextClassRef(classValue);
                  }
                  break;
                case org.opensaml.saml.saml2.core.Attribute.DEFAULT_ELEMENT_LOCAL_NAME:
                  attribute = new AttributeSaml();
                  if (attributeStatement != null) {
                    attributeStatement.addAttribute(attribute);
                  }
                  attrs = xmlStreamReader.getAttributeCount();
                  for (int i = 0; i < attrs; i++) {
                    String name = xmlStreamReader.getAttributeLocalName(i);
                    String value = xmlStreamReader.getAttributeValue(i);
                    if (org.opensaml.saml.saml2.core.Attribute.NAME_ATTTRIB_NAME.equals(name)) {
                      attribute.setName(value);
                    } else if (org.opensaml.saml.saml2.core.Attribute.NAME_FORMAT_ATTRIB_NAME
                        .equals(name)) {
                      attribute.setNameFormat(value);
                    }
                  }
                  break;
                case AttributeValue.DEFAULT_ELEMENT_LOCAL_NAME:
                  if (attribute != null) {
                    attribute.addValue(xmlStreamReader.getElementText());
                  }
                  break;
                case Issuer.DEFAULT_ELEMENT_LOCAL_NAME:
                  issuer = xmlStreamReader.getElementText();
                  break;
                case Conditions.DEFAULT_ELEMENT_LOCAL_NAME:
                  attrs = xmlStreamReader.getAttributeCount();
                  for (int i = 0; i < attrs; i++) {
                    String name = xmlStreamReader.getAttributeLocalName(i);
                    String value = xmlStreamReader.getAttributeValue(i);
                    if (Conditions.NOT_BEFORE_ATTRIB_NAME.equals(name)) {
                      notBefore = DatatypeConverter.parseDateTime(value).getTime();
                    } else if (Conditions.NOT_ON_OR_AFTER_ATTRIB_NAME.equals(name)) {
                      notOnOrAfter = DatatypeConverter.parseDateTime(value).getTime();
                    }
                  }
                  break;
                case SubjectConfirmation.DEFAULT_ELEMENT_LOCAL_NAME:
                  attrs = xmlStreamReader.getAttributeCount();
                  for (int i = 0; i < attrs; i++) {
                    String name = xmlStreamReader.getAttributeLocalName(i);
                    String value = xmlStreamReader.getAttributeValue(i);
                    if (SubjectConfirmation.METHOD_ATTRIB_NAME.equals(name)) {
                      subjectConfirmations.add(value);
                    }
                  }
                  break;
                case Assertion.DEFAULT_ELEMENT_LOCAL_NAME:
                  attrs = xmlStreamReader.getAttributeCount();
                  for (int i = 0; i < attrs; i++) {
                    String name = xmlStreamReader.getAttributeLocalName(i);
                    String value = xmlStreamReader.getAttributeValue(i);
                    if (Assertion.VERSION_ATTRIB_NAME.equals(name)) {
                      if ("2.0".equals(value)) {
                        tokenType =
                            "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0";
                      } else if ("1.1".equals(value)) {
                        tokenType =
                            "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV1.1";
                      }
                    }
                  }
              }
              break;
            }
          case XMLStreamConstants.END_ELEMENT:
            {
              String localName = xmlStreamReader.getLocalName();
              switch (localName) {
                case org.opensaml.saml.saml2.core.AttributeStatement.DEFAULT_ELEMENT_LOCAL_NAME:
                  attributeStatement = null;
                  break;
                case org.opensaml.saml.saml2.core.Attribute.DEFAULT_ELEMENT_LOCAL_NAME:
                  attribute = null;
                  break;
                default:
                  break;
              }
              break;
            }
        }
      }
    } catch (XMLStreamException e) {
      LOGGER.info("Unable to parse security token.", e);
    } finally {
      try {
        xmlStreamReader.close();
      } catch (XMLStreamException ignore) {
        // ignore
      }
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see ddf.security.assertion.SecurityAssertion#getPrincipal()
   */
  @Override
  public Principal getPrincipal() {
    if (securityToken != null) {
      if (principal == null || !principal.getName().equals(name)) {
        String authMethod = null;
        if (authenticationStatements != null) {
          for (AuthenticationStatement authnStatement : authenticationStatements) {
            authMethod = authnStatement.getAuthnContextClassRef();
          }
        }
        if (SAML2Constants.AUTH_CONTEXT_CLASS_REF_X509.equals(authMethod)
            || SAML2Constants.AUTH_CONTEXT_CLASS_REF_SMARTCARD_PKI.equals(authMethod)
            || SAML2Constants.AUTH_CONTEXT_CLASS_REF_SOFTWARE_PKI.equals(authMethod)
            || SAML2Constants.AUTH_CONTEXT_CLASS_REF_SPKI.equals(authMethod)
            || SAML2Constants.AUTH_CONTEXT_CLASS_REF_TLS_CLIENT.equals(authMethod)) {
          principal = new X500Principal(name);
        } else if (SAML2Constants.AUTH_CONTEXT_CLASS_REF_KERBEROS.equals(authMethod)) {
          principal = new KerberosPrincipal(name);
        } else if (principal instanceof GuestPrincipal
            || name.startsWith(GuestPrincipal.GUEST_NAME_PREFIX)) {
          principal = new GuestPrincipal(name);
        } else {
          principal = new AssertionPrincipal(name);
        }
      }
      return principal;
    }
    return null;
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
          for (final String obj : attr.getValues()) {
            principals.add(new RolePrincipal(obj));
          }
        }
      }
    }

    return principals;
  }

  /*
   * (non-Javadoc)
   *
   * @see ddf.security.assertion.SecurityAssertion#getIssuer()
   */
  @Override
  public String getIssuer() {
    return issuer;
  }

  /*
   * (non-Javadoc)
   *
   * @see ddf.security.assertion.SecurityAssertion#getAttributeStatements()
   */
  @Override
  public List<AttributeStatement> getAttributeStatements() {
    return Collections.unmodifiableList(attributeStatements);
  }

  @Override
  public List<ddf.security.assertion.AuthenticationStatement> getAuthnStatements() {
    return Collections.unmodifiableList(authenticationStatements);
  }

  @Override
  public List<String> getSubjectConfirmations() {
    return Collections.unmodifiableList(subjectConfirmations);
  }

  @Override
  public String getTokenType() {
    return tokenType;
  }

  /*
   * (non-Javadoc)
   *
   * @see ddf.security.assertion.SecurityAssertion#getToken()
   */
  @Override
  public Object getToken() {
    return securityToken;
  }

  /**
   * Checks if the NameIDFormat is of the following formats below, if not, the name is changed to
   * the value of the first matching usernameAttribute.
   */
  private void identifyNameIDFormat() {
    if (!((StringUtils.containsIgnoreCase(nameIDFormat, SAML2Constants.NAMEID_FORMAT_PERSISTENT)
            || StringUtils.containsIgnoreCase(
                nameIDFormat, SAML2Constants.NAMEID_FORMAT_X509_SUBJECT_NAME)
            || StringUtils.containsIgnoreCase(nameIDFormat, SAML2Constants.NAMEID_FORMAT_KERBEROS)
            || StringUtils.containsIgnoreCase(
                nameIDFormat, SAML2Constants.NAMEID_FORMAT_UNSPECIFIED))
        && !name.equals(""))) {
      for (AttributeStatement attributeStatementList : getAttributeStatements()) {
        List<Attribute> attributeList = attributeStatementList.getAttributes();
        for (Attribute attribute : attributeList) {
          if (listContainsIgnoreCase(usernameAttributeList, attribute.getName())) {
            name = attribute.getValues().get(0);
            return;
          }
        }
      }
    }
  }

  private boolean listContainsIgnoreCase(List<String> list, String string) {
    for (String next : list) {
      if (next.equalsIgnoreCase(string)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Date getNotBefore() {
    return notBefore == null ? null : Date.from(notBefore.toInstant());
  }

  @Override
  public Date getNotOnOrAfter() {
    return notOnOrAfter == null ? null : Date.from(notOnOrAfter.toInstant());
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    result.append("Principal: ");
    result.append(getPrincipal());
    result.append(", Attributes: ");
    for (AttributeStatement attributeStatement : getAttributeStatements()) {
      for (Attribute attr : attributeStatement.getAttributes()) {
        result.append("[ ");
        result.append(attr.getName());
        result.append(" : ");
        for (int i = 0; i < attr.getValues().size(); i++) {
          result.append(attr.getValues().get(i));
        }
        result.append("] ");
      }
    }
    // add this back in when we support parsing this information
    result.append(", AuthnStatements: ");
    for (AuthenticationStatement authStatement : getAuthnStatements()) {
      result.append("[ ");
      result.append(authStatement.getAuthnInstant());
      result.append(" : ");
      result.append(authStatement.getAuthnContextClassRef());
      result.append("] ");
    }
    return result.toString();
  }

  @Override
  public int getWeight() {
    return SecurityAssertion.IDP_AUTH_WEIGHT;
  }

  @Override
  public boolean isPresentlyValid() {
    Date now = new Date();

    if (getNotBefore() != null && now.before(getNotBefore())) {
      LOGGER.debug("SAML Assertion Time Bound Check Failed.");
      LOGGER.debug("\t Checked time of {} is before the NotBefore time of {}", now, getNotBefore());
      return false;
    }

    if (getNotOnOrAfter() != null
        && (now.equals(getNotOnOrAfter()) || now.after(getNotOnOrAfter()))) {
      LOGGER.debug("SAML Assertion Time Bound Check Failed.");
      LOGGER.debug(
          "\t Checked time of {} is equal to or after the NotOnOrAfter time of {}",
          now,
          getNotOnOrAfter());
      return false;
    }

    return true;
  }

  /** Principal implementation that returns values obtained from the assertion. */
  private static class AssertionPrincipal implements Principal, Serializable {
    private String name;

    public AssertionPrincipal(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public boolean equals(Object another) {
      if (!(another instanceof Principal)) {
        return false;
      }
      Principal tmpPrin = (Principal) another;
      if (tmpPrin.getName() == null && getName() != null) {
        return false;
      }
      if (tmpPrin.getName() != null && getName() == null) {
        return false;
      }
      if (tmpPrin.getName() == null && getName() == null) {
        return super.equals(another);
      }
      return tmpPrin.getName().equals(getName());
    }

    @Override
    public int hashCode() {
      if (getName() == null) {
        return DEFAULT_HASH;
      }
      return getName().hashCode();
    }

    /** Returns the name of the principal in string format. */
    public String toString() {
      return getName();
    }
  }
}
