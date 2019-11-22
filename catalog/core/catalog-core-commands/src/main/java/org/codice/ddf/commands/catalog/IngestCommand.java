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

import com.google.common.io.ByteSource;
import ddf.catalog.Constants;
import ddf.catalog.content.StorageException;
import ddf.catalog.content.StorageProvider;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.operation.impl.CreateStorageRequestImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputCollectionTransformer;
import ddf.catalog.transform.InputTransformer;
import ddf.security.common.audit.SecurityLogger;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.codice.ddf.commands.util.CatalogCommandRuntimeException;
import org.codice.ddf.commands.util.DigitalSignature;
import org.codice.ddf.log.sanitizer.LogSanitizer;
import org.codice.ddf.platform.util.Exceptions;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.fusesource.jansi.Ansi;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Custom Karaf command for ingesting records into the Catalog. */
@Service
@Command(
  scope = CatalogCommands.NAMESPACE,
  name = "ingest",
  description = "Ingests Metacards into the Catalog."
)
public class IngestCommand extends CatalogCommands {

  private static final Logger LOGGER = LoggerFactory.getLogger(IngestCommand.class);

  private static final String NEW_LINE = System.getProperty("line.separator");

  private static final Logger INGEST_LOGGER = LoggerFactory.getLogger(Constants.INGEST_LOGGER_NAME);

  private static final int DEFAULT_BATCH_SIZE = 500;

  /**
   * The maximum size of the blocking queue that holds metacards in process. This value has been set
   * to be lower than the maximum number of parties to a single Phaser to simplify the Phaser
   * processing (obviating the need for tiered Phasers) and to protect the server from running out
   * of memory with too many objects in the queue at any time.
   */
  private static final int MAX_QUEUE_SIZE = 65000;

  private static final String CONTENT = "content";

  private static final String FILE_NAME = "fileName";

  private static final String ZIP_DECOMPRESSION = "zipDecompression";

  private static final String THREAD_NAME = "ingestCommandThread";

  private static final String CONTENT_PATH = CONTENT + File.separator;

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

  private final Phaser phaser = new Phaser();

  private final AtomicInteger ingestCount = new AtomicInteger();

  private final AtomicInteger ignoreCount = new AtomicInteger();

  private final AtomicInteger fileCount = new AtomicInteger(Integer.MAX_VALUE);

  private DigitalSignature verifier;

  @Argument(
    name = "File path or Directory path",
    description =
        "Path to a file or a directory of file(s) to be ingested. Paths can be absolute or relative to installation directory."
            + " This command can only detect roughly 2 billion files in one directory. Individual operating system limits might also apply.",
    index = 0,
    multiValued = false,
    required = true
  )
  @Completion(FileCompleter.class)
  String filePath;

  // DDF-535: Remove this argument in ddf-3.0
  @Argument(
    name = "Batch size",
    description =
        "Number of Metacards to ingest at a time. Change this argument based on system memory and Catalog Provider limits. [DEPRECATED: use --batchsize option instead]",
    index = 1,
    multiValued = false,
    required = false
  )
  int deprecatedBatchSize = DEFAULT_BATCH_SIZE;

  // DDF-535: remove "Transformer" alias in ddf-3.0
  @Option(
    name = "--transformer",
    required = false,
    aliases = {"-t", "Transformer"},
    multiValued = false,
    description =
        "The metacard transformer ID to use to transform data file(s) into metacard(s). "
            + "The default metacard transformer is the XML transformer."
  )
  String transformerId = DEFAULT_TRANSFORMER_ID;

  // DDF-535: Remove "Multithreaded" alias in ddf-3.0
  @Option(
    name = "--multithreaded",
    required = false,
    aliases = {"-m", "Multithreaded"},
    multiValued = false,
    description =
        "Number of threads to use when ingesting. Setting this value too high for your system can cause performance degradation."
  )
  int multithreaded = 8;

  // DDF-535: remove "-d" and "Ingest Failure Directory" aliases in ddf-3.0
  @Option(
    name = "--failedDir",
    required = false,
    aliases = {"-d", "-f", "Ingest Failure Directory"},
    multiValued = false,
    description =
        "The directory to put file(s) that failed to ingest. Using this option will force a batch size of 1."
  )
  String failedDir = null;

