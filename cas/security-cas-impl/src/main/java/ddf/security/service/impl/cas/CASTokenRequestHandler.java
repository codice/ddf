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
package ddf.security.service.impl.cas;


import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.jasig.cas.client.authentication.AttributePrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.util.DdfConfigurationManager;
import ddf.catalog.util.DdfConfigurationWatcher;
import ddf.security.service.SecurityServiceException;
import ddf.security.service.TokenRequestHandler;


/**
 * Implementation of {@link ddf.security.service.TokenRequestHandler} that is
 * specific to CAS. Allows clients to send in a
 * {@link javax.servlet.http.HttpServletRequest} and retrieve a token that can
 * be used to create a subject.
 * 
 */
public class CASTokenRequestHandler implements TokenRequestHandler, DdfConfigurationWatcher
{

    private static final Logger LOGGER = LoggerFactory.getLogger(CASTokenRequestHandler.class);
    private String stsAddress = "https://server:8993/services/SecurityTokenService";

    @Override
    public Object createToken( HttpServletRequest request ) throws SecurityServiceException
    {
        AttributePrincipal attributePrincipal = (AttributePrincipal) request.getUserPrincipal();
        String proxyTicket = null;

        if (attributePrincipal != null)
        {
            LOGGER.debug("Getting proxy ticket for {}", stsAddress);
            proxyTicket = attributePrincipal.getProxyTicketFor(stsAddress);
            LOGGER.debug("Retrieved proxy ticket: {}", proxyTicket);
            return new CasAuthenticationToken(proxyTicket);
        }
        else
        {
            throw new SecurityServiceException("Could not get the principal from the incoming request.");
        }

    }

    @Override
    public void ddfConfigurationUpdated( @SuppressWarnings( "rawtypes" ) Map properties )
    {
        // only want to update based on the STS Client Settings
        if (!isDdfConfigurationUpdate(properties))
        {
            setStsPropertiesFromConfigAdmin(properties);
        }

    }

    /**
     * Determines if the received update is a DDF System Settings update.
     * 
     * @param properties
     * @return true if update was specific to DDF, false if it was another type
     *         of configuration update (for STS).
     */
    private boolean isDdfConfigurationUpdate( @SuppressWarnings( "rawtypes" ) Map properties )
    {

        return (properties.containsKey(DdfConfigurationManager.TRUST_STORE)
                && properties.containsKey(DdfConfigurationManager.TRUST_STORE_PASSWORD)
                && properties.containsKey(DdfConfigurationManager.KEY_STORE) && properties
            .containsKey(DdfConfigurationManager.KEY_STORE_PASSWORD));
    }

    /**
     * Set properties based on DDF STS Client setting updates.
     * 
     * @param properties
     */
    private void setStsPropertiesFromConfigAdmin( @SuppressWarnings( "rawtypes" ) Map properties )
    {
        String setStsAddress = (String) properties.get("sts.address");
        if (setStsAddress != null)
        {
            LOGGER.debug("Setting STS address for use in the CAS Proxy Ticket: " + setStsAddress);
            this.stsAddress = setStsAddress;
        }

    }

}
