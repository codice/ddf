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
package ddf.camel.component.catalog;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import javax.activation.MimeType;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.Exchange;
import org.apache.camel.FailedToCreateProducerException;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.tika.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.mime.MimeTypeToTransformerMapper;
import de.kalpatec.pojosr.framework.PojoServiceRegistryFactoryImpl;
import de.kalpatec.pojosr.framework.launch.PojoServiceRegistry;

public class CatalogComponentTest extends CamelTestSupport {
	private static final transient Logger LOGGER = Logger.getLogger(CatalogComponentTest.class);

	private static final String SERVICE_ID = "id";

	private static final String SAMPLE_ID = "12345678900987654321abcdeffedcba";

	private BundleContext bundleContext;
	private CatalogComponent catalogComponent;

	private final String ddmsInput = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
			+ "<ddms:Resource xmlns:ddms=\"http://metadata.dod.mil/mdr/ns/DDMS/2.0/\" xmlns:ICISM=\"urn:us:gov:ic:ism:v2\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
			+ "  <ddms:identifier ddms:qualifier=\"http://metadata.dod.mil/mdr/ns/MDR/0.1/MDR.owl#URI\" ddms:value=\"http://www.defenselink.mil/news/May2004/n05172004_200405174.html\"/>\n"
			+ "  <ddms:title ICISM:classification=\"U\" ICISM:ownerProducer=\"AUS GBR USA\">Serengeti Event</ddms:title>\n"
			+ "  <ddms:description ICISM:classification=\"U\" ICISM:ownerProducer=\"AUS GBR USA\">Serengeti information</ddms:description>\n"
			+ "  <ddms:language ddms:qualifier=\"http://metadata.dod.mil/mdr/ns/ExtStd/iso_639-2b.owl#en\" ddms:value=\"en\"/>\n"
			+ "  <ddms:dates ddms:posted=\"2004-08-16\"/>\n"
			+ "  <ddms:rights ddms:copyright=\"true\" ddms:privacyAct=\"false\" ddms:intellectualProperty=\"true\"/>\n"
			+ "  <ddms:creator ICISM:classification=\"U\" ICISM:ownerProducer=\"AUS GBR USA\">\n"
			+ "    <ddms:Person>\n"
			+ "      <ddms:name>Donna Miles</ddms:name>\n"
			+ "      <ddms:surname>Miles</ddms:surname>\n"
			+ "      <ddms:affiliation>American Forces Press Service</ddms:affiliation>\n"
			+ "    </ddms:Person>\n"
			+ "  </ddms:creator>\n"
			+ "  <ddms:publisher ICISM:classification=\"U\" ICISM:ownerProducer=\"AUS GBR USA\">\n"
			+ "    <ddms:Organization>\n"
			+ "      <ddms:name>American Forces Press Service</ddms:name>\n"
			+ "    </ddms:Organization>\n"
			+ "  </ddms:publisher>\n"
			+ "  <ddms:format>\n"
			+ "    <ddms:Media>\n"
			+ "      <ddms:mimeType>text/pdf</ddms:mimeType>\n"
			+ "      <ddms:extent ddms:qualifier=\"http://metadata.dod.mil/mdr/ns/UnitOfMeasure/0.1/ComputerStorage.owl#byte\" ddms:value=\"299000\"/>\n"
			+ "      <ddms:medium>digital</ddms:medium>\n"
			+ "    </ddms:Media>\n"
			+ "  </ddms:format>\n"
			+ "  <ddms:subjectCoverage>\n"
			+ "    <ddms:Subject>\n"
			+ "      <ddms:category ddms:qualifier=\"http://metadata.dod.mil/mdr/ns/TaxFG/0.75c/Core_Tax_0.75c.owl#Terrorist_event \" ddms:code=\"Terrorism_Event\" ddms:label=\"Terrorism Event\"/>\n"
			+ "      <ddms:category ddms:qualifier=\"http://metadata.dod.mil/mdr/ns/DomainSets/1.0/GOP_biofeature_agent_type.owl#_21\" ddms:code=\"_21\" ddms:label=\"NERVE SARIN\"/>\n"
			+ "      <ddms:category ddms:qualifier=\"http://metadata.dod.mil/mdr/ns/DomainSets/1.0/GMI_TargetSystemType.owl#WMD\" ddms:code=\"WMD\" ddms:label=\"Weapons of Mass Destruction\"/>\n"
			+ "      <ddms:keyword ddms:value=\"exercise\"/>\n"
			+ "      <ddms:category ddms:qualifier=\"SubjectCoverageQualifier\" ddms:code=\"nitf\" ddms:label=\"nitf\"/>\n"
			+ "    </ddms:Subject>\n"
			+ "  </ddms:subjectCoverage>\n"
			+ "  <ddms:temporalCoverage>\n"
			+ "    <ddms:TimePeriod>\n"
			+ "      <ddms:start>2004-05-17</ddms:start>\n"
			+ "      <ddms:end>2004-05-17</ddms:end>\n"
			+ "    </ddms:TimePeriod>\n"
			+ "  </ddms:temporalCoverage>\n"
			+ "  <ddms:geospatialCoverage>\n"
			+ "    <ddms:GeospatialExtent>\n"
			+ "      <ddms:geographicIdentifier>\n"
			+ "        <ddms:name>Serengeti</ddms:name>\n"
			+ "        <ddms:countryCode ddms:qualifier=\"http://metadata.dod.mil/mdr/ns/ExtStd/1.0/FIPS10-4-2.owl#IZ\"/>\n"
			+ "      </ddms:geographicIdentifier>\n" + "      <ddms:boundingGeometry>\n"
			+ "        <gml:Polygon gml:id=\"BGE-1\" srsName=\"http://metadata.dod.mil/mdr/ns/GSIP/crs/WGS84E_2D\">\n"
			+ "          <gml:exterior>\n" + "            <gml:LinearRing>\n"
			+ "              <gml:pos>11.0 22.0</gml:pos>\n" + "              <gml:pos>10.0 22.0</gml:pos>\n"
			+ "              <gml:pos>10.0 23.0</gml:pos>\n" + "              <gml:pos>11.0 23.0</gml:pos>\n"
			+ "              <gml:pos>11.0 22.0</gml:pos>\n" + "            </gml:LinearRing>\n"
			+ "          </gml:exterior>\n" + "        </gml:Polygon>\n" + "      </ddms:boundingGeometry>\n"
			+ "    </ddms:GeospatialExtent>\n" + "  </ddms:geospatialCoverage>\n"
			+ "  <ddms:security ICISM:classification=\"U\" ICISM:ownerProducer=\"USA\"/>\n" + "</ddms:Resource>";