  @Option(
    name = "--batchsize",
    required = false,
    aliases = {"-b"},
    multiValued = false,
    description =
        "Number of Metacards to ingest at a time. Change this argument based on system memory and Catalog Provider limits."
  )
  int batchSize = DEFAULT_BATCH_SIZE;

  @Option(
    name = "--ignore",
    required = false,
    aliases = {"-i"},
    multiValued = true,
    description =
        "File extension(s) or file name(s) to ignore during ingestion (-i '.txt' -i 'image.jpg' -i 'file' )"
  )
  List<String> ignoreList;

  @Option(
    name = "--include-content",
    required = false,
    aliases = {},
    multiValued = false,
    description =
        "Ingest a zip file that contains metacards and content using the default transformer. The specified zip must be signed externally using DDF certificates."
  )
  boolean includeContent = false;

  @Option(
    name = "--signature",
    required = false,
    aliases = {"-s"},
    multiValued = false,
    description =
        "Provided absolute path for the digital signature to verify the integrity of the exported data. Required when the `--include-content` option is specified."
  )
  String signatureFile;

  @Reference StorageProvider storageProvider;

  private Map<String, List<File>> metacardFileMapping;

  private File failedIngestDirectory = null;

  private Optional<InputTransformer> transformer = null;

  public IngestCommand() {
    this.verifier = new DigitalSignature();
  }

  public IngestCommand(DigitalSignature verifier) {
    this.verifier = verifier;
  }

  @Override
  protected Object executeWithSubject() throws Exception {
    if (batchSize * multithreaded > MAX_QUEUE_SIZE) {
      throw new IngestException(
          String.format("batchsize * multithreaded cannot be larger than %d.", MAX_QUEUE_SIZE));
    }

    final File inputFile = getInputFile();
    if (inputFile == null) {
      return null;
    }

    int totalFiles = totalFileCount(inputFile);
    fileCount.set(totalFiles);

    final ArrayBlockingQueue<Metacard> metacardQueue =
        new ArrayBlockingQueue<>(batchSize * multithreaded);

    ExecutorService queueExecutor =
        Executors.newSingleThreadExecutor(
            StandardThreadFactoryBuilder.newThreadFactory(THREAD_NAME));

    final long start = System.currentTimeMillis();

    printProgressAndFlush(start, fileCount.get(), 0);

    // Registering for the main thread and on behalf of the buildQueue thread;
    // the buildQueue thread will unregister itself when the files have all
    // been added to the blocking queue and the final registration will
    // be held for the await.
    phaser.register();
    phaser.register();
    queueExecutor.submit(() -> buildQueue(inputFile, metacardQueue, start));

    final ScheduledExecutorService batchScheduler =
        Executors.newSingleThreadScheduledExecutor(
            StandardThreadFactoryBuilder.newThreadFactory(THREAD_NAME));

    BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(multithreaded);
    RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
    ExecutorService executorService =
        new ThreadPoolExecutor(
            multithreaded,
            multithreaded,
            0L,
            TimeUnit.MILLISECONDS,
            blockingQueue,
            StandardThreadFactoryBuilder.newThreadFactory(THREAD_NAME),
            rejectedExecutionHandler);

    final CatalogFacade catalog = getCatalog();
    submitToCatalog(batchScheduler, executorService, metacardQueue, catalog, start);

    // await on catalog processing threads to complete emptying queue
    phaser.awaitAdvance(phaser.arrive());

    try {
      queueExecutor.shutdown();
      executorService.shutdown();
      batchScheduler.shutdown();
    } catch (SecurityException e) {
      LOGGER.info("Executor service shutdown was not permitted: {}", e);
    }

    printProgressAndFlush(start, fileCount.get(), (long) ingestCount.get() + ignoreCount.get());
    long end = System.currentTimeMillis();
    console.println();
    String elapsedTime = timeFormatter.print(new Period(start, end).withMillis(0));

    console.println();
    console.printf(" %d file(s) ingested in %s %n", ingestCount.get(), elapsedTime);

    LOGGER.debug(
        "{} file(s) ingested in {} [{} records/sec]",
        ingestCount.get(),
        elapsedTime,
        calculateRecordsPerSecond(ingestCount.get(), start, end));
    INGEST_LOGGER.info(
        "{} file(s) ingested in {} [{} records/sec]",
        ingestCount.get(),
        elapsedTime,
        calculateRecordsPerSecond(ingestCount.get(), start, end));

    if (fileCount.get() != ingestCount.get()) {
      if ((fileCount.get() - ingestCount.get() - ignoreCount.get()) >= 1) {
        String failedAmount =
            Integer.toString(fileCount.get() - ingestCount.get() - ignoreCount.get());
        console.println();
        printErrorMessage(
            failedAmount + " file(s) failed to be ingested. See the ingest log for more details.");
        INGEST_LOGGER.warn("{} file(s) failed to be ingested.", failedAmount);
      }
      if (ignoreList != null) {
        String ignoredAmount = Integer.toString(ignoreCount.get());
        console.println();
        printColor(
            Ansi.Color.YELLOW,
            ignoredAmount + " file(s) ignored. See the ingest log for more details.");
        INGEST_LOGGER.warn("{} file(s) were ignored.", ignoredAmount);
      }
    }
    console.println();
    SecurityLogger.audit("Ingested {} file(s) from {}", ingestCount.get(), filePath);
    return null;
  }

