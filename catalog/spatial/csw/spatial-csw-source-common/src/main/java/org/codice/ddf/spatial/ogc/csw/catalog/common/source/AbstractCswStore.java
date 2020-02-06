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
package org.codice.ddf.spatial.ogc.csw.catalog.common.source;

import com.thoughtworks.xstream.converters.Converter;
import ddf.catalog.Constants;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.types.Core;
import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.OperationTransaction;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.operation.impl.DeleteResponseImpl;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.UpdateResponseImpl;
import ddf.catalog.source.CatalogStore;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.encryption.EncryptionService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.xml.bind.JAXBElement;
import net.opengis.cat.csw.v_2_0_2.BriefRecordType;
import net.opengis.cat.csw.v_2_0_2.DeleteType;
import net.opengis.cat.csw.v_2_0_2.InsertResultType;
import net.opengis.cat.csw.v_2_0_2.QueryConstraintType;
import net.opengis.cat.csw.v_2_0_2.TransactionResponseType;
import net.opengis.cat.csw.v_2_0_2.dc.elements.SimpleLiteral;
import net.opengis.filter.v_1_1_0.FilterType;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.security.Security;
import org.codice.ddf.spatial.ogc.csw.catalog.actions.DeleteAction;
import org.codice.ddf.spatial.ogc.csw.catalog.common.Csw;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.codice.ddf.spatial.ogc.csw.catalog.common.converter.DefaultCswRecordMap;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.CswTransactionRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.DeleteActionImpl;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.InsertActionImpl;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.UpdateActionImpl;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transformer.TransformerManager;
import org.opengis.filter.Filter;
import org.osgi.framework.BundleContext;

public abstract class AbstractCswStore extends AbstractCswSource implements CatalogStore {

  protected TransformerManager schemaTransformerManager;

  protected MessageBodyWriter<CswTransactionRequest> cswTransactionWriter;

  /**
   * Instantiates a CswStore. This constructor is for unit tests
   *
   * @param context The {@link BundleContext} from the OSGi Framework
   * @param cswSourceConfiguration the configuration of this source
   * @param provider transform provider to transform results
   * @param clientFactoryFactory client factory already configured for this source
   */
  public AbstractCswStore(
      BundleContext context,
      CswSourceConfiguration cswSourceConfiguration,
      Converter provider,
      ClientFactoryFactory clientFactoryFactory,
      EncryptionService encryptionService,
      Security security) {
    super(
        context,
        cswSourceConfiguration,
        provider,
        clientFactoryFactory,
        encryptionService,
        security);
  }

  /** Instantiates a CswStore. */
  public AbstractCswStore(
      EncryptionService encryptionService,
      ClientFactoryFactory clientFactoryFactory,
      Security security) {
    super(encryptionService, clientFactoryFactory, security);
  }

  @Override
  public CreateResponse create(CreateRequest createRequest) throws IngestException {
    Map<String, Serializable> properties = new HashMap<>();

    validateOperation();

    Subject subject = (Subject) createRequest.getPropertyValue(SecurityConstants.SECURITY_SUBJECT);
    Csw csw = factory.getClientForSubject(subject);
    CswTransactionRequest transactionRequest = getTransactionRequest();

    List<Metacard> metacards = createRequest.getMetacards();
    List<String> metacardIds = metacards.stream().map(Metacard::getId).collect(Collectors.toList());
    List<Metacard> createdMetacards = new ArrayList<>();
    List<Filter> createdMetacardFilters;
    HashSet<ProcessingDetails> errors = new HashSet<>();

    String insertTypeName =
        schemaTransformerManager.getTransformerIdForSchema(
            cswSourceConfiguration.getOutputSchema());

    if (insertTypeName == null) {
      throw new IngestException(
          "Could not find transformer for output schema "
              + cswSourceConfiguration.getOutputSchema());
    }

    transactionRequest
        .getInsertActions()
        .add(new InsertActionImpl(insertTypeName, null, metacards));
    try {
      TransactionResponseType response = csw.transaction(transactionRequest);
      Set<String> processedIds = new HashSet<>();
      // dive down into the response to get the created ID's. We need these so we can query
      // the source again to get the created metacards and put them in the result
      createdMetacardFilters =
          response
              .getInsertResult()
              .stream()
              .map(InsertResultType::getBriefRecord)
              .flatMap(Collection::stream)
              .map(BriefRecordType::getIdentifier)
              .flatMap(Collection::stream)
              .map(JAXBElement::getValue)
              .map(SimpleLiteral::getContent)
              .flatMap(Collection::stream)
              .map(
                  id -> {
                    processedIds.add(id);
                    return filterBuilder.attribute(Core.ID).is().equalTo().text(id);
                  })
              .collect(Collectors.toList());
      metacardIds.removeAll(processedIds);

      errors.addAll(
          metacardIds
              .stream()
              .map(id -> new ProcessingDetailsImpl(id, null, "Failed to create metacard"))
              .collect(Collectors.toList()));

    } catch (CswException e) {
      throw new IngestException("Csw Transaction Failed : ", e);
    }

    try {
      createdMetacards = transactionQuery(createdMetacardFilters, subject);
    } catch (UnsupportedQueryException e) {
      errors.add(
          new ProcessingDetailsImpl(this.getId(), e, "Failed to retrieve newly created metacards"));
    }

    return new CreateResponseImpl(createRequest, properties, createdMetacards, errors);
  }