	@BeforeClass
	static public void oneTimeSetup() {
		// Format logger output
		BasicConfigurator.configure();
		((PatternLayout) ((Appender) Logger.getRootLogger().getAllAppenders().nextElement()).getLayout())
				.setConversionPattern("[%30.30t] %-30.30c{1} %-5p %m%n");

		Logger.getRootLogger().setLevel(Level.DEBUG);
	}

	// The route being tested
	@Override
	protected RouteBuilder createRouteBuilder() throws Exception {
		return new RouteBuilder() {
			// NOTE: While this supports defining multiple routes, each route
			// must
			// have a different from URI. So it is not possible to test multiple
			// routes
			// in a single unit test class/file.
			public void configure() {
				LOGGER.debug("INSIDE RouteBuilder.configure()");
				from("catalog:inputtransformer?"+CatalogComponent.MIME_TYPE_PARAMETER+"=text/xml&id=identity").to(
						"catalog:inputtransformer?"+CatalogComponent.MIME_TYPE_PARAMETER+ "=text/xml&id=ddms20").to("mock:result");

			}
		};
	}

	@Override
	protected CamelContext createCamelContext() throws Exception {
		LOGGER.debug("INSIDE createCamelContext");
		CamelContext camelContext = super.createCamelContext();

		// Configure PojoSR to be our mock OSGi Registry
		PojoServiceRegistry reg = new PojoServiceRegistryFactoryImpl().newPojoServiceRegistry(new HashMap());
		bundleContext = reg.getBundleContext();

		// Since the Camel BlueprintComponentResolver does not execute outside
		// of an OSGi container, we cannot
		// rely on the CatalogComponentResolver to be used for resolving the
		// CatalogComponent when Camel loads the route.
		// Therefore, we Mock what the CatalogComponent's blueprint.xml file
		// would have done by creating a
		// CatalogComponent explicitly and adding it to the CamelContext used
		// for this unit test.
		catalogComponent = new CatalogComponent();
		catalogComponent.setBundleContext(bundleContext);
		camelContext.addComponent(CatalogComponent.NAME, catalogComponent);

		return camelContext;
	}

	@Test
	public void testCatalogComponentResolver() throws Exception {
		CatalogComponentResolver resolver = new CatalogComponentResolver(catalogComponent);
		assertNotNull(resolver);

		Component component = resolver.resolveComponent(CatalogComponent.NAME, null);
		assertNotNull(component);

		component = resolver.resolveComponent("invalid_name", null);
		assertNull(component);
	}

