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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.camel.component.catalog.framework.FrameworkProducerException;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.DeleteResponseImpl;
import ddf.catalog.operation.impl.UpdateImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.operation.impl.UpdateResponseImpl;
import ddf.catalog.source.IngestException;
import de.kalpatec.pojosr.framework.PojoServiceRegistryFactoryImpl;
import de.kalpatec.pojosr.framework.launch.PojoServiceRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.osgi.framework.BundleContext;
import org.springframework.util.Assert;

/**
 * Tests the custom Camel CatalogComponent FrameworkProducer. The FrameworkProducer would map to a
 * Camel <to> route node with a URI like <code>catalog:framework</code>. The message sent to this
 * component should have header named "operation" with a value of "CREATE", "UPDATE" or "DELETE".
 *
 * <p>For the CREATE and UPDATE operation, the message body can contain a {@link java.util.List} of
 * Metacards or a single Metacard object.
 *
 * <p>For the DELETE operation, the message body can contain a {@link java.util.List} of {@link
 * String} or a single {@link String} object. The {@link String} objects represent the IDs of
 * Metacards that you would want to delete.
 *
 * <p>The exchange's "in" message will be set with the affected Metacards. In the case of a CREATE,
 * it will be updated with the created Metacards. In the case of the UPDATE, it will be updated with
 * the updated Metacards and with the DELETE it will contain the deleted Metacards.
 *
 * <table border="1">
 * <tr>
 * <th>USE CASE</th>
 * <th>ROUTE NODE</th>
 * <th>HEADER</th>
 * <th>MESSAGE BODY</th>
 * <th>EXCHANGE MODIFICATION</th>
 * </tr>
 * <tr>
 * <td>Create Metacard(s)</td>
 * <td>catalog:framework</td>
 * <td>operation:CREATE</td>
 * <td>List&ltMetacard&gt or Metacard</td>
 * <td>exchange.getIn().getBody() updated with {@link java.util.List} of Metacards created</td>
 * </tr>
 * <tr>
 * <td>Update Metacard(s)</td>
 * <td>catalog:framework</td>
 * <td>operation:UPDATE</td>
 * <td>List&ltMetacard&gt or Metacard</td>
 * <td>exchange.getIn().getBody() updated with {@link java.util.List} of Metacards updated</td>
 * </tr>
 * <tr>
 * <td>Delete Metacard(s)</td>
 * <td>catalog:framework</td>
 * <td>operation:DELETE</td>
 * <td>List&ltString&gt or String (IDs of Metacards to delete)</td>
 * <td>exchange.getIn().getBody() updated with {@link java.util.List} of Metacards deleted</td>
 * </tr>
 * </table>
 *
 * @author Sam Patel
 */
public class CatalogComponentFrameworkTest extends CamelTestSupport {

  private static final String SAMPLE_METACARD_CONTENT1 = "sample1";

  private static final String SAMPLE_METACARD_CONTENT2 = "sample2";

  private static final String SAMPLE_METACARD_ID_1 = "12345678900987654321abcdeffedcba";

  private static final String SAMPLE_METACARD_ID_2 = "12345678900987654321abcdeffedcbb";

  private static MetacardImpl metacard1;

  private static MetacardImpl metacard2;

  private static CatalogFramework catalogFramework;

  @Rule public ExpectedException thrown = ExpectedException.none();

  @BeforeClass
  public static void oneTimeSetup() throws Exception {
    metacard1 = new MetacardImpl();
    metacard1.setMetadata(SAMPLE_METACARD_CONTENT1);
    metacard1.setId(SAMPLE_METACARD_ID_1);

    metacard2 = new MetacardImpl();
    metacard2.setMetadata(SAMPLE_METACARD_CONTENT2);
    metacard2.setId(SAMPLE_METACARD_ID_2);
  }

