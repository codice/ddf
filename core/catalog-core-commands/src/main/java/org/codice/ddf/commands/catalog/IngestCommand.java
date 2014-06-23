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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.fusesource.jansi.Ansi;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import ddf.catalog.Constants;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;

/**
 * Custom Karaf command for ingesting records into the Catalog.
 * 
 */
@Command(scope = CatalogCommands.NAMESPACE, name = "ingest", description = "Ingests Metacards into the Catalog.")
public class IngestCommand extends CatalogCommands {

    private static final double PERCENTAGE_MULTIPLIER = 100.0;

    private static final int PROGESS_BAR_NOTCH_LENGTH = 50;
    
    private static final int DEFAULT_BATCH_SIZE = 1000;

    private static final String DEFAULT_TRANSFORMER_ID = "ser";

    File failedIngestDirectory = null;

    @Argument(name = "File path or Directory path", description = "File path to a record or a directory of files to be ingested. Paths are absolute and must be in quotes."
            + " This command can only detect roughly 2 billion records in one folder. Individual operating system limits might also apply.", index = 0, multiValued = false, required = true)
    String filePath = null;

    @Argument(name = "Batch size", description = "Number of Metacards to ingest at a time. Change this argument based on system memory and catalog provider limits.", index = 1, multiValued = false, required = false)
    int batchSize = DEFAULT_BATCH_SIZE;

    @Option(name = "Transformer", required = false, aliases = {"-t"}, multiValued = false, description = "The metacard transformer ID to use to transform data files into metacards. The default metacard transformer is the Java serialization transformer."
            + "")
    String transformerId = DEFAULT_TRANSFORMER_ID;

    @Option(name = "Multithreaded", required = false, aliases = {"-m"}, multiValued = false, description = "Flag to set number of threads to use when ingesting. Setting this value too high for your system can cause performance degradation.")
    int multithreaded = 1;
    
    @Option(name = "Ingest Failure Directory", required = false, aliases = {"-d"}, multiValued = false, description = "The directory to put files that fail ingest.  Using this option will force a batch size of 1.")
    String directory = null;

    @Override
    protected Object doExecute() throws Exception {

        final CatalogFacade catalog = getCatalog();
        File inputFile = new File(filePath);

        if (!inputFile.exists()) {
            console.println(Ansi.ansi().fg(Ansi.Color.RED).toString() + "File or directory ["
                    + filePath + "] must exist." + Ansi.ansi().reset().toString());
            console.println("If the file does indeed exist, try putting the path in quotes.");
            return null;
        }
        
        if (batchSize <= 0) {
            console.println(Ansi.ansi().fg(Ansi.Color.RED).toString() + "A batch size of ["
                    + batchSize + "] was supplied. Batch size must be greater than 0."
                    + Ansi.ansi().reset().toString());
            return null;
        }
        
        if(!StringUtils.isEmpty(directory)) {
            failedIngestDirectory = new File(directory);
            if(!verifyFailedIngestDirectory()) {
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

        ArrayList<Metacard> metacards = new ArrayList<Metacard>(batchSize);
        final CountRecorder ingestCountObj = new CountRecorder();
        
        if (inputFile.isDirectory()) {
            final long startTime = System.currentTimeMillis();
            final File[] fileList = inputFile.listFiles();

            console.println("Found " + fileList.length + " file(s) to insert.");
            
            printProgressAndFlush(startTime, fileList, ingestCountObj.getCount());
            
            if (multithreaded > 1 && fileList.length > batchSize) {
                BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<Runnable>(
                        multithreaded);
                RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
                final ExecutorService executorService = new ThreadPoolExecutor(multithreaded,
                        multithreaded, 0L, TimeUnit.MILLISECONDS, blockingQueue,
                        rejectedExecutionHandler);

                for (final File file : fileList) {
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            Metacard result = null;
                            try {
                                result = readMetacard(file);
                            } catch (IngestException e) {
                                result = null;
                                logIngestException(e, file);
                                if(failedIngestDirectory == null) {
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
                                    // don't display the error here, or you'll get an entire screen
                                    // full of them need to shutdown on an ingest error so that we capture 
                                    // ctrl+c in the console shutdownNow causes the threadpool to die and 
                                    // then we can exit  semi-gracefully
                                    // console.printf(e.getMessage());
                                    createResponse = null;
                                    if (failedIngestDirectory == null) {
                                        executorService.shutdownNow();
                                    } else {
                                        logIngestException(e, file);
                                        moveToFailedIngestDirectory(file);
                                    }
                                } catch (SourceUnavailableException e) {
                                    executorService.shutdownNow();
                                }
                            }
                            
                            if(createResponse != null) {
                                ingestCountObj.updateCount(createResponse.getCreatedMetacards().size());
                            }
                            
                            printProgressAndFlush(startTime, fileList, ingestCountObj.getCount());

                        }
                    });
                }

                executorService.shutdown();

                while (!executorService.isTerminated()) {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }

            } else {
                for (File file : fileList) {
                    Metacard result = null;
                    try {
                        result = readMetacard(file);
                    } catch (IngestException e) {
                        result = null;
                        logIngestException(e, file);
                        if (failedIngestDirectory != null) {
                            moveToFailedIngestDirectory(file);
                        } else {
                            return null;
                        }
                    }

                    if (result != null) {
                        metacards.add(result);
                    }
                    
                    if (batchSize == 1 && metacards.size() == batchSize) {
                        if(!processBatch(catalog, metacards, file, ingestCountObj) && failedIngestDirectory == null) {
                            return null;
                        }
                    } else if(batchSize > 1 && metacards.size() == batchSize) {
                        if(!processBatch(catalog, metacards, ingestCountObj)) {
                            return null;
                        }
                    }
                    
                    printProgressAndFlush(startTime, fileList, ingestCountObj.getCount());
                }

                if (metacards.size() > 0) {
                    processBatch(catalog, metacards, ingestCountObj);
                }
            }

            printProgressAndFlush(startTime, fileList, ingestCountObj.getCount());

            long end = System.currentTimeMillis();

            console.println();
            console.printf(" %d file(s) ingested in %3.3f seconds%n", ingestCountObj.getCount(),
                    (end - startTime) / MILLISECONDS_PER_SECOND);

            return null;
        }