	@Test
	public void testInvalidContextPathForProducer() {
		try {
			LOGGER.debug("INSIDE testInvalidContextPathForProducer");
			template.sendBody("catalog:unknown?mimeType=text/xml&id=identity", "<xml></xml>");
			fail("Should have thrown a FailedToCreateProducerException");
		} catch (FailedToCreateProducerException e) {
			LOGGER.error("Failed to create producer", e);
			assertTrue("Should be an IllegalArgumentException exception",
					e.getCause() instanceof IllegalArgumentException);
			assertEquals("Unable to create producer for context path [unknown]", e.getCause().getMessage());
		}
	}

	@Test
	public void testInvalidContextPathForProducer2() {
		try {
			LOGGER.debug("INSIDE testInvalidContextPathForProducer2");
			context.getEndpoint("catalog:unknown?mimeType=text/xml&id=identity").createProducer();
			fail("Should have thrown a IllegalArgumentException");
		} catch (Exception e) {
			LOGGER.error("Failed testInvalidContextPathForProducer2", e);
			assertTrue("Should be an IllegalArgumentException exception", e instanceof IllegalArgumentException);
			assertEquals("Unable to create producer for context path [unknown]", e.getMessage());
		}
	}

	@Test
	public void testInvalidContextPathForConsumer() {
		try {
			LOGGER.debug("INSIDE testInvalidContextPathForConsumer");
			context.getEndpoint("catalog:unknown?mimeType=text/xml&id=identity").createConsumer(new Processor() {
				public void process(Exchange exchange) throws Exception {
				}
			});
			fail("Should have thrown a IllegalArgumentException");
		} catch (Exception e) {
			LOGGER.error("Failed testInvalidContextPathForConsumer", e);
			assertTrue("Should be an IllegalArgumentException exception", e instanceof IllegalArgumentException);
			assertEquals("Unable to create consumer for context path [unknown]", e.getMessage());
		}
	}

	@Test
	public void testTransformMetacard_NoMimeTypeToTransformerMapperRegistered() throws Exception {
		LOGGER.debug("Running testTransformMetacard_NoMimeTypeToTransformerMapperRegistered()");

		catalogComponent.setMimeTypeToTransformerMapper(null);

		// Mock a DDMS InputTransformer and register it in the OSGi Registry
		// (PojoSR)
		InputTransformer mockTransformer = getMockInputTransformer();
		Hashtable<String, String> props = new Hashtable<String, String>();
		props.put(MimeTypeToTransformerMapper.ID_KEY, "ddms20");
		props.put(MimeTypeToTransformerMapper.MIME_TYPE_KEY, "text/xml");
		bundleContext.registerService(InputTransformer.class.getName(), mockTransformer, props);

		// Send in sample DDMS as InputStream to InputTransformer
		InputStream input = IOUtils.toInputStream(ddmsInput);

		// Get the InputTransformer registered with the ID associated with the
		// <from> node in the Camel route
		InputTransformer transformer = getTransformer("text/xml", "identity");
		assertNotNull("InputTransformer for text/xml;id=identity not found", transformer);

		// Attempt to transform the DDMS XML input into a Metacard
		try {
			transformer.transform(input);
			fail("Should have thrown a CatalogTransformerException");
		} catch (CatalogTransformerException e) {
			assertEquals("Did not find a MimeTypeToTransformerMapper service", e.getMessage());
		}
	}

	@Test
	public void testTransformMetacard_NoProducerInputTransformerRegistered() throws Exception {
		LOGGER.debug("Running testTransformMetacard()");

		// Mock the MimeTypeToTransformerMapper and register it in the OSGi
		// Registry (PojoSR)
		MimeTypeToTransformerMapper matchingService = mock(MimeTypeToTransformerMapper.class);
		// bundleContext.registerService(
		// MimeTypeToTransformerMapper.class.getName(), matchingService, null );
		catalogComponent.setMimeTypeToTransformerMapper(matchingService);

		// Mock the MimeTypeToTransformerMapper returning empty list of
		// InputTransformers
		List list = new ArrayList<InputTransformer>();
		when(matchingService.findMatches(eq(InputTransformer.class), isA(MimeType.class))).thenReturn(list);

		// Send in sample DDMS as InputStream to InputTransformer
		InputStream input = IOUtils.toInputStream(ddmsInput);

		// Get the InputTransformer registered with the ID associated with the
		// <from> node in the Camel route
		InputTransformer transformer = getTransformer("text/xml", "identity");
		assertNotNull("InputTransformer for text/xml;id=identity not found", transformer);

		// Attempt to transform the DDMS XML input into a Metacard
		try {
			transformer.transform(input);
			fail("Should have thrown a CatalogTransformerException");
		} catch (CatalogTransformerException e) {
			assertEquals("Did not find an InputTransformer for MIME Type [text/xml] and id [ddms20]", e.getMessage());
		}
	}