  @Override
  protected CamelContext createCamelContext() throws Exception {
    // Since the Camel BlueprintComponentResolver does not execute outside
    // of an OSGi container, we cannot
    // rely on the CatalogComponentResolver to be used for resolving the
    // CatalogComponent when Camel loads the route.
    // Therefore, we Mock what the CatalogComponent's blueprint.xml file
    // would have done by creating a
    // CatalogComponent explicitly and adding it to the CamelContext used
    // for this unit test.

    // Configure PojoSR to be our mock OSGi Registry
    final PojoServiceRegistry reg =
        new PojoServiceRegistryFactoryImpl().newPojoServiceRegistry(new HashMap());
    final BundleContext bundleContext = reg.getBundleContext();

    final CamelContext camelContext = super.createCamelContext();
    final CatalogComponent catalogComponent = new CatalogComponent();
    catalogComponent.setBundleContext(bundleContext);
    catalogFramework = mock(CatalogFramework.class);
    catalogComponent.setCatalogFramework(catalogFramework);
    camelContext.addComponent(CatalogComponent.NAME, catalogComponent);

    return camelContext;
  }

  @Override
  protected RouteBuilder createRouteBuilder() throws Exception {
    return new RouteBuilder() {
      @Override
      public void configure() throws Exception {
        from("direct:sampleInput").to("catalog:framework").to("mock:result");
      }
    };
  }

  @Test
  /** Operation: CREATE Body contains: List<Metacard> */
  public void testCreateWithListOfMetacards() throws Exception {
    resetMocks();

    // Setup expectations to verify
    final MockEndpoint mockVerifierEndpoint = getMockEndpoint("mock:result");
    mockVerifierEndpoint.expectedMessageCount(1);

    final List<Metacard> metacards = new ArrayList<Metacard>();
    metacards.add(metacard1);
    metacards.add(metacard2);

    // Mock catalog framework
    final CreateRequest createRequest = new CreateRequestImpl(metacards);
    final CreateResponse createResponse =
        new CreateResponseImpl(createRequest, new HashMap(), metacards);
    when(catalogFramework.create(any(CreateRequest.class))).thenReturn(createResponse);

    // Exercise the route with a CREATE operation
    template.sendBodyAndHeader("direct:sampleInput", metacards, "Operation", "CREATE");

    // Verify that the number of metacards in the exchange after the records
    // is identical to the input
    assertListSize(mockVerifierEndpoint.getExchanges(), 1);
    final Exchange exchange = mockVerifierEndpoint.getExchanges().get(0);
    final List<Metacard> cardsCreated = (List<Metacard>) exchange.getIn().getBody();
    assertListSize(cardsCreated, 2);

    mockVerifierEndpoint.assertIsSatisfied();
  }

  @Test
  /** Operation: CREATE Body contains: null */
  public void testCreateWithNull() throws Exception {
    resetMocks();

    boolean threwException = false;

    // Exercise the route with a CREATE operation
    try {
      template.sendBodyAndHeader("direct:sampleInput", null, "Operation", "CREATE");
    } catch (CamelExecutionException cee) {
      Assert.isInstanceOf(FrameworkProducerException.class, cee.getCause());
      threwException = true;
    }

    Assert.isTrue(threwException);
  }

  @Test
  /** Operation: CREATE Body contains: String */
  public void testCreateWithInvalidType() throws Exception {
    resetMocks();

    boolean threwException = false;

    // Exercise the route with a CREATE operation
    try {
      template.sendBodyAndHeader(
          "direct:sampleInput", new String("WRONG TYPE"), "Operation", "CREATE");
    } catch (CamelExecutionException cee) {
      Assert.isInstanceOf(FrameworkProducerException.class, cee.getCause());
      threwException = true;
    }

    Assert.isTrue(threwException);
  }

  @Test
  /** Operation: CREATE Body contains: List<Metacard> with no members */
  public void testCreateWithEmptyListOfMetacards() throws Exception {
    resetMocks();

    final List<Metacard> metacards = new ArrayList<Metacard>();
    boolean threwException = false;

    // Exercise the route with a CREATE operation
    try {
      template.sendBodyAndHeader("direct:sampleInput", metacards, "Operation", "CREATE");
    } catch (CamelExecutionException cee) {
      Assert.isInstanceOf(FrameworkProducerException.class, cee.getCause());
      threwException = true;
    }

    Assert.isTrue(threwException);
  }

  @Test
  /** Operation: CREATE Body contains: List<Metacard> with null member */
  public void testCreateWithNullInListOfMetacards() throws Exception {
    resetMocks();

    final List<Metacard> metacards = new ArrayList<Metacard>();
    metacards.add(null);

    boolean threwException = false;

    // Exercise the route with a CREATE operation
    try {
      template.sendBodyAndHeader("direct:sampleInput", metacards, "Operation", "CREATE");
    } catch (CamelExecutionException cee) {
      Assert.isInstanceOf(FrameworkProducerException.class, cee.getCause());
      threwException = true;
    }

    Assert.isTrue(threwException);
  }

