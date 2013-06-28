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
package ddf.security.sts.claimsHandler;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.util.Map;

import org.junit.Test;


public class AttributeMapLoaderTest
{

    private static final String BAD_KEY = "BAD_KEY";
    private static final String MAP_FILE = "testMap.properties";
    private static final String attributeString = "[http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier=uid, http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress=mail, http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname=sn, http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname=givenName]";

    /**
     * Tests loading the attributes from a file.
     * 
     * @throws FileNotFoundException
     */
    @Test
    public void testAttributeFile() throws FileNotFoundException
    {
        Map<String, String> returnedMap = AttributeMapLoader.buildClaimsMapFile(MAP_FILE);
        assertEquals("uid", returnedMap.get("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier"));
        assertTrue(returnedMap.containsKey("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role"));
        assertFalse(returnedMap.containsKey(BAD_KEY));
    }

    /**
     * Tests loading the attributes from a string.
     */
    @Test
    public void testAttributeString()
    {
        Map<String, String> returnedMap = AttributeMapLoader.buildClaimsMap(attributeString);
        assertEquals(returnedMap.get("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier"), "uid");
        // took role out of string
        assertFalse(returnedMap.containsKey("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role"));
        assertFalse(returnedMap.containsKey(BAD_KEY));
    }
}
