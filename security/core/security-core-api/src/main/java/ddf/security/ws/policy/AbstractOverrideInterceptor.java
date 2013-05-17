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
package ddf.security.ws.policy;


import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.neethi.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




/**
 * Overrides the default CXF policies with the one specified in the constructor.
 * 
 */
public class AbstractOverrideInterceptor extends AbstractPhaseInterceptor<Message>
{

    private PolicyLoader loader;
    private Logger logger = LoggerFactory.getLogger(AbstractOverrideInterceptor.class);
    private Policy policy = null;

    /**
     * Creates a new instance of the OverrideInterceptor.
     * 
     * @param phase Phase to load in.
     * @param loader PolicyLoader to use to retrieve the policy.
     */
    public AbstractOverrideInterceptor( String phase, PolicyLoader loader )
    {
        super(phase);
        this.loader = loader;
    }

    /**
     * Adds the policy retrieved from the configured policy loader to this message as the override
     * policy.
     * @param message
     */
    @Override
    public void handleMessage( Message message )
    {
        if (policy == null)
        {
            PolicyBuilder builder = message.getExchange().getBus().getExtension(PolicyBuilder.class);

            try
            {
                policy = builder.getPolicy(loader.getPolicy().getDocumentElement());
                logger.trace("Read in policy, adding to policy override of message.");
                message.put(PolicyConstants.POLICY_OVERRIDE, policy);
            }
            catch (Exception e)
            {
                throw new Fault(e);
            }
        }
        else
        {
            message.put(PolicyConstants.POLICY_OVERRIDE, policy);
        }

    }

}