  @Override
  public UpdateResponse update(UpdateRequest updateRequest) throws IngestException {
    Map<String, Serializable> properties = new HashMap<>();

    validateOperation();

    Subject subject = (Subject) updateRequest.getPropertyValue(SecurityConstants.SECURITY_SUBJECT);
    Csw csw = factory.getClientForSubject(subject);
    CswTransactionRequest transactionRequest = getTransactionRequest();

    OperationTransaction opTrans =
        (OperationTransaction) updateRequest.getPropertyValue(Constants.OPERATION_TRANSACTION_KEY);

    String insertTypeName =
        schemaTransformerManager.getTransformerIdForSchema(
            cswSourceConfiguration.getOutputSchema());

    HashSet<ProcessingDetails> errors = new HashSet<>();

    if (insertTypeName == null) {
      insertTypeName = CswConstants.CSW_RECORD;
    }

    ArrayList<Metacard> updatedMetacards = new ArrayList<>(updateRequest.getUpdates().size());

    ArrayList<Filter> updatedMetacardFilters = new ArrayList<>(updateRequest.getUpdates().size());

    for (Map.Entry<Serializable, Metacard> update : updateRequest.getUpdates()) {
      Metacard metacard = update.getValue();
      properties.put(metacard.getId(), metacard);
      updatedMetacardFilters.add(
          filterBuilder
              .attribute(updateRequest.getAttributeName())
              .is()
              .equalTo()
              .text(update.getKey().toString()));
      transactionRequest
          .getUpdateActions()
          .add(new UpdateActionImpl(metacard, insertTypeName, null));
    }
    try {
      TransactionResponseType response = csw.transaction(transactionRequest);
      if (response.getTransactionSummary().getTotalUpdated().longValue()
          != updateRequest.getUpdates().size()) {
        errors.add(new ProcessingDetailsImpl(this.getId(), null, "One or more updates failed"));
      }
    } catch (CswException e) {
      throw new IngestException("Csw Transaction Failed.", e);
    }

    try {
      updatedMetacards.addAll(transactionQuery(updatedMetacardFilters, subject));
    } catch (UnsupportedQueryException e) {
      errors.add(
          new ProcessingDetailsImpl(this.getId(), e, "Failed to retrieve updated metacards"));
    }
    return new UpdateResponseImpl(
        updateRequest,
        properties,
        updatedMetacards,
        new ArrayList(opTrans.getPreviousStateMetacards()),
        errors);
  }

