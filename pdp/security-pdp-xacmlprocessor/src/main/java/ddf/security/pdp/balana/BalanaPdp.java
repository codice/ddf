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
package ddf.security.pdp.balana;


import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;

import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObjectFactory;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.RequestType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ResponseType;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.balana.Balana;
import org.wso2.balana.PDP;
import org.wso2.balana.PDPConfig;
import org.wso2.balana.finder.AttributeFinder;
import org.wso2.balana.finder.AttributeFinderModule;
import org.wso2.balana.finder.PolicyFinder;
import org.wso2.balana.finder.PolicyFinderModule;
import org.wso2.balana.finder.impl.CurrentEnvModule;
import org.wso2.balana.finder.impl.SelectorModule;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderFactory;

import ddf.security.pdp.api.PdpException;
import ddf.security.pdp.api.PolicyDecisionPoint;

/**
 * Balana implementation of a XACML Policy Decision Point (PDP).  This class acts as a proxy to the real
 * Balana PDP.
 */
public class BalanaPdp implements PolicyDecisionPoint
{
    private static final Logger LOGGER = LoggerFactory.getLogger( BalanaPdp.class );
    
    private static final String XACML30_NAMESPACE = "urn:oasis:names:tc:xacml:3.0:core:schema:wd-17";

    private static final String XACML_PREFIX = "xacml";
    
    private static JAXBContext jaxbContext;

    private PDP pdp;
    
    private static final long DEFAULT_POLLING_INTERVAL_IN_SECONDS = 60;
    
    private long pollingInterval = DEFAULT_POLLING_INTERVAL_IN_SECONDS;

    private Set<String> xacmlPolicyDirectories;

    private static final String NULL_DIRECTORY_EXCEPTION_MSG = "Cannot read from null XACML Policy Directory";

    /**
     * Creates the proxy to the real Balana PDP.
     * 
     * @param relativeXacmlPoliciesDirectoryPath  Relative directory path to the root of the DDF installation.
     * @throws PdpException
     */
    public BalanaPdp( String relativeXacmlPoliciesDirectoryPath ) throws PdpException
    {
    	if(relativeXacmlPoliciesDirectoryPath == null)
    	{
    		throw new PdpException(NULL_DIRECTORY_EXCEPTION_MSG);
    	}
        File xacmlPoliciesDirectory = null;

        try
        {
            xacmlPoliciesDirectory = new File( relativeXacmlPoliciesDirectoryPath ).getCanonicalFile();
        }
        catch ( IOException e )
        {
            throw new PdpException( e.getMessage(), e );
        }

        initialize(xacmlPoliciesDirectory);
    }
    

    /**
     * Creates the proxy to the real Balana PDP.
     * 
     * @param relativeXacmlPoliciesDirectoryPath  Relative directory path to the root of the DDF installation.
     * @throws PdpException
     */
    public BalanaPdp(File xacmlPoliciesDirectory  ) throws PdpException
    {
        initialize(xacmlPoliciesDirectory);
    }
    
    private void initialize( File xacmlPoliciesDirectory  ) throws PdpException
    {
    	if(xacmlPoliciesDirectory == null)
    	{
    		throw new PdpException(NULL_DIRECTORY_EXCEPTION_MSG);
    	}
    	
    	try {
    		//Only a single default directory is supported
    		//If the directory path becomes customizable this
    		//functionality should be re-evaluated
			FileUtils.forceMkdir(xacmlPoliciesDirectory);
		} catch (IOException e) {
			LOGGER.error("Unable to create directory: {}", xacmlPoliciesDirectory.getAbsolutePath());
		}
        checkXacmlPoliciesDirectory( xacmlPoliciesDirectory );
        
        createJaxbContext();

        /**
         * We currently only support one XACML policies directory, but we may support multiple directories
         * in the future.
         */
        xacmlPolicyDirectories = new HashSet<String>(1);
        xacmlPolicyDirectories.add( xacmlPoliciesDirectory.getPath() );
        createPdp( createPdpConfig( ) );
    }
    
    

