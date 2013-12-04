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
package ddf.catalog.util;

import java.util.Comparator;

import org.osgi.framework.ServiceReference;

/**
 * Comparator for OSGi {@link ServiceReference} objects.
 * 
 * @deprecated As of release 2.3.0, replaced by
 *             ddf.catalog.util.impl.ServiceComparator
 */
@Deprecated
public class ServiceComparator implements Comparator<ServiceReference> {

    /**
     * Compares this ServiceReference with the specified ServiceReference for order using the OSGi
     * {@link ServiceReference} compare method.
     * 
     * If this ServiceReference and the specified ServiceReference have the same service id they are
     * equal. This ServiceReference is less than the specified ServiceReference if it has a lower
     * service ranking and greater if it has a higher service ranking. Otherwise, if this
     * ServiceReference and the specified ServiceReference have the same service ranking, this
     * ServiceReference is less than the specified ServiceReference if it has a higher service id
     * and greater if it has a lower service id.
     */
    @Override
    public int compare(ServiceReference ref1, ServiceReference ref2) {
        return ref2.compareTo(ref1);
    }

}
