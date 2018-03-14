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

import static java.lang.String.format;
import static org.codice.ddf.catalog.ui.forms.SearchFormsLoader.config;
import static spark.Spark.get;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.security.service.SecurityServiceException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import net.opengis.filter.v_2_0.FilterType;
import org.boon.json.JsonFactory;
import org.boon.json.JsonParserFactory;
import org.boon.json.JsonSerializerFactory;
import org.boon.json.ObjectMapper;
import org.codice.ddf.catalog.ui.forms.data.FormAttributes;
import org.codice.ddf.catalog.ui.forms.data.QueryTemplateMetacardImpl;
import org.codice.ddf.catalog.ui.forms.data.ResultTemplateMetacardImpl;
import org.codice.ddf.catalog.ui.forms.filter.VisitableFilterNode;
import org.codice.ddf.catalog.ui.forms.filter.VisitableXmlElement;
import org.codice.ddf.catalog.ui.forms.model.FilterNodeValueSerializer;
import org.codice.ddf.catalog.ui.forms.model.JsonModel.FieldFilter;
import org.codice.ddf.catalog.ui.forms.model.JsonModel.FormTemplate;
import org.codice.ddf.catalog.ui.forms.model.JsonTransformVisitor;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.codice.ddf.security.common.Security;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.servlet.SparkApplication;

/** Provides an internal REST interface for working with custom form data for Intrigue. */
public class SearchFormsApplication implements SparkApplication {
  private static final Logger LOGGER = LoggerFactory.getLogger(SearchFormsApplication.class);

  private static final Security SECURITY = Security.getInstance();

  private static final ObjectMapper MAPPER =
      JsonFactory.create(
          new JsonParserFactory().usePropertyOnly(),
          new JsonSerializerFactory()
              .addPropertySerializer(new FilterNodeValueSerializer())
              .useAnnotations()
              .includeEmpty()
              .includeDefaultValues()
              .setJsonFormatForDates(false));

  private final CatalogFramework catalogFramework;

  private final EndpointUtil util;

  public SearchFormsApplication(CatalogFramework catalogFramework, EndpointUtil util) {
    this.catalogFramework = catalogFramework;
    this.util = util;
  }

  /**
   * Called via blueprint on initialization. Reads configuration in {@code etc/forms} and
   * initializes Solr with query and result templates using the {@link Metacard#TITLE} field as a
   * unique key.
   */
  public void setup() {
    Function<Map<String, Result>, Set<String>> titles =
        map ->
            map.entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .map(Result::getMetacard)
                .map(Metacard::getTitle)
                .collect(Collectors.toSet());

    Set<String> queryTitles = queryAsAdmin(FormAttributes.Query.TAG, titles);
    Set<String> resultTitles = queryAsAdmin(FormAttributes.Result.TAG, titles);
    List<Metacard> initialTemplates = config().get();

    Stream<Metacard> metacardStream =
        Stream.concat(
            initialTemplates
                .stream()
                .filter(QueryTemplateMetacardImpl::isQueryTemplateMetacard)
                .filter(metacard -> !queryTitles.contains(metacard.getTitle())),
            initialTemplates
                .stream()
                .filter(ResultTemplateMetacardImpl::isResultTemplateMetacard)
                .filter(metacard -> !resultTitles.contains(metacard.getTitle())));

    saveMetacards(metacardStream.collect(Collectors.toList()));
  }

  /** Spark's API-mandated init (not OSGi related) for registering REST functions. */
  @Override
  public void init() {
    get(
        "/forms/query",
        (req, res) ->
            MAPPER.toJson(
                util.getMetacardsByFilter(FormAttributes.Query.TAG)
                    .entrySet()
                    .stream()
                    .map(Map.Entry::getValue)
                    .map(Result::getMetacard)
                    .map(this::toFormTemplate)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList())));

    get(
        "/forms/result",
        (req, res) ->
            MAPPER.toJson(
                util.getMetacardsByFilter(FormAttributes.Result.TAG)
                    .entrySet()
                    .stream()
                    .map(Map.Entry::getValue)
                    .map(Result::getMetacard)
                    .map(this::toFieldFilter)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList())));
  }

  /** Convert a query template metacard into the JSON representation of FormTemplate. */
  @Nullable
  private FormTemplate toFormTemplate(Metacard metacard) {
    if (!QueryTemplateMetacardImpl.isQueryTemplateMetacard(metacard)) {
      LOGGER.debug("Metacard {} was not a query template metacard", metacard);
      return null;
    }
    QueryTemplateMetacardImpl wrapped = new QueryTemplateMetacardImpl(metacard);
    JsonTransformVisitor visitor = new JsonTransformVisitor();
    try {
      FilterReader reader = new FilterReader();
      JAXBElement<FilterType> root =
          reader.unmarshal(
              new ByteArrayInputStream(wrapped.getFormsFilter().getBytes("UTF-8")),
              FilterType.class);
      makeVisitable(root).accept(visitor);
      return new FormTemplate(wrapped, visitor.getResult());
    } catch (JAXBException | UnsupportedEncodingException e) {
      LOGGER.error("Parsing failed for query template metacard's filter xml", e);
    }
    return null;
  }

  /** Convert a result template metacard into the JSON representation of FieldFilter. */
  @Nullable
  private FieldFilter toFieldFilter(Metacard metacard) {
    if (!ResultTemplateMetacardImpl.isResultTemplateMetacard(metacard)) {
      LOGGER.debug("Metacard {} was not a result template metacard", metacard);
      return null;
    }
    ResultTemplateMetacardImpl wrapped = new ResultTemplateMetacardImpl(metacard);
    return new FieldFilter(wrapped, wrapped.getResultDescriptors());
  }

  private Set<String> queryAsAdmin(
      String tag, Function<Map<String, Result>, Set<String>> transform) {
    return SECURITY.runAsAdmin(
        () -> {
          try {
            return SECURITY.runWithSubjectOrElevate(
                () -> transform.apply(util.getMetacardsByFilter(tag)));
          } catch (SecurityServiceException | InvocationTargetException e) {
            LOGGER.warn(
                "Can't query the catalog while trying to initialize system search templates, was "
                    + "unable to elevate privileges",
                e);
          }
          return Collections.emptySet();
        });
  }

  private void saveMetacards(List<Metacard> metacards) {
    SECURITY.runAsAdmin(
        () -> {
          try {
            return SECURITY.runWithSubjectOrElevate(
                () ->
                    catalogFramework
                        .create(new CreateRequestImpl(metacards))
                        .getCreatedMetacards());
          } catch (SecurityServiceException | InvocationTargetException e) {
            LOGGER.warn(
                "Can't create metacard for system search template, was unable to elevate privileges",
                e);
          }
          return null;
        });
  }

  private VisitableXmlElement makeVisitable(JAXBElement element) {
    return new VisitableFilterNode(element);
  }

  private static class FilterReader {
    private final JAXBContext context;

    public FilterReader() throws JAXBException {
      String pkgName = FilterType.class.getPackage().getName();
      this.context = JAXBContext.newInstance(format("%s:%s", pkgName, pkgName));
    }

    @SuppressWarnings("unchecked")
    public <T> JAXBElement<T> unmarshal(InputStream inputStream, Class<T> tClass)
        throws JAXBException {
      Unmarshaller unmarshaller = context.createUnmarshaller();
      Object result = unmarshaller.unmarshal(inputStream);
      if (result instanceof JAXBElement) {
        JAXBElement element = (JAXBElement) result;
        if (tClass.isInstance(element.getValue())) {
          return (JAXBElement<T>) element;
        }
      }
      return null;
    }
  }
}