    /**
     * Evaluates the XACML request and returns a XACML response.
     * 
     * @param xacmlRequestType XACML request
     * @return XACML response
     * @throws PdpException
     */
    @Override
    public ResponseType evaluate( RequestType xacmlRequestType ) throws PdpException
    {
        String xacmlRequest = this.marshal( xacmlRequestType );
        
        String xacmlResponse = this.callPdp( xacmlRequest );

        LOGGER.debug( "\nXACML 3.0 Response from Balana PDP:\n {}", xacmlResponse );
        
        DOMResult domResult = addNamespaceAndPrefixes( xacmlResponse );

        ResponseType xacmlResponseType = unmarshal( domResult );

        return xacmlResponseType;
    }
    
    /**
     * Creates a JAXB context for the XACML request and response types.
     * @throws PdpException
     */
    private void createJaxbContext() throws PdpException
    {
        try
        {
            jaxbContext = JAXBContext.newInstance( RequestType.class, ResponseType.class );
        }
        catch ( JAXBException e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new PdpException( e ); 
        }
    }
    
    
    /**
     * Creates the Balana PDP.
     */
    private void createPdp( PDPConfig pdpConfig )
    {
        LOGGER.debug( "Creating PDP of type: {}", PDP.class.getName() );
        pdp = new PDP( pdpConfig );
        LOGGER.debug( "PDP creation successful." );
    }
    
    
    /**
     * Creates the Balana PDP configuration.
     *  
     * @param xacmlPolicyDirectories he directory containing the XACML policies.
     * @return PDPConfig
     */
    private PDPConfig createPdpConfig( )
    {
        LOGGER.debug( "Creating PDP Config." );
        AttributeFinder attributeFinder = new AttributeFinder();
        List<AttributeFinderModule> attributeFinderModules = new ArrayList<AttributeFinderModule>();
        SelectorModule selectorModule = new SelectorModule();
        CurrentEnvModule currentEnvModule = new CurrentEnvModule();
        attributeFinderModules.add(selectorModule);
        attributeFinderModules.add(currentEnvModule);
        attributeFinder.setModules(attributeFinderModules);
        PDPConfig pdpConfig = new PDPConfig( attributeFinder, createPolicyFinder( xacmlPolicyDirectories ), null, false );
        
        return pdpConfig;
    }
    

    /**
     * Creates a policy finder to find XACML polices.
     * 
     * @param xacmlPolicyDirectories The directory containing the XACML policies.
     * @return PolicyFinder
     */
    private PolicyFinder createPolicyFinder( Set<String> xacmlPolicyDirectories)
    {
       LOGGER.debug( "XACML policies will be looked for in the following location(s): {}", xacmlPolicyDirectories );
       PolicyFinder policyFinder = new PolicyFinder();
       PollingPolicyFinderModule policyFinderModule = new PollingPolicyFinderModule(xacmlPolicyDirectories, pollingInterval);
       policyFinderModule.start();
       Set<PolicyFinderModule> policyFinderModules = new HashSet<PolicyFinderModule>(1);
       policyFinderModules.add( policyFinderModule );
       policyFinder.setModules( policyFinderModules );
        
       return policyFinder;
    }
    
    /**
     * Performs basic checks on the XACML policy directory.
     * 
     * @param xacmlPoliciesDirectory The directory containing the XACML policy.
     * @throws PdpException
     */
    private void checkXacmlPoliciesDirectory( File xacmlPoliciesDirectory ) throws PdpException
    {
        StringBuilder message = new StringBuilder();
        boolean errors = false;
        
        if ( !xacmlPoliciesDirectory.isDirectory() )
        {
            message.append( "The XACML policies directory " + xacmlPoliciesDirectory.getPath() + " does not exist or is not a directory.  " );
            errors = true;
        }
        
        if( !xacmlPoliciesDirectory.canRead() )
        {
            message.append( "The XACML policies directory " + xacmlPoliciesDirectory.getPath() + " is not readable.  " );
            errors = true;
        }

        if ( errors )
        {
            throw new PdpException( message.toString() );
        }
    }
    
    



