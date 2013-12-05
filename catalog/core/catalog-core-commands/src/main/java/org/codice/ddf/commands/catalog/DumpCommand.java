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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.fusesource.jansi.Ansi;
import org.opengis.filter.Filter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import ddf.catalog.Constants;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;

@Command(scope = CatalogCommands.NAMESPACE, name = "dump", description = "Exports Metacards from the current Catalog. Does not remove them.")
public class DumpCommand extends OsgiCommandSupport {

    private static final double MILLISECONDS_PER_SECOND = 1000.0;

    private static final String DEFAULT_TRANSFORMER_ID = "ser";

    private PrintStream console = System.out;

    private static List<MetacardTransformer> transformers = null;

    @Argument(name = "Dump directory path", description = "Directory to export Metacards into. Paths are absolute and must be in quotes.  Files in directory will be overwritten if they already exist.", index = 0, multiValued = false, required = true)
    String dirPath = null;

    @Argument(name = "Batch size", description = "Number of Metacards to retrieve and export at a time until completion. Change this argument based on system memory and CatalogProvider limits.", index = 1, multiValued = false, required = false)
    int pageSize = 1000;

    @Option(name = "Transformer", required = false, aliases = {"-t"}, multiValued = false, description = "The metacard transformer ID to use to transform metacards into data files. The default metacard transformer is the Java serialization transformer.")
    String transformerId = DEFAULT_TRANSFORMER_ID;

    @Option(name = "Extension", required = false, aliases = {"-e"}, multiValued = false, description = "The file extension of the data files.")
    String fileExtension = null;

    @Override
    protected Object doExecute() throws Exception {
        File dumpDir = new File(dirPath);

        if (!dumpDir.exists()) {
            printRed("Directory [" + dirPath + "] must exist.");
            console.println("If the directory does indeed exist, try putting the path in quotes.");
            return null;
        }

        if (!dumpDir.isDirectory()) {
            printRed("Path [" + dirPath + "] must be a directory.");
            return null;
        }

        if (!DEFAULT_TRANSFORMER_ID.matches(transformerId)) {
            transformers = getTransformers();
            if (transformers == null) {
                console.println(transformerId + " is an invalid metacard transformer.");
                return null;
            }
        }

        CatalogProvider catalog = getService(CatalogProvider.class);
        FilterBuilder builder = getService(FilterBuilder.class);

        Filter filter = builder.attribute(Metacard.ID).is().like().text("*");

        QueryImpl query = new QueryImpl(filter);
        query.setRequestsTotalResultsCount(false);
        query.setPageSize(pageSize);

        long resultCount = 0;
        long start = System.currentTimeMillis();

        SourceResponse response = catalog.query(new QueryRequestImpl(query));

        while (response.getResults().size() > 0) {
            response = catalog.query(new QueryRequestImpl(query));

            for (Result result : response.getResults()) {
                Metacard metacard = result.getMetacard();
                exportMetacard(dumpDir, metacard);
                resultCount++;
                if (resultCount % pageSize == 0) {
                    console.print(".");
                }
            }

            if (response.getResults().size() < pageSize || pageSize == -1) {
                break;
            }

            if (pageSize > 0) {
                query.setStartIndex(query.getStartIndex() + pageSize);
            }
        }

        if (resultCount > pageSize) {
            console.println();
        }

        long end = System.currentTimeMillis();
        console.printf(" %d file(s) dumped in %3.3f seconds%n", resultCount, (end - start)
                / MILLISECONDS_PER_SECOND);

        return null;
    }

    private void exportMetacard(File dumpLocation, Metacard metacard) throws IOException,
        CatalogTransformerException {

        String extension = "";
        if (fileExtension != null) {
            extension = "." + fileExtension;
        }

        if (DEFAULT_TRANSFORMER_ID.matches(transformerId)) {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(
                    dumpLocation, metacard.getId() + extension)));
            try {
                oos.writeObject(new MetacardImpl(metacard));

            } finally {
                oos.flush();
                oos.close();
            }
        } else {

            FileOutputStream fos = new FileOutputStream(new File(dumpLocation, metacard.getId()
                    + extension));
            BinaryContent binaryContent;
            try {
                if (metacard != null) {
                    for (MetacardTransformer transformer : transformers) {
                        binaryContent = transformer.transform(metacard, null);
                        if (binaryContent != null) {
                            fos.write(binaryContent.getByteArray());
                            break;
                        }
                    }
                }
            } finally {
                fos.close();
            }
        }
    }

    private void printRed(String message) {
        console.println(Ansi.ansi().fg(Ansi.Color.RED).toString() + message
                + Ansi.ansi().reset().toString());
    }

    private List<MetacardTransformer> getTransformers() {

        BundleContext bundleContext = getBundleContext();
        ServiceReference[] refs = null;
        try {
            refs = bundleContext.getAllServiceReferences(MetacardTransformer.class.getName(), "(|"
                    + "(" + Constants.SERVICE_ID + "=" + transformerId + ")" + ")");

        } catch (InvalidSyntaxException e) {
            console.printf("Fail to get MetacardTransformer references. ", e);
        }
        if (refs == null || refs.length == 0) {
            return null;
        }

        List<MetacardTransformer> metacardTransformerList = new ArrayList<MetacardTransformer>();
        for (int i = 0; i < refs.length; i++) {

            metacardTransformerList.add((MetacardTransformer) bundleContext.getService(refs[i]));
        }

        return metacardTransformerList;
    }

    protected <T> T getService(Class<T> clazz) throws InterruptedException {
        ServiceTracker st = new ServiceTracker(getBundleContext(), clazz.getName(), null);
        st.open();

        @SuppressWarnings("unchecked")
        T service = (T) st.waitForService(1000);
        if (service == null) {
            throw new InterruptedException("Could not find a service for: " + clazz.getName());
        }
        st.close();

        return service;
    }

}
