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
package ddf.catalog.util.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.TreeSet;

import org.osgi.framework.ServiceReference;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.util.ServiceComparator;
import ddf.catalog.util.SortedServiceReferenceList;

/**
 * <p>
 * This utility object sorts ServiceReferences by their service rankings first and then breaks ties
 * with service ids.
 * <p>
 * 
 * <p>
 * It is an implementation of {@link List} but is backed by a sorted {@link TreeSet} of
 * <ServiceReference> where the {@link ServiceReference} objects are what is used to maintain the
 * list order.
 * </p>
 * 
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 */
public class SortedServiceReferenceList implements List<ServiceReference> {

    private static final String READ_ONLY_ERROR_MESSAGE = "This list is meant to be read only.";

    private Set<ServiceReference> serviceSet = Collections
            .synchronizedSet(new TreeSet<ServiceReference>(new ServiceComparator()));

    private static XLogger logger = new XLogger(
            LoggerFactory.getLogger(SortedServiceReferenceList.class));

    /**
     * Adds the newly bound OSGi service and its service reference to the internally maintained and
     * sorted serviceSet. This method is invoked when a service is bound (created/installed).
     * 
     * @param ref
     *            the OSGi service reference
     */
    public void bindService(ServiceReference ref) {

        logger.debug(this + " Binding " + ref);

        serviceSet.add(ref);

    }

    /**
     * Removes the newly bound OSGi service and its service reference to the internally maintained
     * and sorted serviceMap. This method is invoked when a service is unbound
     * (removed/uninstalled).
     * 
     * @param ref
     *            the OSGi service reference
     */
    public void unbindService(ServiceReference ref) {

        logger.debug("Unbinding " + ref);

        serviceSet.remove(ref);

    }

    /**
     * Unsupported operation, throws an UnsupportedOperationException since this list is read-only.
     */
    @Override
    public boolean add(ServiceReference arg0) {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MESSAGE);
    }

    /**
     * Unsupported operation, throws an UnsupportedOperationException since this list is read-only.
     */
    @Override
    public void add(int arg0, ServiceReference arg1) {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MESSAGE);
    }

    /**
     * Unsupported operation, throws an UnsupportedOperationException since this list is read-only.
     */
    @Override
    public boolean addAll(Collection<? extends ServiceReference> arg0) {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MESSAGE);
    }

    /**
     * Unsupported operation, throws an UnsupportedOperationException since this list is read-only.
     */
    @Override
    public boolean addAll(int arg0, Collection<? extends ServiceReference> arg1) {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MESSAGE);
    }

    /**
     * Unsupported operation, throws an UnsupportedOperationException since this list is read-only.
     */
    @Override
    public void clear() {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MESSAGE);
    }

    @Override
    public boolean contains(Object arg0) {
        return serviceSet.contains(arg0);
    }

    @Override
    public boolean containsAll(Collection<?> arg0) {

        return serviceSet.containsAll(arg0);
    }

    @Override
    public ServiceReference get(int arg0) {
        logger.debug("GET called on : " + arg0);

        if (serviceSet != null) {
            ArrayList<ServiceReference> list = new ArrayList<ServiceReference>(serviceSet);
            return list.get(arg0);
        }

        throw new IndexOutOfBoundsException();
    }

    @Override
    public int indexOf(Object arg0) {
        if (serviceSet != null) {
            ArrayList<ServiceReference> list = new ArrayList<ServiceReference>(serviceSet);
            return list.indexOf(arg0);
        }
        return -1;
    }

    @Override
    public boolean isEmpty() {
        return serviceSet.isEmpty();
    }

    @Override
    public Iterator<ServiceReference> iterator() {
        synchronized (serviceSet) {
            return serviceSet.iterator();
        }

    }

    @Override
    public int lastIndexOf(Object arg0) {
        if (serviceSet != null) {
            ArrayList<ServiceReference> list = new ArrayList<ServiceReference>(serviceSet);
            return list.lastIndexOf(arg0);
        }
        return -1;
    }

    @Override
    public ListIterator<ServiceReference> listIterator() {
        if (serviceSet != null) {
            ArrayList<ServiceReference> list = new ArrayList<ServiceReference>(serviceSet);
            return list.listIterator();
        }
        return null;
    }

    @Override
    public ListIterator<ServiceReference> listIterator(int arg0) {
        if (serviceSet != null) {
            ArrayList<ServiceReference> list = new ArrayList<ServiceReference>(serviceSet);
            return list.listIterator(arg0);
        }
        return null;
    }

    /**
     * Unsupported operation, throws an UnsupportedOperationException since this list is read-only.
     */
    @Override
    public boolean remove(Object arg0) {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MESSAGE);
    }

    /**
     * Unsupported operation, throws an UnsupportedOperationException since this list is read-only.
     */
    @Override
    public ServiceReference remove(int arg0) {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MESSAGE);
    }

    /**
     * Unsupported operation, throws an UnsupportedOperationException since this list is read-only.
     */
    @Override
    public boolean removeAll(Collection<?> arg0) {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MESSAGE);
    }

    /**
     * Unsupported operation, throws an UnsupportedOperationException since this list is read-only.
     */
    @Override
    public boolean retainAll(Collection<?> arg0) {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MESSAGE);
    }

    /**
     * Unsupported operation, throws an UnsupportedOperationException since this list is read-only.
     */
    @Override
    public ServiceReference set(int arg0, ServiceReference arg1) {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MESSAGE);
    }

    @Override
    public int size() {
        return serviceSet.size();
    }

    @Override
    public List<ServiceReference> subList(int arg0, int arg1) {
        if (serviceSet != null) {
            ArrayList<ServiceReference> list = new ArrayList<ServiceReference>(serviceSet);
            return list.subList(arg0, arg1);
        }
        return null;
    }

    @Override
    public Object[] toArray() {
        return serviceSet.toArray();
    }

    @Override
    public <T> T[] toArray(T[] arg0) {
        return serviceSet.toArray(arg0);
    }

}
