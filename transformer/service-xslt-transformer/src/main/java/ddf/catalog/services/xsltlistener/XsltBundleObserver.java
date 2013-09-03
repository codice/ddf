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

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.ops4j.pax.swissbox.extender.BundleObserver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import ddf.catalog.Constants;

public class XsltBundleObserver<T extends AbstractXsltTransformer> implements BundleObserver<String>
{
	private Class<T> transformerClass;
	private String publishedInterface;
	private Map<Bundle, List<ServiceRegistration>> serviceRegistrationMap;
	private BundleContext bundleContext;
	private static Logger logger = Logger.getLogger(XsltBundleObserver.class);
	
	public XsltBundleObserver(BundleContext bundleContext, Class<T> transformerClass, String publishedInterface)
	{
		this.transformerClass = transformerClass;
		this.publishedInterface = publishedInterface;
		
		this.bundleContext = bundleContext;
		this.serviceRegistrationMap = new ConcurrentHashMap<Bundle, List<ServiceRegistration>>();
	}
	
	@Override
	public void addingEntries(Bundle bundle, List<String> resources)
	{
		for (String fileName : resources)
		{
			// extract the format from the file name
			File file = new File(fileName);
			String format = file.getName().substring(0, file.getName().lastIndexOf("."));
			Hashtable<String, String> properties = new Hashtable<String, String>();

			logger.debug("Found started bundle with name: " + fileName);

			// setup the properties for the service
			properties.put(Constants.SERVICE_SHORTNAME, format);
			properties.put(Constants.SERVICE_TITLE, "View as " + (format.length() > 4 ? capitalize(format) : format.toUpperCase()) + "...");
			properties.put(Constants.SERVICE_DESCRIPTION, "Transforms query results into " + format);

			// define a transformer object that points to the xsl
			T xmt = null;
			try
			{
				xmt = transformerClass.newInstance();
				xmt.init(bundle, fileName);
			} catch (InstantiationException e)
			{
				logger.debug(e);
				continue;
			} catch (IllegalAccessException e)
			{
				logger.debug(e);
				continue;
			}

			// register the service
			ServiceRegistration sr = bundleContext.registerService(publishedInterface, xmt, properties);

			// store the service registration object
			if (serviceRegistrationMap.containsKey(bundle))
			{
				// if it's already in the map, add the sr to the appropriate
				// list
				serviceRegistrationMap.get(bundle).add(sr);
			} else
			{
				// if it's not in the map, make the initial list and put it in
				// the map
				List<ServiceRegistration> srList = new ArrayList<ServiceRegistration>();
				srList.add(sr);
				serviceRegistrationMap.put(bundle, srList);
			}
		}
		
	}

	@Override
	public void removingEntries(Bundle bundle, List<String> resources)
	{
		List<ServiceRegistration> srList = serviceRegistrationMap.get(bundle);
		for (ServiceRegistration sr : srList)
		{
			if (logger.isDebugEnabled())
			{
				logger.debug(sr.getReference().getBundle().getSymbolicName() + " bundle uninstalled and unregistered.");
			}
			sr.unregister();
		}

		serviceRegistrationMap.remove(bundle);
		
	}
	
	private String capitalize(String format)
	{
		if (format.length() == 0)
			return format;
		return new StringBuilder(format.substring(0, 1).toUpperCase()).append(format.substring(1)).toString();
	}

}
