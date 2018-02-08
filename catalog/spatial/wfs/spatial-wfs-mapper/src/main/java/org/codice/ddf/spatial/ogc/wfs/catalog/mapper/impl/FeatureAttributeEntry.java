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
package org.codice.ddf.spatial.ogc.wfs.catalog.mapper.impl;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.StringHelpers;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.function.Function;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of MetacardMapper.Entry that uses the HandleBars template engine to determine
 * the values to be returned.
 */
final class FeatureAttributeEntry implements Entry {
  private String attributeName;

  private String featureName;

  private String templateText;

  private Template template;

  private String toStringValue;

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureAttributeEntry.class);

  FeatureAttributeEntry(String attributeName, String featureName, String templateText) {
    this.attributeName = attributeName;
    this.featureName = featureName;
    this.templateText = templateText;

    this.toStringValue =
        new ToStringBuilder(this)
            .append(attributeName)
            .append(featureName)
            .append(templateText)
            .toString();

    Handlebars handleBars = new Handlebars();
    handleBars.registerHelpers(StringHelpers.class);

    try {
      this.template = handleBars.compileInline(templateText);
    } catch (IOException e) {
      LOGGER.error("Could not compile handlebars template: {}. ", templateText, e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public String getAttributeName() {
    return attributeName;
  }

  /** {@inheritDoc} */
  @Override
  public String getFeatureProperty() {
    return featureName;
  }

  /** {@inheritDoc} */
  @Override
  public Function<Map<String, Serializable>, String> getMappingFunction() {
    return this::applyTemplate;
  }

  protected String getTemplateText() {
    return templateText;
  }

  private String applyTemplate(Map<String, Serializable> map) {
    try {
      return template.apply(map);
    } catch (IOException ioe) {
      LOGGER.error("Unable to apply template {}", templateText, ioe);
    }

    return null;
  }

  @Override
  public String toString() {
    return toStringValue;
  }
}
