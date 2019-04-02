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
package ddf.camel.component.catalog.framework;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import java.util.ArrayList;
import java.util.List;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConversionException;
import org.apache.camel.impl.DefaultProducer;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Producer for the custom Camel CatalogComponent. This {@link org.apache.camel.Producer} would map
 * to a Camel <to> route node with a URI like <code>catalog:framework</code>. The message sent to
 * this component should have header named "operation" with a value of "CREATE", "UPDATE" or
 * "DELETE".
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
public class FrameworkProducer extends DefaultProducer {

  private static final Logger LOGGER = LoggerFactory.getLogger(FrameworkProducer.class);

  private static final String CREATE_OPERATION = "CREATE";

  private static final String UPDATE_OPERATION = "UPDATE";

  private static final String DELETE_OPERATION = "DELETE";

  private static final String OPERATION_HEADER_KEY = "operation";

  private CatalogFramework catalogFramework;

  private static final String CATALOG_RESPONSE_NULL = "Catalog response object is null";

  /**
   * Constructs the {@link org.apache.camel.Producer} for the custom Camel CatalogComponent.
   *
   * @param endpoint the Camel endpoint that created this consumer
   * @param catalogFramework the DDF Catalog Framework to use
   */
  public FrameworkProducer(Endpoint endpoint, CatalogFramework catalogFramework) {
    super(endpoint);
    this.catalogFramework = catalogFramework;
  }

  @Override
  public void process(Exchange exchange) throws FrameworkProducerException {
    try {
      LOGGER.debug("Entering process method");

      final Object operationValueObj = exchange.getIn().getHeader(OPERATION_HEADER_KEY);
      String operation;

      if (operationValueObj == null) {
        exchange.getIn().setBody(new ArrayList<Metacard>());
        throw new FrameworkProducerException(
            String.format("Missing expected [%s] header!", OPERATION_HEADER_KEY));
      }

      operation = operationValueObj.toString();

      if (operation.trim().equalsIgnoreCase(CREATE_OPERATION)) {
        create(exchange);
      } else if (operation.trim().equalsIgnoreCase(UPDATE_OPERATION)) {
        update(exchange);
      } else if (operation.trim().equalsIgnoreCase(DELETE_OPERATION)) {
        delete(exchange);
      } else {
        exchange.getIn().setBody(new ArrayList<Metacard>());
        LOGGER.debug(
            "Missing expected header \"operation:<CREATE|UPDATE|DELETE>\" but received {}",
            operation);
      }

      LOGGER.debug("Exiting process method");
    } catch (ClassCastException cce) {
      exchange.getIn().setBody(new ArrayList<Metacard>());
      LOGGER.debug("Received a non-String as the operation type");
      throw new FrameworkProducerException(cce);
    } catch (SourceUnavailableException | IngestException e) {
      LOGGER.debug("Exception cataloging metacards", e);
      throw new FrameworkProducerException(e);
    }
  }

  /**
   * Creates metacard(s) in the catalog using the Catalog Framework.
   *
   * @param exchange The {@link org.apache.camel.Exchange} can contain a {@link
   *     org.apache.camel.Message} with a body of type {@link java.util.List} of Metacard or a
   *     single Metacard.
   * @throws ddf.catalog.source.SourceUnavailableException
   * @throws ddf.catalog.source.IngestException
   * @throws ddf.camel.component.catalog.framework.FrameworkProducerException
   */
  private void create(final Exchange exchange)
      throws SourceUnavailableException, IngestException, FrameworkProducerException {
    CreateResponse createResponse = null;

    // read in data
    final List<Metacard> metacardsToBeCreated = readBodyDataAsMetacards(exchange);

    if (!validateList(metacardsToBeCreated, Metacard.class)) {
      processCatalogResponse(createResponse, exchange);
      throw new FrameworkProducerException("Validation of Metacard list failed");
    }

    LOGGER.debug("Validation of Metacard list passed...");

    final CreateRequest createRequest = new CreateRequestImpl(metacardsToBeCreated);
    int expectedNumberOfCreatedMetacards = metacardsToBeCreated.size();

    if (expectedNumberOfCreatedMetacards < 1) {
      LOGGER.debug("Empty list of Metacards...nothing to process");
      processCatalogResponse(createResponse, exchange);
      return;
    }

    LOGGER.debug("Making CREATE call to Catalog Framework...");
    createResponse = catalogFramework.create(createRequest);

    if (createResponse == null) {
      LOGGER.debug("CreateResponse is null from catalog framework");
      processCatalogResponse(createResponse, exchange);
      return;
    }

    final List<Metacard> createdMetacards = createResponse.getCreatedMetacards();

    if (createdMetacards == null) {
      LOGGER.debug("CreateResponse returned null metacards list");
      processCatalogResponse(createResponse, exchange);
      return;
    }

    final int numberOfCreatedMetacards = createdMetacards.size();
    if (numberOfCreatedMetacards != expectedNumberOfCreatedMetacards) {
      LOGGER.debug(
          "Expected {} metacards created but only {} were successfully created",
          expectedNumberOfCreatedMetacards,
          numberOfCreatedMetacards);
      processCatalogResponse(createResponse, exchange);
      return;
    }

    LOGGER.debug("Created {} metacards", numberOfCreatedMetacards);
    processCatalogResponse(createResponse, exchange);
  }

