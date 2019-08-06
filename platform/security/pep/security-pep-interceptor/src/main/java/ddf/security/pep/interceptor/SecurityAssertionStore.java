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
package ddf.security.pep.interceptor;

import ddf.security.assertion.SecurityAssertion;
import ddf.security.assertion.SecurityAssertionPrincipal;
import ddf.security.assertion.saml.impl.SecurityAssertionSaml;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.apache.cxf.message.Message;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreUtils;
import org.apache.wss4j.common.principal.SAMLTokenPrincipal;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;

/**
 * Creates a SecurityAssertion object by extracting the token id from the message and using that id
 * to retrieve the SecurityToken object from the TokenStore instance.
 *
 * @author tustisos
 */
public final class SecurityAssertionStore {
  /**
   * Return the SecurityAssertion wrapper associated with the provided message
   *
   * @param message Message
   * @return SecurityAssertion
   */
  public static SecurityAssertion getSecurityAssertion(Message message) {
    if (message != null) {
      TokenStore tokenStore = getTokenStore(message);
      Principal principal = null;
      SecurityContext context = message.get(SecurityContext.class);
      if (context != null) {
        principal = context.getUserPrincipal();
      }
      if (!(principal instanceof SAMLTokenPrincipal)) {
        // Try to find the SAMLTokenPrincipal if it exists
        List<?> wsResults = List.class.cast(message.get(WSHandlerConstants.RECV_RESULTS));
        if (wsResults != null) {
          for (Object wsResult : wsResults) {
            if (wsResult instanceof WSHandlerResult) {
              List<WSSecurityEngineResult> wsseResults = ((WSHandlerResult) wsResult).getResults();

              for (WSSecurityEngineResult wsseResult : wsseResults) {
                Object principalResult = wsseResult.get(WSSecurityEngineResult.TAG_PRINCIPAL);
                if (principalResult instanceof SAMLTokenPrincipal) {
                  principal = (SAMLTokenPrincipal) principalResult;
                  break;
                }
              }
            }
          }
        }
      }
      if (tokenStore != null && principal instanceof SAMLTokenPrincipal) {
        String id = ((SAMLTokenPrincipal) principal).getId();
        SamlAssertionWrapper samlAssertionWrapper = ((SAMLTokenPrincipal) principal).getToken();
        SecurityToken token = tokenStore.getToken(id);
        if (token == null) {
          if (samlAssertionWrapper.getSaml2().getIssueInstant() != null
              && samlAssertionWrapper.getSaml2().getConditions() != null
              && samlAssertionWrapper.getSaml2().getConditions().getNotOnOrAfter() != null) {
            token =
                new SecurityToken(
                    id,
                    samlAssertionWrapper.getElement(),
                    Instant.ofEpochMilli(
                        samlAssertionWrapper.getSaml2().getIssueInstant().getMillis()),
                    Instant.ofEpochMilli(
                        samlAssertionWrapper
                            .getSaml2()
                            .getConditions()
                            .getNotOnOrAfter()
                            .getMillis()));
          } else {
            // we don't know how long this should last or when it was created, so just
            // set it to 1 minute
            // This shouldn't happen unless someone sets up a third party STS with weird
            // settings.
            Instant now = Instant.now();
            token =
                new SecurityToken(
                    id, samlAssertionWrapper.getElement(), now, now.plus(Duration.ofMinutes(1L)));
          }
          tokenStore.add(token);
        }

        return new SecurityAssertionSaml(token);
      } else if (principal instanceof SecurityAssertionPrincipal) {
        return ((SecurityAssertionPrincipal) principal).getAssertion();
      }
    }
    return new SecurityAssertionSaml();
  }

  /**
   * Return the TokenStore associated with this message.
   *
   * @param message
   * @return TokenStore
   */
  public static TokenStore getTokenStore(Message message) {
    return TokenStoreUtils.getTokenStore(message);
  }
}
