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
package org.codice.ddf.catalog.content.resource.reader;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.action.MultiActionProvider;
import ddf.action.impl.ActionImpl;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.data.Metacard;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DerivedContentActionProvider implements MultiActionProvider {

  private static final String ID = "catalog.data.metacard.derived-content";

  private static final Logger LOGGER = LoggerFactory.getLogger(DerivedContentActionProvider.class);

  private final ActionProvider resourceActionProvider;

  private static final String DESCRIPTION_PREFIX = "Retrieves derived resource: ";

  public DerivedContentActionProvider(ActionProvider actionProvider) {
    this.resourceActionProvider = actionProvider;
  }

  @Override
  public <T> List<Action> getActions(T input) {
    Action resourceAction = resourceActionProvider.getAction(input);
    if (resourceAction == null) {
      return Collections.emptyList();
    }

    return ((Metacard) input)
        .getAttribute(Metacard.DERIVED_RESOURCE_URI).getValues().stream()
            .map(
                value -> {
                  try {
                    URI uri = new URI(value.toString());
                    URIBuilder builder = new URIBuilder(resourceAction.getUrl().toURI());
                    if (StringUtils.equals(uri.getScheme(), ContentItem.CONTENT_SCHEME)) {
                      String qualifier = uri.getFragment();

                      builder.addParameters(
                          Collections.singletonList(
                              new BasicNameValuePair(ContentItem.QUALIFIER_KEYWORD, qualifier)));
                      return Optional.of(
                          new ActionImpl(
                              ID,
                              "View " + qualifier,
                              DESCRIPTION_PREFIX + qualifier,
                              builder.build().toURL()));
                    } else {
                      String uriString = uri.toString();
                      String qualifier = getQualifierForRemoteResource(uriString);
                      if (StringUtils.isNotBlank(qualifier)) {
                        // remote source
                        return Optional.of(
                            new ActionImpl(
                                ID,
                                "View " + qualifier,
                                DESCRIPTION_PREFIX + uriString,
                                uri.toURL()));
                      } else {
                        // fail case
                        return Optional.of(
                            new ActionImpl(
                                ID,
                                "View " + uriString,
                                DESCRIPTION_PREFIX + uriString,
                                uri.toURL()));
                      }
                    }
                  } catch (URISyntaxException | MalformedURLException e) {
                    LOGGER.debug("Unable to create action URL.", e);
                    return Optional.<Action>empty();
                  }
                })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public <T> boolean canHandle(T subject) {
    if (subject instanceof Metacard) {
      Metacard metacard = (Metacard) subject;
      if (metacard.getAttribute(Metacard.DERIVED_RESOURCE_URI) != null
          && !metacard.getAttribute(Metacard.DERIVED_RESOURCE_URI).getValues().isEmpty()) {
        return true;
      }
    }
    return false;
  }

  private String getQualifierForRemoteResource(String uriString) throws URISyntaxException {
    final String QUALIFIER_KEY = "qualifier";

    return URLEncodedUtils.parse(new URI(uriString), StandardCharsets.UTF_8.name()).stream()
        .filter(pair -> QUALIFIER_KEY.equals(pair.getName()))
        .map(NameValuePair::getValue)
        .findFirst()
        .orElse(""); // default
  }
}
