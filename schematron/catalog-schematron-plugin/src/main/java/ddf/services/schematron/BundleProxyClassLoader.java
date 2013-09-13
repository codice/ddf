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

import org.apache.log4j.Logger;
import org.osgi.framework.Bundle;

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

    /** This class' log4j logger */
    Logger logger = Logger.getLogger(CLASS_NAME);

    public BundleProxyClassLoader(Bundle bundle) {
        String methodName = "constructor";
        logger.debug("ENTERING: " + CLASS_NAME + "." + methodName);

        this.bundle = bundle;

        logger.debug("EXITING: " + CLASS_NAME + "." + methodName);
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
        logger.debug("ENTERING: " + CLASS_NAME + "." + methodName);

        return bundle.getResources(name);
    }

    public URL findResource(String name) {
        String methodName = "findResource";
        logger.debug("ENTERING: " + CLASS_NAME + "." + methodName);

        URL url = bundle.getResource(name);

        logger.debug("name = " + name + ",   url = " + url.toString());
        logger.debug("EXITING: " + CLASS_NAME + "." + methodName);

        return url;
    }

    public Class findClass(String name) throws ClassNotFoundException {
        String methodName = "findClass";
        logger.debug("ENTERING: " + CLASS_NAME + "." + methodName);

        Class clazz = bundle.loadClass(name);

        logger.debug("name = " + name + ",   clazz = " + clazz.getName());
        logger.debug("EXITING: " + CLASS_NAME + "." + methodName);

        return clazz;
    }

    public URL getResource(String name) {
        String methodName = "getResource";
        logger.debug("ENTERING: " + CLASS_NAME + "." + methodName);

        URL url = null;
        if (parent == null) {
            url = findResource(name);
        } else {
            url = super.getResource(name);
        }

        logger.debug("name = " + name + ",   url = " + url.toString());
        logger.debug("EXITING: " + CLASS_NAME + "." + methodName);

        return url;
    }

    protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        String methodName = "loadClass";
        logger.debug("ENTERING: " + CLASS_NAME + "." + methodName);
        logger.debug("name = " + name + ",   resolve = " + resolve);

        Class clazz = (parent == null) ? findClass(name) : super.loadClass(name, false);
        if (resolve) {
            logger.debug("Calling super.resolveClass()");
            super.resolveClass(clazz);
        }

        logger.debug("name = " + name + ",   clazz = " + clazz.getName());
        logger.debug("EXITING: " + CLASS_NAME + "." + methodName);

        return clazz;
    }

}
