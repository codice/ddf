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
package ddf.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.event.DeliveryException;
import ddf.catalog.event.EventException;
import ddf.catalog.event.InvalidSubscriptionException;
import ddf.catalog.event.SubscriptionExistsException;
import ddf.catalog.event.SubscriptionNotFoundException;
import ddf.catalog.federation.FederationException;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;


public class ExceptionsTest {
	
	Exception testCause;
	String causeMsg;
	String msg;
	
	@Before
	public void setUp(){
		causeMsg = "Bart!!!";
		msg = "Doh!";
		testCause = new Exception(causeMsg);
	}
	
	@Test
	public void testCatalogTransformerException() {
		CatalogTransformerException cte = new CatalogTransformerException();
		assertNotNull(cte);
		cte = new CatalogTransformerException(msg);
		assertEquals(cte.getMessage(), msg);
		cte = new CatalogTransformerException(testCause);
		assertEquals(cte.getCause(), testCause);
		cte = new CatalogTransformerException(msg, testCause);
		assertEquals(cte.getMessage(), msg);
		assertEquals(cte.getCause(), testCause);
	}

	@Test
	public void testIngestException() {
		IngestException ie = new IngestException();
		assertNotNull(ie);
		ie = new IngestException(msg);
		assertEquals(ie.getMessage(), msg);
		ie = new IngestException(testCause);
		assertEquals(ie.getCause(), testCause);
		ie = new IngestException(msg, testCause);
		assertEquals(ie.getMessage(), msg);
		assertEquals(ie.getCause(), testCause);
	}

	@Test
	public void testDeliveryException() {
		DeliveryException de = new DeliveryException(msg);
		assertEquals(de.getMessage(), msg);
		de = new DeliveryException(testCause);
		assertEquals(de.getCause(), testCause);
		de = new DeliveryException(msg, testCause);
		assertEquals(de.getMessage(), msg);
		assertEquals(de.getCause(), testCause);
	}

	@Test
	public void testEventException() {
		EventException ee = new EventException();
		assertNotNull(ee);
		ee = new EventException(msg);
		assertEquals(ee.getMessage(), msg);
		ee = new EventException(testCause);
		assertEquals(ee.getCause(), testCause);
		ee = new EventException(msg, testCause);
		assertEquals(ee.getMessage(), msg);
		assertEquals(ee.getCause(), testCause);
	}

	@Test
	public void testFederationException() {
		FederationException fe = new FederationException();
		assertNotNull(fe);
		fe = new FederationException(msg);
		assertEquals(fe.getMessage(), msg);
		fe = new FederationException(testCause);
		assertEquals(fe.getCause(), testCause);
		fe = new FederationException(msg, testCause);
		assertEquals(fe.getMessage(), msg);
		assertEquals(fe.getCause(), testCause);
	}

	@Test
	public void testInvalidSubscriptionException() {
		InvalidSubscriptionException ise = new InvalidSubscriptionException(msg);
		assertEquals(ise.getMessage(), msg);
		ise = new InvalidSubscriptionException(testCause);
		assertEquals(ise.getCause(), testCause);
		ise = new InvalidSubscriptionException(msg, testCause);
		assertEquals(ise.getMessage(), msg);
		assertEquals(ise.getCause(), testCause);
	}

	@Test
	public void testMetacardCreationException() {
		MetacardCreationException mce = new MetacardCreationException();
		assertNotNull(mce);
		mce = new MetacardCreationException(msg);
		assertEquals(mce.getMessage(), msg);
		mce = new MetacardCreationException(testCause);
		assertEquals(mce.getCause(), testCause);
		mce = new MetacardCreationException(msg, testCause);
		assertEquals(mce.getMessage(), msg);
		assertEquals(mce.getCause(), testCause);
	}

	@Test
	public void testPluginExecutionException() {
		PluginExecutionException pee = new PluginExecutionException();
		assertNotNull(pee);
		pee = new PluginExecutionException(msg);
		assertEquals(pee.getMessage(), msg);
		pee = new PluginExecutionException(testCause);
		assertEquals(pee.getCause(), testCause);
		pee = new PluginExecutionException(msg, testCause);
		assertEquals(pee.getMessage(), msg);
		assertEquals(pee.getCause(), testCause);
	}
	
	@Test
	public void testResourceNotFoundException() {
		ResourceNotFoundException rnfe = new ResourceNotFoundException();
		assertNotNull(rnfe);
		rnfe = new ResourceNotFoundException(msg);
		assertEquals(rnfe.getMessage(), msg);
		rnfe = new ResourceNotFoundException(testCause);
		assertEquals(rnfe.getCause(), testCause);
		rnfe = new ResourceNotFoundException(msg, testCause);
		assertEquals(rnfe.getMessage(), msg);
		assertEquals(rnfe.getCause(), testCause);
	}

	@Test
	public void testResourceNotSupportedException() {
		ResourceNotSupportedException rnse = new ResourceNotSupportedException();
		assertNotNull(rnse);
		rnse = new ResourceNotSupportedException(msg);
		assertEquals(rnse.getMessage(), msg);
		rnse = new ResourceNotSupportedException(testCause);
		assertEquals(rnse.getCause(), testCause);
		rnse = new ResourceNotSupportedException(msg, testCause);
		assertEquals(rnse.getMessage(), msg);
		assertEquals(rnse.getCause(), testCause);
	}
	
	@Test
	public void testSourceUnavailableException() {
		SourceUnavailableException sue = new SourceUnavailableException();
		assertNotNull(sue);
		sue = new SourceUnavailableException(msg);
		assertEquals(sue.getMessage(), msg);
		sue = new SourceUnavailableException(testCause);
		assertEquals(sue.getCause(), testCause);
		sue = new SourceUnavailableException(msg, testCause);
		assertEquals(sue.getMessage(), msg);
		assertEquals(sue.getCause(), testCause);
	}
	
	@Test
	public void testStopProcessingException() {
		StopProcessingException spe = new StopProcessingException(msg);
		assertEquals(spe.getMessage(), msg);
	}
	
	@Test
	public void testSubscriptionExistsException() {
		SubscriptionExistsException see = new SubscriptionExistsException();
		assertNotNull(see);
	}
	
	@Test
	public void testSubscriptionNotFoundException() {
		SubscriptionNotFoundException snfe = new SubscriptionNotFoundException(msg);
		assertEquals(snfe.getMessage(), msg);
		snfe = new SubscriptionNotFoundException(testCause);
		assertEquals(snfe.getCause(), testCause);
		snfe = new SubscriptionNotFoundException(msg, testCause);
		assertEquals(snfe.getMessage(), msg);
		assertEquals(snfe.getCause(), testCause);
	}
	
	@Test
	public void testUnsupportedQueryException() {
		UnsupportedQueryException uqe = new UnsupportedQueryException();
		assertNotNull(uqe);
		uqe = new UnsupportedQueryException(msg);
		assertEquals(uqe.getMessage(), msg);
		uqe = new UnsupportedQueryException(testCause);
		assertEquals(uqe.getCause(), testCause);
		uqe = new UnsupportedQueryException(msg, testCause);
		assertEquals(uqe.getMessage(), msg);
		assertEquals(uqe.getCause(), testCause);
	}


}
