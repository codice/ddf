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
package ddf.security.ws.policy.impl;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import ddf.security.ws.policy.impl.FilePolicyLoader;
import ddf.security.ws.policy.impl.PolicyWSDLGetInterceptor;


/**
 * Tests the policy adding logic (loading policies and converting the WSDLs).
 * 
 */
public class PolicyTest
{

    private static BundleContext mockContext;
    private static Bundle mockBundle;

    private static final String POLICY_LOCATION = "/policies/ddf_sample_policy.xml";
    private static final String BAD_POLICY_LOCATION = "/policies/bad_policy.xml";
    private static final String TXT_POLICY_LOCATION = "/policies/notXmlPolicy.txt";
    private static final String WSDL_LOCATION = "/wsdl/w3c_example.wsdl";
    
    private Logger logger = LoggerFactory.getLogger(PolicyTest.class);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void setup()
    {
        mockContext = mock(BundleContext.class);
        mockBundle = mock(Bundle.class);
        when(mockBundle.getResource(anyString())).thenAnswer(new Answer<URL>()
        {
            @Override
            public URL answer( InvocationOnMock invocation ) throws Throwable
            {
                Object[] args = invocation.getArguments();
                return (URL) this.getClass().getResource((String) args[0]);
            }
        });
        when(mockContext.getBundle()).thenReturn(mockBundle);
    }

    @Test
    public void filePolicyLoaderTest()
    {
        try
        {
            FilePolicyLoader loader = new FilePolicyLoader(mockContext, POLICY_LOCATION);
            assertNotNull(loader.getPolicy());
        }
        catch (Exception e)
        {
            logger.error("Exception while loading policy: ", e);
            fail("Exception while loading policy: " + e.getMessage());

        }
    }

    @Test
    public void policyWSDLTest()
    {
        try
        {
            FilePolicyLoader loader = new FilePolicyLoader(mockContext, POLICY_LOCATION);
            FilePolicyLoader wsdlLoader = new FilePolicyLoader(mockContext, WSDL_LOCATION);
            assertNotNull(loader.getPolicy());
            assertNotNull(wsdlLoader.getPolicy());
            PolicyWSDLGetInterceptor interceptor = new PolicyWSDLGetInterceptor(loader);
            Node combinedNode = interceptor.addPolicyToWSDL(wsdlLoader.getPolicy(), loader.getPolicy());
            assertNotNull(combinedNode);
            assertFalse(combinedNode.equals(wsdlLoader.getPolicy()));
            assertFalse(combinedNode.equals(loader.getPolicy()));
        }
        catch (Exception e)
        {
            logger.error("Exception while combining policy: ", e);
            fail("Exception while combining policy " + e.getMessage());

        }
    }

    @Test
    public void badFileLocationTest()
    {
        try
        {
            new FilePolicyLoader(mockContext, BAD_POLICY_LOCATION);
            fail("Should have thrown an exception when passed in a bad file location.");
        }
        catch (IOException ioe)
        {
            // Exception successfully thrown.
        }
    }

    @Test
    public void notXmlFile()
    {
        try
        {
            new FilePolicyLoader(mockContext, TXT_POLICY_LOCATION);
            fail("Should have thrown an exception when passed in a non-xml file.");
        }
        catch (IOException ioe)
        {
            // Exception successfully thrown.
        }
    }

}
