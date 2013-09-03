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
package ddf.catalog.util;

import org.apache.log4j.Logger;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;


/**
 * The DDF Configuration Manager manages the DDF system settings. Some of these settings
 * are displayed in the Web Admin Console's Configuration tab under the DDF System Settings configuration.
 * Other settings are read-only, not displayed in the DDF System Settings configuration (but appear in
 * other OSGi bundle configurations such as CXF). These read-only settings are included in the list of
 * configuration settings pushed to registered listeners.
 * 
 * Registered listeners implement the DdfConfigurationWatcher interface and have these DDF configuration settings
 * pushed to them when they come online (aka bind) and when one or more of the settings are changed 
 * in the Admin Console.
 * 
 * @author ddf.isgs@lmco.com
 *
 */
public class DdfConfigurationManager 
{
	private static final Logger logger = Logger.getLogger( DdfConfigurationManager.class );
	
	// Constants for the DDF system settings appearing in the Admin Console	
	
	/** 
	 * Service PID to use to look up System Settings Configuration. 
	 */
	public static final String PID = "ddf.catalog.config";
	
	/**
	 * The directory where DDF is installed
	 */
	public static final String HOME_DIR = "homeDir";
		
	/**
	 * The port number that CXF's underlying Jetty server is listening on, e.g., 8181
	 */
	public static final String HTTP_PORT = "httpPort";
		
	/**
	 * The context root for all DDF services, 
	 * e.g., the /services portion of the http://hostname:8181/services URL
	 */
	public static final String SERVICES_CONTEXT_ROOT = "servicesContextRoot";
		
	/**
	 * The hostname or IP address of the machine that DDF is running on
	 */
	public static final String HOST = "host";
	
	/**
     * The port number that DDF is listening on, e.g., 8181
     */
	public static final String PORT = "port";

    /**
     * The protocol that DDF is using http/https
     */
    public static final String PROTOCOL = "protocol";

    /**
     * Trust store to use for outgoing DDF connections
     */
    public static final String TRUST_STORE = "trustStore";

    /**
     * Password associated with the trust store
     */
    public static final String TRUST_STORE_PASSWORD = "trustStorePassword";

    /**
     * Key store to use for outgoing DDF connections
     */
    public static final String KEY_STORE = "keyStore";

    /**
     * Password associated with the key store
     */
    public static final String KEY_STORE_PASSWORD = "keyStorePassword";
	
	/**
	 * The site name for this DDF instance
	 */
	public static final String SITE_NAME = "id";
	
	/**
	 * The version of DDF currently running
	 */
	public static final String VERSION = "version";
	
	/**
	 * The organization that this instance of DDF is running for
	 */
	public static final String ORGANIZATION = "organization";

    /**
     * The first preference for the type of federated source to create when the CAB registry client
     * attempts to create a source.
     * @deprecated
     */
    public static final String FIRST_FEDERATED_SOURCE_PREFERENCE = "firstSourcePreference";
       
    /**
     * The second preference for the type of federated source to create when the CAB registry client
     * attempts to create a source.
     * @deprecated
     */
    public static final String SECOND_FEDERATED_SOURCE_PREFERENCE = "secondSourcePreference";
       
    /**
     * The third preference for the type of federated source to create when the CAB registry client
     * attempts to create a source.
     * @deprecated
     */
    public static final String THIRD_FEDERATED_SOURCE_PREFERENCE = "thirdSourcePreference";
    
	// Constants for the read-only DDF system settings
	private static final String DDF_HOME_ENVIRONMENT_VARIABLE = "DDF_HOME";
	private static final String CXF_SERVICE_PID = "org.apache.cxf.osgi";
	private static final String CXF_SERVLET_CONTEXT = "org.apache.cxf.servlet.context";
	private static final String PAX_WEB_SERVICE_PID = "org.ops4j.pax.web";
    private static final String JETTY_HTTP_PORT = "org.osgi.service.http.port";
	
	/**
	 * List of DdfManagedServices to push the DDF system settings to.
	 */
    protected List<DdfConfigurationWatcher> services;
	
	
	/**
	 * The map of DDF system settings, including the read-only settings.
	 */
    protected Map configuration;
	
	/**
	 * The map of DDF system settings that are read-only, i.e., they are set in OSGi system
	 * bundles, not displayed in Admin Console's DDF System Settings configuration, but are
	 * pushed out in the configuration settings to DdfConfigurationWatchers.
	 */
    protected Map readOnlySettings;
	
	protected ConfigurationAdmin configurationAdmin;