  @Test
  /** Operation: CREATE Body contains: List<String> */
  public void testCreateWithInvalidListType() throws Exception {
    resetMocks();

    final List<String> metacards = new ArrayList<String>();
    metacards.add(new String("WRONG TYPE"));

    boolean threwException = false;

    // Exercise the route with a CREATE operation
    try {
      template.sendBodyAndHeader("direct:sampleInput", metacards, "Operation", "CREATE");
    } catch (CamelExecutionException cee) {
      Assert.isInstanceOf(FrameworkProducerException.class, cee.getCause());
      threwException = true;
    }

    Assert.isTrue(threwException);
  }

  @Test
  /** Operation: CREATE Body contains: Metacard */
  public void testCreateWithSingleMetacard() throws Exception {
    resetMocks();

    // Setup expectations to verify
    final MockEndpoint mockVerifierEndpoint = getMockEndpoint("mock:result");
    mockVerifierEndpoint.expectedMessageCount(1);

    final List<Metacard> metacards = new ArrayList<Metacard>();
    metacards.add(metacard1);

    // Mock catalog framework
    final CreateRequest createRequest = new CreateRequestImpl(metacards);
    final CreateResponse createResponse =
        new CreateResponseImpl(createRequest, new HashMap(), metacards);
    when(catalogFramework.create(any(CreateRequest.class))).thenReturn(createResponse);

    // Exercise the route with a CREATE operation
    template.sendBodyAndHeader("direct:sampleInput", metacard1, "Operation", "CREATE");

    // Verify that the number of metacards in the exchange after the records
    // is identical to the input
    assertListSize(mockVerifierEndpoint.getExchanges(), 1);
    final Exchange exchange = mockVerifierEndpoint.getExchanges().get(0);
    final List<Metacard> cardsCreated = (List<Metacard>) exchange.getIn().getBody();
    assertListSize(cardsCreated, 1);

    mockVerifierEndpoint.assertIsSatisfied();
  }

  @Test
  /** Operation: CREATE Body contains: Metacard */
  public void testCreateWithDifferentCaseOperation() throws Exception {
    resetMocks();

    // Setup expectations to verify
    final MockEndpoint mockVerifierEndpoint = getMockEndpoint("mock:result");
    mockVerifierEndpoint.expectedMessageCount(1);

    final List<Metacard> metacards = new ArrayList<Metacard>();
    metacards.add(metacard1);

    // Mock catalog framework
    final CreateRequest createRequest = new CreateRequestImpl(metacards);
    final CreateResponse createResponse =
        new CreateResponseImpl(createRequest, new HashMap(), metacards);
    when(catalogFramework.create(any(CreateRequest.class))).thenReturn(createResponse);

    // Exercise the route with a CREATE operation
    template.sendBodyAndHeader("direct:sampleInput", metacard1, "Operation", "create");

    // Verify that the number of metacards in the exchange after the records
    // is identical to the input
    assertListSize(mockVerifierEndpoint.getExchanges(), 1);
    final Exchange exchange = mockVerifierEndpoint.getExchanges().get(0);
    final List<Metacard> cardsCreated = (List<Metacard>) exchange.getIn().getBody();
    assertListSize(cardsCreated, 1);

    mockVerifierEndpoint.assertIsSatisfied();
  }

  @Test
  /** Operation: CREATE Body contains: Metacard */
  public void testCreateWithWrongTypeOperation() throws Exception {
    resetMocks();

    // Setup expectations to verify
    final MockEndpoint mockVerifierEndpoint = getMockEndpoint("mock:result");
    mockVerifierEndpoint.expectedMessageCount(1);

    final List<Metacard> metacards = new ArrayList<Metacard>();
    metacards.add(metacard1);

    // Mock catalog framework
    final CreateRequest createRequest = new CreateRequestImpl(metacards);
    final CreateResponse createResponse =
        new CreateResponseImpl(createRequest, new HashMap(), metacards);
    when(catalogFramework.create(any(CreateRequest.class))).thenReturn(createResponse);

    // Exercise the route with a CREATE operation
    template.sendBodyAndHeader("direct:sampleInput", metacard1, "Operation", new Boolean("CREATE"));

    // Verify that the number of metacards in the exchange after the records
    // is identical to the input
    assertListSize(mockVerifierEndpoint.getExchanges(), 1);
    final Exchange exchange = mockVerifierEndpoint.getExchanges().get(0);
    final List<Metacard> cardsCreated = (List<Metacard>) exchange.getIn().getBody();
    assertListSize(cardsCreated, 0);

    mockVerifierEndpoint.assertIsSatisfied();
  }

