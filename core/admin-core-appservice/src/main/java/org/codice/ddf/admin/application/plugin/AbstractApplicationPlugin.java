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
 * Handles the basic work for an AbstractApplicationPlugin. 
 *
 */
public class AbstractApplicationPlugin implements ApplicationPlugin {
	/** the display name. Protected so implementers can set this.*/
	protected String displayName = null;
	/** the location of the iframe. Protected so implementers can set this.*/
	protected URI iframeLocation = null;
	/** the location of the javascript. Protected so implements can set this.*/
	protected URI javascriptLocation = null;
	/** The id of this plugin.*/
	private UUID id = UUID.randomUUID();
	/** the application name. Protected so implementers can set this.*/
	private List<String> assocations = new ArrayList<String>();
	/** the order of this plugin. Protected so implements can set this.*/
	protected Integer order = Integer.MAX_VALUE;
	
	/**
	 * Constructor.
	 */
	public AbstractApplicationPlugin() {
		this.assocations.add(ApplicationPlugin.ALL_ASSOCATION_KEY);
	}

	/** {@inheritDoc}.*/
	@Override
	public List<String> getAssocations() {
		return this.assocations;
	}

	/** {@inheritDoc}.*/
	@Override
	public String getDisplayName() {
		return this.displayName;
	}
	
	/** {@inheritDoc}.*/
	@Override
	public String getJavascriptLocation() {
		if (this.javascriptLocation == null) {
			return null;
		}
		return this.javascriptLocation.toString();
	}

	/** {@inheritDoc}.*/
	@Override
	public String getIframeLocation() {
		if (this.iframeLocation == null) {
			return null;
		}
		return this.iframeLocation.toString();
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
		
		jsonMapping.put(ApplicationPlugin.APPLICATION_ASSOCIATION_KEY, this.assocations);
		jsonMapping.put(ApplicationPlugin.ID_KEY, this.id.toString());
		jsonMapping.put(ApplicationPlugin.DISPLAY_NAME_KEY, this.displayName);
		jsonMapping.put(ApplicationPlugin.IFRAME_LOCATION_KEY, (this.iframeLocation == null) ? null : this.iframeLocation.toString());
		jsonMapping.put(ApplicationPlugin.JAVASCRIPT_LOCATION_KEY, (this.javascriptLocation == null) ? null : this.javascriptLocation.toString());
		jsonMapping.put(ApplicationPlugin.ORDER_KEY, this.order);
		
		return jsonMapping;
	}

	/** {@inheritDoc}.*/
	@Override
	public boolean matchesAssocationName(String appName) {
		return (this.assocations.contains(ApplicationPlugin.ALL_ASSOCATION_KEY) || this.assocations.contains(appName));
	}

	/** {@inheritDoc}.*/
	@Override
	public UUID getID() {
		return this.id;
	}

	/** {@inheritDoc}.*/
	@Override
	public void setAssociations(List<String> applicationAssociations) {
		this.assocations = applicationAssociations;
	}

	/** {@inheritDoc}.*/
	@Override
	public void addAssociations(String applicationAssocation) {
		if (!this.assocations.contains(applicationAssocation)) {
			this.assocations.add(applicationAssocation);
		}
	}

	/** {@inheritDoc}.*/
	@Override
	public void addAssocations(List<String> applicationAssociations) {
		for (String appAssocation : applicationAssociations) {
			this.addAssociations(appAssocation);
		}
	}
}
