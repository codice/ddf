/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.util.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.util.Describable;

public class DescribableServiceMap<V extends Describable> implements Map<String, V>, EventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DescribableServiceMap.class);

    private static final String READ_ONLY_ERROR_MESSAGE = "This map is meant to be read only.";

    private Map<String, V> serviceMap = Collections.synchronizedMap(new HashMap<>());

    private Map<String, String> pidToNameMap = Collections.synchronizedMap(new HashMap<>());

    public DescribableServiceMap() {
        String[] topics = new String[] {"org/osgi/framework/ServiceEvent/MODIFIED"};

        Dictionary props = new Hashtable();
        props.put(EventConstants.EVENT_TOPIC, topics);
        getContext().registerService(EventHandler.class.getName(), this, props);
    }

    protected BundleContext getContext() {
        Bundle bundle = FrameworkUtil.getBundle(DescribableServiceMap.class);
        if (bundle != null) {
            return bundle.getBundleContext();
        }
        return null;
    }

    public void bind(ServiceReference ref) {

        LOGGER.debug("{} Binding  {}", this, ref);
        BundleContext context = getContext();

        if (ref != null && context != null) {
            try {
                V service = (V) context.getService(ref);
                serviceMap.put(service.getId(), service);
                pidToNameMap.put((String) ref.getProperty(Constants.SERVICE_PID), service.getId());
            } catch (ClassCastException e) {
                LOGGER.debug("Service {} could not be added to service map {} due to incorrect type",
                        ref,
                        this,
                        e);
            }
        } else {
            LOGGER.debug("BundleContext was null, unable to add service reference");
        }
    }

    public void unbind(ServiceReference ref) {
        BundleContext context = getContext();
        if (ref != null && context != null) {
            LOGGER.debug("{} Unbinding {}", this, ref);
            V service = (V) context.getService(ref);

            if (service != null) {
                pidToNameMap.remove(ref.getProperty(Constants.SERVICE_PID));
                serviceMap.remove(service.getId());
            }
        }
    }

    @Override
    public int size() {
        return serviceMap.size();
    }

    @Override
    public boolean isEmpty() {
        return serviceMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return serviceMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return serviceMap.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return serviceMap.get(key);
    }

    @Override
    public V put(String key, V value) {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MESSAGE);
    }

    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MESSAGE);
    }

    @Override
    public void putAll(Map<? extends String, ? extends V> m) {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MESSAGE);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MESSAGE);
    }

    @Override
    public Set<String> keySet() {
        return serviceMap.keySet();
    }

    @Override
    public Collection<V> values() {
        return serviceMap.values();
    }

    @Override
    public Set<Entry<String, V>> entrySet() {
        return serviceMap.entrySet();
    }

    /**
     * The handleEvent method takes care of keeping the internal describable map up to date when
     * service name changes occur.
     *
     * @param event
     */
    @Override
    public void handleEvent(Event event) {
        if (((ServiceEvent) event.getProperty(EventConstants.EVENT)).getType()
                != ServiceEvent.MODIFIED) {
            return;
        }
        String pid = event.getProperty(Constants.SERVICE_PID)
                .toString();
        String oldName = pidToNameMap.get(pid);

        //if we don't have it in our map then it's a service we don't care about
        if (oldName == null) {
            return;
        }

        V service = serviceMap.get(oldName);
        if (service == null) {
            return;
        }

        String newName = service.getId();
        if (!oldName.equals(newName)) {
            pidToNameMap.put(pid, newName);
            serviceMap.remove(oldName);
            serviceMap.put(newName, service);
            LOGGER.debug("Changed mapping name from {} to {}", oldName, newName);
        }
    }
}
