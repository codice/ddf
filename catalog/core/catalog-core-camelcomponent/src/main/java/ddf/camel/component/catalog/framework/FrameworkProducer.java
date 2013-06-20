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
package ddf.camel.component.catalog.framework;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.*;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConversionException;
import org.apache.camel.impl.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Producer for the custom Camel CatalogComponent. This {@link org.apache.camel.Producer} would map to
 * a Camel <to> route node with a URI like <code>catalog:framework</code>.  The message sent to
 * this component should have header named "operation" with a value of "CREATE", "UPDATE" or "DELETE".
 *
 * For the CREATE and UPDATE operation, the message body can contain a {@link java.util.List} of Metacards
 * or a single Metacard object.
 *
 * For the DELETE operation, the message body can contain a {@link java.util.List} of {@link String}
 * or a single {@link String} object.  The {@link String} objects represent the IDs of Metacards
 * that you would want to delete.
 *
 * The exchange's "in" message will be set with the affected Metacards.  In the case of a CREATE, it will be updated
 * with the created Metacards.  In the case of the UPDATE, it will be updated with the updated Metacards and with the
 * DELETE it will contain the deleted Metacards.
 *
 * <table border="1">
 *     <tr>
 *         <th>USE CASE</th>
 *         <th>ROUTE NODE</th>
 *         <th>HEADER</th>
 *         <th>MESSAGE BODY</th>
 *         <th>EXCHANGE MODIFICATION</th>
 *     </tr>
 *     <tr>
 *         <td>Create Metacard(s)</td>
 *         <td>catalog:framework</td>
 *         <td>operation:CREATE</td>
 *         <td>List&ltMetacard&gt or Metacard</td>
 *         <td>exchange.getIn().getBody() updated with {@link java.util.List} of Metacards created</td>
 *     </tr>
 *     <tr>
 *         <td>Update Metacard(s)</td>
 *         <td>catalog:framework</td>
 *         <td>operation:UPDATE</td>
 *         <td>List&ltMetacard&gt or Metacard</td>
 *         <td>exchange.getIn().getBody() updated with {@link java.util.List} of Metacards updated</td>
 *     </tr>
 *     <tr>
 *         <td>Delete Metacard(s)</td>
 *         <td>catalog:framework</td>
 *         <td>operation:DELETE</td>
 *         <td>List&ltString&gt or String (IDs of Metacards to delete)</td>
 *         <td>exchange.getIn().getBody() updated with {@link java.util.List} of Metacards deleted</td>
 *     </tr>
 * </table>
 *
 * @author Sam Patel, Lockheed Martin
 * @author ddf.isgs@lmco.com
 */
public class FrameworkProducer extends DefaultProducer
{
    private static final transient Logger LOGGER = LoggerFactory.getLogger(FrameworkProducer.class);
    private static final String CREATE_OPERATION = "CREATE";
    private static final String UPDATE_OPERATION = "UPDATE";
    private static final String DELETE_OPERATION = "DELETE";
    private static final String OPERATION_HEADER_KEY = "operation";

    private CatalogFramework catalogFramework;

    /**
     * Constructs the {@link org.apache.camel.Producer} for the custom Camel CatalogComponent.
     *
     * @param endpoint the Camel endpoint that created this consumer
     * @param catalogFramework  the DDF Catalog Framework to use
     */
    public FrameworkProducer(Endpoint endpoint, CatalogFramework catalogFramework)
    {
        super(endpoint);
        this.catalogFramework = catalogFramework;
    }

    /**
     * Processes the exchange. Will use the Catalog Framework to create, update, or delete based upon header
     * information as well as message payload.  The exchange's "in" message will be set with the affected Metacards.
     * In the case of a CREATE, it will be updated with the created Metacards.  In the case of the UPDATE, it
     * will be updated with the updated Metacards and with the DELETE it will contain the deleted Metacards.
     * @param exchange the exchange to process
     */
    @Override
    public void process(Exchange exchange)
    {
        try
        {
            LOGGER.debug("Entering process method");

            final Object operationValueObj = exchange.getIn().getHeader(OPERATION_HEADER_KEY);
            String operation = null;

            if (operationValueObj != null)
            {
                operation = (String) operationValueObj;
                //final String operation = (String) exchange.getIn().getHeader(OPERATION_HEADER_KEY);

                if (operation.trim().equalsIgnoreCase(CREATE_OPERATION))
                {
                    create(exchange);
                }
                else if (operation.trim().equalsIgnoreCase(UPDATE_OPERATION))
                {
                    update(exchange);
                }
                else if (operation.trim().equalsIgnoreCase(DELETE_OPERATION))
                {
                    delete(exchange);
                }
                else {
                    exchange.getIn().setBody(new ArrayList<Metacard>());
                    LOGGER.debug("Missing expected header \"operation:<CREATE|UPDATE|DELETE>\" but received " + operation);
                }
            }
            else
            {
                exchange.getIn().setBody(new ArrayList<Metacard>());
                LOGGER.debug("Missing expected header \"operation:<CREATE|UPDATE|DELETE>\" but received " + operation);
            }

            LOGGER.debug("Exiting process method");
        }
        catch (ClassCastException cce)
        {
            exchange.getIn().setBody(new ArrayList<Metacard>());
            LOGGER.debug("Received a non-String as the operation type");
        }
        catch (SourceUnavailableException sue)
        {
            exchange.getIn().setBody(new ArrayList<Metacard>());
            LOGGER.debug("Exception cataloging metacards", sue);
        }
        catch (IngestException ie)
        {
            exchange.getIn().setBody(new ArrayList<Metacard>());
            LOGGER.debug("Exception cataloging metacards", ie);
        }
    }

