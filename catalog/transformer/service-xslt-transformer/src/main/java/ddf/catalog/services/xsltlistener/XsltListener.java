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


import org.ops4j.pax.swissbox.extender.BundleWatcher;
import org.osgi.framework.BundleContext;

import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transform.QueryResponseTransformer;

public class XsltListener
{
	private static final String OSGI_INF_DDF_XSLT_METACARD_TRANSFORMER = "/OSGI-INF/ddf/xslt-metacard-transformer/";
	private static final String OSGI_INF_DDF_XSLT_RESPONSE_QUEUE_TRANSFORMER = "/OSGI-INF/ddf/xslt-response-queue-transformer/";
	private BundleWatcher<String> metacardBundleWatcher;
	private BundleWatcher<String> responseQueueBundleWatcher;
	
	public XsltListener(BundleContext bundleContext)
	{
		this.metacardBundleWatcher = new XsltBundleWatcher<XsltMetacardTransformer>(bundleContext, XsltMetacardTransformer.class, MetacardTransformer.class.getName(), OSGI_INF_DDF_XSLT_METACARD_TRANSFORMER);
		this.responseQueueBundleWatcher = new XsltBundleWatcher<XsltResponseQueueTransformer>(bundleContext, XsltResponseQueueTransformer.class, QueryResponseTransformer.class.getName(), OSGI_INF_DDF_XSLT_RESPONSE_QUEUE_TRANSFORMER);
		
		this.metacardBundleWatcher.start();
		this.responseQueueBundleWatcher.start();
	}
}
