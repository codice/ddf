/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.services.xsltlistener;

import org.ops4j.pax.swissbox.extender.BundleWatcher;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XsltBundleWatcher<T extends AbstractXsltTransformer> extends BundleWatcher<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(XsltBundleWatcher.class);

    @SuppressWarnings("unchecked")
    public XsltBundleWatcher(BundleContext bundleContext, Class<T> transformerClass,
            String publishedInterface, String entryPath) {
        super(bundleContext, new XsltBundleScanner(entryPath),
                new XsltBundleObserver<T>(bundleContext, transformerClass, publishedInterface));

        LOGGER.debug("XsltBundleWatcher constructor.");
    }

}