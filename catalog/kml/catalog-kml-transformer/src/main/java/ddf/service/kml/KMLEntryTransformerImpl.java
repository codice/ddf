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

package ddf.service.kml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.Configuration;
import net.sf.saxon.TransformerFactoryImpl;
import net.sf.saxon.trans.DynamicLoader;

import org.apache.log4j.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import ddf.catalog.Constants;
import ddf.catalog.data.Metacard;
import ddf.catalog.transform.MetacardTransformer;

/**
 * This class is meant to be used by clients who want to override the base default
 * transformation
 * 
 * @author abarakat
 * 
 */
public class KMLEntryTransformerImpl implements KMLEntryTransformer {

	private static final TransformerFactory tFactory = TransformerFactory.newInstance( net.sf.saxon.TransformerFactoryImpl.class.getName(),
			net.sf.saxon.TransformerFactoryImpl.class.getClassLoader() );
	
	private Templates templates;
	private String styling;

	private static final Logger logger = Logger.getLogger(KMLEntryTransformerImpl.class);
	private BundleContext context;

	public KMLEntryTransformerImpl(final Bundle bundle, String xsltFileName) {
		this(bundle, xsltFileName, "");
	}

	public KMLEntryTransformerImpl(final Bundle bundle, String xsltFileName, String styleFileName) {

		context = bundle.getBundleContext();
		
		// Retrieve the styling information
		if (styleFileName.equals("")) {
			this.styling = "";
		} else {

			URL stylingUrl = bundle.getResource(styleFileName);
			
			logger.debug("The stylingURL: " +  stylingUrl) ;

			try {
				this.styling = KMLTransformerImpl.extractStringFrom(stylingUrl);
			} catch (IOException e) {
				logger.warn("Could not retrieve styling file: " + stylingUrl, e);
				this.styling = "";
			}
		}

		// initialize TransformerFactory if not already done
		/*if (tFactory == null) {
			tFactory = TransformerFactory.newInstance();
		}*/

		// Retrieve the xslt
		URL xsltUrl = bundle.getResource(xsltFileName);
		Source xsltSource = new StreamSource(xsltUrl.toString());

		// Build the resolver to resolve any address for those who call base xlsts from extension bundles
		try {
			URIResolver resolver = new URIResolver() {

				@Override
				public Source resolve(String href, String base) throws TransformerException {

					try {
						URL resourceAddressURL = bundle.getResource(href);
						String resourceAddress = resourceAddressURL.toString();
						logger.info("Resolved resource address:" + resourceAddress);

						return new StreamSource(resourceAddress);
					} catch (Exception e) {
						return null ;
					}

				}
			};

			tFactory.setURIResolver(resolver);
			Configuration config = ((TransformerFactoryImpl)tFactory).getConfiguration() ;
			DynamicLoader dynamicLoader = new DynamicLoader() ;
			dynamicLoader.setClassLoader(new BundleProxyClassLoader(bundle)) ;
			config.setDynamicLoader(dynamicLoader) ;
			// Precompile this template 
			this.templates = tFactory.newTemplates(xsltSource);

		} catch (TransformerConfigurationException e) {
			logger.error("Couldn't create transfomer", e);
		}
	}

	@Override
	public String getKMLContent(Metacard metacard, Map<String, Serializable> arguments) {

		String entryDocument = metacard.getMetadata();

		ByteArrayOutputStream resultOS = new ByteArrayOutputStream();
		StringReader entryDocumentReader = new StringReader(entryDocument);
		List<String> serviceList = new ArrayList<String>();

		try {

			StreamResult result = new StreamResult(resultOS);
			Transformer transformer = templates.newTransformer();
			transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes") ;
			transformer.setParameter("id", metacard.getId());
			transformer.setParameter("site", metacard.getSourceId()); 

			for (Map.Entry<String,Serializable> entry : arguments.entrySet())
			{
				transformer.setParameter(entry.getKey(), entry.getValue());
			}
			
			
			ServiceReference[] refs = null;
	        try
			{
				refs = context.getServiceReferences(MetacardTransformer.class.getName(), null);
			} catch (InvalidSyntaxException e)
			{
				//can't happen because filter is null
			}
			
			if (refs != null)
			{
				for (ServiceReference  ref : refs)
				{
					if (ref != null)
					{
						String title = null;
						String shortName = (String) ref.getProperty(Constants.SERVICE_SHORTNAME);
						
						if((title = (String) ref.getProperty(Constants.SERVICE_TITLE)) == null)
						{
							title = "View as " + shortName.toUpperCase();	
						}
												
						String url = "/services/catalog/" + metacard.getId() + "?transform=" + shortName;
				        
				        //define the services
				        serviceList.add(title);
				        serviceList.add(url);
					}
				}
			}
	        
			//pass in the list of server-side services
			transformer.setParameter("services", serviceList);
			
			
			
			transformer.transform(new StreamSource(entryDocumentReader), result);

			return ((ByteArrayOutputStream) (result.getOutputStream())).toString();

		} catch (TransformerException te) {

			logger.warn("TransformerException", te);
		}

		return null;
	}

	@Override
	public String getKMLStyle() {
		return this.styling;
	}

}
