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
package ddf.security.sts;

import java.security.Principal;
import java.security.SecureRandom;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.x500.X500Principal;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.token.provider.AuthenticationStatementProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.wss4j.common.principal.UsernameTokenPrincipal;
import org.apache.wss4j.common.saml.bean.AuthenticationStatementBean;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.codice.ddf.platform.util.RandomNumberGenerator;

/** This class will always return the unspecified string to the SAML Token Provider */
public class AuthNStatementProvider implements AuthenticationStatementProvider {

  private SecureRandom secureRandom;

  /** Get an AuthenticationStatementBean using the given parameters. */
  public AuthNStatementProvider() {
    secureRandom = RandomNumberGenerator.create();
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.apache.cxf.sts.token.provider.AuthenticationStatementProvider#getStatement(org.apache
   * .cxf.sts.token.provider.TokenProviderParameters)
   */
  @Override
  public AuthenticationStatementBean getStatement(TokenProviderParameters providerParameters) {
    AuthenticationStatementBean authBean = new AuthenticationStatementBean();
    authBean.setSessionIndex(Integer.toString(secureRandom.nextInt()));

    TokenRequirements tokenRequirements = providerParameters.getTokenRequirements();
    ReceivedToken receivedToken = null;
    if (tokenRequirements.getValidateTarget() != null) {
      receivedToken = tokenRequirements.getValidateTarget();
    } else if (tokenRequirements.getOnBehalfOf() != null) {
      receivedToken = tokenRequirements.getOnBehalfOf();
    } else if (tokenRequirements.getActAs() != null) {
      receivedToken = tokenRequirements.getActAs();
    } else if (tokenRequirements.getRenewTarget() != null) {
      receivedToken = tokenRequirements.getRenewTarget();
    }

    if (receivedToken != null) {
      Principal principal = receivedToken.getPrincipal();
      if (principal instanceof UsernameTokenPrincipal) {
        authBean.setAuthenticationMethod(
            SAML2Constants.AUTH_CONTEXT_CLASS_REF_PASSWORD_PROTECTED_TRANSPORT);
      } else if (principal instanceof X500Principal) {
        authBean.setAuthenticationMethod(SAML2Constants.AUTH_CONTEXT_CLASS_REF_X509);
      } else if (principal instanceof KerberosPrincipal) {
        authBean.setAuthenticationMethod(SAML2Constants.AUTH_CONTEXT_CLASS_REF_KERBEROS);
      } else {
        authBean.setAuthenticationMethod(SAML2Constants.AUTH_CONTEXT_CLASS_REF_UNSPECIFIED);
      }
    } else {
      authBean.setAuthenticationMethod(SAML2Constants.AUTH_CONTEXT_CLASS_REF_UNSPECIFIED);
    }

    return authBean;
  }
}
