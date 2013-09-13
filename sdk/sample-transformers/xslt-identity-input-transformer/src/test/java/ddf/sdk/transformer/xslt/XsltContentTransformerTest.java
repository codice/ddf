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
package ddf.sdk.transformer.xslt;

import java.io.InputStream;

import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.tika.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

public class XsltContentTransformerTest extends CamelBlueprintTestSupport {
    // override this method, and return the location of our Blueprint XML file to be used for
    // testing
    @Override
    protected String getBlueprintDescriptor() {
        return "OSGI-INF/blueprint/blueprint.xml";
    }

    @Test
    @Ignore("TODO: How to get MimeTypeToTransformerMapper to inject")
    public void testRoute() throws Exception {
        // set mock expectations
        getMockEndpoint("mock:result").expectedMessageCount(1);

        // send a message
        String testData = "<tacrep><data>Hello Hugh</data></tacrep>";
        InputStream input = IOUtils.toInputStream(testData);
        template.sendBody("direct:start", input);

        // assert mocks
        assertMockEndpointsSatisfied();
    }
}
