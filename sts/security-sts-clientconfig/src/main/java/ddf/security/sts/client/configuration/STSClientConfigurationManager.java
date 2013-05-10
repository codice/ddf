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
package ddf.security.sts.client.configuration;

import ddf.catalog.util.DdfConfigurationManager;
import ddf.catalog.util.DdfConfigurationWatcher;
import org.apache.log4j.Logger;
import org.osgi.service.cm.ConfigurationAdmin;

import java.util.Hashtable;
import java.util.List;

/**
 * User: tustisos
 * Date: 2/6/13
 * Time: 3:17 PM
 */
public class STSClientConfigurationManager extends DdfConfigurationManager
{

    private final Logger logger = Logger.getLogger(STSClientConfigurationManager.class);

    public static final String STS_ADDRESS = "sts.address";

    public static final String STS_SERVICE_NAME = "sts.service.name";

    public static final String STS_ENDPOINT_NAME = "sts.endpoint.name";
    
    public static final String STS_CLAIMS = "sts.claims";

    /**
     * Constructs the list of DDF system Settings (read-only and configurable settings) to be pushed to
     * registered DdfConfigurationWatchers.
     *
     * @param services the list of watchers of changes to the DDF System Settings
     * @param configurationAdmin the OSGi Configuration Admin service handle
     */
    public STSClientConfigurationManager( List<DdfConfigurationWatcher> services, ConfigurationAdmin configurationAdmin )
    {
        logger.debug( "ENTERING: STSClientConfigurationManager" );
        this.services = services;
        this.configurationAdmin = configurationAdmin;
        this.readOnlySettings = new Hashtable();
        this.configuration = new Hashtable();
        logger.debug("EXITING: STSClientConfigurationManager");
    }
}
