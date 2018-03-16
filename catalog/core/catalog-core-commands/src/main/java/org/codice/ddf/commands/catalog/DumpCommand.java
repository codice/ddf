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

import ddf.catalog.Constants;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.QueryResponseTransformer;
import ddf.catalog.util.impl.ResultIterable;
import ddf.security.common.audit.SecurityLogger;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.commons.collections.CollectionUtils;
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
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO DDF-3116 The catalog:dump shuts down DDF when it fails to transform
@Service
@Command(
  scope = CatalogCommands.NAMESPACE,
  name = "dump",
  description = "Exports Metacards from the current Catalog. Does not remove them."
)
public class DumpCommand extends CqlCommands {

  public static final String FILE_PATH = "filePath";

  private static final Logger LOGGER = LoggerFactory.getLogger(DumpCommand.class);

  private static final String ZIP_COMPRESSION = "zipCompression";

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

  // DDF-535: remove "TransformerProperties" alias in DDF 3.0
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

  private Map<String, Serializable> zipArgs;

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

    if (!SERIALIZED_OBJECT_ID.matches(transformerId)
        && !getTransform().isMetacardTransformerIdValid(transformerId)) {
      console.println(transformerId + " is an invalid metacard transformer.");
      return null;
    }

    if (StringUtils.isNotBlank(zipFileName) && new File(dirPath + zipFileName).exists()) {
      console.println("Cannot dump Catalog.  Zip file " + zipFileName + " already exists.");
      return null;
    }

    SecurityLogger.audit("Called catalog:dump command with path : {}", dirPath);

    CatalogFacade catalog = getCatalog();

    if (StringUtils.isNotBlank(zipFileName)) {
      zipArgs = new HashMap<>();
      zipArgs.put(FILE_PATH, dirPath + zipFileName);
    }

    QueryImpl query = new QueryImpl(getFilter());
    query.setRequestsTotalResultsCount(false);
    query.setPageSize(pageSize);

    Map<String, Serializable> props = new HashMap<>();
    // Avoid caching all results while dumping with native query mode
    props.put("mode", "native");

    final AtomicLong resultCount = new AtomicLong(0);
    long start = System.currentTimeMillis();

    SourceResponse response = catalog.query(new QueryRequestImpl(query, props));

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

    while (!response.getResults().isEmpty()) {
      response =
          new SourceResponseImpl(
              new QueryRequestImpl(query, props),
              ResultIterable.resultIterable(
                      catalog::query, new QueryRequestImpl(query, props), pageSize)
                  .stream()
                  .collect(Collectors.toList()));

      if (StringUtils.isNotBlank(zipFileName)) {
        try {

          BinaryContent binaryContent =
              getTransform().transform(response, ZIP_COMPRESSION, zipArgs);

          if (binaryContent != null) {
            IOUtils.closeQuietly(binaryContent.getInputStream());
          }
          Long resultSize = (long) response.getResults().size();
          printStatus(resultCount.addAndGet(resultSize));
        } catch (IllegalArgumentException e) {
          LOGGER.info(
              "No Zip TransformerProperties found.  Unable export metacards to a zip file.");
        }
      } else if (multithreaded > 1) {
        final List<Result> results = new ArrayList<>(response.getResults());
        executorService.submit(
            () -> {
              boolean transformationFailed = false;
              for (final Result result : results) {
                Metacard metacard = result.getMetacard();
                try {
                  exportMetacard(dumpDir, metacard);
                  printStatus(resultCount.incrementAndGet());
                } catch (IOException | CatalogTransformerException e) {
                  transformationFailed = true;
                  LOGGER.debug("Failed to dump metacard {}", metacard.getId(), e);
                }
              }
              if (transformationFailed) {
                LOGGER.info(
                    "One or more metacards failed to transform. Enable debug log for more details.");
              }
            });
      } else {
        for (final Result result : response.getResults()) {
          Metacard metacard = result.getMetacard();
          exportMetacard(dumpDir, metacard);
          printStatus(resultCount.incrementAndGet());
        }
      }

      if (response.getResults().size() < pageSize || pageSize == -1) {
        break;
      }

      query.setStartIndex(query.getStartIndex() + pageSize);
    }

    executorService.shutdown();

    while (!executorService.isTerminated()) {
      try {
        TimeUnit.MILLISECONDS.sleep(100);
      } catch (InterruptedException e) {
        // ignore
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

  private void exportMetacard(File dumpLocation, Metacard metacard)
      throws IOException, CatalogTransformerException {
    if (SERIALIZED_OBJECT_ID.matches(transformerId)) {
      try (ObjectOutputStream oos =
          new ObjectOutputStream(new FileOutputStream(getOutputFile(dumpLocation, metacard)))) {
        oos.writeObject(new MetacardImpl(metacard));
        oos.flush();
      }
    } else {
      List<BinaryContent> binaryContents =
          getTransform()
              .transform(
                  Collections.singletonList(metacard), transformerId, Collections.emptyMap());

      if (CollectionUtils.isNotEmpty(binaryContents)) {
        if (binaryContents.size() == 1) {
          try (FileOutputStream fos = new FileOutputStream(getOutputFile(dumpLocation, metacard))) {

            fos.write(binaryContents.get(0).getByteArray());
          }
        } else {
          createZip(binaryContents, dumpLocation, metacard);
        }
      }
    }
  }

  private void createZip(List<BinaryContent> binaryContents, File dumpLocation, Metacard metacard)
      throws CatalogTransformerException, IOException {
    File zipPath = new File(getOutputFile(dumpLocation, metacard).getAbsolutePath() + ".zip");
    try {
      ZipFile zipFile = new ZipFile(zipPath);
      int index = 1;
      for (BinaryContent binaryContent : binaryContents) {
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setSourceExternalStream(true);
        zipParameters.setFileNameInZip(String.format("%d%s", index++, generateExtension()));
        zipFile.addStream(binaryContent.getInputStream(), zipParameters);
      }
    } catch (ZipException e) {
      throw new CatalogTransformerException(
          String.format("unable to create zip file: %s", zipPath.getAbsolutePath()), e);
    }
  }

  private String generateExtension() {
    String extension = "";
    if (fileExtension != null) {
      extension = "." + fileExtension;
    }
    return extension;
  }

  private File getOutputFile(File dumpLocation, Metacard metacard) throws IOException {
    String extension = generateExtension();

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

  private Optional<QueryResponseTransformer> getZipCompression() throws InvalidSyntaxException {
    return getServiceByFilter(
        QueryResponseTransformer.class,
        "(|" + "(" + Constants.SERVICE_ID + "=" + ZIP_COMPRESSION + ")" + ")");
  }
}
