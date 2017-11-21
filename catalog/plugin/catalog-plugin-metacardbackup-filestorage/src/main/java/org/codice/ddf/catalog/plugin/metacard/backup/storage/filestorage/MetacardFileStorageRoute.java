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
package org.codice.ddf.catalog.plugin.metacard.backup.storage.filestorage;

import static org.apache.camel.builder.PredicateBuilder.not;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import org.apache.camel.CamelContext;
import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.catalog.plugin.metacard.backup.common.MetacardStorageRoute;
import org.codice.ddf.catalog.plugin.metacard.backup.common.MetacardTemplate;
import org.codice.ddf.catalog.plugin.metacard.backup.common.ResponseMetacardActionSplitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a camel route for storing metacards from post-ingest on the local file system. This route
 * will transform the metacard using the configured metacard transformer prior to storage.
 */
public class MetacardFileStorageRoute extends MetacardStorageRoute {
  public static final String OUTPUT_PATH_TEMPLATE = "outputPathTemplate";

  protected String outputPathTemplate;

  private static final Logger LOGGER = LoggerFactory.getLogger(MetacardFileStorageRoute.class);

  public MetacardFileStorageRoute(CamelContext camelContext) {
    super(camelContext);
  }

  public String getOutputPathTemplate() {
    return outputPathTemplate;
  }

  public void setOutputPathTemplate(String outputPathTemplate) {
    this.outputPathTemplate = outputPathTemplate;
  }

  @Override
  public void configure() throws Exception {
    String metacardRouteId = "metacard-" + UUID.randomUUID().toString();
    from("catalog:postingest")
        .split(method(ResponseMetacardActionSplitter.class, "split(${body})"))
        .to("direct:" + metacardRouteId);
    from("direct:" + metacardRouteId + "?block=true")
        .setHeader(METACARD_TRANSFORMER_ID_RTE_PROP, simple(metacardTransformerId, String.class))
        .setHeader(
            METACARD_BACKUP_INVALID_RTE_PROP,
            simple(String.valueOf(backupInvalidMetacards), Boolean.class))
        .setHeader(
            METACARD_BACKUP_KEEP_DELETED_RTE_PROP,
            simple(String.valueOf(keepDeletedMetacards), Boolean.class))
        .choice()
        .when(not(getCheckDeletePredicate()))
        .stop()
        .otherwise()
        .choice()
        .when(not(getShouldBackupPredicate()))
        .stop()
        .otherwise()
        .setHeader(
            TEMPLATED_STRING_HEADER_RTE_PROP,
            method(new MetacardTemplate(outputPathTemplate), "applyTemplate(${body})"))
        .to("catalog:metacardtransformer")
        .to(
            "file://"
                + getStartingDir()
                + "?fileName=${in.headers."
                + TEMPLATED_STRING_HEADER_RTE_PROP
                + "}");

    LOGGER.trace("Starting metacard file storage route: {}", this);
  }

  @Override
  public void refresh(Map<String, Object> properties) throws Exception {
    Object outputPathTemplateProp = properties.get(OUTPUT_PATH_TEMPLATE);
    if (outputPathTemplateProp instanceof String
        && StringUtils.isNotBlank((String) outputPathTemplateProp)) {
      this.outputPathTemplate = (String) outputPathTemplateProp;
    }

    super.refresh(properties);
  }

  private String getStartingDir() {
    String startDir = null;
    if (outputPathTemplate.startsWith(File.separator)) {
      startDir = File.separator;
    } else {
      startDir = System.getProperty("karaf.home");
    }

    if (startDir == null) {
      startDir = File.separator;
    }

    return startDir;
  }
}
