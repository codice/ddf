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
package ddf.catalog.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class MetacardTypeImplTest {

	@Test
	public void testNullDescriptors() {

		MetacardType mt = new MetacardTypeImpl("name", null);
		
		assertTrue(mt.getAttributeDescriptors().isEmpty()) ;
		
	}

	@Test
	public void testSerializationSingle() throws IOException, ClassNotFoundException {

		HashSet<AttributeDescriptor> descriptors = new HashSet<AttributeDescriptor>();

		descriptors.add(new AttributeDescriptorImpl("id", true, true, false, false, BasicTypes.STRING_TYPE));

		MetacardTypeImpl metacardType = new MetacardTypeImpl("basic", descriptors);

		String fileLocation = "target/metacardType.ser";

		Serializer<MetacardType> serializer = new Serializer<MetacardType>();

		serializer.serialize(metacardType, fileLocation);

		MetacardType readMetacardType = serializer.deserialize(fileLocation);

		assertEquals(metacardType.getName(), readMetacardType.getName());

		assertEquals(metacardType.getAttributeDescriptor("id").getName(), readMetacardType.getAttributeDescriptor("id")
				.getName());

		assertEquals(metacardType.getAttributeDescriptor("id").getType().getBinding(), readMetacardType
				.getAttributeDescriptor("id").getType().getBinding());

		assertEquals(metacardType.getAttributeDescriptor("id").getType().getAttributeFormat(), readMetacardType
				.getAttributeDescriptor("id").getType().getAttributeFormat());

		Set<AttributeDescriptor> oldAd = metacardType.getAttributeDescriptors();
		Set<AttributeDescriptor> newAd = readMetacardType.getAttributeDescriptors();

		assertTrue(oldAd.iterator().next().equals(newAd.iterator().next()));

	}

	@Test
	public void testSerializationNullDescriptors() throws IOException, ClassNotFoundException {
		MetacardTypeImpl metacardType = new MetacardTypeImpl("basic", null);

		String fileLocation = "target/metacardType.ser";

		Serializer<MetacardType> serializer = new Serializer<MetacardType>();

		serializer.serialize(metacardType, fileLocation);

		MetacardType readMetacardType = serializer.deserialize(fileLocation);

		assertEquals(metacardType.getName(), readMetacardType.getName());

		Set<AttributeDescriptor> oldAd = metacardType.getAttributeDescriptors();
		Set<AttributeDescriptor> newAd = readMetacardType.getAttributeDescriptors();
		
		assertTrue(oldAd.isEmpty());
		assertTrue(newAd.isEmpty());
	}
}