    /**
     * Creates metacard(s) in the catalog using the Catalog Framework.
     * @param exchange The {@link org.apache.camel.Exchange} can contain a {@link org.apache.camel.Message} with a
     * body of type {@link java.util.List} of Metacard or a single Metacard.
     * @throws ddf.catalog.source.SourceUnavailableException
     * @throws ddf.catalog.source.IngestException
     */
    private void create(final Exchange exchange) throws SourceUnavailableException, IngestException
    {
        CreateResponse createResponse = null;

        // read in data
        final List<Metacard> metacardsToBeCreated = readBodyDataAsMetacards(exchange);

        // validate types in list
        final boolean metacardListIsValid = validateMetacardList(metacardsToBeCreated);

        // process data if valid
        if (metacardListIsValid)
        {
            LOGGER.debug("Validation of Metacard list passed...");
            final CreateRequest createRequest = new CreateRequestImpl(metacardsToBeCreated);

            int expectedNumberOfCreatedMetacards = metacardsToBeCreated.size();
            if (expectedNumberOfCreatedMetacards > 0)
            {
                LOGGER.debug("Making CREATE call to Catalog Framework...");
                createResponse = catalogFramework.create(createRequest);

                if (createResponse != null)
                {
                    List<Metacard> createdMetacards = createResponse.getCreatedMetacards();
                    if (createdMetacards != null)
                    {
                        int numberOfCreatedMetacards = createdMetacards.size();
                        if (numberOfCreatedMetacards != expectedNumberOfCreatedMetacards)
                        {
                            LOGGER.debug("Expected " + expectedNumberOfCreatedMetacards +
                                    " metacards created but only " + numberOfCreatedMetacards + " were successfully created");
                        }
                        else
                        {
                            LOGGER.debug("Created " + numberOfCreatedMetacards + " metacards");
                        }
                    }
                    else
                    {
                        LOGGER.debug("CreateResponse returned null metacards list");
                    }
                }
                else
                {
                    LOGGER.debug("CreateResponse is null");
                }
            }
        }
        else
        {
            LOGGER.debug("Validation of Metacard list failed");
        }

        processCatalogResponse(createResponse, exchange);
    }

    /**
     * Updates metacard(s) in the catalog using the Catalog Framework.
     * @param exchange The {@link org.apache.camel.Exchange} can contain a {@link org.apache.camel.Message} with a
     * body of type {@link java.util.List} of Metacard or a single Metacard.
     * @throws ddf.catalog.source.SourceUnavailableException
     * @throws ddf.catalog.source.IngestException
     */
    private void update(final Exchange exchange) throws SourceUnavailableException, IngestException
    {
        UpdateResponse updateResponse = null;

        // read in data from exchange
        final List<Metacard>  metacardsToBeUpdated = readBodyDataAsMetacards(exchange);

        // validate types in list
        final boolean metacardListIsValid = validateMetacardList(metacardsToBeUpdated);

        // process data if valid
        if (metacardListIsValid)
        {
            LOGGER.debug("Validation of Metacard list passed...");
            final String[] metacardIds = new String[metacardsToBeUpdated.size()];
            for (int i = 0; i < metacardsToBeUpdated.size(); i++)
            {
                metacardIds[i] = metacardsToBeUpdated.get(i).getId();
            }

            final UpdateRequest updateRequest = new UpdateRequestImpl(metacardIds, metacardsToBeUpdated);

            int expectedNumberOfUpdatedMetacards = metacardsToBeUpdated.size();
            if (expectedNumberOfUpdatedMetacards > 0)
            {
                LOGGER.debug("Making UPDATE call to Catalog Framework...");
                updateResponse = catalogFramework.update(updateRequest);

                if (updateResponse != null)
                {
                    List<Update> updatedMetacards = updateResponse.getUpdatedMetacards();
                    if (updatedMetacards != null)
                    {
                        int numberOfUpdatedMetacards = updatedMetacards.size();
                        if (numberOfUpdatedMetacards != expectedNumberOfUpdatedMetacards)
                        {
                            LOGGER.debug("Expected " + expectedNumberOfUpdatedMetacards +
                                    " metacards updated but only " + numberOfUpdatedMetacards + " were successfully updated");
                        }
                        else
                        {
                            LOGGER.debug("Updated " + numberOfUpdatedMetacards + " metacards");
                        }
                    }
                    else
                    {
                        LOGGER.debug("UpdateResponse returned null metacards list");
                    }
                }
                else
                {
                    LOGGER.debug("UpdateResponse is null");
                }
            }
        }
        else
        {
            LOGGER.debug("Validation of Metacard list failed");
        }

        processCatalogResponse(updateResponse, exchange);
    }