  @Override
  public DeleteResponse delete(DeleteRequest deleteRequest) throws IngestException {
    Map<String, Serializable> properties = new HashMap<>();

    validateOperation();

    Subject subject = (Subject) deleteRequest.getPropertyValue(SecurityConstants.SECURITY_SUBJECT);
    Csw csw = factory.getClientForSubject(subject);
    CswTransactionRequest transactionRequest = getTransactionRequest();

    OperationTransaction opTrans =
        (OperationTransaction) deleteRequest.getPropertyValue(Constants.OPERATION_TRANSACTION_KEY);
    String typeName =
        schemaTransformerManager.getTransformerIdForSchema(
            cswSourceConfiguration.getOutputSchema());

    if (typeName == null) {
      typeName = CswConstants.CSW_RECORD;
    }

    for (Serializable itemToDelete : deleteRequest.getAttributeValues()) {
      try {
        DeleteType deleteType = new DeleteType();
        deleteType.setTypeName(typeName);
        QueryConstraintType queryConstraintType = new QueryConstraintType();
        Filter filter;
        FilterType filterType;
        filter =
            filterBuilder
                .attribute(deleteRequest.getAttributeName())
                .is()
                .equalTo()
                .text(itemToDelete.toString());
        filterType = filterAdapter.adapt(filter, cswFilterDelegate);
        queryConstraintType.setCqlText(CswCqlTextFilter.getInstance().getCqlText(filterType));
        deleteType.setConstraint(queryConstraintType);

        DeleteAction deleteAction =
            new DeleteActionImpl(deleteType, DefaultCswRecordMap.getPrefixToUriMapping());
        transactionRequest.getDeleteActions().add(deleteAction);
      } catch (UnsupportedQueryException e) {
        throw new IngestException("Unsupported Query.", e);
      }
    }

    try {
      TransactionResponseType response = csw.transaction(transactionRequest);
      if (response.getTransactionSummary().getTotalDeleted().intValue()
          != deleteRequest.getAttributeValues().size()) {
        throw new IngestException(
            "Csw Transaction Failed. Number of metacards deleted did not match number requested.");
      }
    } catch (CswException e) {
      throw new IngestException("Csw Transaction Failed", e);
    }

    return new DeleteResponseImpl(
        deleteRequest, properties, new ArrayList(opTrans.getPreviousStateMetacards()));
  }

  @Override
  protected List<Object> initProviders(
      Converter cswTransformProvider, CswSourceConfiguration cswSourceConfiguration) {

    List providers =
        new ArrayList(super.initProviders(cswTransformProvider, cswSourceConfiguration));
    providers.add(cswTransactionWriter);
    return providers;
  }

  public void setSchemaTransformerManager(TransformerManager schemaTransformerManager) {
    this.schemaTransformerManager = schemaTransformerManager;
  }

  public MessageBodyWriter<CswTransactionRequest> getCswTransactionWriter() {
    return cswTransactionWriter;
  }

  public void setCswTransactionWriter(
      MessageBodyWriter<CswTransactionRequest> cswTransactionWriter) {
    this.cswTransactionWriter = cswTransactionWriter;
  }

  private List<Metacard> transactionQuery(List<Filter> idFilters, Subject subject)
      throws UnsupportedQueryException {
    Filter createFilter =
        filterBuilder.allOf(
            filterBuilder.anyOf(
                filterBuilder
                    .attribute(Core.METACARD_TAGS)
                    .is()
                    .like()
                    .text(FilterDelegate.WILDCARD_CHAR),
                filterBuilder.attribute(Core.METACARD_TAGS).empty()),
            filterBuilder.anyOf(idFilters));

    Query query = new QueryImpl(createFilter);
    Map<String, Serializable> properties = new HashMap<>();
    properties.put(SecurityConstants.SECURITY_SUBJECT, subject);
    QueryRequest queryRequest = new QueryRequestImpl(query, properties);

    SourceResponse sourceResponse = query(queryRequest);
    List<Result> results = sourceResponse.getResults();
    return results.stream().map(Result::getMetacard).collect(Collectors.toList());
  }

  private CswTransactionRequest getTransactionRequest() {
    CswTransactionRequest transactionRequest = new CswTransactionRequest();
    transactionRequest.setVersion(CswConstants.VERSION_2_0_2);
    transactionRequest.setService(CswConstants.CSW);
    transactionRequest.setVerbose(true);
    return transactionRequest;
  }

  protected void validateOperation() {
    if (capabilities == null) {
      throw new UnsupportedOperationException(
          "The CSW Store is not available. Operations can not be performed on it.");
    }

    Optional result =
        capabilities
            .getOperationsMetadata()
            .getOperation()
            .stream()
            .filter(e -> e.getName().equals(CswConstants.TRANSACTION))
            .findFirst();
    if (!result.isPresent()) {
      throw new UnsupportedOperationException(
          "This CSW Endpoint referenced by this store doesn't support the Transaction Operation");
    }
  }
}
