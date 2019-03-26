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
package ddf.catalog.impl;

import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.operation.impl.DeleteResponseImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.operation.impl.UpdateImpl;
import ddf.catalog.operation.impl.UpdateResponseImpl;
import ddf.catalog.source.CatalogProvider;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.opengis.filter.Not;
import org.opengis.filter.PropertyIsNil;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.temporal.After;
import org.opengis.filter.temporal.Before;
import org.opengis.filter.temporal.Begins;
import org.opengis.filter.temporal.BegunBy;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.EndedBy;
import org.opengis.filter.temporal.Ends;
import org.opengis.filter.temporal.Meets;
import org.opengis.filter.temporal.MetBy;
import org.opengis.filter.temporal.OverlappedBy;
import org.opengis.filter.temporal.TContains;
import org.opengis.filter.temporal.TEquals;
import org.opengis.filter.temporal.TOverlaps;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockMemoryProvider extends MockSource implements CatalogProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(MockMemoryProvider.class);

  private HashMap<Serializable, Metacard> store;

  private boolean hasReceivedRead = false;

  private boolean hasReceivedCreate = false;

  private boolean hasReceivedUpdate = false;

  private boolean hasReceivedDelete = false;

  private boolean hasReceivedUpdateByIdentifier = false;

  private boolean hasReceivedDeleteByIdentifier = false;

  private boolean hasReceivedQuery = false;

  private String sourceId;

  public MockMemoryProvider(String id, boolean isAvailable) {
    this(id, "", "", "", new HashSet<>(), isAvailable, new Date());
    sourceId = id;
  }

  /**
   * Mock provider, saves entries in memory. Cannot perform queries.
   *
   * @param shortName
   * @param title
   * @param version
   * @param organization
   * @param catalogTypes
   * @param isAvailable
   * @param lastAvailability
   */
  public MockMemoryProvider(
      String shortName,
      String title,
      String version,
      String organization,
      Set<ContentType> catalogTypes,
      boolean isAvailable,
      Date lastAvailability) {
    super(shortName, title, version, organization, catalogTypes, isAvailable, lastAvailability);
    store = new HashMap<>();
    sourceId = "mockMemoryProvider";
  }

  @Override
  public CreateResponse create(CreateRequest request) {
    List<Metacard> oldCards = request.getMetacards();
    hasReceivedCreate = true;
    List<Metacard> returnedMetacards = new ArrayList<>(oldCards.size());
    Map<String, Serializable> properties = new HashMap<>();

    for (Metacard curCard : oldCards) {

      MetacardImpl card = new MetacardImpl(curCard);
      if (card.getId() == null) {
        card.setId(UUID.randomUUID().toString());
      }
      LOGGER.debug("Storing metacard with id: {}", card.getId());
      store.put(card.getId(), card);
      properties.put(card.getId(), card);
      returnedMetacards.add(card);
    }

    CreateResponse ingestResponseImpl =
        new CreateResponseImpl(request, properties, returnedMetacards);

    return ingestResponseImpl;
  }

  @Override
  public UpdateResponse update(UpdateRequest request) {
    String methodName = "update";
    LOGGER.debug("Entering: {}", methodName);
    hasReceivedUpdate = true;
    hasReceivedUpdateByIdentifier = true;
    List<Entry<Serializable, Metacard>> updatedCards = request.getUpdates();
    Map<String, Serializable> properties = new HashMap<>();

    List<Update> returnedMetacards = new ArrayList<>(updatedCards.size());
    for (Entry<Serializable, Metacard> curCard : updatedCards) {
      if (store.containsKey(curCard.getValue().getId())) {
        LOGGER.debug("Store contains the key");
        Metacard oldMetacard = store.get(curCard.getValue().getId());
        store.put(curCard.getValue().getId(), curCard.getValue());
        properties.put(curCard.getValue().getId(), curCard.getValue());
        LOGGER.debug("adding returnedMetacard");
        returnedMetacards.add(new UpdateImpl(curCard.getValue(), oldMetacard));
      } else {
        LOGGER.debug("Key not contained in the store");
      }
    }

    UpdateResponse response = new UpdateResponseImpl(request, properties, returnedMetacards);
    LOGGER.debug("Exiting:{}", methodName);
    return response;
  }

  @Override
  public DeleteResponse delete(DeleteRequest deleteRequest) {
    hasReceivedDelete = true;
    @SuppressWarnings("unchecked")
    List<String> ids = (List<String>) deleteRequest.getAttributeValues();

    Map<String, Serializable> properties = new HashMap<>();

    List<Metacard> returnedMetacards = new ArrayList<>(ids.size());
    for (int i = 0; i < ids.size(); i++) {
      String id = (String) ids.get(i);
      UUID curUUID = UUID.fromString(id);
      if (store.containsKey(curUUID.toString())) {
        Metacard card = store.remove(curUUID.toString());
        if (card != null) {
          returnedMetacards.add(card);
        }
      }
    }

    DeleteResponse response = new DeleteResponseImpl(deleteRequest, properties, returnedMetacards);

    return response;
  }

  @Override
  public SourceResponse query(QueryRequest query) {
    // TODO currently returning all metacards, not using queryrequest
    List<Result> results = new ArrayList<Result>();

    MockMemoryFilterVisitor mockMemoryFilterVisitor = new MockMemoryFilterVisitor();
    Collection<Metacard> filteredMetacards =
        (Collection<Metacard>) query.getQuery().accept(mockMemoryFilterVisitor, store.values());
    for (Metacard metacard : filteredMetacards) {
      results.add(new ResultImpl(metacard));
    }
    return new SourceResponseImpl(query, results);
  }

  public int size() {
    return store.size();
  }

  public boolean hasReceivedQuery() {
    return hasReceivedQuery;
  }

  public boolean hasReceivedRead() {
    return hasReceivedRead;
  }

  public boolean hasReceivedCreate() {
    return hasReceivedCreate;
  }

  public boolean hasReceivedUpdate() {
    return hasReceivedUpdate;
  }

  public boolean hasReceivedDelete() {
    return hasReceivedDelete;
  }

  public boolean hasReceivedUpdateByIdentifier() {
    return hasReceivedUpdateByIdentifier;
  }

  public boolean hasReceivedDeleteByIdentifier() {
    return hasReceivedDeleteByIdentifier;
  }

  @Override
  public String getId() {
    return sourceId;
  }

  @Override
  public void maskId(String sourceId) {
    this.sourceId = sourceId;
  }

  @Override
  public Set<ContentType> getContentTypes() {
    return new HashSet<>();
  }

  class MockMemoryFilterVisitor extends DefaultFilterVisitor {

    @Override
    public Object visit(Not filter, Object data) {
      LOGGER.trace("entry {},{}", filter, data);
      Set<Metacard> notFilteredSet = new HashSet<>();
      notFilteredSet.addAll((Collection<? extends Metacard>) data);
      Collection<Metacard> filteredSet =
          (Collection<Metacard>) filter.getFilter().accept(this, data);
      notFilteredSet.removeAll(filteredSet);
      LOGGER.trace("exit {}", notFilteredSet.size());
      return notFilteredSet;
    }

    @Override
    public Object visit(After after, Object data) {
      LOGGER.trace("entry {},{}", after, data);
      Set<Metacard> filteredSet = new HashSet<>();
      PropertyName propName = (PropertyName) after.getExpression1();
      Object obj = ((Literal) after.getExpression2()).getValue();
      Date afterFilter;
      LOGGER.debug("what is object? {}", obj);
      if (obj instanceof Period) {
        afterFilter = ((Period) obj).getEnding().getPosition().getDate();
      } else {
        afterFilter = ((Instant) obj).getPosition().getDate();
      }
      if (data instanceof Collection<?>) {
        Collection<Metacard> mcData = (Collection<Metacard>) data;
        for (Metacard mc : mcData) {
          Date mcDate = getMetacardDate(mc, propName.getPropertyName());
          if (mcDate != null) {
            if (mcDate.after(afterFilter)) {
              filteredSet.add(mc);
            }
          }
        }
      }
      LOGGER.trace("exit {}", filteredSet);
      return filteredSet;
    }

    @Override
    public Object visit(Before before, Object data) {
      LOGGER.trace("entry {},{}", before, data);
      Set<Metacard> filteredSet = new HashSet<>();
      PropertyName propName = (PropertyName) before.getExpression1();
      Object obj = ((Literal) before.getExpression2()).getValue();
      Date beforeFilter;
      if (obj instanceof Period) {
        beforeFilter = ((Period) obj).getBeginning().getPosition().getDate();
      } else {
        beforeFilter = ((Instant) obj).getPosition().getDate();
      }
      if (data instanceof Collection<?>) {
        Collection<Metacard> mcData = (Collection<Metacard>) data;
        for (Metacard mc : mcData) {
          Date mcDate = getMetacardDate(mc, propName.getPropertyName());
          if (mcDate != null) {
            if (mcDate.before(beforeFilter)) {
              filteredSet.add(mc);
            }
          }
        }
      }
      LOGGER.trace("exit {}", filteredSet);
      return filteredSet;
    }

    @Override
    public Object visit(Begins begins, Object data) {
      LOGGER.trace("entry {},{}", begins, data);
      Set<Metacard> filteredSet = new HashSet<>();
      PropertyName propName = (PropertyName) begins.getExpression1();
      Object obj = ((Literal) begins.getExpression2()).getValue();
      Date beginsFilter = ((Period) obj).getBeginning().getPosition().getDate();
      if (data instanceof Collection<?>) {
        Collection<Metacard> mcData = (Collection<Metacard>) data;
        for (Metacard mc : mcData) {
          Date mcDate = getMetacardDate(mc, propName.getPropertyName());
          if (mcDate != null) {
            if (mcDate.equals(beginsFilter)) {
              filteredSet.add(mc);
            }
          }
        }
      }
      LOGGER.trace("exit {}", filteredSet);
      return filteredSet;
    }

    @Override
    public Object visit(BegunBy begunBy, Object data) {
      // API dictates that BegunBy filters only on period data fields.
      // Metacard currently has no period fields.
      return super.visit(begunBy, data);
    }

    @Override
    public Object visit(During during, Object data) {
      LOGGER.trace("entry {},{}", during, data);
      Set<Metacard> filteredSet = new HashSet<>();
      PropertyName propName = (PropertyName) during.getExpression1();
      Period filterPeriod = (Period) ((Literal) during.getExpression2()).getValue();
      Date startFilter = filterPeriod.getBeginning().getPosition().getDate();
      Date endFilter = filterPeriod.getEnding().getPosition().getDate();
      if (data instanceof Collection<?>) {
        Collection<Metacard> mcData = (Collection<Metacard>) data;
        for (Metacard mc : mcData) {
          Date mcDate = getMetacardDate(mc, propName.getPropertyName());
          if (mcDate != null) {
            if (mcDate.after(startFilter) && mcDate.before(endFilter)) {
              filteredSet.add(mc);
            }
          }
        }
      }
      LOGGER.trace("exit {}", filteredSet);
      return filteredSet;
    }

    @Override
    public Object visit(EndedBy endedBy, Object data) {
      // API dictates that EndedBy filters only on period data fields.
      // Metacard currently has no period fields.
      return super.visit(endedBy, data);
    }

    @Override
    public Object visit(Ends ends, Object data) {
      LOGGER.trace("entry {},{}", ends, data);
      Set<Metacard> filteredSet = new HashSet<>();
      PropertyName propName = (PropertyName) ends.getExpression1();
      Object obj = ((Literal) ends.getExpression2()).getValue();
      Date endsFilter = ((Period) obj).getEnding().getPosition().getDate();
      if (data instanceof Collection<?>) {
        Collection<Metacard> mcData = (Collection<Metacard>) data;
        for (Metacard mc : mcData) {
          Date mcDate = getMetacardDate(mc, propName.getPropertyName());
          if (mcDate != null) {
            if (mcDate.equals(endsFilter)) {
              filteredSet.add(mc);
            }
          }
        }
      }
      LOGGER.trace("exit {}", filteredSet);
      return filteredSet;
    }

    @Override
    public Object visit(Meets meets, Object data) {
      // API dictates that Meets filters only on period data fields.
      // Metacard currently has no period fields.
      return super.visit(meets, data);
    }

    @Override
    public Object visit(MetBy metBy, Object data) {
      // API dictates that MetBy filters only on period data fields.
      // Metacard currently has no period fields.
      return super.visit(metBy, data);
    }

    @Override
    public Object visit(OverlappedBy overlappedBy, Object data) {
      // API dictates that OverlappedBy filters only on period data
      // fields.
      // Metacard currently has no period fields.
      return super.visit(overlappedBy, data);
    }

    @Override
    public Object visit(TContains contains, Object data) {
      // API dictates that TContains filters only on period data fields.
      // Metacard currently has no period fields.
      return super.visit(contains, data);
    }

    @Override
    public Object visit(TEquals equals, Object data) {
      LOGGER.trace("entry {},{}", equals, data);
      Set<Metacard> filteredSet = new HashSet<>();
      PropertyName propName = (PropertyName) equals.getExpression1();
      Object obj = ((Literal) equals.getExpression2()).getValue();
      Date equalsFilter = ((Instant) obj).getPosition().getDate();
      if (data instanceof Collection<?>) {
        Collection<Metacard> mcData = (Collection<Metacard>) data;
        for (Metacard mc : mcData) {
          Date mcDate = getMetacardDate(mc, propName.getPropertyName());
          if (mcDate != null) {
            if (mcDate.equals(equalsFilter)) {
              filteredSet.add(mc);
            }
          }
        }
      }
      LOGGER.trace("exit {}", filteredSet);
      return filteredSet;
    }

    @Override
    public Object visit(TOverlaps contains, Object data) {
      // API dictates that TOverlaps filters only on period data fields.
      // Metacard currently has no period fields.
      return super.visit(contains, data);
    }

    private Date getMetacardDate(Metacard mc, String propName) {
      LOGGER.trace("entry");
      Date d = null;
      if (propName != null && !propName.isEmpty()) {
        if (propName.equals(Core.CREATED)) {
          d = mc.getCreatedDate();
        } else if (propName.equals(Metacard.EFFECTIVE)) {
          d = mc.getEffectiveDate();
        } else if (propName.equals(Core.EXPIRATION)) {
          d = mc.getExpirationDate();
        } else if (propName.equals(Core.MODIFIED)) {
          d = mc.getModifiedDate();
        }
      }
      LOGGER.trace("exit {}", d);
      return d;
    }

    @Override
    public Object visit(PropertyIsNil arg0, Object arg1) {
      return null;
    }
  }
}
