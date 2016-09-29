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
package ddf.catalog.source.solr;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.codice.solr.factory.impl.ConfigurationFileProxy;
import org.codice.solr.factory.impl.ConfigurationStore;
import org.codice.solr.factory.impl.EmbeddedSolrFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.source.IngestException;

public class ReuterSolrImport implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReuterSolrImport.class);

    private static final String UNABLE_TO_READ_DIR_EXCEPTION_MSG = "unable to read directory";

    private EmbeddedSolrServer solr;

    private SolrCatalogProvider solrProvider;

    private File[] arrayOfFile;

    public ReuterSolrImport(File[] arrayOfFile) {

        this.arrayOfFile = arrayOfFile;

        try {

            this.solr = EmbeddedSolrFactory.getEmbeddedSolrServer("catalog", "solrconfigSoft.xml",
                    "schema.xml",
                    new ConfigurationFileProxy(ConfigurationStore.getInstance()));

            this.solrProvider = new SolrCatalogProvider(this.solr,
                    new GeotoolsFilterAdapterImpl(),
                    new SolrFilterDelegateFactoryImpl());

        } catch (Exception localException) {
            throw new RuntimeException("unable to connect with solr client: ", localException);
        }
    }

    public static void main2(String[] paramArrayOfString) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1; i++) {
            LOGGER.info(Integer.toString(i) + " start");
            main2(paramArrayOfString);
            LOGGER.info(Integer.toString(i) + " done");
        }

        long elapsedTimeMillis = System.currentTimeMillis() - start;

        // Get elapsed time in seconds
        float elapsedTimeSec = elapsedTimeMillis / 1000F;
        LOGGER.info(Float.toString(elapsedTimeSec) + " seconds");
        LOGGER.info(Float.toString(elapsedTimeSec / 60F) + " minutes");
    }

    /**
     * @param paramArrayOfString
     */
    public static void main(String[] paramArrayOfString) throws IOException {
        String str = "Usage: java -jar reutersparser.jar <datadir>";

        File localFile = null;
        try {
            localFile = new File(paramArrayOfString[0]);
            if ((!localFile.exists()) || (!localFile.isDirectory())) {
                LOGGER.info("Second argument needs to be an existing directory: {}", str);
            }
        } catch (Exception localException2) {
            LOGGER.info("Second argument needs to be an existing directory: {}", str);
        }

        if ((localFile != null) && (localFile.exists()) && (localFile.isDirectory())) {

            File[] allFiles = null;
            try {
                allFiles = readDirectory(localFile);
            } catch (XPathExpressionException | IOException | ParserConfigurationException | SAXException | ParseException e) {
                LOGGER.info(UNABLE_TO_READ_DIR_EXCEPTION_MSG, e);
            }

            // int threadCount = 1;
            // Thread[] threads = new Thread[threadCount];

            //
            // for (int i = 0; i < threadCount; i++) {
            // int from = i * count;
            // int to = from + count;
            // File[] threadFiles = Arrays.copyOfRange(allFiles, from, to);
            // threads[i] = new Thread(new ddf.catalog.source.solr.ReuterSolrImport(threadFiles));
            // threads[i].start();
            // }
            //
            // if (allFiles.length % threadCount > 0) {
            // int remainder = allFiles.length % threadCount;
            // int from = allFiles.length - remainder;
            // int to = allFiles.length;
            // File[] threadFiles = Arrays.copyOfRange(allFiles, from, to);
            // threads[threads.length - 1] = new Thread(new ddf.catalog.source.solr.ReuterSolrImport(threadFiles));
            // threads[threads.length - 1].start();
            // }
            //

            ReuterSolrImport importer = new ReuterSolrImport(allFiles);

            LOGGER.info("Starting ingest.");
            long start = System.currentTimeMillis();
            importer.ingest();

            // for (int i = 0; i < threads.length; i++) {
            // try {
            // threads[i].join();
            // } catch (InterruptedException e) {
            //
            // }
            // }

            long elapsedTimeMillis = System.currentTimeMillis() - start;

            // Get elapsed time in seconds
            float elapsedTimeSec = elapsedTimeMillis / 1000F;
            LOGGER.info(Float.toString(elapsedTimeSec) + " seconds");
            LOGGER.info("records/sec = {}", 21578F / elapsedTimeSec);

            LOGGER.info("Done!");
        }

    }

    public static File[] readDirectory(File paramFile)
            throws XPathExpressionException, IOException, ParserConfigurationException,
            SAXException, ParseException {
        File[] allFiles = paramFile.listFiles(new FileFilter() {
            public boolean accept(File paramFile) {
                return paramFile.getName()
                        .contains(".dat");
            }
        });
        if (allFiles.length == 0) {
            throw new RuntimeException("Directory doesn't contain sgml files!");
        }
        Arrays.sort(allFiles);
        return allFiles;
    }

    private Metacard readFile(File localFile) {
        MetacardImpl mc = null;
        try (FileInputStream fin = new FileInputStream(localFile);
                ObjectInputStream ois = new ObjectInputStream(fin)) {
            mc = (MetacardImpl) ois.readObject();
            if (mc.getLocation() != null && mc.getLocation()
                    .length() != 0) {
                // solrProvider.create(new CreateRequestImpl(mc));
                return mc;
            } else {
                return null;
            }
        } catch (Exception e) {
            LOGGER.info("Unable to read file", e);
        }
        return null;
    }

    public void run() {
        List<Metacard> metacards = new ArrayList<Metacard>();
        for (File localFile : arrayOfFile) {
            Metacard mc = readFile(localFile);
            if (mc != null) {
                metacards.add(mc);
            }
        }
        try {

            solrProvider.create(new CreateRequestImpl(metacards));

        } catch (IngestException e) {

            LOGGER.info("Unexpected IngestException", e);
        }
        try {
            solr.close();
        } catch (IOException e) {
            LOGGER.info("Unable to close Solr client.", e);
        }

    }

    public void ingest() throws IOException {
        List<Metacard> metacards = new ArrayList<Metacard>();
        for (File localFile : arrayOfFile) {
            Metacard mc = readFile(localFile);
            if (mc != null) {
                metacards.add(mc);
            }
        }
        try {

            solrProvider.create(new CreateRequestImpl(metacards));

        } catch (IngestException e) {

            LOGGER.info("Unexpected IngestException", e);
        }
        solr.close();

    }

}
