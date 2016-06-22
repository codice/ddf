/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.commands.catalog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.codice.ddf.platform.util.Exceptions;
import org.fusesource.jansi.Ansi;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * Custom Karaf command for ingesting records into the Catalog.
 */
@Command(scope = CatalogCommands.NAMESPACE, name = "ingest", description = "Ingests Metacards into the Catalog.")
public class IngestCommand extends CatalogCommands {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestCommand.class);

    private static final Logger INGEST_LOGGER =
            LoggerFactory.getLogger(Constants.INGEST_LOGGER_NAME);

    private static final int DEFAULT_BATCH_SIZE = 500;

    private static final String CONTENT = "content";

    private static final String FILE_NAME = "fileName";

    private static final String ZIP_DECOMPRESSION = "zipDecompression";

    private static final String METACARD_PATH = "metacards" + File.separator;

    private final PeriodFormatter timeFormatter = new PeriodFormatterBuilder().printZeroRarelyLast()
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

    private final AtomicInteger ingestCount = new AtomicInteger();

    private final AtomicInteger ignoreCount = new AtomicInteger();

    private final AtomicBoolean doneBuildingQueue = new AtomicBoolean();

    private final AtomicInteger processingThreads = new AtomicInteger();

    private final AtomicInteger fileCount = new AtomicInteger(Integer.MAX_VALUE);

    private Map<String, List<File>> metacardFileMapping;

    private File failedIngestDirectory = null;

    private InputTransformer transformer = null;

    private InputCollectionTransformer zipDecompression;

    @Argument(name = "File path or Directory path", description =
            "File path to a record or a directory of files to be ingested. Paths are absolute and must be in quotes."
                    + " This command can only detect roughly 2 billion records in one folder. Individual operating system limits might also apply.", index = 0, multiValued = false, required = true)
    String filePath = null;

    // DDF-535: Remove this argument in ddf-3.0
    @Argument(name = "Batch size", description = "Number of Metacards to ingest at a time. Change this argument based on system memory and catalog provider limits. [DEPRECATED: use --batchsize option instead]", index = 1, multiValued = false, required = false)
    int deprecatedBatchSize = DEFAULT_BATCH_SIZE;

    // DDF-535: remove "Transformer" alias in ddf-3.0
    @Option(name = "--transformer", required = false, aliases = {"-t",
            "Transformer"}, multiValued = false, description = "The metacard transformer ID to use to transform data files into metacards. The default metacard transformer is the Java serialization transformer.")
    String transformerId = DEFAULT_TRANSFORMER_ID;

    // DDF-535: Remove "Multithreaded" alias in ddf-3.0
    @Option(name = "--multithreaded", required = false, aliases = {"-m",
            "Multithreaded"}, multiValued = false, description = "Number of threads to use when ingesting. Setting this value too high for your system can cause performance degradation.")
    int multithreaded = 8;

    // DDF-535: remove "-d" and "Ingest Failure Directory" aliases in ddf-3.0
    @Option(name = "--failedDir", required = false, aliases = {"-d", "-f",
            "Ingest Failure Directory"}, multiValued = false, description = "The directory to put files that failed to ingest.  Using this option will force a batch size of 1.")
    String failedDir = null;

    @Option(name = "--batchsize", required = false, aliases = {
            "-b"}, multiValued = false, description = "Number of Metacards to ingest at a time. Change this argument based on system memory and catalog provider limits.")
    int batchSize = DEFAULT_BATCH_SIZE;

    @Option(name = "--ignore", required = false, aliases = {
            "-i"}, multiValued = true, description = "File extension(s) or file name(s) to ignore during ingestion (-i '.txt' -i 'image.jpg' -i 'file' )")
    List<String> ignoreList;

    @Option(name = "--include-content", required = false, aliases = {}, multiValued = false, description = "Ingest a zip file that contains metacards and content using the default transformer.  The specified zip must be signed externally using DDF certificates.")
    boolean includeContent = false;

    @Override
    protected Object executeWithSubject() throws Exception {

        final CatalogFacade catalog = getCatalog();

        final File inputFile = new File(filePath);

        SecurityLogger.audit("Called catalog:ingest command with path : {}", filePath);

        if (!inputFile.exists()) {
            printErrorMessage("File or directory [" + filePath + "] must exist.");
            console.println("If the file does indeed exist, try putting the path in quotes.");
            return null;
        }

        if (includeContent && !FilenameUtils.isExtension(filePath, "zip")) {
            console.print("File " + filePath + " must be a zip file.");
            return null;
        }

        if (deprecatedBatchSize != DEFAULT_BATCH_SIZE) {
            // user specified the old style batch size, so use that
            printErrorMessage(
                    "Batch size positional argument is DEPRECATED, please use --batchsize option instead.");
            batchSize = deprecatedBatchSize;
        }

        if (batchSize <= 0) {
            printErrorMessage("A batch size of [" + batchSize
                    + "] was supplied. Batch size must be greater than 0.");
            return null;
        }

        if (!StringUtils.isEmpty(failedDir)) {
            failedIngestDirectory = new File(failedDir);
            if (!verifyFailedIngestDirectory()) {
                return null;
            }

            /**
             * Batch size is always set to 1, when using an Ingest Failure Directory.  If a batch size is specified by the user, issue
             * a warning stating that a batch size of 1 will be used.
             */
            if (batchSize != DEFAULT_BATCH_SIZE) {
                console.println(
                        "WARNING: An ingest failure directory was supplied in addition to a batch size of "
                                + batchSize
                                + ". When using an ingest failure directory, the batch size must be 1. Setting batch size to 1.");
            }

            batchSize = 1;
        }

        BundleContext bundleContext = getBundleContext();
        if (!DEFAULT_TRANSFORMER_ID.equals(transformerId)) {
            ServiceReference[] refs;

            try {
                refs = bundleContext.getServiceReferences(InputTransformer.class.getName(),
                        "(|" + "(" + Constants.SERVICE_ID + "=" + transformerId + ")" + ")");
            } catch (InvalidSyntaxException e) {
                throw new IllegalArgumentException(
                        "Invalid transformer transformerId: " + transformerId, e);
            }

            if (refs == null || refs.length == 0) {
                throw new IllegalArgumentException("Transformer " + transformerId + " not found");
            } else {
                transformer = (InputTransformer) bundleContext.getService(refs[0]);
            }
        }

        Stream<Path> ingestStream = Files.walk(inputFile.toPath(), FileVisitOption.FOLLOW_LINKS);

        int totalFiles = (inputFile.isDirectory()) ? inputFile.list().length : 1;
        fileCount.getAndSet(totalFiles);

        final ArrayBlockingQueue<Metacard> metacardQueue = new ArrayBlockingQueue<>(
                batchSize * multithreaded);

        ExecutorService queueExecutor = Executors.newSingleThreadExecutor();

        final long start = System.currentTimeMillis();

        printProgressAndFlush(start, fileCount.get(), 0);

        queueExecutor.submit(() -> buildQueue(ingestStream, metacardQueue, start));

        final ScheduledExecutorService batchScheduler =
                Executors.newSingleThreadScheduledExecutor();

        BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(multithreaded);
        RejectedExecutionHandler rejectedExecutionHandler =
                new ThreadPoolExecutor.CallerRunsPolicy();
        ExecutorService executorService = new ThreadPoolExecutor(multithreaded,
                multithreaded,
                0L,
                TimeUnit.MILLISECONDS,
                blockingQueue,
                rejectedExecutionHandler);

        submitToCatalog(batchScheduler, executorService, metacardQueue, catalog, start);

        while (!doneBuildingQueue.get() || processingThreads.get() != 0) {
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                LOGGER.error("Ingest 'Waiting for processing to finish' thread interrupted: {}", e);
            }
        }

        try {
            queueExecutor.shutdown();
            executorService.shutdown();
            batchScheduler.shutdown();
        } catch (SecurityException e) {
            LOGGER.error("Executor service shutdown was not permitted: {}", e);
        }

        printProgressAndFlush(start, fileCount.get(), ingestCount.get() + ignoreCount.get());
        long end = System.currentTimeMillis();
        console.println();
        String elapsedTime = timeFormatter.print(new Period(start, end).withMillis(0));

        console.println();
        console.printf(" %d file(s) ingested in %s %n", ingestCount.get(), elapsedTime);

        LOGGER.info("{} file(s) ingested in {} [{} records/sec]",
                ingestCount.get(),
                elapsedTime,
                calculateRecordsPerSecond(ingestCount.get(), start, end));
        INGEST_LOGGER.info("{} file(s) ingested in {} [{} records/sec]",
                ingestCount.get(),
                elapsedTime,
                calculateRecordsPerSecond(ingestCount.get(), start, end));

        if (fileCount.get() != ingestCount.get()) {
            console.println();
            if ((fileCount.get() - ingestCount.get() - ignoreCount.get()) >= 1) {
                String failedAmount = Integer.toString(
                        fileCount.get() - ingestCount.get() - ignoreCount.get());
                printErrorMessage(failedAmount
                        + " file(s) failed to be ingested.  See the ingest log for more details.");
                INGEST_LOGGER.warn("{} files(s) failed to be ingested.", failedAmount);
            }
            if (ignoreList != null) {
                String ignoredAmount = Integer.toString(ignoreCount.get());
                printColor(Ansi.Color.YELLOW,
                        ignoredAmount + " file(s) ignored.  See the ingest log for more details.");
                INGEST_LOGGER.warn("{} files(s) were ignored.", ignoredAmount);
            }
        }
        console.println();
        SecurityLogger.audit("Ingested {} files from {}", ingestCount.get(), filePath);
        return null;
    }

    /**
     * Helper method to build ingest log strings
     */
    private String buildIngestLog(ArrayList<Metacard> metacards) {
        StringBuilder strBuilder = new StringBuilder();

        final String newLine = System.getProperty("line.separator");

        for (int i = 0; i < metacards.size(); i++) {
            Metacard card = metacards.get(i);
            strBuilder.append(newLine)
                    .append("Batch #: ")
                    .append(i + 1)
                    .append(" | ");
            if (card != null) {
                if (card.getTitle() != null) {
                    strBuilder.append("Metacard Title: ")
                            .append(card.getTitle())
                            .append(" | ");
                }
                if (card.getId() != null) {
                    strBuilder.append("Metacard ID: ")
                            .append(card.getId())
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
        INGEST_LOGGER.warn("Failed to ingest file [{}]:  \n{}",
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
            if (DEFAULT_TRANSFORMER_ID.matches(transformerId)) {
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
            if (message != null) {
                return transformer.transform(message);
            } else {
                throw new IllegalArgumentException("Data file is null.");
            }

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
            printErrorMessage("Unable to create directory [" + failedIngestDirectory.getAbsolutePath()
                            + "].");
        }
    }

    private boolean processBatch(CatalogFacade catalog, ArrayList<Metacard> metacards)
            throws SourceUnavailableException {
        CreateResponse createResponse = null;

        try {
            createResponse = createMetacards(catalog, metacards);
        } catch (IngestException e) {
            printErrorMessage("Error executing command: " + e.getMessage());
            INGEST_LOGGER.warn("Error ingesting metacard batch {}", buildIngestLog(metacards), e);
        } catch (SourceUnavailableException e) {
            INGEST_LOGGER.warn("Error on process batch, local provider not available. {}"
                    + " metacards failed to ingest. {}",
                    metacards.size(),
                    buildIngestLog(metacards),
                    e);
        } finally {
            processingThreads.decrementAndGet();
        }

        if (createResponse != null) {
            ingestCount.getAndAdd(metacards.size());
        }
        return createResponse != null;
    }

    private void moveToFailedIngestDirectory(File source) {
        File destination = new File(
                failedIngestDirectory.getAbsolutePath() + File.separator + source.getName());

        if (!source.renameTo(destination)) {
            printErrorMessage("Unable to move source file [" + source.getAbsolutePath() + "] to ["
                    + failedIngestDirectory + "].");
        }
    }

    private void buildQueue(Stream<Path> ingestStream, ArrayBlockingQueue<Metacard> metacardQueue,
            long start) {

        if (includeContent) {
            File inputFile = new File(filePath);
            Map<String, Serializable> arguments = new HashMap<>();
            arguments.put(DumpCommand.FILE_PATH, inputFile.getParent() + File.separator);
            arguments.put(FILE_NAME, inputFile.getName());

            ByteSource byteSource = com.google.common.io.Files.asByteSource(inputFile);

            zipDecompression = getZipDecompression();
            if (zipDecompression != null) {

                try (InputStream inputStream = byteSource.openBufferedStream()) {
                    List<Metacard> metacardList = zipDecompression.transform(inputStream,
                            arguments);
                    if (metacardList.size() != 0) {
                        metacardFileMapping = generateFileMap(new File(inputFile.getParent(), METACARD_PATH));
                        fileCount.set(metacardList.size());
                        metacardQueue.addAll(metacardList);
                    }
                } catch (IOException | CatalogTransformerException e) {
                    LOGGER.error("Unable to transform zip file into metacard list.", e);
                    INGEST_LOGGER.error("Unable to transform zip file into metacard list.", e);
                }
            } else {
                LOGGER.error(
                        "No Zip Transformer found.  Unable to transform zip file into metacard list.");
                INGEST_LOGGER.error(
                        "No Zip Transformer found.  Unable to transform zip file into metacard list.");
            }

        } else {

            ingestStream.map(Path::toFile)
                    .filter(file -> !file.isDirectory())
                    .forEach(file -> {

                        if (file.isHidden()) {
                            ignoreCount.incrementAndGet();
                        } else {
                            String extension = "." + FilenameUtils.getExtension(file.getName());

                            if (ignoreList != null && (ignoreList.contains(extension)
                                    || ignoreList.contains(file.getName()))) {
                                ignoreCount.incrementAndGet();
                                printProgressAndFlush(start,
                                        fileCount.get(),
                                        ingestCount.get() + ignoreCount.get());
                            } else {
                                Metacard result;
                                try {
                                    result = readMetacard(file);
                                } catch (IngestException e) {
                                    result = null;
                                    logIngestException(e, file);
                                    if (failedIngestDirectory != null) {
                                        moveToFailedIngestDirectory(file);
                                    }
                                    printErrorMessage(
                                            "Failed to ingest file [" + file.getAbsolutePath()
                                                    + "].");
                                    INGEST_LOGGER.warn("Failed to ingest file [{}].",
                                            file.getAbsolutePath());
                                }

                                if (result != null) {
                                    try {
                                        metacardQueue.put(result);
                                    } catch (InterruptedException e) {
                                        INGEST_LOGGER.error(
                                                "Thread interrupted while waiting to 'put' metacard: {}",
                                                result.getId(),
                                                e);
                                    }
                                }
                            }
                        }
                    });
        }
        doneBuildingQueue.set(true);
    }

    private void submitToStorageProvider(List<Metacard> metacardList) {
        StorageProvider storageProvider = getAllServices(StorageProvider.class).get(0);

        metacardList.stream()
                .filter(metacard -> metacardFileMapping.containsKey(metacard.getId()))
                .map(metacard -> {

                    List<File> fileList = metacardFileMapping.get(metacard.getId());
                    List<ContentItem> contentItemList = new ArrayList<>();
                    ContentItem contentItem;

                    for (File file : fileList) {
                        ByteSource byteSource = com.google.common.io.Files.asByteSource(file);
                        String fileName = file.getName()
                                .split("-")[1];

                        String fragment = null;
                        if (!file.getPath()
                                .contains(CONTENT + File.separator + metacard.getId())) {
                            fragment = StringUtils.substringBetween(file.getPath(),
                                    CONTENT + File.separator,
                                    File.separator + metacard.getId());
                        }
                        contentItem = new ContentItemImpl(metacard.getId(),
                                fragment,
                                byteSource,
                                metacard.getContentTypeName(),
                                fileName,
                                file.length(),
                                metacard);
                        contentItemList.add(contentItem);
                    }

                    return new CreateStorageRequestImpl(contentItemList,
                            metacard.getId(),
                            new HashMap<>());
                })
                .forEach(createStorageRequest -> {
                    try {
                        storageProvider.create(createStorageRequest);
                        storageProvider.commit(createStorageRequest);
                    } catch (StorageException e) {
                        LOGGER.debug("Unable to create content for {}",
                                createStorageRequest.getId(),
                                e);
                        try {
                            storageProvider.rollback(createStorageRequest);
                        } catch (StorageException e1) {
                            LOGGER.debug("Unable to perform rollback on temporary content for {} ",
                                    createStorageRequest.getId(),
                                    e1);
                        }
                    }
                });
    }

    private void submitToCatalog(ScheduledExecutorService batchScheduler,
            ExecutorService executorService, ArrayBlockingQueue<Metacard> metacardQueue,
            CatalogFacade catalog, long start) {

        batchScheduler.scheduleWithFixedDelay(() -> {
            int queueSize = metacardQueue.size();
            if (queueSize > 0) {

                ArrayList<Metacard> metacardBatch = new ArrayList<>(batchSize);

                if (queueSize > batchSize || doneBuildingQueue.get()) {
                    metacardQueue.drainTo(metacardBatch, batchSize);
                    processingThreads.incrementAndGet();
                }

                if (metacardBatch.size() > 0) {
                    executorService.submit(() -> {
                        try {
                            processBatch(catalog, metacardBatch);
                        } catch (SourceUnavailableException e) {
                            INGEST_LOGGER.warn("Error on process batch.", e);
                        }
                    });

                    printProgressAndFlush(start,
                            fileCount.get(),
                            ingestCount.get() + ignoreCount.get());
                }
            }
        }, 100, 100, TimeUnit.MILLISECONDS);
    }

    private Map<String, List<File>> generateFileMap(File inputFile) throws IOException {

        if (!inputFile.exists()) {
            return null;
        }

        Map<String, List<File>> fileMap = new HashMap<>();
        Files.walkFileTree(inputFile.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs)
                    throws IOException {

                File file = filePath.toFile();

                if (file.getParent()
                        .contains(CONTENT) && !file.isDirectory() && !file.isHidden()) {
                    addFileToMap(fileMap, file);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return fileMap;
    }

    private void addFileToMap(Map<String, List<File>> fileMap, File file) {
        String[] fileName = file.getName()
                .split("-");
        if (fileName.length == 2) {
            fileMap.putIfAbsent(fileName[0], new ArrayList<>());
            fileMap.get(fileName[0])
                    .add(file);
        } else if (!file.isHidden()) {
            LOGGER.warn(
                    "Filename {} does not follow expected convention : ID-Filename, and will be skipped.",
                    file.getName());
        }
    }

    private InputCollectionTransformer getZipDecompression() {
        List<InputCollectionTransformer> inputCollectionTransformerList = null;
        try {
            inputCollectionTransformerList = getAllServices(InputCollectionTransformer.class,
                    "(|" + "(" + Constants.SERVICE_ID + "=" + ZIP_DECOMPRESSION + ")" + ")");
        } catch (InvalidSyntaxException e) {
            LOGGER.error("Unable to get transformer id={}", ZIP_DECOMPRESSION, e);
        }

        if (inputCollectionTransformerList != null && inputCollectionTransformerList.size() > 0) {
            return inputCollectionTransformerList.get(0);
        }

        return null;
    }
}