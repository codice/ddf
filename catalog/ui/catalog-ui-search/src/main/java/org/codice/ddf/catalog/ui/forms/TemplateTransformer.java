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

import com.google.common.collect.ImmutableMap;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.types.SecurityAttributes;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Security;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import net.opengis.filter.v_2_0.FilterType;
import org.codice.ddf.catalog.ui.filter.impl.FilterNodeMapImpl;
import org.codice.ddf.catalog.ui.filter.impl.FilterProcessingException;
import org.codice.ddf.catalog.ui.filter.impl.FilterReader;
import org.codice.ddf.catalog.ui.filter.impl.FilterWriter;
import org.codice.ddf.catalog.ui.filter.impl.builder.JsonModelBuilder;
import org.codice.ddf.catalog.ui.filter.impl.builder.XmlModelBuilder;
import org.codice.ddf.catalog.ui.filter.impl.json.JsonFunctionRegistry;
import org.codice.ddf.catalog.ui.filter.impl.visit.TransformVisitor;
import org.codice.ddf.catalog.ui.filter.impl.visit.VisitableJsonElementImpl;
import org.codice.ddf.catalog.ui.filter.impl.visit.VisitableXmlElementImpl;
import org.codice.ddf.catalog.ui.filter.json.JsonFunctionTransform;
import org.codice.ddf.catalog.ui.forms.data.AttributeGroupMetacard;
import org.codice.ddf.catalog.ui.forms.data.QueryTemplateMetacard;
import org.codice.ddf.catalog.ui.forms.model.FieldFilter;
import org.codice.ddf.catalog.ui.forms.model.FormTemplate;
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

  private static final JsonFunctionRegistry REGISTRY = new JsonFunctionRegistry();

  private static final FilterReader READER = new FilterReader();

  private final FilterWriter writer;

  public TemplateTransformer(FilterWriter writer) {
    this.writer = writer;
  }

  public static boolean invalidFormTemplate(Metacard metacard) {
    return TemplateTransformer.toFormTemplate(metacard) == null;
  }

  /** Convert the JSON representation of a FormTemplate to a QueryTemplateMetacard. */
  @Nullable
  public Metacard toQueryTemplateMetacard(Map<String, Object> formTemplate) {
    try {
      Map<String, Object> filterJson = (Map) formTemplate.get("filterTemplate");
      String title = (String) formTemplate.get("title");
      String description = (String) formTemplate.get("description");
      String id = (String) formTemplate.get("id");

      if (filterJson == null) {
        return null;
      }

      TransformVisitor<JAXBElement> visitor = new TransformVisitor<>(new XmlModelBuilder());
      VisitableJsonElementImpl.create(new FilterNodeMapImpl(filterJson)).accept(visitor);
      JAXBElement filter = visitor.getResult();
      if (!filter.getDeclaredType().equals(FilterType.class)) {
        LOGGER.error(
            "Error occurred during filter processing, root type should be a {} but was {}",
            FilterType.class.getName(),
            filter.getDeclaredType().getName());
        return null;
      }

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

      return metacard;

    } catch (JAXBException e) {
      LOGGER.error("XML generation failed for query template metacard's filter", e);
    } catch (FilterProcessingException e) {
      LOGGER.debug("Could not use filter JSON for template", e);
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
    TransformVisitor<Map<String, Object>> visitor = new TransformVisitor<>(new JsonModelBuilder());

    String metacardOwner = retrieveOwnerIfPresent(metacard);
    Map<String, List<Serializable>> securityAttributes = retrieveSecurityIfPresent(metacard);

    try {
      String formsFilter = wrapped.getFormsFilter();
      if (formsFilter == null) {
        LOGGER.debug(
            "Invalid form data was ingested for metacard [{}], no form filter present",
            wrapped.getId());
        return null;
      }
      JAXBElement<FilterType> root =
          READER.unmarshalFilter(
              new ByteArrayInputStream(formsFilter.getBytes(StandardCharsets.UTF_8)));
      VisitableXmlElementImpl.create(root).accept(visitor);
      return new FormTemplate(
          wrapped,
          collapseFilterFunctions(visitor.getResult()),
          securityAttributes,
          metacardOwner,
          wrapped.getQuerySettings());
    } catch (JAXBException e) {
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

  private static Map<String, Object> collapseFilterFunctions(Map<String, Object> filter) {
    return transformFilterFunctions(
        filter, JsonFunctionTransform::canApplyFromFunction, JsonFunctionTransform::fromFunction);
  }

  /**
   * Transform a filter JSON structure based upon the invocations provided.
   *
   * @param filter original filter JSON map.
   * @param shouldDoTransform invocation on transform; calls appropriate boolean check.
   * @param howToDoTransform invocation on transform; calls the actual transform itself.
   * @return transformed filter JSON map.
   */
  private static Map<String, Object> transformFilterFunctions(
      Map<String, Object> filter,
      BiFunction<JsonFunctionTransform, Map<String, Object>, Boolean> shouldDoTransform,
      BiFunction<JsonFunctionTransform, Map<String, Object>, Map<String, Object>>
          howToDoTransform) {
    if ("AND".equals(filter.get("type")) || "OR".equals(filter.get("type"))) {
      return ImmutableMap.<String, Object>builder()
          .put("type", filter.get("type"))
          .put(
              "filters",
              ((List<Map<String, Object>>) filter.get("filters"))
                  .stream()
                  // This will also be a BiFunction param when filter func expansions are needed
                  // Hard-coded to "collapseFilterFunctions" for now
                  .map(TemplateTransformer::collapseFilterFunctions)
                  .collect(Collectors.toList()))
          .build();
    }
    // Note that, depending upon the context:
    // shouldDoTransform.apply is calling either "canApplyFromFunction" or "canApplyToFunction"
    // howToDoTransform.apply is calling either "fromFunction" or "toFunction"
    return REGISTRY
        .getTransforms()
        .stream()
        .filter(t -> shouldDoTransform.apply(t, filter))
        .findFirst()
        .map(t -> howToDoTransform.apply(t, filter))
        .orElse(filter);
  }
}
