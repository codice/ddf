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
package ddf.security.sts;

import org.apache.cxf.sts.token.provider.AuthenticationStatementProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.ws.security.saml.ext.bean.AuthenticationStatementBean;
import org.apache.ws.security.saml.ext.builder.SAML2Constants;


/**
 * This class will always return the unspecified string to the SAML Token Provider 
 *
 */
public class UnspecifiedAuthNStatementProvider implements AuthenticationStatementProvider {

    /**
     * Get an AuthenticationStatementBean using the given parameters.
     */
	
	/* (non-Javadoc)
	 * @see org.apache.cxf.sts.token.provider.AuthenticationStatementProvider#getStatement(org.apache.cxf.sts.token.provider.TokenProviderParameters)
	 */
	@Override
    public AuthenticationStatementBean getStatement(TokenProviderParameters providerParameters) {
        AuthenticationStatementBean authBean = new AuthenticationStatementBean();

        authBean.setAuthenticationMethod(SAML2Constants.AUTH_CONTEXT_CLASS_REF_UNSPECIFIED);

        return authBean;
    }
}
