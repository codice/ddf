/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.data.dynamic.registry;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

import org.junit.Test;

import junit.framework.TestCase;

import ddf.catalog.data.dynamic.api.MetacardFactory;
import ddf.catalog.data.dynamic.api.MetacardPropertyDescriptor;
import ddf.catalog.data.dynamic.impl.MetacardFactoryImpl;

public class MetacardTypeReaderTest extends TestCase {

    @Test
    public void testParseMetacardDefinition() throws Exception {
        MetacardFactory mf = new MetacardFactoryImpl();
        MetacardTypeReader reader = new MetacardTypeReader();
        reader.setMetacardFactory(mf);
        InputStream is = getClass().getClassLoader().getResourceAsStream("TestMetacard.xml");
        assertTrue(reader.parseMetacardDefinition(is));

        MetacardPropertyDescriptor[] descriptors = mf.getMetacardPropertyDescriptors("nitf");
        assertEquals(51, descriptors.length);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseMetacardDefinitionNullInput() throws Exception {
        MetacardFactory mf = new MetacardFactoryImpl();
        MetacardTypeReader reader = new MetacardTypeReader();
        reader.setMetacardFactory(mf);
        assertFalse(reader.parseMetacardDefinition(null));
    }

    @Test
    public void testParseMetacardDefinitionBadFile() throws Exception {
        InputStream is = new ByteArrayInputStream("not an xml file".getBytes());
        MetacardFactory mf = new MetacardFactoryImpl();
        MetacardTypeReader reader = new MetacardTypeReader();
        reader.setMetacardFactory(mf);
        assertFalse(reader.parseMetacardDefinition(is));
    }

    @Test
    public void testRegisterMetacard() throws Exception {
        MetacardFactory mf = new MetacardFactoryImpl();
        URL url = getClass().getClassLoader().getResource("TestMetacard.xml");
        MetacardTypeReader reader = new MetacardTypeReader();
        reader.setMetacardFactory(mf);

        assertTrue(reader.registerMetacard(new File(url.getFile())));

        MetacardPropertyDescriptor[] descriptors = mf.getMetacardPropertyDescriptors("nitf");
        assertEquals(51, descriptors.length);
    }

}