  private File getInputFile() {
    final File inputFile = new File(filePath);

    SecurityLogger.audit("Called catalog:ingest command with path : {}", filePath);

    if (!inputFile.exists()) {
      printErrorMessage(String.format("File or directory [%s] must exist.", filePath));
      console.println("If the file does indeed exist, try putting the path in quotes.");
      return null;
    }

    if (includeContent && !FilenameUtils.isExtension(filePath, "zip")) {
      console.printf("File %s must be a zip file.", filePath);
      return null;
    }

    if (includeContent && StringUtils.isBlank(signatureFile)) {
      console.print(
          "You must provide a signature file when the `--include-content` option is specified.");
      return null;
    }

    if (deprecatedBatchSize != DEFAULT_BATCH_SIZE) {
      // user specified the old style batch size, so use that
      printErrorMessage(
          "Batch size positional argument is DEPRECATED, please use --batchsize option instead.");
      batchSize = deprecatedBatchSize;
    }

    if (batchSize <= 0) {
      printErrorMessage(
          String.format(
              "A batch size of [%d] was supplied. Batch size must be greater than 0.", batchSize));
      return null;
    }

    if (StringUtils.isNotEmpty(failedDir)) {
      failedIngestDirectory = new File(failedDir);
      if (!verifyFailedIngestDirectory()) {
        return null;
      }

      // Batch size is always set to one when using an Ingest Failure Directory. If a batch
      // size is specified by the user, issue a warning stating that a batch size of one will
      // be used.
      if (batchSize != DEFAULT_BATCH_SIZE) {
        console.printf(
            "WARNING: An ingest failure directory was supplied in addition to a batch "
                + "size of %d. When using an ingest failure directory, the batch "
                + "size must be 1. Setting batch size to 1.%n",
            batchSize);
      }

      batchSize = 1;
    }

    if (!SERIALIZED_OBJECT_ID.matches(transformerId)) {
      transformer = getTransformer();
      if (!transformer.isPresent()) {
        console.println(transformerId + " is an invalid input transformer.");
        return null;
      }
    }
    return inputFile;
  }

