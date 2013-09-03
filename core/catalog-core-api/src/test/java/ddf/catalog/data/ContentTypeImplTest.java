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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

public class ContentTypeImplTest {
	private static final Logger LOGGER = Logger
			.getLogger(ContentTypeImplTest.class);
	URI testUri;
	String testName;
	String testVersion;
	
	@Before
	public void setUp() {
		testName = "JunitTest";
		testVersion = "1.2.3";
	    try {
			testUri = new URI("http://ddf.catalog.data/junitTest");
		} catch (URISyntaxException e) {
			// Don't use an invalid uri here!
			e.printStackTrace();
		}
		
	}

	@Test
	public void testContentTypeImplConstuctors() {
		ContentTypeImpl cti = new ContentTypeImpl();
		assertEquals(null, cti.getName());
		assertEquals(null, cti.getNamespace());
		assertEquals(null, cti.getVersion());
		
		cti = new ContentTypeImpl(testName, testVersion);
		assertEquals(testName, cti.getName());
		assertEquals(testVersion, cti.getVersion());
		
		cti = new ContentTypeImpl(testName, testVersion, testUri);
		assertEquals(testName, cti.getName());
		assertEquals(testUri, cti.getNamespace());
		assertEquals(testVersion, cti.getVersion());
	}
	
	@Test
	public void testContentTypeImplSetters() {
		ContentTypeImpl cti = new ContentTypeImpl();
		assertEquals(null, cti.getName());
		assertEquals(null, cti.getNamespace());
		assertEquals(null, cti.getVersion());
		
		cti.setName(testName);
		assertEquals(testName, cti.getName());
		
		cti.setVersion(testVersion);
		assertEquals(testVersion, cti.getVersion());
		
		cti.setNamespace(testUri);
		assertEquals(testUri, cti.getNamespace());

		cti.setName("");
		assertEquals("", cti.getName());
		
		cti.setVersion("");
		assertEquals("", cti.getVersion());

		cti.setName(null);
		cti.setNamespace(null);
		cti.setVersion(null);
		assertEquals(null, cti.getName());
		assertEquals(null, cti.getNamespace());
		assertEquals(null, cti.getVersion());

	}
	
	@Test
	public void testContentTypeEquals() {
		
		ContentTypeImpl contentTypeImpl1 = new ContentTypeImpl(null, "a") ;
		
		ContentTypeImpl contentTypeImpl2 = new ContentTypeImpl("a", null) ;
		
		assertFalse(contentTypeImpl1.hashCode() == contentTypeImpl2.hashCode()) ;
		assertFalse(contentTypeImpl1.equals(contentTypeImpl2)) ;
		
		contentTypeImpl1 = new ContentTypeImpl("b", "a") ;
		
		contentTypeImpl2 = new ContentTypeImpl("a", "b") ;
		
		assertFalse(contentTypeImpl1.hashCode() == contentTypeImpl2.hashCode()) ;
		assertFalse(contentTypeImpl1.equals(contentTypeImpl2)) ;
		
		contentTypeImpl1 = new ContentTypeImpl("a", "b") ;
		
		contentTypeImpl2 = new ContentTypeImpl("a", "b") ;
		
		assertTrue(contentTypeImpl1.hashCode() == contentTypeImpl2.hashCode()) ;
		assertTrue(contentTypeImpl1.equals(contentTypeImpl2)) ;
		
		contentTypeImpl1 = new ContentTypeImpl(null, null) ;
		
		contentTypeImpl2 = new ContentTypeImpl(null, null) ;
		
		assertTrue(contentTypeImpl1.hashCode() == contentTypeImpl2.hashCode()) ;
		assertTrue(contentTypeImpl1.equals(contentTypeImpl2)) ;
		
		contentTypeImpl1 = new ContentTypeImpl("contentType1", "mockVersion");
		contentTypeImpl2 = new ContentTypeImpl("contentType1", "mockVersion");
		
		assertTrue(contentTypeImpl1.hashCode() == contentTypeImpl2.hashCode()) ;
		assertTrue(contentTypeImpl1.equals(contentTypeImpl2)) ;
		
		
	}
}
