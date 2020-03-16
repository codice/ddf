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
package ddf.security.realm.sts;

import ddf.security.assertion.SecurityAssertion;
import ddf.security.assertion.saml.impl.SecurityAssertionSaml;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.codice.ddf.platform.filter.AuthenticationFailureException;
import org.codice.ddf.security.handler.BaseAuthenticationToken;
import org.codice.ddf.security.handler.SAMLAuthenticationToken;
import org.codice.ddf.security.saml.assertion.validator.SamlAssertionValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class SamlRealm extends AuthenticatingRealm {
  private static final Logger LOGGER = (LoggerFactory.getLogger(SamlRealm.class));

  private SamlAssertionValidator samlAssertionValidator;

  private List<String> usernameAttributeList;

  public SamlRealm() {
    setCredentialsMatcher(new STSCredentialsMatcher());
  }

  /** Determine if the supplied token is supported by this realm. */
  @Override
  public boolean supports(AuthenticationToken token) {
    boolean supported =
        token != null && token.getCredentials() != null && token instanceof SAMLAuthenticationToken;

    if (supported) {
      LOGGER.debug("Token {} is supported by {}.", token.getClass(), SamlRealm.class.getName());
    } else if (token != null) {
      LOGGER.debug("Token {} is not supported by {}.", token.getClass(), SamlRealm.class.getName());
    } else {
      LOGGER.debug("The supplied authentication token is null. Sending back not supported.");
    }

    return supported;
  }

  /** Perform authentication based on the supplied token. */
  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) {
    Object credential = null;

    // perform validation
    if (token instanceof SAMLAuthenticationToken) {
      try {
        samlAssertionValidator.validate((SAMLAuthenticationToken) token);
        credential = token.getCredentials();
      } catch (AuthenticationFailureException e) {
        String msg = "Unable to validate request's authentication.";
        LOGGER.info(msg);
        throw new AuthenticationException(msg, e);
      }
    }

    if (credential == null) {
      String msg =
          "Unable to authenticate credential.  A NULL credential was provided in the supplied authentication token. This may be due to an error with the SSO server that created the token.";
      LOGGER.info(msg);
      throw new AuthenticationException(msg);
    }
    LOGGER.debug("Received credentials.");

    LOGGER.debug("Creating token authentication information with SAML.");
    SimpleAuthenticationInfo simpleAuthenticationInfo = new SimpleAuthenticationInfo();
    Element securityToken = checkForSecurityToken(credential);
    SimplePrincipalCollection principals = createPrincipalFromToken(securityToken);
    simpleAuthenticationInfo.setPrincipals(principals);
    simpleAuthenticationInfo.setCredentials(credential);

    return simpleAuthenticationInfo;
  }

  private Element checkForSecurityToken(final Object credential) {
    if (credential instanceof PrincipalCollection) {
      Optional<SecurityAssertionSaml> assertionSamlOptional =
          ((PrincipalCollection) credential)
              .byType(SecurityAssertionSaml.class)
              .stream()
              .filter(sa -> sa.getToken() instanceof Element)
              .findFirst();
      if (assertionSamlOptional.isPresent()) {
        SecurityAssertionSaml assertion = assertionSamlOptional.get();
        return (Element) assertion.getToken();
      }
    }

    return null;
  }

  /**
   * Creates a new principal object from an incoming security token.
   *
   * @param token SecurityToken that contains the principals.
   * @return new SimplePrincipalCollection
   */
  private SimplePrincipalCollection createPrincipalFromToken(Element token) {
    SimplePrincipalCollection principals = new SimplePrincipalCollection();
    SecurityAssertion securityAssertion = null;
    try {
      securityAssertion = new SecurityAssertionSaml(token, usernameAttributeList);
      Principal principal = securityAssertion.getPrincipal();
      if (principal != null) {
        principals.add(principal.getName(), getName());
      }
    } catch (Exception e) {
      LOGGER.warn(
          "Encountered error while trying to get the Principal for the SecurityToken. Security functions may not work properly.",
          e);
    }
    if (securityAssertion != null) {
      principals.add(securityAssertion, getName());
    }
    return principals;
  }

  public List<String> getUsernameAttributeList() {
    return usernameAttributeList;
  }

  public void setUsernameAttributeList(List<String> usernameAttributeList) {
    this.usernameAttributeList = usernameAttributeList;
  }

  public SamlAssertionValidator getSamlAssertionValidator() {
    return samlAssertionValidator;
  }

  public void setSamlAssertionValidator(SamlAssertionValidator samlAssertionValidator) {
    this.samlAssertionValidator = samlAssertionValidator;
  }

  /**
   * Credentials matcher class that ensures the AuthInfo received from the STS matches the AuthToken
   */
  protected static class STSCredentialsMatcher implements CredentialsMatcher {

    @Override
    public boolean doCredentialsMatch(
        org.apache.shiro.authc.AuthenticationToken token, AuthenticationInfo info) {
      if (token instanceof SAMLAuthenticationToken) {
        Object oldToken = token.getCredentials();
        Object newToken = info.getCredentials();
        return oldToken.equals(newToken);
      } else if (token instanceof BaseAuthenticationToken) {
        String xmlCreds = ((BaseAuthenticationToken) token).getCredentialsAsString();
        if (xmlCreds != null && info.getCredentials() != null) {
          return xmlCreds.equals(info.getCredentials());
        }
      } else {
        if (token.getCredentials() != null && info.getCredentials() != null) {
          return token.getCredentials().equals(info.getCredentials());
        }
      }
      return false;
    }
  }
}
