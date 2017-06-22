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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import ddf.catalog.Constants;
import ddf.catalog.transform.InputTransformer;

/**
 * Provides information on available transformers
 */
@Service
@Command(scope = CatalogCommands.NAMESPACE, name = "transformers", description = "Provides information on available transformers.")
public class TransformersCommand extends CatalogCommands {

    // output strings

    static final String ACTIVE_TRANSFORMERS_HEADER = "Active Transformers: ";

    static final String NO_ACTIVE_TRANSFORMERS = "There are no active transformers";

    private static final String LINE = "------------------------";

    // Transformer Properties

    private static final String MIME_TYPE = "mime-type";

    private static final String SCHEMA = "schema";

    private static final String NOT_AVAILABLE = "N/A";

    @Override
    protected Object executeWithSubject() throws Exception {

        // ServiceReferences are needed to get property information from InputTransformers
        List<ServiceReference<InputTransformer>> serviceReferences =
                (List<ServiceReference<InputTransformer>>) bundleContext.getServiceReferences(
                        InputTransformer.class,
                        "(id=*)");

        List<TransformerProperties> transformersProperties = serviceReferences.stream()
                .map(TransformerProperties::new)
                .collect(Collectors.toList());

        int activeTransformers = transformersProperties.size();

        if (activeTransformers == 0) {
            console.printf("%s%n%n", NO_ACTIVE_TRANSFORMERS);
            return null;
        }

        console.printf("%n%s%d%n%s%n%n", ACTIVE_TRANSFORMERS_HEADER, activeTransformers, LINE);

        Iterator<TransformerProperties> tpIterator = transformersProperties.iterator();
        TransformerProperties tp;

        while (tpIterator.hasNext()) {
            tp = tpIterator.next();
            console.printf("%s", tp.printProperties());

            if (tpIterator.hasNext()) {
                console.printf("%n%s%n", StringUtils.repeat(LINE, 3));
            }
            console.printf("%n");
        }

        return null;
    }

    private static class TransformerProperties {

        private String id;

        private String schema;

        private List<String> mimeTypes;

        public TransformerProperties(ServiceReference ref) {

            this.id = getTransformerPropertyString(ref, Constants.SERVICE_ID);
            this.schema = getTransformerPropertyString(ref, SCHEMA);
            this.mimeTypes = getTransformerMimeTypes(ref);
        }

        public String printProperties() {

            StringBuilder s = new StringBuilder(MessageFormat.format(
                    "{0}: {1}\n\n\t{2}: {3}\n\t{4}s: {5}\n",
                    Constants.SERVICE_ID,
                    id,
                    SCHEMA,
                    schema,
                    MIME_TYPE,
                    mimeTypes.remove(0)));

            for (String mimeType : mimeTypes) {
                s.append("\t\t    ")
                        .append(mimeType)
                        .append("\n");
            }
            return s.toString();
        }

        private String getTransformerPropertyString(ServiceReference ref, String property) {

            return Optional.ofNullable(ref.getProperty(property))
                    .map(String.class::cast)
                    .filter(String.class::isInstance)
                    .orElse(NOT_AVAILABLE);
        }

        private List<String> getTransformerMimeTypes(ServiceReference ref) {

            List<String> mimeProperties;

            if (ref.getProperty(MIME_TYPE) instanceof List) {
                mimeProperties = (ArrayList<String>) ref.getProperty(MIME_TYPE);
            } else {
                mimeProperties = new ArrayList<>();
                mimeProperties.add(getTransformerPropertyString(ref, MIME_TYPE));
            }
            return mimeProperties;
        }
    }

    // Used for testing TransformersCommand
    protected void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
