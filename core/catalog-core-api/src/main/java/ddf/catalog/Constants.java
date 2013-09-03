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
package ddf.catalog;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.event.EventProcessor;

/**
 * The Constants class is used to capture key values that can be re-used
 * throughout DDF.
 * 
 * @author ddf.isgs@lmco.com
 */
public final class Constants {

	/**
	 * This constant should be used to pass the users credentials in the form of
	 * a Subject object through any request/response property map.
	 */
	public static final String SUBJECT_PROPERTY = "subject";

	/**
	 * The Constant LOCAL_SITE_ID_VALUE. public final static String
	 * LOCAL_SITE_ID_VALUE = "local";
	 * 
	 */ 
	 /** The constant SERVICE_SHORTNAME.
	 * 
	 * @deprecated use {@link #SERVICE_ID}
	 */
	public final static String SERVICE_SHORTNAME = "shortname";
	/**
	 * The constant to be used by Catalog Services to register a unique name by
	 * which they can be referred
	 */
	public final static String SERVICE_ID = "id";

	/** The constant SERVICE_TITLE. */
	public final static String SERVICE_TITLE = "title";

	/** The constant SERVICE_DESCRIPTION. */
	public final static String SERVICE_DESCRIPTION = "description";

	/** The constant HTTP_INVOCATION_ABSOLUTE_PATH_URI */
	public final static String HTTP_INVOCATION_ABSOLUTE_PATH_URI = "http-absolute-path";

	// /** The Constant FACTORY_PID. */
	// public final static String FACTORY_PID = "factory-pid";

	/** The constant FEDERATED_SITE_STATE. */
	public final static String FEDERATED_SITE_STATE = "federated-site-state";

	/** The constant FEDERATED_SITE_STATE_ACTIVE. */
	public final static String FEDERATED_SITE_STATE_ACTIVE = "active";

	/** The constant FEDERATED_SITE_STATE_INACTIVE. */
	public final static String FEDERATED_SITE_STATE_INACTIVE = "inactive";

	/**
	 * The constant EVENTS_ENTRY_NAME.
	 * 
	 * @deprecated Use {@link EventProcessor#EVENT_METACARD} instead
	 */
	public final static String EVENTS_ENTRY_NAME = EventProcessor.EVENT_METACARD;

	/**
	 * The constant EVENTS_TIME_NAME.
	 * 
	 * @deprecated Use {@link EventProcessor#EVENT_TIME} instead
	 */
	public final static String EVENTS_TIME_NAME = EventProcessor.EVENT_TIME;

	/**
	 * The constant EVENTS_TOPIC_CREATED.
	 * 
	 * @deprecated Use {@link EventProcessor#EVENTS_TOPIC_CREATED} instead
	 */
	public final static String EVENTS_TOPIC_CREATED = EventProcessor.EVENTS_TOPIC_CREATED;

	/**
	 * The constant EVENTS_TOPIC_UPDATED.
	 * 
	 * @deprecated Use {@link EventProcessor#EVENTS_TOPIC_UPDATED} instead
	 */
	public final static String EVENTS_TOPIC_UPDATED = EventProcessor.EVENTS_TOPIC_UPDATED;

	/**
	 * The constant EVENTS_TOPIC_DELETED.
	 * 
	 * @deprecated Use {@link EventProcessor#EVENTS_TOPIC_DELETED} instead
	 */
	public final static String EVENTS_TOPIC_DELETED = EventProcessor.EVENTS_TOPIC_DELETED;

	/** The constant DEFAULT_PAGE_SIZE. */
	public final static Integer DEFAULT_PAGE_SIZE = 20;

	/** The constant DEFAULT_START_INDEX for the default index to start query results with in a query response. */
	public final static Integer DEFAULT_START_INDEX = 0;

	/**
	 * @deprecated Use {@link Result#RELEVANCE} instead
	 */
	public final static String SORT_POLICY_VALUE_RELEVANCE = Result.RELEVANCE;
	/**
	 * @deprecated Use {@link Result#DISTANCE} instead
	 */
	public final static String SORT_POLICY_VALUE_DISTANCE = Result.DISTANCE;

	/**
	 * @deprecated Use the appropriate temporal field from {@link Metacard}
	 */
	public final static String SORT_POLICY_VALUE_TEMPORAL = "TEMPORAL";


	public static final String SUBSCRIPTION_KEY = "subscription";

	/**
	 * Constant for the String representation of the MIME type for a JPEG image
	 */
	public static final String MIME_TYPE_JPEG = "image/jpeg";
	
	public static final String SAML_ASSERTION = "saml.assertion";
	
	public static final String OAUTH_RESOURCE_OWNER_USERNAME = "resource.owner.username";

}