/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.security.assertion;

import java.io.Serializable;
import java.security.Principal;
import java.util.List;

import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.AuthnStatement;
import org.opensaml.saml2.core.AuthzDecisionStatement;

/**
 * This class serves as a wrapper for a CXF SecurityToken
 * @author tustisos
 *
 */
public interface SecurityAssertion extends Serializable
{
    /**
     * Returns the Principal contained within the SecurityToken
     * @return Principal
     */
    Principal getPrincipal();

    /**
     *  Returns the name of the entity that issued the SecurityToken
     * @return String - token issuer
     */
    String getIssuer();

    /**
     * Returns the list of attribute statements contained in the SecurityToken
     * @return List<AttributeStatement>
     */
    List<AttributeStatement> getAttibuteStatements();

    /**
     * Returns the list of authn statements contained in the SecurityToken
     * @return List<AuthnStatement>
     */
    List<AuthnStatement> getAuthnStatements();

    /**
     * Returns the list of authz statements contained in the SecurityToken
     * @return List<AuthzDecisionStatement>
     */
    List<AuthzDecisionStatement> getAuthzDecisionStatements();
    
    /**
     * Returns the underlying SecurityToken that this object wraps
     * @return SecurityToken
     */
    SecurityToken getSecurityToken();
    
    /**
     * Returns a String representation of this Assertion
     * @return String
     */
    String toString();
}
