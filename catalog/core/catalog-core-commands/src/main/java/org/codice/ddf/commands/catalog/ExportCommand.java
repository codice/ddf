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
package org.codice.ddf.commands.catalog;

import static ddf.catalog.util.impl.ResultIterable.resultIterable;

import ddf.catalog.content.StorageException;
import ddf.catalog.content.StorageProvider;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.operation.impl.DeleteStorageRequestImpl;
import ddf.catalog.core.versioning.DeletedMetacard;
import ddf.catalog.core.versioning.MetacardVersion;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.ResourceRequestByProductUri;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.source.IngestException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.security.common.audit.SecurityLogger;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.catalog.transformer.zip.JarSigner;
import org.codice.ddf.commands.catalog.export.ExportItem;
import org.codice.ddf.commands.catalog.export.IdAndUriMetacard;
import org.codice.ddf.commands.util.CatalogCommandRuntimeException;
import org.fusesource.jansi.Ansi;
import org.geotools.filter.text.cql2.CQLException;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exports Metacards, History, and their content into a zip file. <b> This code is experimental.
 * While this interface is functional and tested, it may change or be removed in a future version of
 * the library. </b>
 */
@Service
@Command(
  scope = CatalogCommands.NAMESPACE,
  name = "export",
  description = "Exports Metacards and history from the current Catalog"
)
public class ExportCommand extends CqlCommands {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExportCommand.class);

  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss.SSS'Z'").withZone(ZoneOffset.UTC);

  private static final Supplier<String> FILE_NAMER =
      () ->
          String.format(
              "export-%s.zip",
              LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).format(FORMATTER));

  private static final int PAGE_SIZE = 64;

  private Filter revisionFilter;

  private JarSigner jarSigner = new JarSigner();

  @Reference protected StorageProvider storageProvider;

  @Option(
    name = "--output",
    description =
        "Output file to export Metacards and contents into. Paths are absolute and must be in quotes. Will default to auto generated name inside of ddf.home",
    multiValued = false,
    required = false,
    aliases = {"-o"}
  )
  String output = Paths.get(System.getProperty("ddf.home"), FILE_NAMER.get()).toString();

  @Option(
    name = "--delete",
    required = true,
    multiValued = false,
    description = "Delete Metacards and content after export. E.g., --delete=true or --delete=false"
  )
  boolean delete = false;

  @Option(
    name = "--archived",
    required = false,
    aliases = {"-a", "archived"},
    multiValued = false,
    description = "Equivalent to --cql \"\\\"metacard-tags\\\" like 'deleted'\""
  )
  boolean archived = false;

  @Option(
    name = "--force",
    required = false,
    aliases = {"-f"},
    multiValued = false,
    description = "Do not prompt"
  )
  boolean force = false;

  @Option(
    name = "--skip-signature-verification",
    required = false,
    multiValued = false,
    description =
        "Produces the export zip but does NOT sign the resulting zip file. This file will not be able to be verified on import for integrity and security."
  )
  boolean unsafe = false;

  @Override
  protected Object executeWithSubject() throws Exception {
    Filter filter = getFilter();
    if (!getTransform().isMetacardTransformerIdValid(DEFAULT_TRANSFORMER_ID)) {
      throw new IllegalArgumentException(
          "Could not get " + DEFAULT_TRANSFORMER_ID + " transformer");
    }

    revisionFilter = initRevisionFilter();

    final File outputFile = initOutputFile(output);
    if (outputFile.exists()) {
      printErrorMessage(String.format("File [%s] already exists!", outputFile.getPath()));
      throw new IllegalStateException("File already exists");
    }

    final File parentDirectory = outputFile.getParentFile();
    if (parentDirectory == null || !parentDirectory.isDirectory()) {
      printErrorMessage(String.format("Directory [%s] must exist.", output));
      console.println("If the directory does indeed exist, try putting the path in quotes.");
      throw new IllegalStateException("Must be inside of a directory");
    }

    String filename = FilenameUtils.getName(outputFile.getPath());
    if (StringUtils.isBlank(filename) || !filename.endsWith(".zip")) {
      console.println("Filename must end with '.zip' and not be blank");
      throw new IllegalStateException("Filename must not be blank and must end with '.zip'");
    }

    if (delete && !force) {
      final String input =
          session.readLine(
              "This action will remove all exported metacards and content from the catalog. Are you sure you wish to continue? (y/N):",
              null);
      if (!input.matches("^[yY][eE]?[sS]?$")) {
        console.println("ABORTED EXPORT.");
        return null;
      }
    }

    SecurityLogger.audit("Called catalog:export command with path : {}", output);

    ZipFile zipFile = new ZipFile(outputFile);

    console.println("Starting metacard export...");
    Instant start = Instant.now();
    List<ExportItem> exportedItems = doMetacardExport(zipFile, filter);
    if (exportedItems.isEmpty()) {
      console.println("No metacards found to export, exiting.");
      FileUtils.deleteQuietly(zipFile.getFile());
      return null;
    }

    console.println("Metacards exported in: " + getFormattedDuration(start));
    console.println("Number of metacards exported: " + exportedItems.size());
    console.println();

    SecurityLogger.audit(
        "Ids of exported metacards and content:\n{}",
        exportedItems
            .stream()
            .map(ExportItem::getId)
            .distinct()
            .collect(Collectors.joining(", ", "[", "]")));

    console.println("Starting content export...");
    start = Instant.now();
    List<ExportItem> exportedContentItems = doContentExport(zipFile, exportedItems);
    console.println("Content exported in: " + getFormattedDuration(start));
    console.println("Number of content exported: " + exportedContentItems.size());

    console.println();

    if (delete) {
      doDelete(exportedItems, exportedContentItems);
    }

    if (!unsafe) {
      SecurityLogger.audit("Signing exported data. file: [{}]", zipFile.getFile().getName());
      console.println("Signing zip file...");
      start = Instant.now();
      jarSigner.signJar(
          zipFile.getFile(),
          System.getProperty("org.codice.ddf.system.hostname"),
          System.getProperty("javax.net.ssl.keyStorePassword"),
          System.getProperty("javax.net.ssl.keyStore"),
          System.getProperty("javax.net.ssl.keyStorePassword"));
      console.println("zip file signed in: " + getFormattedDuration(start));
    }

    console.println("Export complete.");
    console.println("Exported to: " + zipFile.getFile().getCanonicalPath());
    return null;
  }

  private File initOutputFile(String output) {
    String resolvedOutput;
    File initialOutputFile = new File(output);
    if (initialOutputFile.isDirectory()) {
      // If directory was specified, auto generate file name
      resolvedOutput = Paths.get(initialOutputFile.getPath(), FILE_NAMER.get()).toString();
    } else {
      resolvedOutput = output;
    }

    return new File(resolvedOutput);
  }

  private List<ExportItem> doMetacardExport(/*Mutable,IO*/ ZipFile zipFile, Filter filter) {
    Set<String> seenIds = new HashSet<>(1024);
    List<ExportItem> exportedItems = new ArrayList<>();

    QueryImpl query = new QueryImpl(filter);
    QueryRequest queryRequest = new QueryRequestImpl(query);

    query.setPageSize(PAGE_SIZE);

    for (Result result : resultIterable(catalogFramework, queryRequest)) {
      if (!seenIds.contains(result.getMetacard().getId())) {
        writeToZip(zipFile, result);
        exportedItems.add(
            new ExportItem(
                result.getMetacard().getId(),
                getTag(result),
                result.getMetacard().getResourceURI(),
                getDerivedResources(result)));
        seenIds.add(result.getMetacard().getId());
      }

      // Fetch and export all history for each exported item
      QueryImpl historyQuery = new QueryImpl(getHistoryFilter(result));
      QueryRequest historyQueryRequest = new QueryRequestImpl(historyQuery);

      historyQuery.setPageSize(PAGE_SIZE);

      for (Result revision : resultIterable(catalogFramework, historyQueryRequest)) {
        if (seenIds.contains(revision.getMetacard().getId())) {
          continue;
        }
        writeToZip(zipFile, revision);
        exportedItems.add(
            new ExportItem(
                revision.getMetacard().getId(),
                getTag(revision),
                revision.getMetacard().getResourceURI(),
                getDerivedResources(result)));
        seenIds.add(revision.getMetacard().getId());
      }
    }
    return exportedItems;
  }

  private List<String> getDerivedResources(Result result) {
    if (result.getMetacard().getAttribute(Metacard.DERIVED_RESOURCE_URI) == null) {
      return Collections.emptyList();
    }

    return result
        .getMetacard()
        .getAttribute(Metacard.DERIVED_RESOURCE_URI)
        .getValues()
        .stream()
        .filter(Objects::nonNull)
        .map(String::valueOf)
        .collect(Collectors.toList());
  }

  private List<ExportItem> doContentExport(
      /*Mutable,IO*/ ZipFile zipFile, List<ExportItem> exportedItems) throws ZipException {
    List<ExportItem> contentItemsToExport =
        exportedItems
            .stream()
            // Only things with a resource URI
            .filter(ei -> ei.getResourceUri() != null)
            // Only our content scheme
            .filter(ei -> ei.getResourceUri().getScheme() != null)
            .filter(ei -> ei.getResourceUri().getScheme().startsWith(ContentItem.CONTENT_SCHEME))
            // Deleted Metacards have no content associated
            .filter(ei -> !ei.getMetacardTag().equals("deleted"))
            // for revision metacards, only those that have their own content
            .filter(
                ei ->
                    !ei.getMetacardTag().equals("revision")
                        || ei.getResourceUri().getSchemeSpecificPart().equals(ei.getId()))
            .filter(distinctByKey(ei -> ei.getResourceUri().getSchemeSpecificPart()))
            .collect(Collectors.toList());

    List<ExportItem> exportedContentItems = new ArrayList<>();
    for (ExportItem contentItem : contentItemsToExport) {
      ResourceResponse resource;
      try {
        resource =
            catalogFramework.getLocalResource(
                new ResourceRequestByProductUri(contentItem.getResourceUri()));
      } catch (IOException | ResourceNotSupportedException e) {
        throw new CatalogCommandRuntimeException(
            "Unable to retrieve resource for " + contentItem.getId(), e);
      } catch (ResourceNotFoundException e) {
        continue;
      }
      writeToZip(zipFile, contentItem, resource);
      exportedContentItems.add(contentItem);
      if (!contentItem.getMetacardTag().equals("revision")) {
        for (String derivedUri : contentItem.getDerivedUris()) {
          URI uri;
          try {
            uri = new URI(derivedUri);
          } catch (URISyntaxException e) {
            LOGGER.debug(
                "Uri [{}] is not a valid URI. Derived content will not be included in export",
                derivedUri);
            continue;
          }

          ResourceResponse derivedResource;
          try {
            derivedResource =
                catalogFramework.getLocalResource(new ResourceRequestByProductUri(uri));
          } catch (IOException e) {
            throw new CatalogCommandRuntimeException(
                "Unable to retrieve resource for " + contentItem.getId(), e);
          } catch (ResourceNotFoundException | ResourceNotSupportedException e) {
            LOGGER.warn("Could not retreive resource [{}]", uri, e);
            console.printf(
                "%sUnable to retrieve resource for export : %s%s%n",
                Ansi.ansi().fg(Ansi.Color.RED).toString(), uri, Ansi.ansi().reset().toString());
            continue;
          }
          writeToZip(zipFile, contentItem, derivedResource);
        }
      }
    }
    return exportedContentItems;
  }

  private void doDelete(List<ExportItem> exportedItems, List<ExportItem> exportedContentItems) {
    Instant start;
    console.println("Starting delete");
    start = Instant.now();
    for (ExportItem exportedContentItem : exportedContentItems) {
      try {
        DeleteStorageRequestImpl deleteRequest =
            new DeleteStorageRequestImpl(
                Collections.singletonList(
                    new IdAndUriMetacard(
                        exportedContentItem.getId(), exportedContentItem.getResourceUri())),
                exportedContentItem.getId(),
                Collections.emptyMap());
        storageProvider.delete(deleteRequest);
        storageProvider.commit(deleteRequest);
      } catch (StorageException e) {
        printErrorMessage(
            "Could not delete content for metacard: " + exportedContentItem.toString());
      }
    }
    for (ExportItem exported : exportedItems) {
      try {
        catalogProvider.delete(new DeleteRequestImpl(exported.getId()));
      } catch (IngestException e) {
        printErrorMessage("Could not delete metacard: " + exported.toString());
      }
    }

    // delete items from cache
    try {
      getCacheProxy()
          .removeById(
              exportedItems
                  .stream()
                  .map(ExportItem::getId)
                  .collect(Collectors.toList())
                  .toArray(new String[exportedItems.size()]));
    } catch (Exception e) {
      LOGGER.warn(
          "Could not delete all exported items from cache (Results will eventually expire)", e);
    }

    console.println("Metacards and Content deleted in: " + getFormattedDuration(start));
    console.println("Number of metacards deleted: " + exportedItems.size());
    console.println("Number of content deleted: " + exportedContentItems.size());
  }

  private void writeToZip(
      /*Mutable,IO*/ ZipFile zipFile, ExportItem exportItem, ResourceResponse resource)
      throws ZipException {
    ZipParameters parameters = new ZipParameters();
    parameters.setSourceExternalStream(true);
    String id = exportItem.getId();
    String path = getContentPath(id, resource);
    parameters.setFileNameInZip(path);
    try (InputStream resourceStream = resource.getResource().getInputStream()) {
      zipFile.addStream(resourceStream, parameters);
    } catch (IOException e) {
      LOGGER.warn(
          "Could not get content. Content will not be included in export [{}]", exportItem.getId());
      console.printf(
          "%sCould not get Content. Content will not be included in export. %s (%s)%s%n",
          Ansi.ansi().fg(Ansi.Color.RED).toString(),
          exportItem.getId(),
          exportItem.getResourceUri(),
          Ansi.ansi().reset().toString());
    }
  }

  private String getContentPath(String id, ResourceResponse resource) {
    String path = Paths.get("metacards", id.substring(0, 3), id).toString();
    String fragment = ((URI) resource.getRequest().getAttributeValue()).getFragment();

    if (fragment == null) { // is root content, put in root id folder
      path = Paths.get(path, "content", resource.getResource().getName()).toString();
    } else { // is derived content, put in subfolder
      path = Paths.get(path, "derived", fragment, resource.getResource().getName()).toString();
    }
    return path;
  }

  private void writeToZip(/*Mutable,IO*/ ZipFile zipFile, Result result) {
    ZipParameters parameters = new ZipParameters();
    parameters.setSourceExternalStream(true);
    String id = result.getMetacard().getId();
    parameters.setFileNameInZip(
        Paths.get("metacards", id.substring(0, 3), id, "metacard", id + ".xml").toString());

    try {
      List<BinaryContent> binaryMetacards =
          getTransform()
              .transform(
                  Collections.singletonList(result.getMetacard()),
                  DEFAULT_TRANSFORMER_ID,
                  Collections.emptyMap());
      for (BinaryContent binaryMetacard : binaryMetacards) {
        try (InputStream metacard = binaryMetacard.getInputStream()) {
          zipFile.addStream(metacard, parameters);
        }
      }
    } catch (ZipException e) {
      LOGGER.error("Error processing result and adding to ZIP", e);
      throw new CatalogCommandRuntimeException(e);
    } catch (CatalogTransformerException | IOException e) {
      LOGGER.warn(
          "Could not transform metacard. Metacard will not be added to zip [{}]",
          result.getMetacard().getId());
      console.printf(
          "%sCould not transform metacard. Metacard will not be included in export. %s - %s%s%n",
          Ansi.ansi().fg(Ansi.Color.RED).toString(),
          result.getMetacard().getId(),
          result.getMetacard().getTitle(),
          Ansi.ansi().reset().toString());
    }
  }

  /**
   * Generates stateful predicate to filter distinct elements by a certain key in the object.
   *
   * @param keyExtractor Function to pull the desired key out of the object
   * @return the stateful predicate
   */
  private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Map<Object, Boolean> seen = new ConcurrentHashMap<>();
    return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
  }

  private String getTag(Result r) {
    Set<String> tags = r.getMetacard().getTags();
    if (tags.contains("deleted")) {
      return "deleted";
    } else if (tags.contains("revision")) {
      return "revision";
    } else {
      return "nonhistory";
    }
  }

  private Filter initRevisionFilter() {
    return filterBuilder.attribute(Metacard.TAGS).is().like().text("revision");
  }

  private Filter getHistoryFilter(Result result) {
    String id;
    String typeName = result.getMetacard().getMetacardType().getName();
    switch (typeName) {
      case DeletedMetacard.PREFIX:
        id = String.valueOf(result.getMetacard().getAttribute("metacard.deleted.id").getValue());
        break;
      case MetacardVersion.PREFIX:
        return null;
      default:
        id = result.getMetacard().getId();
        break;
    }

    return filterBuilder.allOf(
        revisionFilter, filterBuilder.attribute("metacard.version.id").is().equalTo().text(id));
  }

  @Override
  protected Filter getFilter() throws ParseException, CQLException {
    Filter filter = super.getFilter();
    if (archived) {
      filter =
          filterBuilder.allOf(
              filter, filterBuilder.attribute(Metacard.TAGS).is().like().text("deleted"));
    }
    return filter;
  }
}
