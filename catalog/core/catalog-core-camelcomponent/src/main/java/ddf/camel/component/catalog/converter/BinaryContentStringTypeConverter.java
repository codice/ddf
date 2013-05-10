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
package ddf.camel.component.catalog.converter;

import java.io.InputStream;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.camel.Exchange;
import org.apache.camel.support.TypeConverterSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import ddf.catalog.data.BinaryContentImpl;

/**
 * An added CamelTypeConverter which can convert objects to a {@link ddf.catalog.data.BinaryContent}.
 * Leverages default Camel Conversions that can convert any type into an {@link java.io.InputStream} 
 * 
 * @author William Miller, Lockheed Martin
 * @author ddf.isgs@lmco.com
 *
 */
public class BinaryContentStringTypeConverter extends TypeConverterSupport {

    private static final transient Logger LOGGER = LoggerFactory.getLogger( BinaryContentStringTypeConverter.class );

	@Override
	public <T> T convertTo(Class<T> type, Exchange exchange, Object value) {

		String mimeTypeString = exchange.getOut().getHeader(HttpHeaders.CONTENT_TYPE , String.class);
		if(null == mimeTypeString) {
			mimeTypeString = MediaType.TEXT_PLAIN ;
		}
		MimeType mimeType = null;
		try {
			mimeType = new MimeType(mimeTypeString);
		} catch (MimeTypeParseException e) {
			LOGGER.warn("Failed to parse mimetype: " + mimeTypeString, e);
		}
		
		T result = null;
		try {
			result = type.cast(new BinaryContentImpl(exchange.getOut().getBody(InputStream.class), mimeType));
		} catch (ClassCastException e) {
			LOGGER.error("Failed to create BinaryContent", e);
		}
		
		return result;
	}

}