  @Test
  /** Operation: CREATE Body contains: Metacard */
  public void testCreateWithInvalidOperation() throws Exception {
    resetMocks();

    // Setup expectations to verify
    final MockEndpoint mockVerifierEndpoint = getMockEndpoint("mock:result");
    mockVerifierEndpoint.expectedMessageCount(1);

    final List<Metacard> metacards = new ArrayList<Metacard>();
    metacards.add(metacard1);

    // Mock catalog framework
    final CreateRequest createRequest = new CreateRequestImpl(metacards);
    final CreateResponse createResponse =
        new CreateResponseImpl(createRequest, new HashMap(), metacards);
    when(catalogFramework.create(any(CreateRequest.class))).thenReturn(createResponse);

    // Exercise the route with a CREATE operation
    template.sendBodyAndHeader("direct:sampleInput", metacard1, "Operation", "WRONG OPERATION");

    // Verify that the number of metacards in the exchange after the records
    // is identical to the input
    assertListSize(mockVerifierEndpoint.getExchanges(), 1);
    final Exchange exchange = mockVerifierEndpoint.getExchanges().get(0);
    final List<Metacard> cardsCreated = (List<Metacard>) exchange.getIn().getBody();
    assertListSize(cardsCreated, 0);

    mockVerifierEndpoint.assertIsSatisfied();
  }

  @Test(expected = CamelExecutionException.class)
  /** Operation: CREATE Body contains: Metacard */
  public void testCreateWithIngestException() throws Exception {
    resetMocks();

    // Setup expectations to verify
    final MockEndpoint mockVerifierEndpoint = getMockEndpoint("mock:result");
    mockVerifierEndpoint.expectedMessageCount(1);

    final List<Metacard> metacards = new ArrayList<Metacard>();
    metacards.add(metacard1);

    // Mock catalog framework
    when(catalogFramework.create(any(CreateRequest.class))).thenThrow(new IngestException());

    // Exercise the route with a CREATE operation
    template.sendBodyAndHeader("direct:sampleInput", metacard1, "Operation", "CREATE");
  }

  @Test
  /** Operation: UPDATE Body contains: List<Metacard> */
  public void testUpdateWithListOfMetacards() throws Exception {
    resetMocks();

    // Setup expectations to verify
    final MockEndpoint mockVerifierEndpoint = getMockEndpoint("mock:result");
    mockVerifierEndpoint.expectedMessageCount(1);

    final List<Metacard> metacards = new ArrayList<Metacard>();
    metacards.add(metacard1);

    // setup mock catalog framework
    final Update update = new UpdateImpl(metacard1, metacard2);
    List<Update> updates = new ArrayList<Update>();
    updates.add(update);

    final String[] metacardIds = new String[metacards.size()];
    for (int i = 0; i < metacards.size(); i++) {
      metacardIds[i] = metacards.get(i).getId();
    }

    UpdateRequest updateRequest = new UpdateRequestImpl(metacardIds, metacards);
    UpdateResponse updateResponse = new UpdateResponseImpl(updateRequest, new HashMap(), updates);
    when(catalogFramework.update(any(UpdateRequest.class))).thenReturn(updateResponse);

    // Exercise the route with a UPDATE operation
    template.sendBodyAndHeader("direct:sampleInput", metacards, "Operation", "UPDATE");

    // Verify that the number of metacards in the exchange after the records
    // is identical to the input
    assertListSize(mockVerifierEndpoint.getExchanges(), 1);
    final Exchange exchange = mockVerifierEndpoint.getExchanges().get(0);
    final List<Update> cardsUpdated = (List<Update>) exchange.getIn().getBody();
    assertListSize(cardsUpdated, 1);

    mockVerifierEndpoint.assertIsSatisfied();
  }

