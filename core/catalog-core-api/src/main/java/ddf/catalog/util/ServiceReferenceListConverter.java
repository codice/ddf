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

import java.util.List;

import org.osgi.service.blueprint.container.Converter;
import org.osgi.service.blueprint.container.ReifiedType;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

/**
 * This converter is used to allow {@link ServiceReferenceListConverter} objects
 * to pass through for {@link List} implementations. This was originally
 * intended to allow plugins to be automatically sorted in the list. Without
 * this converter, blueprint will copy the list and lose the reference.
 * 
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 */
public class ServiceReferenceListConverter implements Converter {

	private static XLogger logger = new XLogger(LoggerFactory.getLogger(ServiceReferenceListConverter.class));

	/**
	 * @parameter sourceObject object considering to be converted
	 * @parameter targetType
	 * 
	 * @return true if sourceObject is an instance of SortedServiceList; false
	 *         otherwise
	 */
	@Override
	public boolean canConvert(Object sourceObject, ReifiedType targetType) {
		logger.trace("Deciphering if canConvert ");

		if (targetType != null) {
			logger.debug("ReifiedType:" + targetType.getRawClass());
		}
		return (sourceObject instanceof SortedServiceReferenceList);
	}

	/**
	 * Converts (casts) the sourceObject to a SortedServiceList.
	 * 
	 * @parameter sourceObject object being converted
	 * @parameter targetType
	 * 
	 * @return sourceObject cast to a SortedServiceList
	 */
	@Override
	public Object convert(Object sourceObject, ReifiedType targetType) throws Exception {

		logger.trace("Converting " + sourceObject);

		SortedServiceReferenceList list = ((SortedServiceReferenceList) sourceObject);

		return list;
	}

}
