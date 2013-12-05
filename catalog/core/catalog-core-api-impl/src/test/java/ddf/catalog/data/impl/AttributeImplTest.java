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
package ddf.catalog.data.impl;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.StreamCorruptedException;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.impl.AttributeImpl;

public class AttributeImplTest {
    AttributeImpl toTest;

    @Before
    public void setup() {
        toTest = new AttributeImpl("id", UUID.randomUUID().toString());
    }

    @Test
    public void testCreateImplementation() {
        assertEquals("id", toTest.getName());
        assertNotNull(toTest.getValue().toString());
    }

    @Test
    public void testValueRemoval() {
        toTest.clearValues();
        assertNull(toTest.getValue());
        toTest.addValue(1);
        assertEquals(1, toTest.getValue());
    }

    @Test
    public void testSerializationSingle() throws IOException, ClassNotFoundException {
        Attribute read = serializationLoop(toTest);
        assertEquals(toTest.getName(), read.getName());
        assertEquals(toTest.getValue(), read.getValue());
        assertEquals(toTest.getValues(), read.getValues());

    }

    private Attribute serializationLoop(Attribute toSerialize) throws FileNotFoundException,
        IOException, ClassNotFoundException {
        String fileLocation = "target/attribute1.ser";
        Serializer<Attribute> serializer = new Serializer<Attribute>();
        serializer.serialize(toSerialize, fileLocation);
        return serializer.deserialize(fileLocation);
    }

    @Test
    public void testSerializationMultiple() throws IOException, ClassNotFoundException {

        toTest = new AttributeImpl("id", UUID.randomUUID().toString());

        toTest.addValue(UUID.randomUUID().toString());
        toTest.addValue(UUID.randomUUID().toString());
        toTest.addValue(UUID.randomUUID().toString());
        toTest.addValue(UUID.randomUUID().toString());

        Attribute read = serializationLoop(toTest);

        assertEquals(toTest.getName(), read.getName());

        assertEquals(toTest.getValue(), read.getValue());

        assertEquals(toTest.getValues(), read.getValues());

    }

    /**
     * Tests what happens when someone tries to manually change the serialized object after it has
     * been serialized. The expected outcome is that it will be detected that the object is corrupt.
     * The original serialized object's name field was "id", it was manually changed, then saved
     * again.
     * 
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test(expected = StreamCorruptedException.class)
    public void testDeserializationCorruption() throws IOException, ClassNotFoundException {

        String fileLocation = "src/test/resources/tamperedAttributeImpl.ser";

        Serializer<Attribute> serializer = new Serializer<Attribute>();

        Attribute readAttribute1 = serializer.deserialize(fileLocation);

        readAttribute1.getName();

    }
}
