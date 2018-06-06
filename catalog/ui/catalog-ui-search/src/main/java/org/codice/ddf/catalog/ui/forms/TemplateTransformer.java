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
package org.codice.ddf.catalog.ui.forms;

import static org.codice.ddf.catalog.ui.forms.data.AttributeGroupType.ATTRIBUTE_GROUP_LIST;
import static org.codice.ddf.catalog.ui.forms.data.QueryTemplateType.QUERY_TEMPLATE_FILTER;

import com.google.common.collect.ImmutableMap;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.types.SecurityAttributes;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Security;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import net.opengis.filter.v_2_0.FilterType;
import org.codice.ddf.catalog.ui.forms.api.FilterNode;
import org.codice.ddf.catalog.ui.forms.builder.JsonModelBuilder;
import org.codice.ddf.catalog.ui.forms.builder.XmlModelBuilder;
import org.codice.ddf.catalog.ui.forms.data.AttributeGroupMetacard;
import org.codice.ddf.catalog.ui.forms.data.QueryTemplateMetacard;
import org.codice.ddf.catalog.ui.forms.filter.FilterProcessingException;
import org.codice.ddf.catalog.ui.forms.filter.FilterReader;
import org.codice.ddf.catalog.ui.forms.filter.FilterWriter;
import org.codice.ddf.catalog.ui.forms.filter.TransformVisitor;
import org.codice.ddf.catalog.ui.forms.filter.VisitableJsonElementImpl;
import org.codice.ddf.catalog.ui.forms.filter.VisitableXmlElementImpl;
import org.codice.ddf.catalog.ui.forms.model.FilterNodeMapImpl;
import org.codice.ddf.catalog.ui.forms.model.pojo.FieldFilter;
import org.codice.ddf.catalog.ui.forms.model.pojo.FormTemplate;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transform metacards into the data model expected by the frontend.
 *
 * <p><i>This code is experimental. While it is functional and tested, it may change or be removed
 * in a future version of the library.</i>
 */
public class TemplateTransformer {
  private static final Logger LOGGER = LoggerFactory.getLogger(TemplateTransformer.class);

  private final FilterWriter writer;

  private final CatalogFramework catalogFramework;

  private final EndpointUtil util;

  private final FilterBuilder filterBuilder;

  public TemplateTransformer(
      FilterBuilder filterBuilder,
      EndpointUtil util,
      CatalogFramework catalogFramework,
      FilterWriter writer) {
    this.filterBuilder = filterBuilder;
    this.util = util;
    this.writer = writer;
    this.catalogFramework = catalogFramework;
  }

  public static boolean invalidFormTemplate(Metacard metacard) {
    return TemplateTransformer.toFormTemplate(metacard) == null;
  }

  /** Convert the JSON representation of a FormTemplate to a QueryTemplateMetacard. */
  @Nullable
  public Metacard toQueryTemplateMetacard(Map<String, Object> formTemplate) {
    Map<String, Object> filterJson = (Map) formTemplate.get("filterTemplate");
    String title = (String) formTemplate.get("title");
    String description = (String) formTemplate.get("description");

    if (filterJson == null) {
      return null;
    }

    TransformVisitor<JAXBElement> visitor = new TransformVisitor<>(new XmlModelBuilder());
    try {
      VisitableJsonElementImpl.create(new FilterNodeMapImpl(filterJson)).accept(visitor);
      JAXBElement filter = visitor.getResult();
      if (!filter.getDeclaredType().equals(FilterType.class)) {
        LOGGER.error(
            "Error occurred during filter processing, root type should be a {} but was {}",
            FilterType.class.getName(),
            filter.getDeclaredType().getName());
        return null;
      }

      String id = (String) formTemplate.get("id");
      QueryTemplateMetacard metacard =
          (id == null)
              ? new QueryTemplateMetacard(title, description)
              : new QueryTemplateMetacard(title, description, id);

      String filterXml = writer.marshal(filter);
      metacard.setFormsFilter(filterXml);
      Map<String, Object> querySettings = (Map<String, Object>) formTemplate.get("querySettings");
      if (querySettings != null) {
        metacard.setQuerySettings(querySettings);
      }

      Result result = metacardAlreadyExists(id, filterBuilder, catalogFramework);

      // Perform update of old Metacard if already exists
      if (result != null) {
        Metacard oldMetacard = null;
        oldMetacard = result.getMetacard();
        oldMetacard.setAttribute(new AttributeImpl(Core.MODIFIED, new Date()));
        oldMetacard.setAttribute(new AttributeImpl(Core.TITLE, title));
        oldMetacard.setAttribute(new AttributeImpl(Core.DESCRIPTION, description));
        oldMetacard.setAttribute(new AttributeImpl(QUERY_TEMPLATE_FILTER, filterXml));
        return oldMetacard;
      }
      return metacard;
    } catch (JAXBException e) {
      LOGGER.error("XML generation failed for query template metacard's filter", e);
    } catch (FilterProcessingException e) {
      LOGGER.error("Could not use filter JSON for template - {}", e.getMessage());
    } catch (SourceUnavailableException e) {
      LOGGER.error("Source unavailable, {}", e.getMessage());
    } catch (FederationException e) {
      LOGGER.error("Error during federation, {}", e.getMessage());
    } catch (UnsupportedQueryException e) {
      LOGGER.error("Query unsupported, {}", e.getMessage());
    }
    return null;
  }

