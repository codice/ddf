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
package ddf.catalog.services.xsltlistener;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.log4j.Logger;
import org.ops4j.pax.swissbox.extender.BundleScanner;
import org.osgi.framework.Bundle;

public class XsltBundleScanner implements BundleScanner<String>
{
	private String entryPath;
	private static Logger logger = Logger.getLogger(XsltBundleScanner.class);
	
	public XsltBundleScanner(String entryPath)
	{
		super();
		
		this.entryPath = entryPath;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public List<String> scan(Bundle bundle)
	{
		//logger.debug("Entering XsltBundleScanner.scan().");
		// find bundles that use the xslt listener
		
		List<String> resources = new ArrayList<String>();

		Enumeration<String> bundleEnum;
		String fileName;
		bundleEnum = bundle.getEntryPaths(entryPath);

		if (logger.isDebugEnabled())
		{
			if (bundleEnum != null)
			{
				//logger.debug("Found " + (bundleEnum.hasMoreElements() ? "some" : "no") + " resources of path: " + entryPath);
			}
			else
			{
				//logger.debug("Found no resources of path: " + entryPath);
			}
		}
		
		while (bundleEnum != null && bundleEnum.hasMoreElements())
		{
			fileName = bundleEnum.nextElement();

			// if non-xsl/xslt files are found, ignore them
			if (fileName == null || (!fileName.endsWith(".xsl") && !fileName.endsWith(".xslt")))
			{
				continue;
			}
			
			resources.add(fileName);
		}
		
		return resources;
	}

}
