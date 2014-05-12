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
package ddf.catalog.services.xsltlistener;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.ops4j.pax.swissbox.extender.BundleScanner;
import org.osgi.framework.Bundle;

public class XsltBundleScanner implements BundleScanner<String> {
    private String entryPath;

    public XsltBundleScanner(String entryPath) {
        super();

        this.entryPath = entryPath;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> scan(Bundle bundle) {
        // find bundles that use the xslt listener

        List<String> resources = new ArrayList<String>();

        Enumeration<String> bundleEnum;
        String fileName;
        bundleEnum = bundle.getEntryPaths(entryPath);

         while (bundleEnum != null && bundleEnum.hasMoreElements()) {
            fileName = bundleEnum.nextElement();

            // if non-xsl/xslt files are found, ignore them
            if (fileName == null || (!fileName.endsWith(".xsl") && !fileName.endsWith(".xslt"))) {
                continue;
            }

            resources.add(fileName);
        }

        return resources;
    }

}
