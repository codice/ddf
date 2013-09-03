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

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Test;

public class AttributeDescriptorImplTest {

	@Test
	public void testSerialization() throws FileNotFoundException, IOException, ClassNotFoundException {
		AttributeDescriptorImpl adImpl = new AttributeDescriptorImpl("name", true, true, true, false,
				BasicTypes.BINARY_TYPE);

		Serializer<AttributeDescriptor> serializer = new Serializer<AttributeDescriptor>();

		String fileLocation = "target/attributeDescriptor1.ser";

		serializer.serialize(adImpl, fileLocation);

		AttributeDescriptor readAdImpl = serializer.deserialize(fileLocation);

		assertEquals(adImpl.getName(), readAdImpl.getName());

		assertEquals(adImpl.getType().getAttributeFormat(), readAdImpl.getType().getAttributeFormat());

		assertEquals(adImpl.getType().getBinding(), readAdImpl.getType().getBinding());

		assertEquals(adImpl.isIndexed(), readAdImpl.isIndexed());

		assertEquals(adImpl.isMultiValued(), readAdImpl.isMultiValued());

		assertEquals(adImpl.isStored(), readAdImpl.isStored());

		assertEquals(adImpl.isTokenized(), readAdImpl.isTokenized());

	}

	@Test
	public void testHashCode() {

		AttributeDescriptorImpl[] ad = new AttributeDescriptorImpl[5];

		ad[0] = new AttributeDescriptorImpl("name", true, true, true, true, BasicTypes.BINARY_TYPE);

		ad[1] = new AttributeDescriptorImpl("name", true, true, true, true, BasicTypes.BINARY_TYPE);

		ad[2] = new AttributeDescriptorImpl("name", true, true, true, true, BasicTypes.STRING_TYPE);

		ad[3] = new AttributeDescriptorImpl("mane", true, true, true, true, BasicTypes.BINARY_TYPE);

		ad[4] = new AttributeDescriptorImpl("name", false, true, true, true, BasicTypes.BINARY_TYPE);

		assertEquals(ad[0], ad[1]);

		assertEquals(ad[0].hashCode(), ad[1].hashCode());

		assertFalse(ad[0].equals(ad[2]));

		assertFalse(ad[0].hashCode() == ad[2].hashCode());

		for (AttributeDescriptorImpl adi : ad) {

			assertTrue(adi.equals(adi));

			assertTrue(adi.hashCode() == adi.hashCode());

		}

		assertFalse(ad[1].hashCode() == ad[3].hashCode());

		assertFalse(ad[0].hashCode() == ad[4].hashCode());

		// CHANGING values of ad4

		ad[4].indexed = true;

		assertEquals(ad[0], ad[4]);

		assertEquals(ad[0].hashCode(), ad[4].hashCode());

	}

}
