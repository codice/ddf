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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class AttributeTest {
	
	Attribute toTest;
	
	@Before
	public void setup(){
		toTest = new StubAttribute("foo", (Serializable)"bar", createValues());
	}
	
	private List<Serializable> createValues(){
		return new ArrayList<Serializable>( 
				Arrays.asList(0,1,2));
	}
	
	@Test
	public void testName() {
		assertEquals("foo", toTest.getName());
	}
	
	@Test
	public void testValue() {
		assertEquals("bar", toTest.getValue().toString());
	}
	
	@Test
	public void testValues() {
		List<Serializable> vals = toTest.getValues();
		assertEquals(createValues(), vals);
	}
	
	@Test
	public void testSerialization(){
		
			try{
				// serialize
				ByteArrayOutputStream out = new ByteArrayOutputStream();
			    ObjectOutputStream oos = null;
				oos = new ObjectOutputStream(out);
				oos.writeObject(toTest);
				oos.close();

			    //deserialize
			    byte[] pickled = out.toByteArray();
			    InputStream in = new ByteArrayInputStream(pickled);
			    ObjectInputStream ois = null;
				ois = new ObjectInputStream(in);
				Object o = null;
				o = ois.readObject();
				Attribute copy = (Attribute) o;

			    // test the result
			    assertEquals("foo", copy.getName());
			    assertEquals("bar", copy.getValue().toString());
			}
			catch(Exception e){
				fail(e.getMessage());
			}
			
	}
}
