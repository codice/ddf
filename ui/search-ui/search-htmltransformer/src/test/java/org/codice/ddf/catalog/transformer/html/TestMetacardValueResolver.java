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
package org.codice.ddf.catalog.transformer.html;

import static org.junit.Assert.assertEquals;

import javax.xml.bind.DatatypeConverter;

import org.junit.Test;

import com.github.jknack.handlebars.ValueResolver;

import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;

public class TestMetacardValueResolver {


    @Test
    public void testResolveGeometry() {
        MetacardImpl mc = new MetacardImpl();
        String expected = "POINT(10 5)";
        mc.setLocation(expected);
        
        MetacardValueResolver mvr = new MetacardValueResolver();
        Object actual = mvr.resolve(mc, "geometry");
        assertEquals(expected, actual);
    }
    
    @Test
    public void testResolveSourceId() {
        MetacardImpl mc = new MetacardImpl();
        String expected = "src";
        mc.setSourceId(expected);
        
        MetacardValueResolver mvr = new MetacardValueResolver();
        Object props = mvr.resolve(mc, "properties");
        Object actual = mvr.resolve(props, "source-id");
        assertEquals(expected, actual);        
    }
    
    @Test
    public void testResolveThumbnail() {
        MetacardImpl mc = new MetacardImpl();
        byte[] bytes = new byte[] {97, 98, 99};
        String expected = DatatypeConverter.printBase64Binary(bytes);
        mc.setThumbnail(bytes);
        
        MetacardValueResolver mvr = new MetacardValueResolver();
        Object props = mvr.resolve(mc, "properties");
        Object actual = mvr.resolve(props, "thumbnail");
        assertEquals(expected, actual);        
    }
    
    @Test
    public void testResolveType() {
        MetacardImpl mc = new MetacardImpl();
        String expected = "feature";
        MetacardType expectedType = new MetacardTypeImpl(expected, null);
        mc.setType(expectedType);
        
        MetacardValueResolver mvr = new MetacardValueResolver();
        Object props = mvr.resolve(mc, "properties");
        Object actual = mvr.resolve(props, "type");
        assertEquals(expected, actual);
    }
    
    @Test
    public void testResolveProperty() {
        MetacardImpl mc = new MetacardImpl();
        String expectedId = "id";
        String expectedType = "content";
        mc.setId(expectedId);
        mc.setContentTypeName(expectedType);
        
        MetacardValueResolver mvr = new MetacardValueResolver();
        Object props = mvr.resolve(mc, "properties");
        Object actualId = mvr.resolve(props, "id");
        assertEquals(expectedId, actualId);

        Object actualType = mvr.resolve(props, "metadata-content-type");
        assertEquals(expectedType, actualType);
    }

    @Test
    public void testResolveUnknown() {
        String unknown = "unknown";
        MetacardValueResolver mvr = new MetacardValueResolver();
        Object actual = mvr.resolve(unknown, "anything");
        assertEquals(ValueResolver.UNRESOLVED, actual);
    }

    @Test
    public void testResolveUnknownProperty() {
        String unknown = "unknownProp";
        MetacardImpl mc = new MetacardImpl();
        MetacardValueResolver mvr = new MetacardValueResolver();
        Object props = mvr.resolve(mc, "properties");
        Object actual = mvr.resolve(props, unknown);
        assertEquals(ValueResolver.UNRESOLVED, actual);
        
    }
}