  /**
   * Updates metacard(s) in the catalog using the Catalog Framework.
   *
   * @param exchange The {@link org.apache.camel.Exchange} can contain a {@link
   *     org.apache.camel.Message} with a body of type {@link java.util.List} of Metacard or a
   *     single Metacard.
   * @throws ddf.catalog.source.SourceUnavailableException
   * @throws ddf.catalog.source.IngestException
   * @throws ddf.camel.component.catalog.framework.FrameworkProducerException
   */
  private void update(final Exchange exchange)
      throws SourceUnavailableException, IngestException, FrameworkProducerException {
    UpdateResponse updateResponse = null;

    // read in data from exchange
    final List<Metacard> metacardsToBeUpdated = readBodyDataAsMetacards(exchange);

    // process data if valid
    if (!validateList(metacardsToBeUpdated, Metacard.class)) {
      processCatalogResponse(updateResponse, exchange);
      throw new FrameworkProducerException("Validation of Metacard list failed");
    }

    LOGGER.debug("Validation of Metacard list passed...");

    final String[] metacardIds = new String[metacardsToBeUpdated.size()];
    for (int i = 0; i < metacardsToBeUpdated.size(); i++) {
      metacardIds[i] = metacardsToBeUpdated.get(i).getId();
    }

    final UpdateRequest updateRequest = new UpdateRequestImpl(metacardIds, metacardsToBeUpdated);
    final int expectedNumberOfUpdatedMetacards = metacardsToBeUpdated.size();

    if (expectedNumberOfUpdatedMetacards < 1) {
      LOGGER.debug("Empty list of Metacards...nothing to process");
      processCatalogResponse(updateResponse, exchange);
      return;
    }

    LOGGER.debug("Making UPDATE call to Catalog Framework...");
    updateResponse = catalogFramework.update(updateRequest);

    if (updateResponse == null) {
      LOGGER.debug("UpdateResponse is null from catalog framework");
      processCatalogResponse(updateResponse, exchange);
      return;
    }

    final List<Update> updatedMetacards = updateResponse.getUpdatedMetacards();

    if (updatedMetacards == null) {
      LOGGER.debug("UpdateResponse returned null metacards list");
      processCatalogResponse(updateResponse, exchange);
      return;
    }

    final int numberOfUpdatedMetacards = updatedMetacards.size();
    if (numberOfUpdatedMetacards != expectedNumberOfUpdatedMetacards) {
      LOGGER.debug(
          "Expected {} metacards updated but only {} were successfully updated",
          expectedNumberOfUpdatedMetacards,
          numberOfUpdatedMetacards);
      processCatalogResponse(updateResponse, exchange);
      return;
    }

    LOGGER.debug("Updated {} metacards", numberOfUpdatedMetacards);
    processCatalogResponse(updateResponse, exchange);
  }