  @Test
  /** Operation: UPDATE Body contains: List<Metacard> with no members */
  public void testUpdateWithEmptyListOfMetacards() throws Exception {
    resetMocks();

    final List<Metacard> metacards = new ArrayList<Metacard>();
    boolean threwException = false;

    // Exercise the route with a UPDATE operation
    try {
      template.sendBodyAndHeader("direct:sampleInput", metacards, "Operation", "UPDATE");
    } catch (CamelExecutionException cee) {
      Assert.isInstanceOf(FrameworkProducerException.class, cee.getCause());
      threwException = true;
    }

    Assert.isTrue(threwException);
  }

  @Test
  /** Operation: UPDATE Body contains: List<String> */
  public void testUpdateWithInvalidListType() throws Exception {
    resetMocks();

    final List<String> metacards = new ArrayList<String>();
    metacards.add(new String("WRONG TYPE"));
    boolean threwException = false;

    // Exercise the route with a UPDATE operation
    try {
      template.sendBodyAndHeader("direct:sampleInput", metacards, "Operation", "UPDATE");
    } catch (CamelExecutionException cee) {
      Assert.isInstanceOf(FrameworkProducerException.class, cee.getCause());
      threwException = true;
    }

    Assert.isTrue(threwException);
  }

  @Test
  /** Operation: UPDATE Body contains: List<Metacard> with null member */
  public void testUpdateWithNullInListOfMetacards() throws Exception {
    resetMocks();

    final List<Metacard> metacards = new ArrayList<Metacard>();
    metacards.add(null);

    boolean threwException = false;

    // Exercise the route with a UPDATE operation
    try {
      template.sendBodyAndHeader("direct:sampleInput", metacards, "Operation", "UPDATE");
    } catch (CamelExecutionException cee) {
      Assert.isInstanceOf(FrameworkProducerException.class, cee.getCause());
      threwException = true;
    }

    Assert.isTrue(threwException);
  }

  @Test
  /** Operation: UPDATE Body contains: Metacard */
  public void testUpdateWithSingleMetacard() throws Exception {
    resetMocks();

    // Setup expectations to verify
    final MockEndpoint mockVerifierEndpoint = getMockEndpoint("mock:result");
    mockVerifierEndpoint.expectedMessageCount(1);

    final List<Metacard> metacards = new ArrayList<Metacard>();
    metacards.add(metacard1);

    // setup mock catalog framework
    final Update update = new UpdateImpl(metacard1, metacard2);
    List<Update> updates = new ArrayList<Update>();
    updates.add(update);

    final String[] metacardIds = new String[metacards.size()];
    for (int i = 0; i < metacards.size(); i++) {
      metacardIds[i] = metacards.get(i).getId();
    }

    UpdateRequest updateRequest = new UpdateRequestImpl(metacardIds, metacards);
    UpdateResponse updateResponse = new UpdateResponseImpl(updateRequest, new HashMap(), updates);
    when(catalogFramework.update(any(UpdateRequest.class))).thenReturn(updateResponse);

    // Exercise the route with a UPDATE operation
    template.sendBodyAndHeader("direct:sampleInput", metacard1, "Operation", "UPDATE");

    // Verify that the number of metacards in the exchange after the records
    // is identical to the input
    assertListSize(mockVerifierEndpoint.getExchanges(), 1);
    final Exchange exchange = mockVerifierEndpoint.getExchanges().get(0);
    final List<Update> cardsUpdated = (List<Update>) exchange.getIn().getBody();
    assertListSize(cardsUpdated, 1);

    mockVerifierEndpoint.assertIsSatisfied();
  }

  @Test(expected = CamelExecutionException.class)
  /** Operation: UPDATE Body contains: Metacard */
  public void testUpdateWithIngestException() throws Exception {
    resetMocks();

    // Setup expectations to verify
    final MockEndpoint mockVerifierEndpoint = getMockEndpoint("mock:result");
    mockVerifierEndpoint.expectedMessageCount(1);

    final List<Metacard> metacards = new ArrayList<Metacard>();
    metacards.add(metacard1);

    // setup mock catalog framework
    final Update update = new UpdateImpl(metacard1, metacard2);
    List<Update> updates = new ArrayList<Update>();
    updates.add(update);

    final String[] metacardIds = new String[metacards.size()];
    for (int i = 0; i < metacards.size(); i++) {
      metacardIds[i] = metacards.get(i).getId();
    }

    UpdateRequest updateRequest = new UpdateRequestImpl(metacardIds, metacards);
    UpdateResponse updateResponse = new UpdateResponseImpl(updateRequest, new HashMap(), updates);
    when(catalogFramework.update(any(UpdateRequest.class))).thenThrow(new IngestException());

    // Exercise the route with a UPDATE operation
    template.sendBodyAndHeader("direct:sampleInput", metacards, "Operation", "UPDATE");
  }

