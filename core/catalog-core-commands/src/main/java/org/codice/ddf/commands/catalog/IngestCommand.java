/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package org.codice.ddf.commands.catalog;

import ddf.catalog.Constants;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.codice.ddf.platform.util.Exceptions;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom Karaf command for ingesting records into the Catalog.
 * 
 */
@Command(scope = CatalogCommands.NAMESPACE, name = "ingest", description = "Ingests Metacards into the Catalog.")
public class IngestCommand extends CatalogCommands {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestCommand.class);

    private static final Logger INGEST_LOGGER = LoggerFactory.getLogger(Constants.INGEST_LOGGER_NAME);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    @Argument(name = "File path or Directory path", description = "File path to a record or a directory of files to be ingested. Paths are absolute and must be in quotes."
            + " This command can only detect roughly 2 billion records in one folder. Individual operating system limits might also apply.", index = 0, multiValued = false, required = true)
    String filePath = null;

    // DDF-535: Remove this argument in ddf-3.0
    @Argument(name = "Batch size", description = "Number of Metacards to ingest at a time. Change this argument based on system memory and catalog provider limits. [DEPRECATED: use --batchsize option instead]", index = 1, multiValued = false, required = false)
    int deprecatedBatchSize = DEFAULT_BATCH_SIZE;

    // DDF-535: remove "Transformer" alias in ddf-3.0
    @Option(name = "--transformer", required = false, aliases = {"-t", "Transformer"}, multiValued = false, description = "The metacard transformer ID to use to transform data files into metacards. The default metacard transformer is the Java serialization transformer.")
    String transformerId = DEFAULT_TRANSFORMER_ID;

    // DDF-535: Remove "Multithreaded" alias in ddf-3.0
    @Option(name = "--multithreaded", required = false, aliases = {"-m", "Multithreaded"}, multiValued = false, description = "Number of threads to use when ingesting. Setting this value too high for your system can cause performance degradation.")
    int multithreaded = 8;

    // DDF-535: remove "-d" and "Ingest Failure Directory" aliases in ddf-3.0
    @Option(name = "--failedDir", required = false, aliases = {"-d", "-f", "Ingest Failure Directory"}, multiValued = false, description = "The directory to put files that failed to ingest.  Using this option will force a batch size of 1.")
    String failedDir = null;

    @Option(name = "--batchsize", required = false, aliases = {"-b"}, multiValued = false, description = "Number of Metacards to ingest at a time. Change this argument based on system memory and catalog provider limits.")
    int batchSize = DEFAULT_BATCH_SIZE;

    private final PeriodFormatter timeFormatter = new PeriodFormatterBuilder()
            .printZeroRarelyLast()
            .appendDays().appendSuffix(" day", " days").appendSeparator(" ")
            .appendHours().appendSuffix(" hour", " hours").appendSeparator(" ")
            .appendMinutes().appendSuffix(" minute", " minutes").appendSeparator(" ")
            .appendSeconds().appendSuffix(" second", " seconds")
            .toFormatter();

    private final AtomicInteger ingestCount = new AtomicInteger();
    private final AtomicInteger fileCount = new AtomicInteger(Integer.MAX_VALUE);

    File failedIngestDirectory = null;

    @Override
    protected Object doExecute() throws Exception {

        final CatalogFacade catalog = getCatalog();
        final File inputFile = new File(filePath);

        if (!inputFile.exists()) {
            printErrorMessage("File or directory [" + filePath + "] must exist.");
            console.println("If the file does indeed exist, try putting the path in quotes.");
            return null;
        }

        if (deprecatedBatchSize != DEFAULT_BATCH_SIZE) {
            // user specified the old style batch size, so use that
            printErrorMessage("Batch size positional argument is DEPRECATED, please use --batchsize option instead.");
            batchSize = deprecatedBatchSize;
        }

        if (batchSize <= 0) {
            printErrorMessage("A batch size of [" + batchSize + "] was supplied. Batch size must be greater than 0.");
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
                console.println("WARNING: An ingest failure directory was supplied in addition to a batch size of "
                        + batchSize
                        + ". When using an ingest failure directory, the batch size must be 1. Setting batch size to 1.");
            }

            batchSize = 1;
        }

        BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<Runnable>(
                multithreaded);
        RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor
                .CallerRunsPolicy();
        ExecutorService executorService = new ThreadPoolExecutor(multithreaded,
                multithreaded, 0L, TimeUnit.MILLISECONDS, blockingQueue,
                rejectedExecutionHandler);

        if (inputFile.isDirectory()) {
            final long start = System.currentTimeMillis();

            executorService.submit(new Runnable() {
                @Override public void run() {
                    fileCount.getAndSet(getFileCount(inputFile));
                }
            });

            printProgressAndFlush(start, fileCount.get(), ingestCount.get());

            IngestVisitor visitor = new IngestVisitor(start, catalog, executorService);
            try {
                Files.walkFileTree(Paths.get(filePath), visitor);
                visitor.done();
            } finally {
                executorService.shutdown();
            }

            while (!executorService.isTerminated()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    // ignore
                }
            }

            printProgressAndFlush(start, fileCount.get(), ingestCount.get());

            long end = System.currentTimeMillis();

            console.println();
            String elapsedTime = timeFormatter.print(new Period(start, end).withMillis(0));
            console.printf(" %d file(s) ingested in %s", ingestCount.get(), elapsedTime);
            LOGGER.info("{} file(s) ingested in {} [{} records/sec]", ingestCount.get(),
                    elapsedTime, calculateRecordsPerSecond(ingestCount.get(), start, end));
            INGEST_LOGGER.info("{} file(s) ingested in {} [{} records/sec]", ingestCount.get(),
                    elapsedTime, calculateRecordsPerSecond(ingestCount.get(), start, end));
            if (INGEST_LOGGER.isWarnEnabled() && fileCount.get() != ingestCount.get()) {
                console.println();
                String failedAmount = Integer.toString(fileCount.get() - ingestCount.get());
                printErrorMessage(failedAmount
                        + " file(s) failed to be ingested.  See the ingest log for more details.");
                INGEST_LOGGER.warn("{} files(s) failed to be ingested.", failedAmount);
            }

            return null;
        } else if (inputFile.isFile()) {
            executorService.shutdown();

            ArrayList<Metacard> metacards = new ArrayList<Metacard>(batchSize);
            Metacard result = null;
            try {
                result = readMetacard(inputFile);
            } catch (IngestException e) {
                result = null;
                printErrorMessage("Failed to ingest file [" + inputFile.getAbsolutePath()
                        + "].  See the ingest log for more details.");
                logIngestException(e, inputFile);
                if (failedIngestDirectory != null) {
                    moveToFailedIngestDirectory(inputFile);
                }
            }
            
            if (result != null) {
                metacards.add(result);
            }
            
            if (metacards.size() > 0) {
                CreateResponse createResponse = null;
                try {
                    createResponse = createMetacards(catalog, metacards);
                } catch (IngestException e) {
                    createResponse = null;
                    printErrorMessage(inputFile.getAbsolutePath() + " failed ingest.");
                    if (INGEST_LOGGER.isWarnEnabled()) {
                        INGEST_LOGGER.warn("{} failed ingest", inputFile.getAbsolutePath(), e);
                    }
                    if (failedIngestDirectory != null) {
                        moveToFailedIngestDirectory(inputFile);
                    }
                } catch (SourceUnavailableException e) {
                    createResponse = null;
                    printErrorMessage(inputFile.getAbsolutePath() + " failed ingest.");
                    if (INGEST_LOGGER.isWarnEnabled()) {
                        INGEST_LOGGER.warn("{} failed ingest", inputFile.getAbsolutePath(), e);
                    }
                    if (failedIngestDirectory != null) {
                        moveToFailedIngestDirectory(inputFile);
                    }
                }

                if (createResponse != null) {
                    List<Metacard> recordsCreated = createResponse.getCreatedMetacards();
                    console.println(recordsCreated.size() + " file(s) created.");
                    for (Metacard record : recordsCreated) {
                        console.println("ID: " + record.getId() + " created.");
                    }
                    if (INGEST_LOGGER.isDebugEnabled()) {
                        INGEST_LOGGER.debug("{} file(s) created. {}", metacards.size(),
                                buildIngestLog(metacards));
                    }
                }
            }
            return null;
        }

        executorService.shutdown();
        console.println("Could not execute command.");
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
            strBuilder.append(newLine).append("Batch #: ").append(i + 1).append(" | ");
            if (card != null) {
                if (card.getTitle() != null) {
                    strBuilder.append("Metacard Title: ").append(card.getTitle()).append(" | ");
                }
                if (card.getId() != null) {
                    strBuilder.append("Metacard ID: ").append(card.getId()).append(" | ");
                }
            } else {
                strBuilder.append("Null Metacard");
            }
        }
        return strBuilder.toString();
    }

    private int getFileCount(File dir) {
        int count = 0;

        File[] files = dir.listFiles();
        if (null != files) {
            for (File file : files) {
                if (file.isDirectory()) {
                    count += getFileCount(file);
                } else {
                    count++;
                }
            }
        }

        return count;
    }

    private void logIngestException(IngestException exception, File inputFile) {
        LOGGER.debug("Failed to ingest file [{}].", inputFile.getAbsolutePath(), exception);
        if (INGEST_LOGGER.isWarnEnabled()) {
            INGEST_LOGGER.warn("\nFailed to ingest file [{}]:  \n" + Exceptions.getFullMessage(exception), inputFile.getAbsolutePath());
        }
    }

    private CreateResponse createMetacards(CatalogFacade catalog, List<Metacard> listOfMetaCards)
        throws IngestException, SourceUnavailableException {
        CreateRequest createRequest = new CreateRequestImpl(listOfMetaCards);
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
        } catch (IOException e) {
            throw new IngestException(e);
        } catch (ClassNotFoundException e) {
            throw new IngestException(e);
        } catch (IllegalArgumentException e) {
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

    private Metacard generateMetacard(InputStream message) throws IOException  {

        BundleContext bundleContext = getBundleContext();

        ServiceReference[] refs = null;

        try {
            refs = bundleContext.getServiceReferences(InputTransformer.class.getName(), "(|" + "("
                    + Constants.SERVICE_ID + "=" + transformerId + ")" + ")");
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid transformer transformerId: "
                    + transformerId, e);
        }
        if (refs == null || refs.length == 0) {
            throw new IllegalArgumentException("Transformer " + transformerId + " not found");
        } else {
            try {
                InputTransformer transformer = (InputTransformer) bundleContext.getService(refs[0]);
                if (message != null) {
                    return transformer.transform(message);
                } else {
                    throw new IllegalArgumentException("data file is null.");
                }

            } catch (CatalogTransformerException e) {
                throw new IllegalArgumentException("Transformation Failed for transformer: "
                        + transformerId, e);
            }
        }

    }

    private boolean verifyFailedIngestDirectory() {
        if (!failedIngestDirectory.exists()) {
            makeFailedIngestDirectory();
        }

        if (!failedIngestDirectory.canWrite()) {
            printErrorMessage("Directory [" + failedIngestDirectory.getAbsolutePath() + "] is not writable.");
            return false;
        } else {
            return true;
        }
    }

    private void makeFailedIngestDirectory() {
        if (!failedIngestDirectory.mkdirs()) {
            printErrorMessage("Unable to create directory [" + failedIngestDirectory.getAbsolutePath() + "].");
        }
    }

    private boolean processBatch(CatalogFacade catalog, ArrayList<Metacard> metacards) throws
            SourceUnavailableException {
        CreateResponse createResponse = null;
        boolean success = false;

        try {
            createResponse = createMetacards(catalog, metacards);
            success = true;
        } catch (IngestException e) {
            createResponse = null;
            success = false;
            printErrorMessage("Error executing command: " + e.getMessage());
            if (INGEST_LOGGER.isWarnEnabled()) {
                INGEST_LOGGER.warn("Error ingesting metacard batch {}",
                        buildIngestLog(metacards), e);
            }
        } catch (SourceUnavailableException e) {
            if (INGEST_LOGGER.isWarnEnabled()) {
                INGEST_LOGGER.warn("Error on process batch, local provider not available. {}"
                                + " metacards failed to ingest. {}",
                        metacards.size(),
                        buildIngestLog(metacards), e);
            }
        }

        if (success) {
            ingestCount.getAndAdd(createResponse.getCreatedMetacards().size());
        }

        metacards.clear();
        return success;
    }

    private boolean processBatch(CatalogFacade catalog, ArrayList<Metacard> metacards,
            File inputFile) throws SourceUnavailableException {
        boolean success = false;

        if (!processBatch(catalog, metacards)) {
            if (failedIngestDirectory != null) {
                printErrorMessage("Failed to ingest file [" + inputFile.getAbsolutePath() + "].");
                if (INGEST_LOGGER.isWarnEnabled()) {
                    INGEST_LOGGER.warn("Failed to ingest file [{}].", inputFile.getAbsolutePath());
                }
                moveToFailedIngestDirectory(inputFile);
            }
            success = false;
        } else {
            success = true;
        }

        return success;
    }

    private void moveToFailedIngestDirectory(File source) {
        File destination = new File(failedIngestDirectory.getAbsolutePath() + File.separator + source.getName());

        if (!source.renameTo(destination)) {
            printErrorMessage("Unable to move source file [" + source.getAbsolutePath()
                    + "] to [" + failedIngestDirectory + "].");
        }
    }

    private class IngestVisitor extends SimpleFileVisitor<Path> {

        private long start;

        private CatalogFacade catalog;

        private ExecutorService executorService;

        private ArrayList<Metacard> metacards = new ArrayList<Metacard>(batchSize);

        public IngestVisitor(long start, CatalogFacade catalog, ExecutorService executorService) {
            this.start = start;
            this.catalog = catalog;
            this.executorService = executorService;
        }

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                throws IOException {

            final File file = path.toFile();

            if (multithreaded > 1) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        Metacard result;

                        try {
                            result = readMetacard(file);
                        } catch (IngestException e) {
                            result = null;
                            logIngestException(e, file);
                            if (failedIngestDirectory == null) {
                                executorService.shutdownNow();
                            } else {
                                moveToFailedIngestDirectory(file);
                            }
                        }

                        CreateResponse createResponse = null;

                        if (result != null) {
                            try {
                                CreateRequest createRequest = new CreateRequestImpl(result);
                                createResponse = catalog.create(createRequest);
                            } catch (IngestException e) {
                                createResponse = null;
                                if (failedIngestDirectory == null) {
                                    executorService.shutdownNow();
                                } else {
                                    logIngestException(e, file);
                                    moveToFailedIngestDirectory(file);
                                }
                            } catch (SourceUnavailableException e) {
                                // Catalog framework logs these exceptions to the ingest logger so we don't have to.
                                executorService.shutdownNow();
                            }
                        }

                        if (createResponse != null) {
                            ingestCount.getAndAdd(
                                    createResponse.getCreatedMetacards().size());
                        }

                        printProgressAndFlush(start, fileCount.get(), ingestCount.get());
                    }
                });
            } else {
                Metacard result;
                try {
                    result = readMetacard(file);
                } catch (IngestException e) {
                    result = null;
                    logIngestException(e, file);
                    if (failedIngestDirectory != null) {
                        moveToFailedIngestDirectory(file);
                    } else {
                        return FileVisitResult.TERMINATE;
                    }
                }

                if (result != null) {
                    metacards.add(result);
                }

                try {
                    if (batchSize == 1 && metacards.size() == batchSize) {
                        if (!processBatch(catalog, metacards, file)
                                && failedIngestDirectory == null) {
                            return FileVisitResult.TERMINATE;
                        }
                    } else if (batchSize > 1 && metacards.size() == batchSize) {
                        if (!processBatch(catalog, metacards)) {
                            return FileVisitResult.TERMINATE;
                        }
                    }
                } catch (SourceUnavailableException e) {
                    // Catalog framework logs these exceptions to the ingest logger so we don't have to.
                    return FileVisitResult.TERMINATE;
                }

                printProgressAndFlush(start, fileCount.get(), ingestCount.get());
            }

            return FileVisitResult.CONTINUE;
        }

        public void done() throws SourceUnavailableException {
            if (metacards.size() > 0) {
                processBatch(catalog, metacards);
            }
        }
    }

}
