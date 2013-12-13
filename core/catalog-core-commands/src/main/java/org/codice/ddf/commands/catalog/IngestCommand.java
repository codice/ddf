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
import java.io.FileNotFoundException;
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

import javax.activation.MimeTypeParseException;

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
import ddf.catalog.data.MetacardCreationException;
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

    private static final String DEFAULT_TRANSFORMER_ID = "ser";

    private PrintStream console = System.out;

    @Argument(name = "File path or Directory path", description = "File path to a record or a directory of files to be ingested. Paths are absolute and must be in quotes."
            + " This command can only detect roughly 2 billion records in one folder. Individual operating system limits might also apply.", index = 0, multiValued = false, required = true)
    String filePath = null;

    @Argument(name = "Batch size", description = "Number of Metacards to ingest at a time. Change this argument based on system memory and catalog provider limits.", index = 1, multiValued = false, required = false)
    int batchSize = 1000;

    @Option(name = "Transformer", required = false, aliases = {"-t"}, multiValued = false, description = "The metacard transformer ID to use to transform data files into metacards. The default metacard transformer is the Java serialization transformer."
            + "")
    String transformerId = DEFAULT_TRANSFORMER_ID;

    @Option(name = "Multithreaded", required = false, aliases = {"-m"}, multiValued = false, description = "Flag to set number of threads to use when ingesting. Setting this value too high for your system can cause performance degradation.")
    int multithreaded = 1;

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

        ArrayList<Metacard> metacards = new ArrayList<Metacard>(batchSize);
        if (inputFile.isDirectory()) {
            final long startTime = System.currentTimeMillis();
            final File[] fileList = inputFile.listFiles();

            console.println("Found " + fileList.length + " file(s) to insert.");
            final CountRecorder ingestCountObj = new CountRecorder();
            CreateResponse createResponse;

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
                            } catch (FileNotFoundException e) {
                                console.printf(e.getMessage());
                            } catch (ClassNotFoundException e) {
                                console.printf(e.getMessage());
                            } catch (MimeTypeParseException e) {
                                console.printf(e.getMessage());
                            } catch (MetacardCreationException e) {
                                console.printf(e.getMessage());
                            } catch (InterruptedException e) {
                                console.printf(e.getMessage());
                            } catch (CatalogTransformerException e) {
                                console.printf(e.getMessage());
                            }

                            CreateResponse createResponse = null;
                            try {
                                CreateRequest createRequest = new CreateRequestImpl(result);
                                createResponse = catalog.create(createRequest);
                            } catch (IngestException e) {
                                // don't display the error here, or you'll get an entire screen full
                                // of them
                                // need to shutdown on an ingest error so that we capture ctrl+c in
                                // the console
                                // shutdownNow causes the threadpool to die and then we can exit
                                // semi-gracefully
                                // console.printf(e.getMessage());
                                executorService.shutdownNow();
                            } catch (SourceUnavailableException e) {
                                // console.printf(e.getMessage());
                                executorService.shutdownNow();
                            }
                            ingestCountObj.updateCount(createResponse.getCreatedMetacards().size());
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
                    Metacard result = readMetacard(file);
                    if (result != null) {
                        metacards.add(result);
                    }

                    if (metacards.size() == batchSize) {
                        createResponse = createMetacards(catalog, metacards);
                        ingestCountObj.updateCount(createResponse.getCreatedMetacards().size());
                        metacards.clear();
                        printProgressAndFlush(startTime, fileList, ingestCountObj.getCount());

                    }
                }

                if (metacards.size() > 0) {
                    createResponse = createMetacards(catalog, metacards);
                    ingestCountObj.updateCount(createResponse.getCreatedMetacards().size());
                }
            }

            printProgressAndFlush(startTime, fileList, ingestCountObj.getCount());
            console.println();

            long end = System.currentTimeMillis();

            console.printf(" %d file(s) ingested in %3.3f seconds%n", ingestCountObj.getCount(),
                    (end - startTime) / MILLISECONDS_PER_SECOND);

            return null;
        }

        if (inputFile.isFile()) {
            Metacard result = readMetacard(inputFile);
            if (result != null) {
                metacards.add(result);
            }
            if (metacards.size() > 0) {
                CreateResponse createResponse = createMetacards(catalog, metacards);
                List<Metacard> recordsCreated = createResponse.getCreatedMetacards();
                console.println(recordsCreated.size() + " file(s) created.");
                for (Metacard record : recordsCreated) {
                    console.println("ID: " + record.getId() + " created.");
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

    private Metacard readMetacard(File file) throws FileNotFoundException, ClassNotFoundException,
        MimeTypeParseException, MetacardCreationException, InterruptedException,
        CatalogTransformerException {
        Metacard result = null;

        try {
            if (DEFAULT_TRANSFORMER_ID.matches(transformerId)) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
                result = (Metacard) ois.readObject();
                ois.close();
            } else {
                FileInputStream fis = new FileInputStream(file);
                result = generateMetacard(fis);
                fis.close();
            }
        } catch (IOException e) {

            console.println(e);
        }
        return result;
    }

    private Metacard generateMetacard(InputStream message) throws MetacardCreationException,
        IOException, CatalogTransformerException, InterruptedException {

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