  @Test
  /** Operation: UPDATE Body contains: String */
  public void testUpdateWithInvalidType() throws Exception {
    resetMocks();

    boolean threwException = false;

    // Exercise the route with a UPDATE operation
    try {
      template.sendBodyAndHeader(
          "direct:sampleInput", new String("WRONG TYPE"), "Operation", "UPDATE");
    } catch (CamelExecutionException cee) {
      Assert.isInstanceOf(FrameworkProducerException.class, cee.getCause());
      threwException = true;
    }

    Assert.isTrue(threwException);
  }

  @Test
  /** Operation: UPDATE Body contains: null */
  public void testUpdateWithNull() throws Exception {
    resetMocks();

    boolean threwException = false;

    // Exercise the route with a UPDATE operation
    try {
      template.sendBodyAndHeader("direct:sampleInput", null, "Operation", "UPDATE");
    } catch (CamelExecutionException cee) {
      Assert.isInstanceOf(FrameworkProducerException.class, cee.getCause());
      threwException = true;
    }

    Assert.isTrue(threwException);
  }

  @Test
  /** Operation: DELETE Body contains: 12345678900987654321abcdeffedcba */
  public void testDeleteWithSingleId() throws Exception {
    resetMocks();

    // Setup expectations to verify
    final MockEndpoint mockVerifierEndpoint = getMockEndpoint("mock:result");
    mockVerifierEndpoint.expectedMessageCount(1);

    final List<Metacard> metacards = new ArrayList<Metacard>();
    metacards.add(metacard1);

    // setup mock catalog framework
    final String[] metacardIds = new String[metacards.size()];
    for (int i = 0; i < metacards.size(); i++) {
      metacardIds[i] = metacards.get(i).getId();
    }

    DeleteRequest deleteRequest = new DeleteRequestImpl(metacardIds);
    DeleteResponse deleteResponse = new DeleteResponseImpl(deleteRequest, new HashMap(), metacards);
    when(catalogFramework.delete(any(DeleteRequest.class))).thenReturn(deleteResponse);

    // Exercise the route with a DELETE operation
    template.sendBodyAndHeader("direct:sampleInput", metacardIds, "Operation", "DELETE");

    // Verify that the number of metacards in the exchange after the records
    // is identical to the input
    assertListSize(mockVerifierEndpoint.getExchanges(), 1);
    final Exchange exchange = mockVerifierEndpoint.getExchanges().get(0);
    final List<Update> cardsDeleted = (List<Update>) exchange.getIn().getBody();
    assertListSize(cardsDeleted, 1);

    mockVerifierEndpoint.assertIsSatisfied();
  }

  @Test(expected = CamelExecutionException.class)
  /** Operation: DELETE Body contains: 12345678900987654321abcdeffedcba */
  public void testDeleteWithIngestException() throws Exception {
    resetMocks();

    // Setup expectations to verify
    final MockEndpoint mockVerifierEndpoint = getMockEndpoint("mock:result");
    mockVerifierEndpoint.expectedMessageCount(1);

    final List<Metacard> metacards = new ArrayList<Metacard>();
    metacards.add(metacard1);

    // setup mock catalog framework
    final String[] metacardIds = new String[metacards.size()];
    for (int i = 0; i < metacards.size(); i++) {
      metacardIds[i] = metacards.get(i).getId();
    }

    DeleteRequest deleteRequest = new DeleteRequestImpl(metacardIds);
    DeleteResponse deleteResponse = new DeleteResponseImpl(deleteRequest, new HashMap(), metacards);
    when(catalogFramework.delete(any(DeleteRequest.class))).thenThrow(new IngestException());

    // Exercise the route with a DELETE operation
    template.sendBodyAndHeader("direct:sampleInput", metacardIds, "Operation", "DELETE");
  }

  @Test
  /** Operation: DELETE Body contains: null */
  public void testDeleteWithNull() throws Exception {
    resetMocks();

    boolean threwException = false;

    // Exercise the route with a DELETE operation
    try {
      template.sendBodyAndHeader("direct:sampleInput", null, "Operation", "DELETE");
    } catch (CamelExecutionException cee) {
      Assert.isInstanceOf(FrameworkProducerException.class, cee.getCause());
      threwException = true;
    }

    Assert.isTrue(threwException);
  }

