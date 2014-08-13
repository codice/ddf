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

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the basic work for an ApplicationConfigurationPlugin. 
 *
 */
public class AbstractApplicationConfigurationPlugin implements ApplicationConfigurationPlugin {
	/** the display name. Protected so implementers can set this.*/
	protected String displayName = null;
	/** the location of the iframe. Protected so implementers can set this.*/
	protected String iframeLocation = null;
	/** the location of the javascript. Protected so implements can set this.*/
	protected String javascriptLocation = null;
	/** The id of this plugin.*/
	private UUID id = UUID.randomUUID();
	/** the application name. Protected so implementers can set this.*/
	private List<String> applicationNames = new ArrayList<String>();
	/** the order of this plugin. Protected so implements can set this.*/
	protected Integer order = Integer.MAX_VALUE;
	
	/**
	 * Constructor.
	 */
	public AbstractApplicationConfigurationPlugin() {
		this.applicationNames.add(ApplicationConfigurationPlugin.ALL_APPLICATION_KEY);
	}

	/** {@inheritDoc}.*/
	@Override
	public List<String> getAssociatedApplications() {
		return this.applicationNames;
	}

	/** {@inheritDoc}.*/
	@Override
	public String getDisplayName() {
		return this.displayName;
	}
	
	/** {@inheritDoc}.*/
	@Override
	public URI getJavascriptLocation() {
		if (this.javascriptLocation == null) {
			return null;
		}
		return URI.create(this.javascriptLocation);
	}

	/** {@inheritDoc}.*/
	@Override
	public URI getIframeLocation() {
		if (this.iframeLocation == null) {
			return null;
		}
		return URI.create(this.iframeLocation);
	}
	
	/** {@inheritDoc}.*/
	@Override
	public Integer getOrder() {
		return this.order;
	}
	
	/** {@inheritDoc}.*/
	@Override
	public Map<String, Object> toJSON() {
		Map<String, Object> jsonMapping = new HashMap<String, Object>();
		
		jsonMapping.put(ApplicationConfigurationPlugin.APPLICATION_ASSOCIATION_KEY, this.applicationNames);
		jsonMapping.put(ApplicationConfigurationPlugin.ID_KEY, this.id.toString());
		jsonMapping.put(ApplicationConfigurationPlugin.DISPLAY_NAME_KEY, this.displayName);
		jsonMapping.put(ApplicationConfigurationPlugin.IFRAME_LOCATION_KEY, this.iframeLocation);
		jsonMapping.put(ApplicationConfigurationPlugin.JAVASCRIPT_LOCATION_KEY, this.javascriptLocation);
		jsonMapping.put(ApplicationConfigurationPlugin.ORDER_KEY, this.order);
		
		return jsonMapping;
	}

	/** {@inheritDoc}.*/
	@Override
	public boolean matchesApplicationName(String appName) {
		return (this.applicationNames.contains(ApplicationConfigurationPlugin.ALL_APPLICATION_KEY) || this.applicationNames.contains(appName));
	}

	/** {@inheritDoc}.*/
	@Override
	public UUID getID() {
		return this.id;
	}

	/** {@inheritDoc}.*/
	@Override
	public void setApplicationAssociations(List<String> applicationAssociations) {
		this.applicationNames = applicationAssociations;
	}

	/** {@inheritDoc}.*/
	@Override
	public void addApplicationAssociations(String applicationAssocation) {
		if (!this.applicationNames.contains(applicationAssocation)) {
			this.applicationNames.add(applicationAssocation);
		}
	}

	/** {@inheritDoc}.*/
	@Override
	public void addApplicationAssocations(List<String> applicationAssociations) {
		for (String appAssocation : applicationAssociations) {
			this.addApplicationAssociations(appAssocation);
		}
	}

}