        if (inputFile.isFile()) {
            Metacard result = null;
            try {
                result = readMetacard(inputFile);
            } catch(IngestException e) {
                result = null;
                logIngestException(e, inputFile);
                if(failedIngestDirectory != null) {
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
                    console.println(Ansi.ansi().fg(Ansi.Color.RED).toString()
                            + inputFile.getAbsolutePath() + " failed ingest."
                            + Ansi.ansi().reset().toString());
                    if(failedIngestDirectory != null) {
                        moveToFailedIngestDirectory(inputFile);
                    }
                }

                if (createResponse != null) {
                    List<Metacard> recordsCreated = createResponse.getCreatedMetacards();
                    console.println(recordsCreated.size() + " file(s) created.");
                    for (Metacard record : recordsCreated) {
                        console.println("ID: " + record.getId() + " created.");
                    }
                }
            }
            return null;
        } 

        console.println("Could not execute command.");
        return null;
    }

    void printProgressAndFlush(long start, File[] fileList, int ingestCount) {
        console.print(getProgressBar(ingestCount, fileList.length, start,
                System.currentTimeMillis()));
        console.flush();
    }
    
    private void logIngestException(IngestException exception, File inputFile) {
        console.println(Ansi.ansi().fg(Ansi.Color.RED).toString()
                + "Failed to ingest file [" + inputFile.getAbsolutePath() + "]."
                + Ansi.ansi().reset().toString());
        console.println(Ansi.ansi().fg(Ansi.Color.RED).toString() + exception.getMessage()
                + Ansi.ansi().reset().toString());
    }

    private String getProgressBar(int ingestCount, int totalPossible, long start, long end) {

        int notches = calculateNotches(ingestCount, totalPossible);

        int progressPercentage = calculateProgressPercentage(ingestCount, totalPossible);

        int rate = calculateRecordsPerSecond(ingestCount, start, end);

        String progressArrow = ">";

        // /r is required, it allows for the update in place
        String progressBarFormat = "%1$4s%% [=%2$-50s] %3$5s records/sec\t\r";

        return String.format(progressBarFormat, progressPercentage,
                StringUtils.repeat("=", notches) + progressArrow, rate);
    }

    private int calculateRecordsPerSecond(int ingestCount, long start, long end) {

        return (int) (new Double(ingestCount) / (new Double(end - start) / MILLISECONDS_PER_SECOND));
    }

    private int calculateProgressPercentage(int ingestCount, int totalPossible) {
        return (int) ((new Double(ingestCount) / new Double(totalPossible)) * PERCENTAGE_MULTIPLIER);
    }

    private int calculateNotches(int ingestCount, int totalPossible) {

        return (int) ((new Double(ingestCount) / new Double(totalPossible)) * PROGESS_BAR_NOTCH_LENGTH);
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
       
        if(!failedIngestDirectory.exists()) {
            makeFailedIngestDirectory();
        }
        
       if(!failedIngestDirectory.canWrite()) {
           console.println(Ansi.ansi().fg(Ansi.Color.RED).toString() + "Directory ["
                   + failedIngestDirectory.getAbsolutePath() + "] is not writable." + Ansi.ansi().reset().toString());
           return false;
       } else {
           return true;
       }
    }
    
    private void makeFailedIngestDirectory() {
        if(!failedIngestDirectory.mkdirs()) {
            console.println(Ansi.ansi().fg(Ansi.Color.RED).toString() + "Unable to create directory ["
                    + failedIngestDirectory.getAbsolutePath() + "]." + Ansi.ansi().reset().toString());
        }
    }
    
    private boolean processBatch(CatalogFacade catalog, ArrayList<Metacard> metacards,
            CountRecorder ingestCountObj) throws SourceUnavailableException {
        CreateResponse createResponse = null;
        boolean success = false;

        try {
            createResponse = createMetacards(catalog, metacards);
            success = true;
        } catch (IngestException e) {
            createResponse = null;
            success = false;
            console.println(Ansi.ansi().fg(Ansi.Color.RED).toString() + "Error executing command: "
                    + e.getMessage() + Ansi.ansi().reset().toString());
        }
        
        if (success) {
            ingestCountObj.updateCount(createResponse.getCreatedMetacards().size());
        }
        
        metacards.clear();
        return success;
    }
    
    private boolean processBatch(CatalogFacade catalog, ArrayList<Metacard> metacards,
            File inputFile, CountRecorder ingestCountObj) throws SourceUnavailableException {
        boolean success = false;

        if (!processBatch(catalog, metacards, ingestCountObj)) {
            if (failedIngestDirectory != null) {
                console.println(Ansi.ansi().fg(Ansi.Color.RED).toString()
                        + "Failed to ingest file [" + inputFile.getAbsolutePath() + "]."
                        + Ansi.ansi().reset().toString());
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
            console.println(Ansi.ansi().fg(Ansi.Color.RED).toString()
                    + "Unable to move source file [" + source.getAbsolutePath()
                    + "] to [" + failedIngestDirectory + "]." + Ansi.ansi().reset().toString());
        }
    }

    private static class CountRecorder {
        private int count = 0;

        public synchronized void updateCount(int i) {
            count += i;
        }

        public int getCount() {
            return count;
        }
    }

}