  @Test
  /** Operation: DELETE Body contains: Integer */
  public void testDeleteWithInvalidType() throws Exception {
    resetMocks();

    class InvalidObject {
      public String toString() {
        return null;
      }
    }

    boolean threwException = false;

    // Exercise the route with a DELETE operation
    try {
      template.sendBodyAndHeader("direct:sampleInput", new InvalidObject(), "Operation", "DELETE");
    } catch (CamelExecutionException cee) {
      Assert.isInstanceOf(FrameworkProducerException.class, cee.getCause());
      threwException = true;
    }

    Assert.isTrue(threwException);
  }

  @Test
  /** Operation: DELETE Body contains: List<String> */
  public void testDeleteWithListOfIds() throws Exception {
    resetMocks();

    // Setup expectations to verify
    final MockEndpoint mockVerifierEndpoint = getMockEndpoint("mock:result");
    mockVerifierEndpoint.expectedMessageCount(1);

    final List<Metacard> metacards = new ArrayList<Metacard>();
    metacards.add(metacard1);
    metacards.add(metacard2);

    // setup mock catalog framework
    final String[] metacardIds = new String[metacards.size()];
    for (int i = 0; i < metacards.size(); i++) {
      metacardIds[i] = metacards.get(i).getId();
    }
    final List<String> metacardIdList = Arrays.asList(metacardIds);

    DeleteRequest deleteRequest = new DeleteRequestImpl(metacardIds);
    DeleteResponse deleteResponse = new DeleteResponseImpl(deleteRequest, new HashMap(), metacards);
    when(catalogFramework.delete(any(DeleteRequest.class))).thenReturn(deleteResponse);

    // Exercise the route with a DELETE operation
    template.sendBodyAndHeader("direct:sampleInput", metacardIdList, "Operation", "DELETE");

    // Verify that the number of metacards in the exchange after the records
    // is identical to the input
    assertListSize(mockVerifierEndpoint.getExchanges(), 1);
    final Exchange exchange = mockVerifierEndpoint.getExchanges().get(0);
    final List<Update> cardsDeleted = (List<Update>) exchange.getIn().getBody();
    assertListSize(cardsDeleted, 2);

    mockVerifierEndpoint.assertIsSatisfied();
  }

  @Test
  /** Operation: DELETE Body contains: List<String> with no members */
  public void testDeleteWithEmptyListOfIds() throws Exception {
    resetMocks();

    final List<String> metacardIdList = new ArrayList<String>();
    boolean threwException = false;

    // Exercise the route with a DELETE operation
    try {
      template.sendBodyAndHeader("direct:sampleInput", metacardIdList, "Operation", "DELETE");
    } catch (CamelExecutionException cee) {
      Assert.isInstanceOf(FrameworkProducerException.class, cee.getCause());
      threwException = true;
    }

    Assert.isTrue(threwException);
  }

  @Test
  /** Operation: DELETE Body contains: List<Integer> */
  public void testDeleteWithInvalidListType() throws Exception {
    resetMocks();

    final List<Integer> metacards = new ArrayList<Integer>();
    metacards.add(1);
    metacards.add(2);

    boolean threwException = false;

    // Exercise the route with a DELETE operation
    try {
      template.sendBodyAndHeader("direct:sampleInput", metacards, "Operation", "DELETE");
    } catch (CamelExecutionException cee) {
      Assert.isInstanceOf(FrameworkProducerException.class, cee.getCause());
      threwException = true;
    }

    Assert.isTrue(threwException);
  }

  @Test
  /** Operation: DELETE Body contains: List<String> with null member */
  public void testDeleteWithNullInListOfIds() throws Exception {
    resetMocks();

    final List<String> metacardIdList = new ArrayList<String>();
    metacardIdList.add(null);

    boolean threwException = false;

    // Exercise the route with a DELETE operation
    try {
      template.sendBodyAndHeader("direct:sampleInput", metacardIdList, "Operation", "DELETE");
    } catch (CamelExecutionException cee) {
      Assert.isInstanceOf(FrameworkProducerException.class, cee.getCause());
      threwException = true;
    }

    Assert.isTrue(threwException);
  }
}
