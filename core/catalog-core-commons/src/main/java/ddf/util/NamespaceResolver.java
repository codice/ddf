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
package ddf.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

/**
 * DDF namespace resolver loads all registered NamespaceMap interfaces and builds two HashMaps: one
 * for namespace URIs to prefixes, and one for namespace prefixes to URIs. If there are multiple
 * entries found for a single URI or prefix, the last one loaded from the OSGi Service Registry will
 * be the one used.
 * 
 * @author Hugh Rodgers
 */
public class NamespaceResolver implements NamespaceContext {
    private static XLogger logger = new XLogger(LoggerFactory.getLogger(NamespaceResolver.class));

    private BundleContext bundleContext;

    protected ArrayList<NamespaceContext> namespaceContexts;

    /**
     * DDF namespace resolver loads all registered NamespaceMap interfaces and builds two HashMaps:
     * one for namespace URIs to prefixes, and one for namespace prefixes to URIs. If there are
     * multiple entries found for a single URI or prefix, the last one loaded from the OSGi Service
     * Registry will be the one used.
     */
    public NamespaceResolver() {
        logger.trace("ENTERING: NamespaceResolver constructor");

        logger.trace("EXITING: NamespaceResolver constructor");
    }

    /**
     * Retrieve the namespace URI for the given namespace prefix. Prefix is retrieved from the list
     * of namespace URIs and prefixes mapped from NamespaceMap entries in the OSGi Service Registry.
     * 
     * @parameter prefix the namespace prefix to look up the URI for
     * 
     * @return the namespace URI for the given namespace prefix
     */
    public String getNamespaceURI(String prefix) {
        String methodName = "getNamespaceURI";
        logger.trace("ENTERING: " + methodName);

        getNamespaceContexts();

        String namespaceUri = null;

        for (NamespaceContext nc : namespaceContexts) {
            namespaceUri = nc.getNamespaceURI(prefix);
            if (namespaceUri != null)
                break;
        }

        logger.trace("EXITING: " + methodName + "    (namespaceUri = " + namespaceUri + ")");

        return namespaceUri;
    }

    /**
     * Retrieve the namespace prefix for the given namespace URI. URI is retrieved from the list of
     * namespace URIs and prefixes mapped from NamespaceMap entries in the OSGi Service Registry.
     * 
     * @parameter namespace the namespace URI to look up the prefix for
     * 
     * @return the namespace prefix for the given namespace URI
     */
    public String getPrefix(String namespace) {
        String methodName = "getPrefix";
        logger.trace("ENTERING: " + methodName + ",   namespace = " + namespace);

        getNamespaceContexts();

        String prefix = null;

        for (NamespaceContext nc : namespaceContexts) {
            prefix = nc.getPrefix(namespace);
            if (prefix != null)
                break;
        }

        logger.trace("EXITING: " + methodName + "    (prefix = " + prefix + ")");

        return prefix;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.namespace.NamespaceContext#getPrefixes(java.lang.String)
     */
    public Iterator getPrefixes(String namespace) {
        return null;
    }

    private void getNamespaceContexts() {
        // Determine the OSGi bundle context for the NamespaceResolver
        if (this.bundleContext == null) {
            logger.debug("Setting bundleContext");
            this.bundleContext = BundleReference.class.cast(this.getClass().getClassLoader())
                    .getBundle().getBundleContext();
        }

        namespaceContexts = new ArrayList<NamespaceContext>();

        if (bundleContext != null) {
            ServiceReference[] refs = null;
            try {
                // Retrieve all of the namespace mappings from the OSGi Service Registry
                refs = bundleContext.getServiceReferences(NamespaceContext.class.getName(), null);
                logger.debug("num NamespaceContexts service refs found = " + refs.length);
            } catch (InvalidSyntaxException e) {
                logger.warn("Invalid NamespaceContext syntax", e);
            }

            // If no NamespaceMaps found, nothing further to be done
            if (refs == null || refs.length == 0) {
                logger.warn("No NamespaceContext services found");
            } else {
                // For each NamespaceMap found, add its namespace mappings to the two HashMaps
                // maintained for prefix-to-uri and uri=to-prefix
                for (ServiceReference ref : refs) {
                    NamespaceContext namespaceContext = (NamespaceContext) bundleContext
                            .getService(ref);
                    if (namespaceContext != null) {
                        namespaceContexts.add(namespaceContext);
                    } else {
                        logger.debug("NamespaceContext for ServiceReference was null");
                    }
                }
            }
        } else {
            logger.warn("BundleContext is NULL");
        }
    }
}