    /**
     * Deletes metacard(s) in the catalog using the Catalog Framework.
     * @param exchange The {@link org.apache.camel.Exchange} can contain a {@link org.apache.camel.Message} with a
     * body of type {@link java.util.List} of {@link String} or a single {@link String}.
     * Each String represents the ID of a Metacard to be deleted.
     * @throws ddf.catalog.source.SourceUnavailableException
     * @throws ddf.catalog.source.IngestException
     */
    private void delete(final Exchange exchange) throws SourceUnavailableException, IngestException
    {
        DeleteResponse deleteResponse = null;

        // read in data
        final List<String> metacardIdsToBeDeleted = readBodyDataAsMetacardIds(exchange);

        // validate data
        final boolean metacardIdListIsValid = validateMetacardIdList(metacardIdsToBeDeleted);

        // process if data is valid
        if (metacardIdListIsValid)
        {
            LOGGER.debug("Validation of Metacard id list passed...");
            final String[] metacardIdsToBeDeletedArray = new String[metacardIdsToBeDeleted.size()];
            final DeleteRequest deleteRequest = new DeleteRequestImpl(metacardIdsToBeDeleted.toArray(metacardIdsToBeDeletedArray));

            int expectedNumberOfDeletedMetacards = metacardIdsToBeDeleted.size();
            if (expectedNumberOfDeletedMetacards > 0)
            {
                LOGGER.debug("Making DELETE call to Catalog Framework...");
                deleteResponse = catalogFramework.delete(deleteRequest);

                if (deleteResponse != null)
                {
                    List<Metacard> deletedMetacards = deleteResponse.getDeletedMetacards();
                    if (deletedMetacards != null)
                    {
                        int numberOfDeletedMetacards = deletedMetacards.size();
                        if (numberOfDeletedMetacards != expectedNumberOfDeletedMetacards)
                        {
                            LOGGER.debug("Expected " + expectedNumberOfDeletedMetacards +
                                    " metacards deleted but only " + numberOfDeletedMetacards + " were successfully deleted");
                        }
                        else
                        {
                            LOGGER.debug("Deleted " + numberOfDeletedMetacards + " metacards");
                        }
                    }
                    else
                    {
                        LOGGER.debug("DeleteResponse returned null metacards list");
                    }
                }
                else
                {
                    LOGGER.debug("DeleteResponse is null");
                }
            }
        }
        else
        {
            LOGGER.debug("Validation of Metacard id list failed");
        }


        processCatalogResponse(deleteResponse, exchange);
    }

    /**
     * Makes sure that metacard list has objects of type Metacard
     * @param metacardList {@link java.util.List} of Metacard objects
     * @return true if the list is not empty and has valid types inside, else false.
     */
    private boolean validateMetacardList(List<?> metacardList)
    {
        if (metacardList.size() == 0)
        {
            LOGGER.debug("No Metacards to process");
            return false;
        }

        for (int i = 0; i < metacardList.size(); i++)
        {
            Object o = metacardList.get(i);
            if (!(o instanceof Metacard))
            {
                LOGGER.debug("Received a list of non-Metacard objects");
                return false;
            }
        }

        return true;
    }

    /**
     * Makes sure that metacard id list has objects of type {@link String}
     * @param metacardList {@link java.util.List} of Metacard IDs
     * @return true if the list is not empty and has valid types inside, else false.
     */
    private boolean validateMetacardIdList(List<?> metacardList)
    {
        if (metacardList.size() == 0)
        {
            LOGGER.debug("No Metacard IDs to process");
            return false;
        }

        for (int i = 0; i < metacardList.size(); i++)
        {
            Object o = metacardList.get(i);
            if (!(o instanceof String))
            {
                LOGGER.debug("Received a list of non-String objects");
                return false;
            }
        }

        return true;
    }