  /** Convert a query template metacard into the JSON representation of FormTemplate. */
  @Nullable
  public static FormTemplate toFormTemplate(Metacard metacard) {
    if (!QueryTemplateMetacard.isQueryTemplateMetacard(metacard)) {
      LOGGER.debug("Metacard {} was not a query template metacard", metacard.getId());
      return null;
    }

    QueryTemplateMetacard wrapped = new QueryTemplateMetacard(metacard);
    TransformVisitor<FilterNode> visitor = new TransformVisitor<>(new JsonModelBuilder());

    String metacardOwner = retrieveOwnerIfPresent(metacard);
    Map<String, List<Serializable>> securityAttributes = retrieveSecurityIfPresent(metacard);

    try {
      FilterReader reader = new FilterReader();
      String formsFilter = wrapped.getFormsFilter();
      if (formsFilter == null) {
        LOGGER.debug(
            "Invalid form data was ingested for metacard [{}], no form filter present",
            wrapped.getId());
        return null;
      }
      JAXBElement<FilterType> root =
          reader.unmarshalFilter(new ByteArrayInputStream(formsFilter.getBytes("UTF-8")));
      VisitableXmlElementImpl.create(root).accept(visitor);
      return new FormTemplate(
          wrapped,
          visitor.getResult(),
          securityAttributes,
          metacardOwner,
          wrapped.getQuerySettings());
    } catch (JAXBException | UnsupportedEncodingException e) {
      LOGGER.error(
          "XML parsing failed for query template metacard's filter, with metacard id "
              + metacard.getId(),
          e);
    } catch (FilterProcessingException e) {
      LOGGER.error(
          "Could not use filter XML for template - {} [metacard id = {}]",
          e.getMessage(),
          metacard.getId());
    } catch (UnsupportedOperationException e) {
      LOGGER.error(
          "Could not use filter XML because it contains unsupported operations - {} [metacard id = {}]",
          e.getMessage(),
          metacard.getId());
    }
    return null;
  }

