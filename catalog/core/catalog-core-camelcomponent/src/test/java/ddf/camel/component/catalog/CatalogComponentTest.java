/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.camel.component.catalog;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.mime.MimeTypeToTransformerMapper;
import de.kalpatec.pojosr.framework.PojoServiceRegistryFactoryImpl;
import de.kalpatec.pojosr.framework.launch.PojoServiceRegistry;
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
import org.apache.tika.io.IOUtils;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CatalogComponentTest extends CamelTestSupport {
  private static final transient Logger LOGGER =
      LoggerFactory.getLogger(CatalogComponentTest.class);

  private static final String SAMPLE_ID = "12345678900987654321abcdeffedcba";

  private final String xmlInput =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
          + "<ns3:metacard xmlns:ns1=\"http://www.opengis.net/gml\" xmlns:ns2=\"http://www.w3.org/1999/xlink\" xmlns:ns3=\"urn:catalog:metacard\" xmlns:ns4=\"http://www.w3.org/2001/SMIL20/\" xmlns:ns6=\"http://www.w3.org/2001/SMIL20/Language\" ns1:id=\"1234567890987654321\">\n"
          + "<ns3:type>ddf.metacard</ns3:type>\n"
          + "<ns3:source>foobar</ns3:source>\n"
          + "<ns3:string name=\"title\">\n"
          + "<ns3:value>Title!</ns3:value>\n"
          + "</ns3:string>\n"
          + "<ns3:dateTime name=\"modified\"/>\n"
          + "<ns3:string name=\"metadata-content-type-version\"/>\n"
          + "<ns3:base64Binary name=\"thumbnail\">\n"
          + "<ns3:value>AAABAAABAQEAAQAAAQEBAAEAAAEBAQABAAABAQEAAQAAAQEBAAEAAAEBAQABAAABAQE=</ns3:value>\n"
          + "</ns3:base64Binary>\n"
          + "<ns3:dateTime name=\"expiration\">\n"
          + "<ns3:value>2012-12-27T16:31:01.641-07:00</ns3:value>\n"
          + "</ns3:dateTime>\n"
          + "<ns3:string name=\"metadata-target-namespace\"/>\n"
          + "<ns3:dateTime name=\"created\"/>\n"
          + "<ns3:stringxml name=\"metadata\">\n"
          + "<ns3:value>\n"
          + "<foo xmlns=\"http://foo.com\">\n"
          + "<bar/>\n"
          + "</foo>\n"
          + "</ns3:value>\n"
          + "</ns3:stringxml>\n"
          + "<ns3:string name=\"resource-size\"/>\n"
          + "<ns3:string name=\"metadata-content-type\"/>\n"
          + "<ns3:geometry name=\"location\">\n"
          + "<ns3:value>\n"
          + "<ns1:Polygon>\n"
          + "<ns1:exterior>\n"
          + "<ns1:LinearRing>\n"
          + "<ns1:pos>35.0 10.0</ns1:pos>\n"
          + "<ns1:pos>10.0 20.0</ns1:pos>\n"
          + "<ns1:pos>15.0 40.0</ns1:pos>\n"
          + "<ns1:pos>45.0 45.0</ns1:pos>\n"
          + "<ns1:pos>35.0 10.0</ns1:pos>\n"
          + "</ns1:LinearRing>\n"
          + "</ns1:exterior>\n"
          + "<ns1:interior>\n"
          + "<ns1:LinearRing>\n"
          + "<ns1:pos>20.0 30.0</ns1:pos>\n"
          + "<ns1:pos>35.0 35.0</ns1:pos>\n"
          + "<ns1:pos>30.0 20.0</ns1:pos>\n"
          + "<ns1:pos>20.0 30.0</ns1:pos>\n"
          + "</ns1:LinearRing>\n"
          + "</ns1:interior>\n"
          + "</ns1:Polygon>\n"
          + "</ns3:value>\n"
          + "</ns3:geometry>\n"
          + "<ns3:string name=\"resource-uri\"/>\n"
          + "<ns3:dateTime name=\"effective\"/>\n"
          + "</ns3:metacard>";

  private BundleContext bundleContext;

  private CatalogComponent catalogComponent;

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
        from("catalog:inputtransformer?"
                + CatalogComponent.MIME_TYPE_PARAMETER
                + "=text/xml&id=identity")
            .to(
                "catalog:inputtransformer?"
                    + CatalogComponent.MIME_TYPE_PARAMETER
                    + "=text/xml&id=xml")
            .to("mock:result");
      }
    };
  }

  @Override
  protected CamelContext createCamelContext() throws Exception {
    LOGGER.debug("INSIDE createCamelContext");
    CamelContext camelContext = super.createCamelContext();

    // Configure PojoSR to be our mock OSGi Registry
    PojoServiceRegistry reg =
        new PojoServiceRegistryFactoryImpl().newPojoServiceRegistry(new HashMap());
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
      assertTrue(
          "Should be an IllegalArgumentException exception",
          e.getCause() instanceof IllegalArgumentException);
      assertEquals(
          "Unable to create producer for context path [unknown]", e.getCause().getMessage());
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
      assertTrue(
          "Should be an IllegalArgumentException exception", e instanceof IllegalArgumentException);
      assertEquals("Unable to create producer for context path [unknown]", e.getMessage());
    }
  }

  @Test
  public void testInvalidContextPathForConsumer() {
    try {
      LOGGER.debug("INSIDE testInvalidContextPathForConsumer");
      context
          .getEndpoint("catalog:unknown?mimeType=text/xml&id=identity")
          .createConsumer(
              new Processor() {
                public void process(Exchange exchange) throws Exception {}
              });
      fail("Should have thrown a IllegalArgumentException");
    } catch (Exception e) {
      LOGGER.error("Failed testInvalidContextPathForConsumer", e);
      assertTrue(
          "Should be an IllegalArgumentException exception", e instanceof IllegalArgumentException);
      assertEquals("Unable to create consumer for context path [unknown]", e.getMessage());
    }
  }

  @Test
  public void testTransformMetacardNoMimeTypeToTransformerMapperRegistered() throws Exception {
    LOGGER.debug("Running testTransformMetacard_NoMimeTypeToTransformerMapperRegistered()");

    catalogComponent.setMimeTypeToTransformerMapper(null);

    // Mock a XML InputTransformer and register it in the OSGi Registry
    // (PojoSR)
    InputTransformer mockTransformer = getMockInputTransformer();
    Hashtable<String, String> props = new Hashtable<String, String>();
    props.put(MimeTypeToTransformerMapper.ID_KEY, "xml");
    props.put(MimeTypeToTransformerMapper.MIME_TYPE_KEY, "text/xml");
    bundleContext.registerService(InputTransformer.class.getName(), mockTransformer, props);

    // Send in sample XML as InputStream to InputTransformer
    InputStream input = IOUtils.toInputStream(xmlInput);

    // Get the InputTransformer registered with the ID associated with the
    // <from> node in the Camel route
    InputTransformer transformer = getTransformer("text/xml", "identity");
    assertNotNull("InputTransformer for text/xml;id=identity not found", transformer);

    // Attempt to transform the XML input into a Metacard
    try {
      transformer.transform(input);
      fail("Should have thrown a CatalogTransformerException");
    } catch (CatalogTransformerException e) {
      assertEquals("Did not find a MimeTypeToTransformerMapper service", e.getMessage());
    }
  }

  @Test
  public void testTransformMetacardNoProducerInputTransformerRegistered() throws Exception {
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
    when(matchingService.findMatches(eq(InputTransformer.class), isA(MimeType.class)))
        .thenReturn(list);

    // Send in sample XML as InputStream to InputTransformer
    InputStream input = IOUtils.toInputStream(xmlInput);

    // Get the InputTransformer registered with the ID associated with the
    // <from> node in the Camel route
    InputTransformer transformer = getTransformer("text/xml", "identity");
    assertNotNull("InputTransformer for text/xml;id=identity not found", transformer);

    // Attempt to transform the XML input into a Metacard
    try {
      transformer.transform(input);
      fail("Should have thrown a CatalogTransformerException");
    } catch (CatalogTransformerException e) {
      assertEquals(
          "Did not find an InputTransformer for MIME Type [text/xml] and id [xml]", e.getMessage());
    }
  }

  @Test
  public void testTransformMetacard() throws Exception {
    LOGGER.debug("Running testTransformMetacard()");
    MockEndpoint mock = getMockEndpoint("mock:result");
    mock.expectedMinimumMessageCount(1);

    // Mock a XML InputTransformer and register it in the OSGi Registry
    // (PojoSR)
    InputTransformer mockTransformer = getMockInputTransformer();
    Hashtable<String, String> props = new Hashtable<String, String>();
    props.put(MimeTypeToTransformerMapper.ID_KEY, "xml");
    props.put(MimeTypeToTransformerMapper.MIME_TYPE_KEY, "text/xml");
    bundleContext.registerService(InputTransformer.class.getName(), mockTransformer, props);

    // Mock the MimeTypeToTransformerMapper and register it in the OSGi
    // Registry (PojoSR)
    MimeTypeToTransformerMapper matchingService = mock(MimeTypeToTransformerMapper.class);
    // HUGH bundleContext.registerService(
    // MimeTypeToTransformerMapper.class.getName(), matchingService, null );
    catalogComponent.setMimeTypeToTransformerMapper(matchingService);

    // Mock the MimeTypeToTransformerMapper returning the mock XML
    // InputTransformer
    when(matchingService.findMatches(eq(InputTransformer.class), isA(MimeType.class)))
        .thenReturn((List) Arrays.asList(mockTransformer));

    // Send in sample XML as InputStream to InputTransformer
    InputStream input = IOUtils.toInputStream(xmlInput);

    // Get the InputTransformer registered with the ID associated with the
    // <from> node in the Camel route
    InputTransformer transformer = getTransformer("text/xml", "identity");
    assertNotNull("InputTransformer for mimeType=text/xml&id=identity not found", transformer);

    // Transform the XML input into a Metacard
    Metacard metacard = transformer.transform(input);
    assertNotNull(metacard);

    assertMockEndpointsSatisfied();
  }

  /**
   * Looks up the InputTransformer registered in the OSGi registry (PojoSR) that maps to the mime
   * type that the <from> node in the Camel route registered as.
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
      String filter =
          "(&("
              + MimeTypeToTransformerMapper.MIME_TYPE_KEY
              + "="
              + mimeType
              + ")("
              + MimeTypeToTransformerMapper.ID_KEY
              + "="
              + id
              + "))";
      LOGGER.debug("Looking for InputTransformer with filter: {}", filter);
      refs = bundleContext.getServiceReferences(InputTransformer.class.getName(), filter);
    } catch (Exception e) {
      String msg = "Invalid input transformer for mime type: " + mimeType;
      LOGGER.debug(msg, e);
      throw new Exception(msg);
    }

    if (refs != null && refs.length > 0) {
      transformer = (InputTransformer) bundleContext.getService(refs[0]);
      LOGGER.debug("Found an InputTransformer: {}", transformer.getClass().getName());
    }

    LOGGER.trace("EXITING: getTransformer");

    return transformer;
  }

  /**
   * Mock an InputTransformer, returning a canned Metacard when the transform() method is invoked on
   * the InputTransformer.
   *
   * @return
   */
  protected InputTransformer getMockInputTransformer() {
    InputTransformer inputTransformer = mock(InputTransformer.class);

    Metacard generatedMetacard = getSimpleMetacard();

    try {
      when(inputTransformer.transform(isA(InputStream.class))).thenReturn(generatedMetacard);
      when(inputTransformer.transform(isA(InputStream.class), isA(String.class)))
          .thenReturn(generatedMetacard);
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
