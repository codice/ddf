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

import com.google.common.io.FileBackedOutputStream;
import ddf.catalog.Constants;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.ResourceRequestById;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.util.impl.ResultIterable;
import ddf.security.common.audit.SecurityLogger;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Command(
  scope = CatalogCommands.NAMESPACE,
  name = "dump",
  description = "Exports Metacards from the current Catalog. Does not remove them."
)
public class DumpCommand extends CqlCommands {

  public static final String FILE_PATH = "filePath";

  private static final Logger LOGGER = LoggerFactory.getLogger(DumpCommand.class);

  private List<MetacardTransformer> transformers = null;

  private static final String METACARD_PATH = "metacards" + File.separator;

  private static final String CONTENT = "content";

  private static final int BUFFER_SIZE = 10000000;

  private final PeriodFormatter timeFormatter =
      new PeriodFormatterBuilder()
          .printZeroRarelyLast()
          .appendDays()
          .appendSuffix(" day", " days")
          .appendSeparator(" ")
          .appendHours()
          .appendSuffix(" hour", " hours")
          .appendSeparator(" ")
          .appendMinutes()
          .appendSuffix(" minute", " minutes")
          .appendSeparator(" ")
          .appendSeconds()
          .appendSuffix(" second", " seconds")
          .toFormatter();

  @Argument(
    name = "Dump directory path",
    description =
        "Directory to export Metacards into. Paths are absolute and must be in quotes.  Files in directory will be overwritten if they already exist.",
    index = 0,
    multiValued = false,
    required = true
  )
  String dirPath = null;

  @Argument(
    name = "Batch size",
    description =
        "Number of Metacards to retrieve and export at a time until completion. Change this argument based on system memory and CatalogProvider limits.",
    index = 1,
    multiValued = false,
    required = false
  )
  int pageSize = 1000;

  // DDF-535: remove "Transformer" alias in DDF 3.0
  @Option(
    name = "--transformer",
    required = false,
    aliases = {"-t", "Transformer"},
    multiValued = false,
    description =
        "The metacard transformer ID to use to transform metacards into data files. "
            + "The default metacard transformer is the XML transformer."
  )
  String transformerId = DEFAULT_TRANSFORMER_ID;

  // DDF-535: remove "Extension" alias in DDF 3.0
  @Option(
    name = "--extension",
    required = false,
    aliases = {"-e", "Extension"},
    multiValued = false,
    description = "The file extension of the data files."
  )
  String fileExtension = null;

  @Option(
    name = "--multithreaded",
    required = false,
    aliases = {"-m", "Multithreaded"},
    multiValued = false,
    description =
        "Number of threads to use when dumping. Setting "
            + "this value too high for your system can cause performance degradation."
  )
  int multithreaded = Runtime.getRuntime().availableProcessors();

  @Option(
    name = "--dirlevel",
    required = false,
    multiValued = false,
    description =
        "Number of subdirectory levels to create.  Two characters from the ID "
            + "will be used to name each subdirectory level."
  )
  int dirLevel = 0;

  @Option(
    name = "--include-content",
    required = false,
    aliases = {},
    multiValued = false,
    description =
        "Dump the entire Catalog and local content into a zip file with the specified name using the default transformer."
  )
  String zipFileName;

  @Override
  protected Object executeWithSubject() throws Exception {
    if (FilenameUtils.getExtension(dirPath).equals("") && !dirPath.endsWith(File.separator)) {
      dirPath += File.separator;
    }

    final File dumpDir = new File(dirPath);

    if (!dumpDir.exists()) {
      printErrorMessage("Directory [" + dirPath + "] must exist.");
      console.println("If the directory does indeed exist, try putting the path in quotes.");
      return null;
    }

    if (!dumpDir.isDirectory()) {
      printErrorMessage("Path [" + dirPath + "] must be a directory.");
      return null;
    }

    if (!SERIALIZED_OBJECT_ID.matches(transformerId)) {
      transformers = getTransformers();
      if (transformers == null) {
        console.println(transformerId + " is an invalid metacard transformer.");
        return null;
      }
    }

    if (StringUtils.isNotBlank(zipFileName) && new File(dirPath + zipFileName).exists()) {
      console.println("Cannot dump Catalog.  Zip file " + zipFileName + " already exists.");
      return null;
    }

    if (StringUtils.isNotBlank(zipFileName) && !zipFileName.endsWith(".zip")) {
      zipFileName = zipFileName + ".zip";
    }

    SecurityLogger.audit("Called catalog:dump command with path : {}", dirPath);

    CatalogFacade catalog = getCatalog();

    SortBy sort = new SortByImpl(Core.ID, SortOrder.ASCENDING);

    QueryImpl query = new QueryImpl(getFilter());
    query.setRequestsTotalResultsCount(true);
    query.setPageSize(pageSize);
    query.setSortBy(sort);

    Map<String, Serializable> props = new HashMap<>();
    // Avoid caching all results while dumping with native query mode
    props.put("mode", "native");

    final AtomicLong resultCount = new AtomicLong(0);
    long start = System.currentTimeMillis();

    BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(multithreaded);
    RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
    final ExecutorService executorService =
        new ThreadPoolExecutor(
            multithreaded,
            multithreaded,
            0L,
            TimeUnit.MILLISECONDS,
            blockingQueue,
            StandardThreadFactoryBuilder.newThreadFactory("dumpCommandThread"),
            rejectedExecutionHandler);

    QueryRequest queryRequest = new QueryRequestImpl(query, props);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Hits for Search: {}", catalog.query(queryRequest).getHits());
    }