  /**
   * Deletes metacard(s) in the catalog using the Catalog Framework.
   *
   * @param exchange The {@link org.apache.camel.Exchange} can contain a {@link
   *     org.apache.camel.Message} with a body of type {@link java.util.List} of {@link String} or a
   *     single {@link String}. Each String represents the ID of a Metacard to be deleted.
   * @throws ddf.catalog.source.SourceUnavailableException
   * @throws ddf.catalog.source.IngestException
   * @throws ddf.camel.component.catalog.framework.FrameworkProducerException
   */
  private void delete(final Exchange exchange)
      throws SourceUnavailableException, IngestException, FrameworkProducerException {
    DeleteResponse deleteResponse = null;

    // read in data
    final List<String> metacardIdsToBeDeleted = readBodyDataAsMetacardIds(exchange);

    // process if data is valid
    if (!validateList(metacardIdsToBeDeleted, String.class)) {
      LOGGER.debug("Validation of Metacard id list failed");
      processCatalogResponse(deleteResponse, exchange);
      throw new FrameworkProducerException("Validation of Metacard id list failed");
    }

    LOGGER.debug("Validation of Metacard id list passed...");

    final String[] metacardIdsToBeDeletedArray = new String[metacardIdsToBeDeleted.size()];
    final DeleteRequest deleteRequest =
        new DeleteRequestImpl(metacardIdsToBeDeleted.toArray(metacardIdsToBeDeletedArray));
    final int expectedNumberOfDeletedMetacards = metacardIdsToBeDeleted.size();

    if (expectedNumberOfDeletedMetacards < 1) {
      LOGGER.debug("Empty list of Metacard id...nothing to process");
      processCatalogResponse(deleteResponse, exchange);
      return;
    }

    LOGGER.debug("Making DELETE call to Catalog Framework...");
    deleteResponse = catalogFramework.delete(deleteRequest);

    if (deleteResponse == null) {
      LOGGER.debug("DeleteResponse is null from catalog framework");
      processCatalogResponse(deleteResponse, exchange);
      return;
    }

    final List<Metacard> deletedMetacards = deleteResponse.getDeletedMetacards();

    if (deletedMetacards == null) {
      LOGGER.debug("DeleteResponse returned null metacards list");
      processCatalogResponse(deleteResponse, exchange);
      return;
    }

    final int numberOfDeletedMetacards = deletedMetacards.size();

    if (numberOfDeletedMetacards != expectedNumberOfDeletedMetacards) {
      LOGGER.debug(
          "Expected {} metacards deleted but only {} were successfully deleted",
          expectedNumberOfDeletedMetacards,
          numberOfDeletedMetacards);
      processCatalogResponse(deleteResponse, exchange);
      return;
    }

    LOGGER.debug("Deleted {} metacards", numberOfDeletedMetacards);
    processCatalogResponse(deleteResponse, exchange);
  }

  /**
   * Makes sure that a Metacard or Metacard ID list contains objects of a particular type
   *
   * @param list {@link java.util.List} of Metacard IDs
   * @param cls {@link java.lang.Class} type that the objects inside the list should be
   * @return true if the list is not empty and has valid types inside, else false.
   */
  private boolean validateList(List<?> list, Class<?> cls) {
    if (CollectionUtils.isEmpty(list)) {
      LOGGER.debug("No Metacard or Metacard IDs to process");
      return false;
    }

    for (final Object o : list) {
      if (!cls.isInstance(o)) {
        LOGGER.debug("Received a list of non-{} objects", cls.getName());
        return false;
      }
    }

    return true;
  }

  /**
   * Processes the response from the Catalog Framework and updates the exchange accordingly.
   *
   * @param response response of type CreateResponse
   * @param exchange the exchange to update
   */
  private void processCatalogResponse(final CreateResponse response, final Exchange exchange) {
    if (response == null) {
      LOGGER.debug(CATALOG_RESPONSE_NULL);
      exchange.getIn().setBody((List) (new ArrayList<Metacard>()));
      return;
    }

    if (response.getCreatedMetacards() == null) {
      LOGGER.debug("No Metacards created by catalog framework");
      exchange.getIn().setBody(new ArrayList<Metacard>());
      return;
    }

    exchange.getIn().setBody(response.getCreatedMetacards());
  }

  /**
   * Processes the response from the Catalog Framework and updates the exchange accordingly.
   *
   * @param response response of type UpdateResponse
   * @param exchange the exchange to update
   */
  private void processCatalogResponse(final UpdateResponse response, final Exchange exchange) {
    if (response == null) {
      LOGGER.debug(CATALOG_RESPONSE_NULL);
      exchange.getIn().setBody(new ArrayList<Metacard>());
      return;
    }

    if (response.getUpdatedMetacards() == null) {
      LOGGER.debug("No Metacards updated by catalog framework");
      exchange.getIn().setBody(new ArrayList<Metacard>());
      return;
    }

    exchange.getIn().setBody(response.getUpdatedMetacards());
  }

