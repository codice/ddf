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
package ddf.catalog.transformer.xml.adapter;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceProcessingDetails;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.xml.binding.MetacardElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @see http://stackoverflow.com/a/11967459 */
@XmlRootElement(name = "metacards", namespace = "urn:catalog:metacard")
@XmlType(
    name = "",
    propOrder = {"metacard"})
@XmlAccessorType(XmlAccessType.NONE)
public class AdaptedSourceResponse implements SourceResponse {

  public static final SourceResponseImpl EMPTY_SOURCE_RESPONSE =
      new SourceResponseImpl(new QueryRequestImpl(null), new ArrayList<Result>());

  private static final String METACARD_URI = "urn:catalog:metacard";

  private static final Logger LOGGER = LoggerFactory.getLogger(AdaptedSourceResponse.class);

  private SourceResponse delegate;

  public AdaptedSourceResponse(SourceResponse response) {
    if (response == null) {
      delegate = EMPTY_SOURCE_RESPONSE;
    } else {
      delegate = response;
    }
  }

  public AdaptedSourceResponse() {
    this(EMPTY_SOURCE_RESPONSE);
  }

  @Override
  public QueryRequest getRequest() {
    return delegate.getRequest();
  }

  @Override
  public Set<String> getPropertyNames() {
    return delegate.getPropertyNames();
  }

  @Override
  public Serializable getPropertyValue(String name) {
    return delegate.getPropertyValue(name);
  }

  @Override
  public boolean containsPropertyName(String name) {
    return delegate.containsPropertyName(name);
  }

  @Override
  public boolean hasProperties() {
    return delegate.hasProperties();
  }

  @Override
  public Map<String, Serializable> getProperties() {
    return delegate.getProperties();
  }

  @Override
  public long getHits() {
    return delegate.getHits();
  }

  @XmlElement(namespace = METACARD_URI)
  public List<MetacardElement> getMetacard() {
    List<MetacardElement> metacards = new ArrayList<MetacardElement>();

    for (Result r : delegate.getResults()) {

      Metacard metacard = r.getMetacard();
      if (metacard == null) {
        continue;
      }

      MetacardElement element = new MetacardElement();

      element.setId(metacard.getId());

      element.setSource(metacard.getSourceId());

      if (metacard.getMetacardType() != null) {

        String metacardTypeName = MetacardImpl.BASIC_METACARD.getName();

        if (isNotBlank(metacard.getMetacardType().getName())) {
          metacardTypeName = metacard.getMetacardType().getName();
        }

        element.setType(metacardTypeName);

        AttributeAdapter attributeAdapter = new AttributeAdapter(metacard.getMetacardType());

        for (AttributeDescriptor descriptor :
            metacard.getMetacardType().getAttributeDescriptors()) {

          try {
            element
                .getAttributes()
                .add(attributeAdapter.marshal(metacard.getAttribute(descriptor.getName())));
          } catch (CatalogTransformerException e) {
            LOGGER.info("Marshalling error with attribute", e);
          }
        }
      }

      metacards.add(element);
    }

    return metacards;
  }

  public List<Result> getResults() {
    return delegate.getResults();
  }

  @Override
  public Set<? extends SourceProcessingDetails> getProcessingDetails() {
    return delegate.getProcessingDetails();
  }
}
