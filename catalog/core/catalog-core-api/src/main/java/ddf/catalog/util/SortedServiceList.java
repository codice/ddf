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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.CatalogFramework;
import ddf.catalog.plugin.PreIngestPlugin;

/**
 * <p>
 * This utility object sorts Services by their service rankings first and then breaks ties with
 * service ids. Its initial intent is to be used with {@link CatalogFramework} plugins, but can be
 * used with any service.
 * <p>
 * 
 * <p>
 * It is an implementation of {@link List} but is backed by a sorted {@link TreeMap} of
 * <ServiceReference, T> where the {@link ServiceReference} objects are what is used to maintain the
 * list order but the objects passed by this {@link List} to clients are the actual service objects
 * and not the service references.
 * </p>
 * 
 * <p>
 * For instance if this was a SortedServiceList<PreIngestPlugin> object, then in the internal
 * TreeMap the {@link ServiceReference} objects would be maintained as keys but
 * {@link PreIngestPlugin} objects would be what is passed to clients. Therefore, a call to a
 * populated {@link SortedServiceList} list such as <code>list.get(0)</code> would return the first
 * {@link PreIngestPlugin} object.
 * </p>
 * 
 * @author ddf.isgs@lmco.com
 * 
 * @param <T>
 */
public class SortedServiceList<T> implements List<T> {

    private static final String READ_ONLY_ERROR_MESSAGE = "This list is meant to be read only.";

    private Map<ServiceReference, T> serviceMap = Collections
            .synchronizedMap(new TreeMap<ServiceReference, T>(new ServiceComparator()));

    private BundleContext context;

    private static XLogger logger = new XLogger(LoggerFactory.getLogger(SortedServiceList.class));

    /**
     * Constructor accepting OSGi bundle context. This constructor is currently invoked by the
     * ddf-catalog-framework bundle's blueprint and the fanout-catalogframework bundle's blueprint
     * upon framework construction.
     * 
     * @param bundleContext
     *            the OSGi bundle's context
     */
    public SortedServiceList(BundleContext bundleContext) {
        context = bundleContext;
    }

    /**
     * Adds the newly bound OSGi service and its service reference to the internally maintained and
     * sorted serviceMap. This method is invoked when a plugin is bound (created/installed). This
     * includes preingest, postingest, prequery, postquery, preresource, postresource plugins.
     * 
     * @param ref
     *            the OSGi service reference
     */
    public void bindPlugin(ServiceReference ref) {

        logger.debug(this + " Binding " + ref);

        T service = (T) context.getService(ref);

        serviceMap.put(ref, service);

        logger.debug(Arrays.asList(serviceMap.values()).toString());

    }

    /**
     * Removes the newly bound OSGi service and its service reference to the internally maintained
     * and sorted serviceMap. This method is invoked when a plugin is unbound (removed/uninstalled).
     * This includes preingest, postingest, prequery, postquery, preresource, postresource plugins.
     * 
     * @param ref
     *            the OSGi service reference
     */
    public void unbindPlugin(ServiceReference ref) {

        logger.debug("Unbinding " + ref);

        serviceMap.remove(ref);

        logger.debug(Arrays.asList(serviceMap.values()).toString());
    }

    /**
     * Unsupported operation, throws an UnsupportedOperationException since this list is read-only.
     */
    @Override
    public boolean add(T arg0) {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MESSAGE);
    }

    /**
     * Unsupported operation, throws an UnsupportedOperationException since this list is read-only.
     */
    @Override
    public void add(int arg0, T arg1) {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MESSAGE);
    }

    /**
     * Unsupported operation, throws an UnsupportedOperationException since this list is read-only.
     */
    @Override
    public boolean addAll(Collection<? extends T> arg0) {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MESSAGE);
    }

    /**
     * Unsupported operation, throws an UnsupportedOperationException since this list is read-only.
     */
    @Override
    public boolean addAll(int arg0, Collection<? extends T> arg1) {
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
        return serviceMap.containsValue(arg0);
    }

    @Override
    public boolean containsAll(Collection<?> arg0) {

        return serviceMap.values().containsAll(arg0);
    }

    @Override
    public T get(int arg0) {
        logger.debug("GET called on : " + arg0);
        if (serviceMap.values() != null) {
            ArrayList<T> list = new ArrayList<T>(serviceMap.values());
            return list.get(arg0);
        }
        return null;
    }

    @Override
    public int indexOf(Object arg0) {
        if (serviceMap.values() != null) {
            ArrayList<T> list = new ArrayList<T>(serviceMap.values());
            return list.indexOf(arg0);
        }
        return -1;
    }

    @Override
    public boolean isEmpty() {
        return serviceMap.isEmpty();
    }

    @Override
    public Iterator<T> iterator() {
        synchronized (serviceMap) { // Synchronizing on m, not s!
            return serviceMap.values().iterator();
        }

    }

    @Override
    public int lastIndexOf(Object arg0) {
        if (serviceMap.values() != null) {
            ArrayList<T> list = new ArrayList<T>(serviceMap.values());
            return list.lastIndexOf(arg0);
        }
        return -1;
    }

    @Override
    public ListIterator<T> listIterator() {
        if (serviceMap.values() != null) {
            ArrayList<T> list = new ArrayList<T>(serviceMap.values());
            return list.listIterator();
        }
        return null;
    }

    @Override
    public ListIterator<T> listIterator(int arg0) {
        if (serviceMap.values() != null) {
            ArrayList<T> list = new ArrayList<T>(serviceMap.values());
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
    public T remove(int arg0) {
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
    public T set(int arg0, T arg1) {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MESSAGE);
    }

    @Override
    public int size() {
        return serviceMap.size();
    }

    @Override
    public List<T> subList(int arg0, int arg1) {
        if (serviceMap.values() != null) {
            ArrayList<T> list = new ArrayList<T>(serviceMap.values());
            return list.subList(arg0, arg1);
        }
        return null;
    }

    @Override
    public Object[] toArray() {
        return serviceMap.values().toArray();
    }

    @Override
    public <T> T[] toArray(T[] arg0) {
        return serviceMap.values().toArray(arg0);
    }

}