    List<Result> results =
        ResultIterable.resultIterable(catalog::query, queryRequest)
            .stream()
            .collect(Collectors.toList());

    if (StringUtils.isNotBlank(zipFileName)) {
      SourceResponse sourceResponse = new SourceResponseImpl(queryRequest, results);
      createZip(sourceResponse, dirPath + zipFileName);
      resultCount.set(sourceResponse.getHits());
    } else {
      results
          .stream()
          .map(Collections::singletonList)
          .map(result -> new SourceResponseImpl(queryRequest, result))
          .forEach(response -> handleResult(response, executorService, dumpDir, resultCount));
    }

    executorService.shutdown();

    boolean interrupted = false;
    try {
      while (!executorService.isTerminated()) {
        try {
          TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
          interrupted = true;
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }

    long end = System.currentTimeMillis();
    String elapsedTime = timeFormatter.print(new Period(start, end).withMillis(0));
    console.printf(" %d file(s) dumped in %s\t%n", resultCount.get(), elapsedTime);
    LOGGER.debug("{} file(s) dumped in {}", resultCount.get(), elapsedTime);
    console.println();
    SecurityLogger.audit("Exported {} files to {}", resultCount.get(), dirPath);
    return null;
  }

  private void handleResult(
      SourceResponse response,
      ExecutorService executorService,
      File dumpDir,
      AtomicLong resultCount) {
    final List<Result> results = response.getResults();

    if (multithreaded > 1) {
      executorService.submit(() -> processResults(results, dumpDir, resultCount));
    } else {
      processResults(results, dumpDir, resultCount);
    }
  }

  private void processResults(List<Result> results, File dumpDir, AtomicLong resultCount) {
    for (final Result result : results) {
      Metacard metacard = result.getMetacard();
      try {
        exportMetacard(dumpDir, metacard, resultCount);
      } catch (IOException e) {
        LOGGER.debug(
            "Unable to export metacard: {} [{}]", metacard.getId(), metacard.getTitle(), e);
      }
    }
  }

  private void exportMetacard(File dumpLocation, Metacard metacard, AtomicLong resultCount)
      throws IOException {
    if (SERIALIZED_OBJECT_ID.matches(transformerId)) {
      try (ObjectOutputStream oos =
          new ObjectOutputStream(new FileOutputStream(getOutputFile(dumpLocation, metacard)))) {
        oos.writeObject(new MetacardImpl(metacard));
        oos.flush();
        resultCount.incrementAndGet();
      }
    } else {
      BinaryContent binaryContent;
      if (metacard != null) {

        for (MetacardTransformer transformer : transformers) {
          try {
            binaryContent = transformer.transform(metacard, new HashMap<>());

            if (binaryContent != null) {
              try (FileOutputStream fos =
                  new FileOutputStream(getOutputFile(dumpLocation, metacard))) {
                fos.write(binaryContent.getByteArray());
                fos.flush();
              }
              resultCount.incrementAndGet();
              break;
            }

          } catch (CatalogTransformerException e) {
            LOGGER.info(
                "One or more metacards failed to transform. Enable debug log for more details.");
          }
        }
      }
    }
  }

  private File getOutputFile(File dumpLocation, Metacard metacard) throws IOException {
    String extension = "";
    if (fileExtension != null) {
      extension = "." + fileExtension;
    }

    String id = metacard.getId();
    File parent = dumpLocation;

    if (dirLevel > 0 && id.length() >= dirLevel * 2) {
      for (int i = 0; i < dirLevel; i++) {
        parent = new File(parent, id.substring(i * 2, i * 2 + 2));
      }
      FileUtils.forceMkdir(parent);
    }

    return new File(parent, id + extension);
  }

  protected void printStatus(long count) {
    console.print(String.format(" %d file(s) dumped\t\r", count));
    console.flush();
  }

  protected List<MetacardTransformer> getTransformers() {
    ServiceReference[] refs = null;
    try {
      refs =
          bundleContext.getAllServiceReferences(
              MetacardTransformer.class.getName(),
              "(|" + "(" + Constants.SERVICE_ID + "=" + transformerId + ")" + ")");

    } catch (InvalidSyntaxException e) {
      console.printf("Fail to get MetacardTransformer references due to %s", e.getMessage());
    }
    if (refs == null || refs.length == 0) {
      return Collections.emptyList();
    }

    List<MetacardTransformer> metacardTransformerList = new ArrayList<>();
    for (ServiceReference ref : refs) {
      metacardTransformerList.add((MetacardTransformer) bundleContext.getService(ref));
    }

    return metacardTransformerList;
  }

  private void createZip(SourceResponse upstreamResponse, String filePath)
      throws CatalogTransformerException {
    if (upstreamResponse.getResults() == null || upstreamResponse.getResults().isEmpty()) {
      throw new CatalogTransformerException("Source response cannot be null or empty");
    }

    try (FileOutputStream fileOutputStream = new FileOutputStream(filePath);
        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {

      List<Result> resultList = upstreamResponse.getResults();
      Map<String, Resource> resourceMap = new HashMap<>();

      // write the metacards to the zip
      resultList
          .stream()
          .map(Result::getMetacard)
          .forEach(
              metacard -> {
                writeMetacardToZip(zipOutputStream, metacard);

                if (hasLocalResources(metacard)) {
                  resourceMap.putAll(getAllMetacardContent(metacard));
                }
              });

      // write the resources to the zip
      resourceMap.forEach(
          (filename, resource) -> writeResourceToZip(zipOutputStream, filename, resource));

    } catch (IOException e) {
      throw new CatalogTransformerException(
          String.format(
              "Error occurred when initializing/closing ZipOutputStream with path %s.", filePath),
          e);
    }
  }

  private void writeMetacardToZip(ZipOutputStream zipOutputStream, Metacard metacard) {
    InputStream inputStream = null;

    try (FileBackedOutputStream fileBackedOutputStream = new FileBackedOutputStream(BUFFER_SIZE);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileBackedOutputStream)) {

      ZipEntry zipEntry = new ZipEntry(METACARD_PATH + metacard.getId());
      zipOutputStream.putNextEntry(zipEntry);

      objectOutputStream.writeObject(new MetacardImpl(metacard));
      inputStream = fileBackedOutputStream.asByteSource().openStream();

      IOUtils.copy(inputStream, zipOutputStream);
    } catch (IOException e) {
      LOGGER.debug("Failed to add metacard with id {}.", metacard.getId(), e);
    } finally {
      try {
        if (inputStream != null) {
          inputStream.close();
        }
      } catch (IOException e) {
        LOGGER.debug("Failed to close input stream", e);
      }
    }
  }

  private boolean hasLocalResources(Metacard metacard) {
    URI uri = metacard.getResourceURI();
    return (uri != null && ContentItem.CONTENT_SCHEME.equals(uri.getScheme()));
  }

  private Map<String, Resource> getAllMetacardContent(Metacard metacard) {
    Map<String, Resource> resourceMap = new HashMap<>();
    Attribute attribute = metacard.getAttribute(Metacard.DERIVED_RESOURCE_URI);

    if (attribute != null) {
      List<Serializable> serializables = attribute.getValues();
      serializables.forEach(
          serializable -> {
            String fragment = CONTENT + File.separator;
            URI uri = null;
            try {
              uri = new URI((String) serializable);
              String derivedResourceFragment = uri.getFragment();
              if (ContentItem.CONTENT_SCHEME.equals(uri.getScheme())
                  && StringUtils.isNotBlank(derivedResourceFragment)) {
                fragment += derivedResourceFragment + File.separator;
                Resource resource = getResource(metacard);
                if (resource != null) {
                  resourceMap.put(
                      fragment + uri.getSchemeSpecificPart() + "-" + resource.getName(), resource);
                }
              }
            } catch (URISyntaxException e) {
              LOGGER.debug(
                  "Invalid Derived Resource URI Syntax for metacard : {}", metacard.getId(), e);
            }
          });
    }

    URI resourceUri = metacard.getResourceURI();

    Resource resource = getResource(metacard);

    if (resource != null) {
      resourceMap.put(
          CONTENT + File.separator + resourceUri.getSchemeSpecificPart() + "-" + resource.getName(),
          resource);
    }

    return resourceMap;
  }

  private Resource getResource(Metacard metacard) {
    Resource resource = null;

    try {
      ResourceRequest resourceRequest = new ResourceRequestById(metacard.getId());
      ResourceResponse resourceResponse = catalogFramework.getLocalResource(resourceRequest);
      resource = resourceResponse.getResource();
    } catch (IOException | ResourceNotFoundException | ResourceNotSupportedException e) {
      LOGGER.debug("Unable to retrieve content from metacard : {}", metacard.getId(), e);
    }
    return resource;
  }

  private void writeResourceToZip(
      ZipOutputStream zipOutputStream, String filename, Resource resource) {

    try (InputStream inputStream = resource.getInputStream()) {
      ZipEntry zipEntry = new ZipEntry(filename);
      zipOutputStream.putNextEntry(zipEntry);

      IOUtils.copy(inputStream, zipOutputStream);
    } catch (IOException e) {
      LOGGER.debug("Failed to add resource with id {} to zip.", resource.getName(), e);
    }
  }
}
