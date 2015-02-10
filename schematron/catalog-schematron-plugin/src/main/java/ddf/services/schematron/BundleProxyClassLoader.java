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

package ddf.services.schematron;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to proxy the classloader of other bundles so that external processors such as a Saxon Engine
 * can have access to resources outside the scope of the current bundle.
 * 
 * @author abarakat
 * 
 */
public class BundleProxyClassLoader extends ClassLoader {
    private Bundle bundle;

    private ClassLoader parent;

    private static final String CLASS_NAME = BundleProxyClassLoader.class.getName();

    private static final Logger LOGGER = LoggerFactory.getLogger(BundleProxyClassLoader.class);

    public BundleProxyClassLoader(Bundle bundle) {
        String methodName = "constructor";
        LOGGER.debug("ENTERING: {}.{}", CLASS_NAME, methodName);

        this.bundle = bundle;

        LOGGER.debug("EXITING: {}.{}", CLASS_NAME, methodName);
    }

    public BundleProxyClassLoader(Bundle bundle, ClassLoader parent) {
        super(parent);
        this.parent = parent;
        this.bundle = bundle;
    }

    // Note: Both ClassLoader.getResources(...) and bundle.getResources(...) consult
    // the boot classloader. As a result, BundleProxyClassLoader.getResources(...)
    // might return duplicate results from the boot classloader. Prior to Java 5
    // Classloader.getResources was marked final. If your target environment requires
    // at least Java 5 you can prevent the occurrence of duplicate boot classloader
    // resources by overriding ClassLoader.getResources(...) instead of
    // ClassLoader.findResources(...).
    public Enumeration findResources(String name) throws IOException {
        String methodName = "findResources";
        LOGGER.debug("ENTERING: {}.{}", CLASS_NAME, methodName);

        return bundle.getResources(name);
    }

    public URL findResource(String name) {
        String methodName = "findResource";
        LOGGER.debug("ENTERING: {}.{}", CLASS_NAME, methodName);

        URL url = bundle.getResource(name);

        LOGGER.debug("name = {}, = {}", name, url.toString());
        LOGGER.debug("EXITING: {}.{}", CLASS_NAME, methodName);

        return url;
    }

    public Class findClass(String name) throws ClassNotFoundException {
        String methodName = "findClass";
        LOGGER.debug("ENTERING: {}.{}", CLASS_NAME, methodName);

        Class clazz = bundle.loadClass(name);

        LOGGER.debug("name = {},   clazz = {}", name, clazz.getName());
        LOGGER.debug("EXITING: {}.{}", CLASS_NAME, methodName);

        return clazz;
    }

    public URL getResource(String name) {
        String methodName = "getResource";
        LOGGER.debug("ENTERING: {}.{}", CLASS_NAME, methodName);

        URL url = null;
        if (parent == null) {
            url = findResource(name);
        } else {
            url = super.getResource(name);
        }

        if (null != url) {
            LOGGER.debug("name = {},   url = {}", name, url.toString());
        }
        LOGGER.debug("EXITING: {}.{}", CLASS_NAME, methodName);

        return url;
    }

    protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        String methodName = "loadClass";
        LOGGER.debug("ENTERING: {}.{}", CLASS_NAME, methodName);
        LOGGER.debug("name = {},   resolve = {}", name, resolve);

        Class clazz = (parent == null) ? findClass(name) : super.loadClass(name, false);
        if (resolve) {
            LOGGER.debug("Calling super.resolveClass()");
            super.resolveClass(clazz);
        }

        LOGGER.debug("name = {},   clazz = {}", name, clazz.getName());
        LOGGER.debug("EXITING: {}.{}", CLASS_NAME, methodName);

        return clazz;
    }

}