	@Test
	public void testTransformMetacard() throws Exception {
		LOGGER.debug("Running testTransformMetacard()");
		MockEndpoint mock = getMockEndpoint("mock:result");
		mock.expectedMinimumMessageCount(1);

		// Mock a DDMS InputTransformer and register it in the OSGi Registry
		// (PojoSR)
		InputTransformer mockTransformer = getMockInputTransformer();
		Hashtable<String, String> props = new Hashtable<String, String>();
		props.put(MimeTypeToTransformerMapper.ID_KEY, "ddms20");
		props.put(MimeTypeToTransformerMapper.MIME_TYPE_KEY, "text/xml");
		bundleContext.registerService(InputTransformer.class.getName(), mockTransformer, props);

		// Mock the MimeTypeToTransformerMapper and register it in the OSGi
		// Registry (PojoSR)
		MimeTypeToTransformerMapper matchingService = mock(MimeTypeToTransformerMapper.class);
		// HUGH bundleContext.registerService(
		// MimeTypeToTransformerMapper.class.getName(), matchingService, null );
		catalogComponent.setMimeTypeToTransformerMapper(matchingService);

		// Mock the MimeTypeToTransformerMapper returning the mock DDMS
		// InputTransformer
		when(matchingService.findMatches(eq(InputTransformer.class), isA(MimeType.class))).thenReturn((List) Arrays.asList(mockTransformer));

		// Send in sample DDMS as InputStream to InputTransformer
		InputStream input = IOUtils.toInputStream(ddmsInput);

		// Get the InputTransformer registered with the ID associated with the
		// <from> node in the Camel route
		InputTransformer transformer = getTransformer("text/xml", "identity");
		assertNotNull("InputTransformer for mimeType=text/xml&id=identity not found", transformer);

		// Transform the DDMS XML input into a Metacard
		Metacard metacard = transformer.transform(input);
		assertNotNull(metacard);

		assertMockEndpointsSatisfied();
	}

	/**
	 * Looks up the InputTransformer registered in the OSGi registry (PojoSR)
	 * that maps to the mime type that the <from> node in the Camel route
	 * registered as.
	 * 
	 * @param mimeType
	 * @return
	 * @throws Exception
	 */
	private InputTransformer getTransformer(String mimeType, String id) throws Exception {
		LOGGER.trace("ENTERING: getTransformer");

		InputTransformer transformer = null;

		ServiceReference[] refs = null;
		try {
			String filter = "(&(" + MimeTypeToTransformerMapper.MIME_TYPE_KEY + "=" + mimeType + ")("+MimeTypeToTransformerMapper.ID_KEY+"="+id+"))";
			LOGGER.debug("Looking for InputTransformer with filter: " + filter);
			refs = bundleContext.getServiceReferences(InputTransformer.class.getName(), filter);
		} catch (Exception e) {
			String msg = "Invalid input transformer for mime type: " + mimeType;
			LOGGER.warn(msg, e);
			throw new Exception(msg);
		}

		if (refs != null && refs.length > 0) {
			transformer = (InputTransformer) bundleContext.getService(refs[0]);
			LOGGER.debug("Found an InputTransformer:" + transformer.getClass().getName());
		}

		LOGGER.trace("EXITING: getTransformer");

		return transformer;
	}

	/**
	 * Mock an InputTransformer, returning a canned Metacard when the
	 * transform() method is invoked on the InputTransformer.
	 * 
	 * @return
	 */
	protected InputTransformer getMockInputTransformer() {
		InputTransformer inputTransformer = mock(InputTransformer.class);

		Metacard generatedMetacard = getSimpleMetacard();

		try {
			when(inputTransformer.transform(isA(InputStream.class))).thenReturn(generatedMetacard);
			when(inputTransformer.transform(isA(InputStream.class), isA(String.class))).thenReturn(generatedMetacard);
		} catch (IOException e) {
			LOGGER.debug("IOException", e);
		} catch (CatalogTransformerException e) {
			LOGGER.debug("CatalogTransformerException", e);
		}
		return inputTransformer;
	}

	protected Metacard getSimpleMetacard() {
		MetacardImpl generatedMetacard = new MetacardImpl();
		generatedMetacard.setMetadata(getSample());
		generatedMetacard.setId(SAMPLE_ID);

		return generatedMetacard;
	}

	private String getSample() {
		return "<xml></xml>";
	}
}