    /**
     * Default Constructor
     */
    protected DdfConfigurationManager()
    {

    }

	
	/**
	 * Constructs the list of DDF system Settings (read-only and configurable settings) to be pushed to
	 * registered DdfConfigurationWatchers.
	 * 
	 * @param services the list of watchers of changes to the DDF System Settings
	 * @param configurationAdmin the OSGi Configuration Admin service handle
	 */
	public DdfConfigurationManager( List<DdfConfigurationWatcher> services, ConfigurationAdmin configurationAdmin )
	{
		logger.info( "ENTERING: ctor" );
		this.services = services;
		this.configurationAdmin = configurationAdmin;
		
		this.readOnlySettings = new Hashtable();
		readOnlySettings.put( HOME_DIR, System.getenv( DDF_HOME_ENVIRONMENT_VARIABLE ) );
		readOnlySettings.put( HTTP_PORT, getConfigurationValue( PAX_WEB_SERVICE_PID, JETTY_HTTP_PORT ) );
		readOnlySettings.put( SERVICES_CONTEXT_ROOT, getConfigurationValue( CXF_SERVICE_PID, CXF_SERVLET_CONTEXT ) );
		
		this.configuration = new Hashtable();
		
		// Append the read-only settings to the DDF System Settings so that all
		// settings are pushed to registered listeners
		configuration.putAll( readOnlySettings );
	}
	
	
	/**
	 * Invoked when the DDF system settings are changed in the Admin Console, this method then
	 * pushes those DDF system settings to each of the registered DdfConfigurationWatchers.
	 * 
	 * @param configuration list of DDF system settings, not including the read-only settings
	 */
	public void updated( Map configuration )
	{
		String methodName = "updated";
		logger.info( "ENTERING: " + methodName );
		
		if ( configuration != null && !configuration.isEmpty() )
		{
			this.configuration = configuration;
			
			// Add the read-only settings to list to be pushed out to watchers
			this.configuration.putAll( readOnlySettings );
		}

		for ( DdfConfigurationWatcher service : services )
		{
			service.ddfConfigurationUpdated( this.configuration );
		}
		
		logger.info( "EXITING: " + methodName );
	}
	
	
	/**
	 * Invoked when a DdfConfigurationWatcher first comes online, e.g., when a federated source is configured,
	 * this method pushes the DDF system settings to the newly registered (bound) DdfConfigurationWatcher.
     * 
	 * @param service
	 * @param properties
	 */
	public void bind( DdfConfigurationWatcher service, Map properties )
	{
		String methodName = "bind";
		logger.info( "ENTERING: " + methodName );
		
		if ( service != null )
		{
			service.ddfConfigurationUpdated( this.configuration );
		}
		
		logger.info( "EXITING: " + methodName );
	}
	
	
    /**
     * @return OSGi Configuratrion Admin service handle
     */
    public ConfigurationAdmin getConfigurationAdmin()
    {
        return configurationAdmin;
    }

    
    /**
     * @param configurationAdmin
     */
    public void setConfigurationAdmin( ConfigurationAdmin configurationAdmin )
    {
        this.configurationAdmin = configurationAdmin;
    }
    
    
    /**
     * Retrieves the value of an OSGi bundle's configuration property
     * 
     * @param servicePid PID for an OSGi bundle
     * @param propertyName name of the bundle's configuration property to get a value for
     * 
     * @return the value of the specified bundle's configuration property
     */
    public String getConfigurationValue( String servicePid, String propertyName )
    {
        String methodName = "getConfigurationValue";
        logger.info( "ENTERING: " + methodName + "   servicePid = " + servicePid + ",  propertyName = " + propertyName );
        
        String value = "";
        
        try
        {
            if ( this.configurationAdmin != null )
            {
                Configuration currentConfiguration = this.configurationAdmin.getConfiguration( servicePid );
    
                if ( currentConfiguration != null )
                {
                    Dictionary properties = currentConfiguration.getProperties();
        
                    if ( properties != null && properties.get( propertyName ) != null )
                    {
                        value = (String) properties.get( propertyName );
                    }
                    else
                    {
                        logger.debug( "properties for servicePid = " + servicePid + " were NULL or empty" );
                    }
                }
                else
                {
                    logger.debug( "configuration for servicePid = " + servicePid + " was NULL" );
                }
            }
            else
            {
                logger.debug( "configurationAdmin is NULL" );
            }
        }
        catch ( IOException e )
        {
            logger.warn( e );
        }
        
        logger.info( "EXITING: " + methodName + "    value = [" + value + "]" );
        
        return value;
    }
}
