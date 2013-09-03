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

package ddf.catalog.pubsub.internal;


public class PubSubConstants 
{
	public static final String HEADER_OPERATION_KEY = "operation";
	public static final String HEADER_CONTEXTUAL_KEY = "contextualInput";
	public static final String HEADER_GEOSPATIAL_KEY = "geospatialInput";
	public static final String HEADER_TEMPORAL_KEY = "temporalInput";
	public static final String HEADER_XPATH_KEY = "xpathInput";
	public static final String HEADER_ENTRY_KEY = "entry";
	public static final String HEADER_DELIVERY_METHOD = "delivery_method";
	public static final String HEADER_CONTENT_TYPE_KEY = "content_type";
	public static final String HEADER_ID_KEY = "id";
	public static final String HEADER_DAD_KEY = "dad";
	public static final String CREATE = "CREATE" ;
	public static final String DELETE = "DELETE" ;
	public static final String UPDATE = "UPDATE" ;
	public static final String WKT = "geometry";
	public static final String GML_NAMESPACE = "http://www.opengis.net/gml" ;
	public static final String XML_PARSERS = "javax.xml.parsers.DocumentBuilderFactory";
	public static final String XERCES_IMPLEMENTATION = "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl";
	public static final String ENDPOINT_EVALUATION = "seda:startPubSubEvaluation?multipleConsumers=true";
	public static final String PUBLISHED_EVENT_TOPIC_NAME = "ddf/pubsub/publish/event";
	
	public static final String EVENTS_ENTRY_NAME = "entry";
    public static final String EVENTS_TIME_NAME = "time";
    
    public static final String METADATA_DELETED = "<deleted/>";
}