  /** Convert the JSON representation of FieldFilter to an AttributeGroupMetacard. */
  @Nullable
  public Metacard toAttributeGroupMetacard(Map<String, Object> resultTemplateMap) {
    FieldFilter fieldFilter = new FieldFilter(resultTemplateMap);
    String id = fieldFilter.getId();

    AttributeGroupMetacard metacard =
        (id == null)
            ? new AttributeGroupMetacard(fieldFilter.getTitle(), fieldFilter.getDescription())
            : new AttributeGroupMetacard(fieldFilter.getTitle(), fieldFilter.getDescription(), id);

    metacard.setGroupDescriptors(fieldFilter.getDescriptors());
    Result result = null;

    try {
      result = metacardAlreadyExists(id, filterBuilder, catalogFramework);
    } catch (SourceUnavailableException e) {
      LOGGER.error("Source unavailable, {}", e.getMessage());
      return null;
    } catch (FederationException e) {
      LOGGER.error("Error during federation, {}", e.getMessage());
      return null;
    } catch (UnsupportedQueryException e) {
      LOGGER.error("Query unsupported, {}", e.getMessage());
      return null;
    }

    // Perform update of old Metacard if already exists
    if (result != null) {
      Metacard oldMetacard = null;
      oldMetacard = result.getMetacard();
      oldMetacard.setAttribute(new AttributeImpl(Core.MODIFIED, new Date()));
      oldMetacard.setAttribute(new AttributeImpl(Core.TITLE, fieldFilter.getTitle()));
      oldMetacard.setAttribute(new AttributeImpl(Core.DESCRIPTION, fieldFilter.getDescription()));
      oldMetacard.setAttribute(
          new AttributeImpl(
              ATTRIBUTE_GROUP_LIST,
              (List<Serializable>) new ArrayList<Serializable>(fieldFilter.getDescriptors())));
      return oldMetacard;
    }
    return metacard;
  }

  /** Convert an attribute group metacard into the JSON representation of FieldFilter. */
  @Nullable
  public FieldFilter toFieldFilter(Metacard metacard) {

    if (!AttributeGroupMetacard.isAttributeGroupMetacard(metacard)) {
      LOGGER.debug("Metacard {} was not a result template metacard", metacard);
      return null;
    }

    String metacardOwner = retrieveOwnerIfPresent(metacard);
    Map<String, List<Serializable>> securityAttributes = retrieveSecurityIfPresent(metacard);

    AttributeGroupMetacard wrapped = new AttributeGroupMetacard(metacard);
    return new FieldFilter(
        wrapped, wrapped.getGroupDescriptors(), metacardOwner, securityAttributes);
  }

  /** Retrieves original creator of metacard if present to determine if system template or not */
  private static String retrieveOwnerIfPresent(Metacard inputMetacard) {

    String metacardOwner = "system";

    if (inputMetacard.getAttribute(Core.METACARD_OWNER) != null) {
      metacardOwner = inputMetacard.getAttribute(Core.METACARD_OWNER).getValue().toString();
    }

    return metacardOwner;
  }

  /**
   * Attaches relevant security attributes to metacard is present to be returned on the JSON
   * response
   */
  private static Map<String, List<Serializable>> retrieveSecurityIfPresent(Metacard inputMetacard) {
    List<Serializable> accessIndividuals = new ArrayList<>();
    List<Serializable> accessGroups = new ArrayList<>();

    if (inputMetacard.getAttribute(SecurityAttributes.ACCESS_INDIVIDUALS) != null) {
      accessIndividuals.addAll(
          inputMetacard.getAttribute(SecurityAttributes.ACCESS_INDIVIDUALS).getValues());
    }

    if (inputMetacard.getAttribute(SecurityAttributes.ACCESS_GROUPS) != null) {
      accessGroups.addAll(inputMetacard.getAttribute(SecurityAttributes.ACCESS_GROUPS).getValues());
    }

    return ImmutableMap.of(
        Security.ACCESS_INDIVIDUALS, accessIndividuals, Security.ACCESS_GROUPS, accessGroups);
  }

  private static Result metacardAlreadyExists(
      String id, FilterBuilder filterBuilder, CatalogFramework catalogFramework)
      throws UnsupportedQueryException, SourceUnavailableException, FederationException {
    Filter idFilter = filterBuilder.attribute(Metacard.ID).is().equalTo().text(id);
    Filter tagsFilter = filterBuilder.attribute(Metacard.TAGS).is().like().text("*");
    Filter queryFilter = filterBuilder.allOf(idFilter, tagsFilter);

    QueryResponse queryResponse =
        catalogFramework.query(new QueryRequestImpl(new QueryImpl(queryFilter), false));

    if (queryResponse.getResults().isEmpty()) {
      return null;
    }

    return queryResponse.getResults().get(0);
  }
}