    /**
     * Calls the real Balana PDP to evaluate the XACML request.
     * 
     * @param xacmlRequest The XACML request as a string.
     * @return The XACML response as a string.
     */
    private String callPdp( String xacmlRequest )
    {
        String xacmlResponse = pdp.evaluate( xacmlRequest );

        return xacmlResponse;
    }


    /**
     * Adds namespaces and namespace prefixes to the XACML response returned by the Balana PDP.  The Balana
     * PDP returns a response with no namespaces, so we need to add them to unmarshal the response.
     * 
     * @param xacmlResponse The XACML response as a string.
     * @return DOM representation of the XACML response with namespaces and namespace prefixes.
     * @throws PdpException
     */
    private DOMResult addNamespaceAndPrefixes( String xacmlResponse ) throws PdpException
    {
        XMLReader xmlReader = null;

        try
        {
            xmlReader = new XMLFilterImpl( XMLReaderFactory.createXMLReader() )
            {
                @Override
                public void startElement( String uri, String localName, String qName, Attributes attributes ) throws SAXException
                {
                    super.startElement( XACML30_NAMESPACE, localName, XACML_PREFIX + ":" + qName, attributes );
                }


                @Override
                public void endElement( String uri, String localName, String qName ) throws SAXException
                {
                    super.endElement( XACML30_NAMESPACE, localName, XACML_PREFIX + ":" + qName );
                }
            };
        }
        catch ( SAXException e )
        {
            String message = "Unable to read XACML response:\n" + xacmlResponse;
            LOGGER.error( message );
            throw new PdpException( message, e );
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        DOMResult domResult = new DOMResult();

        try
        {
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform( new SAXSource( xmlReader, new InputSource( new StringReader( xacmlResponse ) ) ), domResult );
        }
        catch ( TransformerException e )
        {
            String message = "Unable to transform XACML response:\n" + xacmlResponse;
            LOGGER.error( message );
            throw new PdpException( message, e );
        }

        return domResult;
    }


    /**
     * Marshalls the XACML request to a string.
     * 
     * @param xacmlRequestType The XACML request to marshal.
     * @return A string representation of the XACML request.
     */
    private String marshal( RequestType xacmlRequestType ) throws PdpException
    {
        ObjectFactory objectFactory = new ObjectFactory();
        Writer writer = new StringWriter();
        JAXBElement<RequestType> xacmlRequestTypeElement = objectFactory.createRequest( xacmlRequestType );

        Marshaller marshaller = null;
        String xacmlRequest = null;
        
        try
        {
            marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal( xacmlRequestTypeElement, writer );

           xacmlRequest = writer.toString();
        }
        catch ( JAXBException e )
        {
            String message = "Unable to marshal XACML request.";
            LOGGER.error( message );
            throw new PdpException( message, e );
        }

        LOGGER.debug( "\nXACML 3.0 Request:\n{}", xacmlRequest );
        
        return xacmlRequest;
    }


    /**
     * Unmarshalls the XACML response.
     * 
     * @param xacmlResponse The XACML response with all namespaces and namespace prefixes added.
     * @return The XACML response.
     * @throws PdpException
     */
    private ResponseType unmarshal( DOMResult xacmlResponse ) throws PdpException
    {
        JAXBElement<ResponseType> xacmlResponseTypeElement = null;

        try
        {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            xacmlResponseTypeElement = unmarshaller.unmarshal( xacmlResponse.getNode(), ResponseType.class );
        }
        catch ( JAXBException e )
        {
            String message = "Unable to unmarshal XACML response.";
            LOGGER.error( message );
            throw new PdpException( message, e );
        }

        return xacmlResponseTypeElement.getValue();
    }

    public long getPollingInterval() {
        return pollingInterval;
    }

    /**
     * Sets the pollingInterval. The PDP configuration
     * is reset as a result to adjust to the new interval
     * @param pollingInterval - in seconds
     */
    public void setPollingInterval(long pollingInterval) {
        this.pollingInterval = pollingInterval;
      
        createPdp( createPdpConfig( ) );

    }
}
