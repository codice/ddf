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

import static org.apache.camel.builder.PredicateBuilder.and;
import static org.apache.camel.builder.PredicateBuilder.not;

import ddf.camel.component.catalog.ingest.PostIngestConsumer;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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

  private List<String> routeIds = new ArrayList<>();

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
    routeIds.clear();

    String metacardRouteId = "metacard-" + UUID.randomUUID().toString();
    String route1Id = metacardRouteId + "1";
    from("catalog:postingest")
        .routeId(route1Id)
        .split(method(ResponseMetacardActionSplitter.class, "split(${body})"))
        .to("direct:" + metacardRouteId);
    routeIds.add(route1Id);

    String route2Id = metacardRouteId + "2";
    from("direct:" + metacardRouteId + "?block=true")
        .routeId(route2Id)
        .setHeader(METACARD_TRANSFORMER_ID_RTE_PROP, simple(metacardTransformerId, String.class))
        .setHeader(
            METACARD_BACKUP_INVALID_RTE_PROP,
            simple(String.valueOf(backupInvalidMetacards), Boolean.class))
        .setHeader(
            METACARD_BACKUP_KEEP_DELETED_RTE_PROP,
            simple(String.valueOf(keepDeletedMetacards), Boolean.class))
        .choice()
        .when(not(getShouldBackupPredicate()))
        .stop()
        .otherwise()
        .setHeader(
            TEMPLATED_STRING_HEADER_RTE_PROP,
            method(new MetacardTemplate(outputPathTemplate), "applyTemplate(${body})"))
        .to("catalog:metacardtransformer")
        .choice()
        .when(
            and(
                header(PostIngestConsumer.ACTION).isEqualTo(PostIngestConsumer.DELETE),
                getCheckDeletePredicate()))
        .bean(
            MetacardFileStorageRoute.class,
            String.format(
                "deleteFile(%s, ${in.headers.%s})",
                URLEncoder.encode(getStartingDir(), StandardCharsets.UTF_8.name()),
                URLEncoder.encode(TEMPLATED_STRING_HEADER_RTE_PROP, StandardCharsets.UTF_8.name())))
        .stop()
        .otherwise()
        .to(
            "file://"
                + getStartingDir()
                + "?fileName=${in.headers."
                + TEMPLATED_STRING_HEADER_RTE_PROP
                + "}");
    routeIds.add(route2Id);

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

  @Override
  public List<String> getRouteIds() {
    return routeIds;
  }

  public static void deleteFile(String startingDir, String fileName) {

    String fullFilePath = null;
    try {
      fullFilePath =
          String.format(
              "%s%s%s",
              URLDecoder.decode(startingDir, StandardCharsets.UTF_8.name()),
              File.separator,
              URLDecoder.decode(fileName, StandardCharsets.UTF_8.name()));
      Files.deleteIfExists(Paths.get(fullFilePath));
      LOGGER.trace("Deleted File : {}", fullFilePath);
    } catch (IOException e) {
      LOGGER.debug("Could not delete file at path : {}", fullFilePath, e);
    }
  }

  private String getStartingDir() {
    String startDir;
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
