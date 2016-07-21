/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.metacard.security;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.HashMap;

import org.apache.shiro.subject.PrincipalCollection;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.osgi.service.cm.ConfigurationAdmin;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;

public class DefaultSecurityAttributeValuesPluginTest {

    private DefaultSecurityAttributeValuesPlugin defaultSecurityAttributeValuesPlugin;

    @Mock
    private Metacard unmarkedMetacard;

    @Mock
    private Metacard markedMetacard;

    @Mock
    private Result unmarkedResult;

    @Mock
    private Result markedResult;

    @Mock
    private Attribute emptySecurityAttributeList;

    @Mock
    private Attribute securityAttributeList;

    @Mock
    private HashMap<String, Object> mockedEmptyAttributeMap;

    @Mock
    private HashMap<String, Object> mockedFilledAttributeMap;

    @Mock
    private Subject mockedSubject;

    @Mock
    private ConfigurationAdmin configurationAdmin;

    @Mock
    private PrincipalCollection mockedPrincipalCollection;

    @Mock
    private SecurityAssertion mockedAssertion;

    @Mock
    private AttributeStatement mockedAttributeStatement1;

    @Mock
    private org.opensaml.saml.saml2.core.Attribute mockedAttribute1;

    @Mock
    private AttributeStatement mockedAttributeStatement2;

    @Mock
    private org.opensaml.saml.saml2.core.Attribute mockedAttribute2;

    @Mock
    private XSString mockedXss1;

    @Mock
    private XSString mockedXss2;

    @Mock
    private MetacardType metacardType;

    @Mock
    private AttributeDescriptor attrivuteDescriptor;

    @Before
    public void setup() {
        initMocks(this);
        defaultSecurityAttributeValuesPlugin = spy(new DefaultSecurityAttributeValuesPlugin());
        doReturn(mockedSubject).when(defaultSecurityAttributeValuesPlugin)
                .getSystemSubject();
        when(mockedSubject.getPrincipals()).thenReturn(mockedPrincipalCollection);
        when(mockedPrincipalCollection.oneByType(anyObject())).thenReturn(mockedAssertion);
        when(mockedAssertion.getAttributeStatements()).thenReturn(Arrays.asList(
                mockedAttributeStatement1,
                mockedAttributeStatement2));
        when(mockedAttributeStatement1.getAttributes()).thenReturn(Arrays.asList(mockedAttribute1));
        when(mockedAttributeStatement2.getAttributes()).thenReturn(Arrays.asList(mockedAttribute2));
        when(mockedXss1.getValue()).thenReturn("1");
        when(mockedXss2.getValue()).thenReturn("2");
        when(mockedAttribute1.getAttributeValues()).thenReturn(Arrays.asList(mockedXss1));
        when(mockedAttribute2.getAttributeValues()).thenReturn(Arrays.asList(mockedXss2));
        when(mockedAttribute1.getName()).thenReturn("UserAttrib1");
        when(mockedAttribute2.getName()).thenReturn("UserAttrib2");
        defaultSecurityAttributeValuesPlugin.setMetacardMarkingMappings(Arrays.asList(
                "UserAttrib1=metacardAttrib1",
                "UserAttrib2=metacardAttrib1",
                "UserAttrib2=metacardAttrib2"));

        when(markedMetacard.getAttribute("metacardAttrib1")).thenReturn(
                securityAttributeList);
        when(unmarkedMetacard.getMetacardType()).thenReturn(metacardType);
        when(metacardType.getAttributeDescriptor(any(String.class))).thenReturn(attrivuteDescriptor);
    }

    @Test
    public void testProcessUnmarkedMetacard() throws Exception {
        when(emptySecurityAttributeList.getValue()).thenReturn(mockedEmptyAttributeMap);
        when(mockedEmptyAttributeMap.isEmpty()).thenReturn(true);
        defaultSecurityAttributeValuesPlugin.addDefaults(unmarkedMetacard);
        verify(unmarkedMetacard, times(2)).setAttribute(any(AttributeImpl.class));
    }

    @Test
    public void testProcessMarkedMetacard() throws Exception {
        when(emptySecurityAttributeList.getValue()).thenReturn(mockedEmptyAttributeMap);
        when(mockedEmptyAttributeMap.isEmpty()).thenReturn(true);
        defaultSecurityAttributeValuesPlugin.addDefaults(markedMetacard);
        verify(markedMetacard, times(0)).setAttribute(any(AttributeImpl.class));
    }

}