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
package ddf.catalog.pubsub.command;

import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.apache.log4j.Logger;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class SubscriptionsCommand extends OsgiCommandSupport {

    public static final String NAMESPACE = "subscriptions";

    public static final String SERVICE_PID = "ddf.catalog.event.Subscription";
    
    private static final Logger LOGGER = Logger.getLogger(SubscriptionsCommand.class);
    

    @Override
    protected Object doExecute() throws Exception {

        return null;
    }
    
    protected List<String> getSubscriptions(String id, boolean ldapFilter)
        throws InvalidSyntaxException
    {
        List<String> subscriptionIds = new ArrayList<String>();
        
        String subscriptionIdFilter = null;
        if (ldapFilter)
        {
            subscriptionIdFilter = id;
        }
        else if (id != null)
        {
            subscriptionIdFilter = "(subscription-id=" + id + ")";
        }
        LOGGER.debug("subscriptionIdFilter = " + subscriptionIdFilter);
        ServiceReference[] serviceReferences = bundleContext.getServiceReferences(SERVICE_PID, subscriptionIdFilter);
        
        if (serviceReferences == null || serviceReferences.length == 0)
        {
            LOGGER.debug( "Found no service references for " + SERVICE_PID);
        }
        else
        {
            LOGGER.debug( "Found " + serviceReferences.length + " service references for " + SERVICE_PID);

            for (ServiceReference ref : serviceReferences)
            {
                String[] propertyKeys = ref.getPropertyKeys();
                if (propertyKeys != null)
                {
                    for (String key : propertyKeys)
                    {
                        LOGGER.debug("key = " + key);
                    }
                    String subscriptionId = (String) ref.getProperty("subscription-id");
                    LOGGER.debug("subscriptionId = " + subscriptionId);
                    subscriptionIds.add(subscriptionId);
                }
                else
                {
                    LOGGER.debug("propertyKeys = NULL");
                }
            }
        }

        return subscriptionIds;
    }

}
