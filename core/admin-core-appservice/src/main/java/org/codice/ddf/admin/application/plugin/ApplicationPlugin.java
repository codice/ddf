/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.admin.application.plugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Defines an application configuration plugin. 
 *
 */
//FIXME: remove all references to applications.
public interface ApplicationPlugin {
	/** key to mark that a plugin should be used for ALL applications.*/
	public static final String ALL_ASSOCATION_KEY = "ALL";
	/** key for the display name. Used for creating json.*/
	public static final String DISPLAY_NAME_KEY = "displayName";
	/** key for the application name. Used for creating json.*/
	public static final String APPLICATION_ASSOCIATION_KEY = "applicationAssociation";
	/** key for the iframe location. Used for creating json.*/
	public static final String IFRAME_LOCATION_KEY = "iframeLocation";
	/** key for the javascript location. Used for creating json.*/
	public static final String JAVASCRIPT_LOCATION_KEY = "javascriptLocation";
	/** key for the id location. Used for creating json.*/
	public static final String ID_KEY = "id";
	/** key for the order. Used for creating json.*/
	public static final String ORDER_KEY = "order";
	
	/**
	 * Returns a list of applications that this plugin should be associated with.
	 * @return a list of applications that this plugin should be associated with.
	 */
	public List<String> getAssocations();
	
	/**
	 * Returns the display name. This is the value that will be display to the user.
	 * @return the display name.
	 */
	public String getDisplayName();
	
	/**
	 * Returns the id of this plugin. This is an unique identifier for the front end javascript.
	 * @return a unique identifier for this plugin as a uuid.
	 */
	public UUID getID();
	
	/**
	 * Returns the location of the javascript. Can be null.
	 * @return the location of the javascript.
	 */
	public String getJavascriptLocation();
	
	/**
	 * Returns the iframe location. Can be null.
	 * @return the iframe location.
	 */
    public String getIframeLocation();
    
    /**
     * Returns the prefered order. This value can be duplicated between plugins, at which
     * point the front end will use the name of the plugin to sort those of the same order
     * number.
     * @return the order of how this plugin should be displayed.
     */
    public Integer getOrder();
    
    /**
     * Utility method that will handle the conversion of this object to something
     * jolokia can convert to json.
     * @return a constructed  map that jolokia can convert to json.
     */
    public Map<String, Object> toJSON();
    
    /**
     * Handles figuring out if this plugin is matching to the app name sent in. This will
     * handle the case where a plugin should be used for all.
     * @param appName - the name of the application we are going to test.
     * @return yes if the application matches, or should be applied to all applications, false if it doesn't.
     */
    public boolean matchesAssocationName(String assocationName);
        
    /**
     * Sets the application assocations to the inputted values. This will overwrite all previous
     * values.
     * @param appName - the string name of an application.
     */
    public void setAssociations(List<String> assocations);
    
    /**
     * Adds an application assocation list to the existing list. This does not overwrite the
     * previous values, and if there is an existing value it wont add it.
     * @param applicationAssociations
     */
    public void addAssocations(List<String> assocations);
    
    /**
     * Adds a single application association to this plugin. If the application is already
     * there, then nothing will happen.
     * @param applicationAssocation - the string name of the application.
     */
    public void addAssociations(String assocations);

}
