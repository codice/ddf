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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.log4j.BasicConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;


public class SecurityPropertyInterceptorTest
{

    private static final String KEY1 = "key1";
    private static final String KEY2 = "key2";
    private static final String VALUE1 = "value1";
    private static final String VALUE2 = "value2";
    private static final String ENTRY1 = KEY1 + "=" + VALUE1;
    private static final String ENTRY2 = KEY2 + "=" + VALUE2;

    @BeforeClass
    public static void setUp()
    {
        BasicConfigurator.configure();
    }

    @Test
    public void handleMessageTest()
    {
        WSSecurityPropertiesImpl properties = new WSSecurityPropertiesImpl();
        ArrayList<String> propertyList = new ArrayList<String>();
        propertyList.add(ENTRY1);
        propertyList.add(ENTRY2);
        properties.setSecurityPropList(propertyList);
        WSSecurityPropertyInterceptor interceptor = new WSSecurityPropertyInterceptor();
        interceptor.setProperties(properties);
        assertEquals(properties, interceptor.getProperties());
        Message message = new MessageImpl();
        // verify message is empty
        assertNull(message.getContextualProperty(KEY1));
        assertNull(message.getContextualProperty(KEY2));
        message.setExchange(null);
        interceptor.handleMessage(message);
        // check for set values
        assertEquals(VALUE1, message.getContextualProperty(KEY1));
        assertEquals(VALUE2, message.getContextualProperty(KEY2));
    }
}
