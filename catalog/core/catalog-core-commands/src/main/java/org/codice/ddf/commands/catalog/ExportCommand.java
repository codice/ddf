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

import ddf.catalog.CatalogFramework;
import ddf.catalog.content.StorageException;
import ddf.catalog.content.StorageProvider;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.operation.impl.DeleteStorageRequestImpl;
import ddf.catalog.core.versioning.DeletedMetacard;
import ddf.catalog.core.versioning.MetacardVersion;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterBuilder;
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
import ddf.catalog.transform.MetacardTransformer;
import ddf.security.common.audit.SecurityLogger;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.commands.catalog.export.ExportItem;
import org.codice.ddf.commands.catalog.export.IdAndUriMetacard;
import org.codice.ddf.commands.util.CatalogCommandRuntimeException;
import org.codice.ddf.commands.util.DigitalSignature;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.fusesource.jansi.Ansi;
import org.geotools.filter.text.cql2.CQLException;
import org.opengis.filter.Filter;
import org.osgi.framework.BundleContext;
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

  private static final Function<String, String> FILE_NAMER =
      ext ->
          String.format(
              "export-%s.%s",
              LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).format(FORMATTER), ext);

  private static final int PAGE_SIZE = 64;

  private static final String DELETED_METACARD = "deleted";

  private static final String REVISION_METACARD = "revision";

  private MetacardTransformer transformer;

  private Filter revisionFilter;

  private static final String SECURITY_AUDIT_DELIMITER = ", ";

  //  Number of bytes that can be sent is 65,507 (due to udp constraints). This gives a
  //  2002 byte buffer to account for anything the security log prefaces our message with
  private static final int LOG4J_MAX_BUF_SIZE = 63505;

  @Reference protected StorageProvider storageProvider;

  private DigitalSignature signer;

  @Option(
    name = "--output",
    description =
        "Output file to export Metacards and contents into. Paths are absolute and must be in quotes. Will default to auto generated name inside of ddf.home",
    multiValued = false,
    required = false,
    aliases = {"-o"}
  )
  String output = getExportFilePath();

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

  public ExportCommand() {
    this.signer = new DigitalSignature();
  }

  public ExportCommand(
      FilterBuilder filterBuilder,
      BundleContext bundleContext,
      CatalogFramework catalogFramework,
      DigitalSignature signer) {
    this.filterBuilder = filterBuilder;
    this.bundleContext = bundleContext;
    this.catalogFramework = catalogFramework;
    this.signer = signer;
  }

  @Override
  protected Object executeWithSubject() throws Exception {
    Filter filter = getFilter();
    transformer =
        getServiceByFilter(
                MetacardTransformer.class, String.format("(%s=%s)", "id", DEFAULT_TRANSFORMER_ID))
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Could not get " + DEFAULT_TRANSFORMER_ID + " transformer"));
    revisionFilter = initRevisionFilter();

    final File outputFile = initOutputFile(output);
    checkFile(outputFile);

    if (delete && !force) {
      final String input =
          session.readLine(
              "This action will remove all exported metacards and content from the catalog. Are you sure you wish to continue? (y/N):",
              null);
      if (input.length() == 0 || Character.toLowerCase(input.charAt(0)) != 'y') {
        console.println("ABORTED EXPORT.");
        return null;
      }
    }

    SecurityLogger.audit("Called catalog:export command with path : {}", output);

    try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {

      return doExport(outputFile, zipOutputStream, filter);

    } catch (FileNotFoundException e) {
      throw new FileNotFoundException(
          String.format(
              "ZipOutputStream could not be created for the path %s", outputFile.getPath()));
    }
  }

  private File initOutputFile(String output) {
    String resolvedOutput;
    File initialOutputFile = new File(output);
    if (initialOutputFile.isDirectory()) {
      // If directory was specified, auto generate file name
      resolvedOutput = Paths.get(initialOutputFile.getPath(), FILE_NAMER.apply("zip")).toString();
    } else {
      resolvedOutput = output;
    }

    return new File(resolvedOutput);
  }

  private void checkFile(File outputFile) {
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
  }

  private Object doExport(File outputFile, ZipOutputStream zipOutputStream, Filter filter)
      throws IOException {
    console.println("Starting metacard export...");
    Instant start = Instant.now();
    List<ExportItem> exportedItems = doMetacardExport(zipOutputStream, filter);
    if (exportedItems.isEmpty()) {
      console.println("No metacards found to export, exiting.");
      try {
        zipOutputStream.close();
      } finally {
        FileUtils.deleteQuietly(outputFile);
      }
      return null;
    }

    console.println("Metacards exported in: " + getFormattedDuration(start));
    console.println("Number of metacards exported: " + exportedItems.size());
    console.println();

    auditRecords(exportedItems);

    console.println("Starting content export...");
    start = Instant.now();
    List<ExportItem> exportedContentItems = doContentExport(zipOutputStream, exportedItems);
    console.println("Content exported in: " + getFormattedDuration(start));
    console.println("Number of content exported: " + exportedContentItems.size());
    console.println();

    if (delete) {
      doDelete(exportedItems, exportedContentItems);
    }

    if (!unsafe) {
      //  close the stream here to allow the jar writer to certify the full zip.
      //  Try with resources will close the stream if this is not the case.
      zipOutputStream.close();
      signJar(outputFile);
    }

    console.println("Export complete.");
    console.println("Exported to: " + outputFile.getCanonicalPath());
    return null;
  }

  private void signJar(File outputFile) {
    SecurityLogger.audit("Signing exported data. file: [{}]", outputFile.getName());
    console.println("Signing zip file...");
    Instant start = Instant.now();

    try (InputStream inputStream = new FileInputStream(outputFile)) {
      String alias = System.getProperty(SystemBaseUrl.EXTERNAL_HOST);
      String password = System.getProperty("javax.net.ssl.keyStorePassword");

      byte[] signature = signer.createDigitalSignature(inputStream, alias, password);

      if (signature != null) {
        String signatureFilepath =
            Paths.get(System.getProperty("ddf.home"), FILE_NAMER.apply("sig")).toString();
        FileUtils.writeByteArrayToFile(new File(signatureFilepath), signature);

        console.println("zip file signed in: " + getFormattedDuration(start));
      } else {
        console.println("An error occurred while signing export");
      }
    } catch (CatalogCommandRuntimeException | IOException e) {
      String message = "Unable to sign export of data";
      LOGGER.debug(message, e);
      console.println(message);
    }
  }

  private void auditRecords(List<ExportItem> exportedItems) {
    AtomicInteger counter = new AtomicInteger();
    exportedItems
        .stream()
        .map(ExportItem::getId)
        .distinct()
        .collect(Collectors.groupingBy(e -> logPartition(e, counter)))
        .values()
        .forEach(this::writePartitionToLog);
  }

  private int logPartition(String e, AtomicInteger counter) {
    return counter.getAndAdd(e.length() + SECURITY_AUDIT_DELIMITER.length()) / LOG4J_MAX_BUF_SIZE;
  }

  private void writePartitionToLog(List<String> idList) {
    SecurityLogger.audit(
        "Ids of exported metacards and content:\n{}",
        idList.stream().collect(Collectors.joining(SECURITY_AUDIT_DELIMITER, "[", "]")));
  }

  private List<ExportItem> doMetacardExport(
      /*Mutable,IO*/ ZipOutputStream zipOutputStream, Filter filter) {
    Set<String> seenIds = new HashSet<>(1024);
    List<ExportItem> exportedItems = new ArrayList<>();

    QueryImpl query = new QueryImpl(filter);
    QueryRequest queryRequest = new QueryRequestImpl(query);

    query.setPageSize(PAGE_SIZE);

    for (Result result : resultIterable(catalogFramework, queryRequest)) {
      if (!seenIds.contains(result.getMetacard().getId())) {
        writeResultToZip(zipOutputStream, result);
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
        writeResultToZip(zipOutputStream, revision);
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

  @SuppressWarnings("squid:S3776")
  private List<ExportItem> doContentExport(
      ZipOutputStream zipOutputStream, List<ExportItem> exportedItems) {
    List<ExportItem> contentItemsToExport =
        exportedItems
            .stream()
            // Only things with a resource URI
            .filter(ei -> ei.getResourceUri() != null)
            // Only our content scheme
            .filter(ei -> ei.getResourceUri().getScheme() != null)
            .filter(ei -> ei.getResourceUri().getScheme().startsWith(ContentItem.CONTENT_SCHEME))
            // Deleted Metacards have no content associated
            .filter(ei -> !ei.getMetacardTag().equals(DELETED_METACARD))
            // for revision metacards, only those that have their own content
            .filter(
                ei ->
                    !ei.getMetacardTag().equals(REVISION_METACARD)
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
      writeResourceToZip(zipOutputStream, contentItem, resource);
      exportedContentItems.add(contentItem);
      if (!contentItem.getMetacardTag().equals(REVISION_METACARD)) {
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
          writeResourceToZip(zipOutputStream, contentItem, derivedResource);
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

  private void writeResourceToZip(
      /*Mutable,IO*/ ZipOutputStream zipOutputStream,
      ExportItem exportItem,
      ResourceResponse resource) {
    String id = exportItem.getId();
    String path = getContentPath(id, resource);
    ZipEntry zipEntry = new ZipEntry(path);

    try (InputStream resourceStream = resource.getResource().getInputStream()) {
      zipOutputStream.putNextEntry(zipEntry);
      IOUtils.copy(resourceStream, zipOutputStream);
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

  private void writeResultToZip(/*Mutable,IO*/ ZipOutputStream zipOutputStream, Result result) {
    String id = result.getMetacard().getId();
    ZipEntry zipEntry =
        new ZipEntry(
            Paths.get("metacards", id.substring(0, 3), id, "metacard", id + ".xml").toString());

    try {
      BinaryContent binaryMetacard =
          transformer.transform(result.getMetacard(), Collections.emptyMap());
      try (InputStream metacardStream = binaryMetacard.getInputStream()) {
        zipOutputStream.putNextEntry(zipEntry);
        IOUtils.copy(metacardStream, zipOutputStream);
      }
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
    if (tags.contains(DELETED_METACARD)) {
      return DELETED_METACARD;
    } else if (tags.contains(REVISION_METACARD)) {
      return REVISION_METACARD;
    } else {
      return "nonhistory";
    }
  }

  private Filter initRevisionFilter() {
    return filterBuilder.attribute(Metacard.TAGS).is().like().text(REVISION_METACARD);
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
              filter, filterBuilder.attribute(Metacard.TAGS).is().like().text(DELETED_METACARD));
    }
    return filter;
  }

  private String getExportFilePath() {
    return Paths.get(System.getProperty("ddf.home"), FILE_NAMER.apply("zip")).toString();
  }
}
