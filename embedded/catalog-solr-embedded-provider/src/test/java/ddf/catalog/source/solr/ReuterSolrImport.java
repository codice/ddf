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

import org.apache.solr.client.solrj.SolrServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.source.IngestException;

public class ReuterSolrImport implements Runnable {

    private SolrServer solrServer;

    private SolrCatalogProvider solrProvider;

    private File[] arrayOfFile;

    private static final Logger LOGGER = LoggerFactory.getLogger(ReuterSolrImport.class);

    private static final String UNABLE_TO_READ_DIR_EXCEPTION_MSG = "unable to read directory";

    public ReuterSolrImport(File[] arrayOfFile) {

        this.arrayOfFile = arrayOfFile;

        try {

            this.solrServer = SolrServerFactory.getEmbeddedSolrServer("solrconfigSoft.xml",
                    "schema.xml",
                    new ConfigurationFileProxy(null, ConfigurationStore.getInstance()));

            this.solrProvider = new SolrCatalogProvider(this.solrServer,
                    new GeotoolsFilterAdapterImpl(), new SolrFilterDelegateFactoryImpl());

        } catch (Exception localException) {
            throw new RuntimeException("unable to connect to solr server: ", localException);
        }
    }

    public static void main2(String[] paramArrayOfString) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1; i++) {
            System.out.println(Integer.toString(i) + " start");
            main2(paramArrayOfString);
            System.out.println(Integer.toString(i) + " done");
        }

        long elapsedTimeMillis = System.currentTimeMillis() - start;

        // Get elapsed time in seconds
        float elapsedTimeSec = elapsedTimeMillis / 1000F;
        System.out.println(Float.toString(elapsedTimeSec) + " seconds");
        System.out.println(Float.toString(elapsedTimeSec / 60F) + " minutes");
    }

    /**
     * @param args
     */
    public static void main(String[] paramArrayOfString) {
        String str = "Usage: java -jar reutersparser.jar <datadir>";

        File localFile = null;
        try {
            localFile = new File(paramArrayOfString[0]);
            if ((!localFile.exists()) || (!localFile.isDirectory())) {
                System.out.println("Second argument needs to be an existing directory!");
                System.out.println(str);
            }
        } catch (Exception localException2) {
            System.out.println("Second argument needs to be an existing directory!");
            System.out.println(str);
        }

        if ((localFile != null) && (localFile.exists()) && (localFile.isDirectory())) {

            File[] allFiles = null;
            try {
                allFiles = readDirectory(localFile);
            } catch (XPathExpressionException e) {
                LOGGER.error(UNABLE_TO_READ_DIR_EXCEPTION_MSG, e);
            } catch (IOException e) {

                LOGGER.error(UNABLE_TO_READ_DIR_EXCEPTION_MSG, e);
            } catch (ParserConfigurationException e) {

                LOGGER.error(UNABLE_TO_READ_DIR_EXCEPTION_MSG, e);
            } catch (SAXException e) {

                LOGGER.error(UNABLE_TO_READ_DIR_EXCEPTION_MSG, e);
            } catch (ParseException e) {

                LOGGER.error(UNABLE_TO_READ_DIR_EXCEPTION_MSG, e);
            }

            // int threadCount = 1;
            // Thread[] threads = new Thread[threadCount];

            //
            // for (int i = 0; i < threadCount; i++) {
            // int from = i * count;
            // int to = from + count;
            // File[] threadFiles = Arrays.copyOfRange(allFiles, from, to);
            // threads[i] = new Thread(new ReuterSolrImport(threadFiles));
            // threads[i].start();
            // }
            //
            // if (allFiles.length % threadCount > 0) {
            // int remainder = allFiles.length % threadCount;
            // int from = allFiles.length - remainder;
            // int to = allFiles.length;
            // File[] threadFiles = Arrays.copyOfRange(allFiles, from, to);
            // threads[threads.length - 1] = new Thread(new ReuterSolrImport(threadFiles));
            // threads[threads.length - 1].start();
            // }
            //

            ReuterSolrImport importer = new ReuterSolrImport(allFiles);

            System.out.println("Starting ingest.");
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
            System.out.println(Float.toString(elapsedTimeSec) + " seconds");
            System.out.println("records/sec = " + 21578F / elapsedTimeSec);

            System.out.println("Done!");
        }

    }

    public static File[] readDirectory(File paramFile) throws XPathExpressionException,
        IOException, ParserConfigurationException, SAXException, ParseException {
        File[] allFiles = paramFile.listFiles(new FileFilter() {
            public boolean accept(File paramFile) {
                return paramFile.getName().contains(".dat");
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
        try {
            FileInputStream fin = new FileInputStream(localFile);
            ObjectInputStream ois = new ObjectInputStream(fin);
            mc = (MetacardImpl) ois.readObject();
            ois.close();
            if (mc.getLocation() != null && mc.getLocation().length() != 0) {
                // solrProvider.create(new CreateRequestImpl(mc));
                return mc;
            } else {
                return null;
                // System.out.println("No location set.");
            }
        } catch (Exception e) {
            LOGGER.error("Unable to read file", e);
        }
        return null;
    }

    public void run() {
        List<Metacard> metacards = new ArrayList<Metacard>();
        for (int i = 0; i < arrayOfFile.length; i++) {
            File localFile = arrayOfFile[i];
            // System.out.println(localFile.getName());
            Metacard mc = readFile(localFile);
            if (mc != null) {
                metacards.add(mc);
            }
        }
        try {

            solrProvider.create(new CreateRequestImpl(metacards));

        } catch (IngestException e) {

            LOGGER.error("Unexpected IngestException", e);
        }
        solrServer.shutdown();

    }

    public void ingest() {
        List<Metacard> metacards = new ArrayList<Metacard>();
        for (int i = 0; i < arrayOfFile.length; i++) {
            File localFile = arrayOfFile[i];
            // System.out.println(localFile.getName());
            Metacard mc = readFile(localFile);
            if (mc != null) {
                metacards.add(mc);
            }
        }
        try {

            solrProvider.create(new CreateRequestImpl(metacards));

        } catch (IngestException e) {

            LOGGER.error("Unexpected IngestException", e);
        }
        solrServer.shutdown();

    }

}