  /**
   * Processes the response from the Catalog Framework and updates the exchange accordingly.
   *
   * @param response response of type DeleteResponse
   * @param exchange the exchange to update
   */
  private void processCatalogResponse(final DeleteResponse response, final Exchange exchange) {
    if (response == null) {
      LOGGER.debug(CATALOG_RESPONSE_NULL);
      exchange.getIn().setBody(new ArrayList<Metacard>());
      return;
    }

    if (response.getDeletedMetacards() == null) {
      LOGGER.debug("No Metacards deleted by catalog framework");
      exchange.getIn().setBody(new ArrayList<Metacard>());
      return;
    }

    exchange.getIn().setBody(response.getDeletedMetacards());
  }

  /**
   * Reads in Metacard ids from message body of exchange
   *
   * @param exchange the exchange with the message payload
   * @return {@link java.util.List} of {@link String} representing Metacard IDs
   */
  private List<String> readBodyDataAsMetacardIds(final Exchange exchange) {
    List<String> metacardIdsToBeProcessed = new ArrayList<>();

    try {
      if (exchange.getIn().getBody() == null) {
        LOGGER.debug("Body is null");
        return metacardIdsToBeProcessed;
      }

      // first see if we have a have List<String>
      LOGGER.debug("Reading in body data as List<?>...");
      metacardIdsToBeProcessed = exchange.getIn().getBody(List.class);
      if (metacardIdsToBeProcessed != null) {
        LOGGER.debug("Successfully read in body data as List<?>");
        return metacardIdsToBeProcessed;
      }

      LOGGER.debug("Problem reading in body data as List<?>");
      LOGGER.debug("Reading in body data as String...");

      // if we get here, see if we have a single ID as a String
      final String metacardIdToBeProcessed = exchange.getIn().getBody(String.class);

      if (metacardIdToBeProcessed != null) {
        metacardIdsToBeProcessed = new ArrayList<>();
        metacardIdsToBeProcessed.add(metacardIdToBeProcessed);
        LOGGER.debug("Successfully read in body data as String");
        return metacardIdsToBeProcessed;
      }

      // if we get here, we neither had String or List<?>, so set a
      // default list
      metacardIdsToBeProcessed = new ArrayList<>();
    } catch (TypeConversionException tce1) {
      LOGGER.debug("Invalid message body. Expected either String or List<String>", tce1);
    }

    return metacardIdsToBeProcessed;
  }

  /**
   * Reads in Metacard data from message body of exchange
   *
   * @param exchange the exchange containing the message data
   * @return {@link java.util.List} of Metacard objects
   */
  private List<Metacard> readBodyDataAsMetacards(final Exchange exchange) {
    List<Metacard> metacardsToProcess = new ArrayList<>();

    try {
      if (exchange.getIn().getBody() == null) {
        LOGGER.debug("Body is null");
        return metacardsToProcess;
      }

      // first try to read in a single Metacard
      LOGGER.debug("Reading in body data as Metacard...");
      final Metacard metacard = exchange.getIn().getBody(Metacard.class);

      if (metacard != null) {
        metacardsToProcess.add(metacard);
        LOGGER.debug("Successfully read in body data as Metacard ");
        return metacardsToProcess;
      }

      LOGGER.debug("Problem reading in body data as Metacard");

      // if we get here, then we possibly have List<Metacard>
      LOGGER.debug("Reading in body data as List<Metacard>...");
      metacardsToProcess = exchange.getIn().getBody(List.class);
      if (metacardsToProcess == null) {
        LOGGER.debug("Problem reading in body data as List<?>");
        metacardsToProcess = new ArrayList<>();
        return metacardsToProcess;
      }
      LOGGER.debug("Successfully read in body data as List<?>");
    } catch (TypeConversionException tce1) {
      LOGGER.debug("Invalid message body. Expected either Metacard or List<Metacard>", tce1);
    }

    return metacardsToProcess;
  }
}
