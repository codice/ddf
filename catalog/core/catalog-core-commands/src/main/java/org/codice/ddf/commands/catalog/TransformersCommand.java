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

import com.google.common.annotations.VisibleForTesting;
import ddf.catalog.Constants;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transform.MetacardTransformer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/** Provides information on available transformers */
@Service
@Command(
    scope = CatalogCommands.NAMESPACE,
    name = "transformers",
    description =
        "Provides information on available transformers. By default, prints out all Metacard and Input transformers.")
public class TransformersCommand extends CatalogCommands {

  // output strings

  private static final String METACARD = "Metacard";

  private static final String INPUT = "Input";

  private static final String FILTER = "(id=*)";

  private static final String LINE = "------------------------";

  // Transformer Properties

  private static final String MIME_TYPE = "mime-type";

  private static final String SCHEMA = "schema";

  private static final String NOT_AVAILABLE = "N/A";

  @Option(
      name = "--input",
      required = false,
      aliases = {"-i", "-input"},
      multiValued = false,
      description = "Displays only input transformers")
  private boolean inputOption = false;

  @Option(
      name = "--metacard",
      required = false,
      aliases = {"-m", "-metacard"},
      multiValued = false,
      description = "Displays only metacard transformers")
  private boolean metacardOption = false;

  @Option(
      name = "--all",
      required = false,
      aliases = {"-a", "-all"},
      multiValued = false,
      description = "Displays all the properties of the desired transformers")
  private boolean allOption = false;

  @Override
  protected Object executeWithSubject() throws Exception {
    if (!inputOption && !metacardOption) {
      inputOption = true;
      metacardOption = true;
    }

    if (inputOption) {
      printTransformers(InputTransformer.class, INPUT);
    }
    if (metacardOption) {
      printTransformers(MetacardTransformer.class, METACARD);
    }

    return null;
  }

  private void printTransformers(Class transformerClass, String type)
      throws InvalidSyntaxException {

    Collection<ServiceReference> sref =
        bundleContext.getServiceReferences(transformerClass, FILTER);

    List<TransformerProperties> transformersProperties =
        sref.stream().map(TransformerProperties::new).collect(Collectors.toList());

    int activeTransformers = transformersProperties.size();

    if (activeTransformers == 0) {
      console.printf("There are no active %s transformers%n%n", type);
      return;
    }

    console.printf("%n%n%n%nActive %s Transformers: %d%n%s%n%n", type, activeTransformers, LINE);

    Iterator<TransformerProperties> tpIterator = transformersProperties.iterator();
    TransformerProperties tp;

    while (tpIterator.hasNext()) {
      tp = tpIterator.next();

      if (allOption) {
        console.printf("%s", tp.printAllProperties());
      } else {
        console.printf("%s", tp.printDefaultProperties());
      }

      if (tpIterator.hasNext()) {
        console.printf("%n%s%n", StringUtils.repeat(LINE, 3));
      }
      console.printf("%n");
    }
  }

  private static class TransformerProperties {

    private ServiceReference ref;

    private TransformerProperties(ServiceReference ref) {
      this.ref = ref;
    }

    private String printDefaultProperties() {

      String id = getTransformerPropertyString(Constants.SERVICE_ID);
      String schema = getTransformerPropertyString(SCHEMA);
      List<String> mimeTypes = getTransformerMimeTypes();

      StringBuilder s =
          new StringBuilder(
              MessageFormat.format(
                  "{0}: {1}\n\n\t{2}: {3}\n\t{4}s: {5}\n",
                  Constants.SERVICE_ID, id, SCHEMA, schema, MIME_TYPE, mimeTypes.remove(0)));

      s.append(printMimeTypes(mimeTypes));

      return s.toString();
    }

    private String printAllProperties() {

      StringBuilder s = new StringBuilder();
      String[] propertyKeys = ref.getPropertyKeys();
      List<String> mimeTypes = getTransformerMimeTypes();
      String id = getTransformerPropertyString(Constants.SERVICE_ID);
      String value;

      s.append(MessageFormat.format("{0}: {1}\n\n\t", Constants.SERVICE_ID, id));

      for (String key : propertyKeys) {
        if (key.equals(MIME_TYPE) || key.equals(Constants.SERVICE_ID)) {
          continue;
        }

        value = getTransformerPropertyString(key);
        s.append(MessageFormat.format("\n\t{0}: {1}", key, value));
      }

      s.append(MessageFormat.format("\n\t{0}s: {1}\n", MIME_TYPE, mimeTypes.remove(0)));
      s.append(printMimeTypes(mimeTypes));
      return s.toString();
    }

    private String getTransformerPropertyString(String property) {

      return Optional.ofNullable(ref.getProperty(property))
          .map(Object::toString)
          .orElse(NOT_AVAILABLE);
    }

    private List<String> getTransformerMimeTypes() {

      List<String> mimeProperties;

      if (ref.getProperty(MIME_TYPE) instanceof List) {
        mimeProperties = (ArrayList<String>) ref.getProperty(MIME_TYPE);
      } else {
        mimeProperties = new ArrayList<>();
        mimeProperties.add(getTransformerPropertyString(MIME_TYPE));
      }
      return mimeProperties;
    }

    private String printMimeTypes(List<String> mimeTypes) {
      StringBuilder s = new StringBuilder();

      for (String mimeType : mimeTypes) {
        s.append("\t\t    ").append(mimeType).append("\n");
      }

      return s.toString();
    }
  }

  @VisibleForTesting
  protected void setBundleContext(BundleContext bundleContext) {
    this.bundleContext = bundleContext;
  }

  @VisibleForTesting
  protected void setInputOption(Boolean b) {
    inputOption = b;
  }

  @VisibleForTesting
  protected void setMetacardOption(Boolean b) {
    metacardOption = b;
  }

  @VisibleForTesting
  protected void setAllOption(Boolean b) {
    allOption = b;
  }
}