    /**
     * Processes the response from the Catalog Framework and updates the exchange accordingly.
     * @param response response of type CreateResponse, UpdateResponse, or DeleteResponse
     * @param exchange the exchange to update
     */
    private void processCatalogResponse(final Response response, final Exchange exchange)
    {
        boolean unexpectedResponse = false;

        if (response instanceof CreateResponse)
        {
            final CreateResponse cr = (CreateResponse) response;
            if (cr.getCreatedMetacards() != null)
            {
                exchange.getIn().setBody(cr.getCreatedMetacards());
            }
        }
        else if (response instanceof UpdateResponse)
        {
            final UpdateResponse ur = (UpdateResponse) response;
            if (ur.getUpdatedMetacards() != null)
            {
                exchange.getIn().setBody(ur.getUpdatedMetacards());
            }
        }
        else if (response instanceof DeleteResponse)
        {
            final DeleteResponse dr = (DeleteResponse) response;

            if (dr.getDeletedMetacards() != null)
            {
                exchange.getIn().setBody(dr.getDeletedMetacards());
            }
        }
        else
        {
            if (response != null)
            {
                LOGGER.debug("Received unexpected response from Catalog Framework: " + response);
                unexpectedResponse = true;
            }
        }

        // make sure we set something in body
        if ( (unexpectedResponse) || (response == null) || (exchange.getIn().getBody() == null))
        {
            final List<Metacard> emptyList = new ArrayList<Metacard>();
            exchange.getIn().setBody(emptyList);
        }
    }

    /**
     * Reads in Metacard ids from message body of exchange
     * @param exchange the exchange with the message payload
     * @return {@link java.util.List}  of {@link String} representing Metacard IDs
     */
    private List<String> readBodyDataAsMetacardIds(final Exchange exchange)
    {
        List<String> metacardIdsToBeProcessed = new ArrayList<String>();

        if (exchange.getIn().getBody() != null)
        {
            String metacardIdToBeProcessed = null;
            try
            {
                LOGGER.debug("Reading in body data as String...");
                metacardIdToBeProcessed = exchange.getIn().getBody(String.class);
                if (metacardIdToBeProcessed != null)
                {
                    metacardIdsToBeProcessed.add(metacardIdToBeProcessed);
                    LOGGER.debug("Successfully read in body data as String");
                }
                else
                {
                    LOGGER.debug("Problem reading in body data as String");
                }
            }
            catch (TypeConversionException tce1)
            {
                LOGGER.debug("Invalid message body. Expected either String or List<String>", tce1);
            }

            if (metacardIdToBeProcessed == null)
            {
                try
                {
                    LOGGER.debug("Reading in body data as List<?>...");
                    metacardIdsToBeProcessed = exchange.getIn().getBody(List.class);
                    if (metacardIdsToBeProcessed == null)
                    {
                        LOGGER.debug("Problem reading in body data as List<?>");
                        metacardIdsToBeProcessed = new ArrayList<String>();
                    }
                }
                catch (TypeConversionException tce2)
                {
                    LOGGER.debug("Invalid message body. Expected either String or List<String>", tce2);
                }
            }
        }
        else
        {
            LOGGER.debug("Body is null");
        }

        return metacardIdsToBeProcessed;
    }

    /**
     * Reads in Metacard data from message body of exchange
     * @param exchange the exchange containing the message data
     * @return {@link java.util.List} of Metacard objects
     */
    private List<Metacard> readBodyDataAsMetacards(final Exchange exchange)
    {
        List<Metacard> metacardsToProcess = new ArrayList<Metacard>();

        if (exchange.getIn().getBody() != null)
        {
            Metacard metacardToProcess = null;
            try
            {
                LOGGER.debug("Reading in body data as Metacard...");
                metacardToProcess = exchange.getIn().getBody(Metacard.class);
                if (metacardToProcess != null)
                {
                    metacardsToProcess.add(metacardToProcess);
                    LOGGER.debug("Successfully read in body data as Metacard ");
                }
                else
                {
                    LOGGER.debug("Problem reading in body data as Metacard");
                }

            }
            catch (TypeConversionException tce1)
            {
                LOGGER.debug("Invalid message body. Expected either Metacard or List<Metacard>", tce1);
            }

            if (metacardToProcess == null)
            {
                try
                {
                    LOGGER.debug("Reading in body data as List<Metacard>...");
                    metacardsToProcess = exchange.getIn().getBody(List.class);
                    if (metacardsToProcess == null)
                    {
                        LOGGER.debug("Problem reading in body data as List<?>");
                        metacardsToProcess = new ArrayList<Metacard>();
                    }
                    else
                    {
                        LOGGER.debug("Successfully read in body data as List<?>");
                    }
                }
                catch (TypeConversionException tce2)
                {
                    LOGGER.debug("Invalid message body. Expected either Metacard or List<?>", tce2);
                }
            }
        }
        else
        {
            LOGGER.debug("Body is null");
        }

        return metacardsToProcess;
    }

}