  private int totalFileCount(File inputFile) throws IOException {
    if (inputFile.isDirectory()) {
      int currentFileCount = 0;
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputFile.toPath())) {
        for (Path entry : stream) {
          if (!entry.toFile().isHidden()) {
            currentFileCount++;
          }
        }
        return currentFileCount;
      }
    }

    return inputFile.isHidden() ? 0 : 1;
  }

  /** Helper method to build ingest log strings */
  private String buildIngestLog(ArrayList<Metacard> metacards) {
    StringBuilder strBuilder = new StringBuilder();

    for (int i = 0; i < metacards.size(); i++) {
      Metacard card = metacards.get(i);
      strBuilder.append(NEW_LINE).append("Batch #: ").append(i + 1).append(" | ");
      if (card != null) {
        if (card.getTitle() != null) {
          strBuilder
              .append("Metacard Title: ")
              .append(LogSanitizer.sanitize(card.getTitle()))
              .append(" | ");
        }
        if (card.getId() != null) {
          strBuilder
              .append("Metacard ID: ")
              .append(LogSanitizer.sanitize(card.getId()))
              .append(" | ");
        }
      } else {
        strBuilder.append("Null Metacard");
      }
    }
    return strBuilder.toString();
  }

  private void logIngestException(IngestException exception, File inputFile) {
    LOGGER.debug("Failed to ingest file [{}].", inputFile.getAbsolutePath(), exception);
    INGEST_LOGGER.warn(
        "Failed to ingest file [{}]:\n{}",
        inputFile.getAbsolutePath(),
        Exceptions.getFullMessage(exception));
  }

  private CreateResponse createMetacards(CatalogFacade catalog, List<Metacard> listOfMetaCards)
      throws IngestException, SourceUnavailableException {
    CreateRequest createRequest = new CreateRequestImpl(listOfMetaCards);
    if (metacardFileMapping != null) {
      submitToStorageProvider(listOfMetaCards);
    }
    return catalog.create(createRequest);
  }

  private Metacard readMetacard(File file) throws IngestException {
    Metacard result = null;

    FileInputStream fis = null;
    ObjectInputStream ois = null;

    try {
      if (SERIALIZED_OBJECT_ID.matches(transformerId)) {
        ois = new ObjectInputStream(new FileInputStream(file));
        result = (Metacard) ois.readObject();
        ois.close();
      } else {
        fis = new FileInputStream(file);
        result = generateMetacard(fis);
        if (StringUtils.isBlank(result.getTitle())) {
          LOGGER.debug("Metacard title was blank. Setting title to filename.");
          result.setAttribute(new AttributeImpl(Metacard.TITLE, file.getName()));
        }
        fis.close();
      }
    } catch (IOException | IllegalArgumentException | ClassNotFoundException e) {
      throw new IngestException(e);
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e1) {
          console.println(e1);
        }
      }

      if (ois != null) {
        try {
          ois.close();
        } catch (IOException e2) {
          console.println(e2);
        }
      }
    }
    return result;
  }

  private Metacard generateMetacard(InputStream message) throws IOException {
    try {
      if (message == null) {
        throw new IllegalArgumentException("Data file is null.");
      }
      if (!transformer.isPresent()) {
        throw new IllegalArgumentException(
            "Transformation Failed for transformer: " + transformerId);
      }
      return transformer.get().transform(message);

    } catch (CatalogTransformerException e) {
      throw new IllegalArgumentException(
          "Transformation Failed for transformer: " + transformerId, e);
    }
  }

  private boolean verifyFailedIngestDirectory() {
    if (!failedIngestDirectory.exists()) {
      makeFailedIngestDirectory();
    }

    if (!failedIngestDirectory.canWrite()) {
      printErrorMessage(
          "Directory [" + failedIngestDirectory.getAbsolutePath() + "] is not writable.");
      return false;
    } else {
      return true;
    }
  }

  private void makeFailedIngestDirectory() {
    if (!failedIngestDirectory.mkdirs()) {
      printErrorMessage(
          String.format(
              "Unable to create directory [%s].", failedIngestDirectory.getAbsolutePath()));
    }
  }

  private boolean processBatch(CatalogFacade catalog, ArrayList<Metacard> metacards)
      throws SourceUnavailableException {
    CreateResponse createResponse = null;

    try {
      createResponse = createMetacards(catalog, metacards);
    } catch (IngestException e) {
      printErrorMessage("Error executing command: " + e.getMessage());
      if (INGEST_LOGGER.isWarnEnabled()) {
        INGEST_LOGGER.warn("Error ingesting metacard batch {}", buildIngestLog(metacards), e);
      }
    } catch (SourceUnavailableException e) {
      if (INGEST_LOGGER.isWarnEnabled()) {
        INGEST_LOGGER.warn(
            "Error on process batch, local Provider not available. {}"
                + " metacards failed to ingest. {}",
            metacards.size(),
            buildIngestLog(metacards),
            e);
      }
    } finally {
      IntStream range = IntStream.range(0, metacards.size());
      range.forEach(i -> phaser.arriveAndDeregister());
      range.close();
    }

    if (createResponse != null) {
      ingestCount.getAndAdd(metacards.size());
    }
    return createResponse != null;
  }

  private void moveToFailedIngestDirectory(File source) {
    File destination =
        Paths.get(failedIngestDirectory.getAbsolutePath(), source.getName()).toFile();

    if (!source.renameTo(destination)) {
      printErrorMessage(
          "Unable to move source file ["
              + source.getAbsolutePath()
              + "] to ["
              + failedIngestDirectory
              + "].");
    }
  }

  private void buildQueue(File inputFile, ArrayBlockingQueue<Metacard> metacardQueue, long start) {
    try {
      if (includeContent) {
        try (InputStream data = new FileInputStream(inputFile);
            InputStream signature = new FileInputStream(signatureFile)) {
          String alias =
              AccessController.doPrivileged(
                  (PrivilegedAction<String>)
                      () -> System.getProperty("org.codice.ddf.system.hostname"));

          if (verifier.verifyDigitalSignature(data, signature, alias)) {
            processIncludeContent(metacardQueue);
          } else {
            console.println("The provided signature was invalid for the provided data");
          }
        } catch (IOException | CatalogCommandRuntimeException e) {
          throw new CatalogCommandRuntimeException(
              "An error occurred while verifying digital signature", e);
        }
      } else {
        try (Stream<Path> ingestStream =
            Files.walk(inputFile.toPath(), FileVisitOption.FOLLOW_LINKS)) {
          ingestStream
              .map(Path::toFile)
              .filter(file -> !file.isDirectory())
              .forEach(file -> addFileToQueue(metacardQueue, start, file));
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
    } finally {
      phaser.arriveAndDeregister();
    }
  }

  private void addFileToQueue(ArrayBlockingQueue<Metacard> metacardQueue, long start, File file) {
    if (file.isHidden()) {
      fileCount.incrementAndGet();
      ignoreCount.incrementAndGet();
      return;
    }

    String extension = "." + FilenameUtils.getExtension(file.getName());
    if (ignoreList != null
        && (ignoreList.contains(extension) || ignoreList.contains(file.getName()))) {
      ignoreCount.incrementAndGet();
      printProgressAndFlush(start, fileCount.get(), (long) ingestCount.get() + ignoreCount.get());
      return;
    }

    Metacard result = null;
    try {
      result = readMetacard(file);
    } catch (IngestException e) {
      logIngestException(e, file);
      if (failedIngestDirectory != null) {
        moveToFailedIngestDirectory(file);
      }
    }

    if (result != null) {
      putMetacardOnQueue(metacardQueue, result);
    }
  }

  private void putMetacardOnQueue(ArrayBlockingQueue<Metacard> metacardQueue, Metacard metacard) {
    try {
      phaser.register();
      metacardQueue.put(metacard);
    } catch (InterruptedException e) {
      phaser.arriveAndDeregister();

      INGEST_LOGGER.error(
          "Thread interrupted while waiting to 'put' metacard: {}",
          LogSanitizer.sanitize(metacard.getId()),
          e);

      Thread.currentThread().interrupt();
    }
  }

  private void processIncludeContent(ArrayBlockingQueue<Metacard> metacardQueue) {
    File inputFile = new File(filePath);
    Map<String, Serializable> arguments = new HashMap<>();
    arguments.put(DumpCommand.FILE_PATH, inputFile.getParent() + File.separator);
    arguments.put(FILE_NAME, inputFile.getName());

    ByteSource byteSource = com.google.common.io.Files.asByteSource(inputFile);

    Optional<InputCollectionTransformer> zipDecompression = getZipDecompression();
    if (zipDecompression.isPresent()) {
      try (InputStream inputStream = byteSource.openBufferedStream()) {
        List<Metacard> metacardList =
            zipDecompression
                .get()
                .transform(inputStream, arguments)
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (metacardList.size() != 0) {
          metacardFileMapping = generateFileMap(new File(inputFile.getParent(), CONTENT_PATH));
          fileCount.set(metacardList.size());

          for (Metacard metacard : metacardList) {
            putMetacardOnQueue(metacardQueue, metacard);
          }
        }
      } catch (IOException | CatalogTransformerException e) {
        LOGGER.info("Unable to transform zip file into metacard list.", e);
        INGEST_LOGGER.warn("Unable to transform zip file into metacard list.", e);
      }
    } else {
      LOGGER.info("No Zip Transformer found. Unable to transform zip file into metacard list.");
      INGEST_LOGGER.warn(
          "No Zip Transformer found. Unable to transform zip file into metacard list.");
    }
  }

  private void submitToStorageProvider(List<Metacard> metacardList) {
    metacardList
        .stream()
        .filter(metacard -> metacardFileMapping.containsKey(metacard.getId()))
        .map(
            metacard -> {
              List<File> fileList = metacardFileMapping.get(metacard.getId());
              List<ContentItem> contentItemList = new ArrayList<>();
              ContentItem contentItem;

              for (File file : fileList) {
                ByteSource byteSource = com.google.common.io.Files.asByteSource(file);
                String fileName = file.getName().split("-")[1];

                String fragment = null;
                if (!file.getPath().contains(CONTENT + File.separator + metacard.getId())) {
                  fragment =
                      StringUtils.substringBetween(
                          file.getPath(),
                          CONTENT + File.separator,
                          File.separator + metacard.getId());
                }
                contentItem =
                    new ContentItemImpl(
                        metacard.getId(),
                        fragment,
                        byteSource,
                        metacard.getContentTypeName(),
                        fileName,
                        file.length(),
                        metacard);
                contentItemList.add(contentItem);
              }

              return new CreateStorageRequestImpl(
                  contentItemList, metacard.getId(), new HashMap<>());
            })
        .forEach(
            createStorageRequest -> {
              try {
                storageProvider.create(createStorageRequest);
                storageProvider.commit(createStorageRequest);
              } catch (StorageException e) {
                LOGGER.debug("Unable to create content for {}", createStorageRequest.getId(), e);
                try {
                  storageProvider.rollback(createStorageRequest);
                } catch (StorageException e1) {
                  LOGGER.debug(
                      "Unable to perform rollback on temporary content for {} ",
                      createStorageRequest.getId(),
                      e1);
                }
              }
            });
  }

  private void submitToCatalog(
      ScheduledExecutorService batchScheduler,
      ExecutorService executorService,
      ArrayBlockingQueue<Metacard> metacardQueue,
      CatalogFacade catalog,
      long start) {

    batchScheduler.scheduleWithFixedDelay(
        () -> {
          int queueSize = metacardQueue.size();
          if (queueSize > 0) {
            ArrayList<Metacard> metacardBatch = new ArrayList<>(batchSize);

            // When the producer has finished populating the queue, it will countdown
            // the phaser. The remaining count in the phaser will be metacardCount + main thread
            if (queueSize >= batchSize || queueSize == phaser.getRegisteredParties() - 1) {
              metacardQueue.drainTo(metacardBatch, batchSize);
            }

            if (!metacardBatch.isEmpty()) {
              executorService.submit(
                  () -> {
                    try {
                      processBatch(catalog, metacardBatch);
                    } catch (SourceUnavailableException e) {
                      INGEST_LOGGER.warn("Error on process batch.", e);
                    }
                  });

              printProgressAndFlush(
                  start, fileCount.get(), (long) ingestCount.get() + ignoreCount.get());
            }
          }
        },
        100,
        100,
        TimeUnit.MILLISECONDS);
  }

  private Map<String, List<File>> generateFileMap(File inputFile) throws IOException {
    if (!inputFile.exists()) {
      return null;
    }

    Map<String, List<File>> fileMap = new HashMap<>();
    Files.walkFileTree(
        inputFile.toPath(),
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path filePathToVisit, BasicFileAttributes attrs)
              throws IOException {

            File file = filePathToVisit.toFile();

            if (file.getParent().contains(CONTENT) && !file.isDirectory() && !file.isHidden()) {
              addFileToMap(fileMap, file);
            }
            return FileVisitResult.CONTINUE;
          }
        });
    return fileMap;
  }

  private void addFileToMap(Map<String, List<File>> fileMap, File file) {
    String[] fileName = file.getName().split("-");
    if (fileName.length == 2) {
      fileMap.putIfAbsent(fileName[0], new ArrayList<>());
      fileMap.get(fileName[0]).add(file);
    } else if (!file.isHidden()) {
      LOGGER.debug(
          "Filename {} does not follow expected convention : ID-Filename, and will be skipped.",
          file.getName());
    }
  }

  private Optional<InputCollectionTransformer> getZipDecompression() {
    try {
      return getServiceByFilter(
          InputCollectionTransformer.class,
          "(|" + "(" + Constants.SERVICE_ID + "=" + ZIP_DECOMPRESSION + ")" + ")");
    } catch (InvalidSyntaxException e) {
      LOGGER.info("Unable to get transformer id={}", ZIP_DECOMPRESSION, e);
      return Optional.empty();
    }
  }

  private Optional<InputTransformer> getTransformer() {
    try {
      return getServiceByFilter(
          InputTransformer.class,
          "(|" + "(" + Constants.SERVICE_ID + "=" + transformerId + ")" + ")");
    } catch (InvalidSyntaxException e) {
      throw new IllegalArgumentException("Invalid transformer transformerId: " + transformerId, e);
    }
  }
}
