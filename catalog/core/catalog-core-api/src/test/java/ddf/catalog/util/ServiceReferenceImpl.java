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

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class ServiceReferenceImpl implements ServiceReference {

	Map<String, Object> map = new HashMap<String, Object>();

	public ServiceReferenceImpl(int i) {

		map.put(Constants.SERVICE_RANKING, i);
	}

	@Override
	public Object getProperty(String key) {
		return map.get(key);
	}

	@Override
	public String[] getPropertyKeys() {
		return null;
	}

	@Override
	public Bundle getBundle() {
		return null;
	}

	@Override
	public Bundle[] getUsingBundles() {
		return null;
	}

	@Override
	public boolean isAssignableTo(Bundle bundle, String className) {
		return false;
	}

	@Override
	public int compareTo(Object other) {

		ServiceReference otherServiceReference = (ServiceReference) other;

		int otherRanking = (Integer) otherServiceReference.getProperty(Constants.SERVICE_RANKING);

		int thisRanking = (Integer) map.get(Constants.SERVICE_RANKING);
		if (otherRanking != thisRanking) {
			if (thisRanking < otherRanking) {
				return -1;
			}
			return 1;
		}

		// check ids, then it is the same serviceReference

		return 0;
	}

}
