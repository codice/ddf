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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import ddf.catalog.Constants;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transform.MetacardTransformer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

@RunWith(MockitoJUnitRunner.class)
public class TransformersCommandTest extends ConsoleOutputCommon {

  private static final String SERVICE_ID = "id";

  private static final String MIME_TYPE = "mime-type";

  private static final String SCHEMA = "schema";

  private static final String FILTER = "(id=*)";

  private static final String ACTIVE_METACARD_TRANSFORMERS =
      "%n%n%n%nActive Metacard Transformers: %d%n";

  private static final String ACTIVE_INPUT_TRANSFORMERS = "%n%n%n%nActive Input Transformers: %d%n";

  @Mock BundleContext bundleContext;

  TransformersCommand transformersCommand;

  @Before
  public void setUp() {

    transformersCommand = new TransformersCommand();
    transformersCommand.setBundleContext(bundleContext);
  }

  @Test
  public void testNullIdsForInputTransformers() throws Exception {

    List<ServiceReference<InputTransformer>> serviceReferences = new ArrayList<>();
    ServiceReference<InputTransformer> serviceReference = mock(ServiceReference.class);
    serviceReferences.add(serviceReference);

    List<ServiceReference<InputTransformer>> serviceReferencesFiltered =
        serviceReferences
            .stream()
            .filter(ref -> ref.getProperty(Constants.SERVICE_ID) != null)
            .collect(Collectors.toList());

    when(bundleContext.getServiceReferences(InputTransformer.class, FILTER))
        .thenReturn(serviceReferencesFiltered);

    transformersCommand.setInputOption(true);
    transformersCommand.executeWithSubject();
    assertThat(
        consoleOutput.getOutput(),
        containsString(String.format("%s%n%n", "There are no active Input transformers")));
  }

  @Test
  public void testNullIdsForMetacardTransformers() throws Exception {

    List<ServiceReference<MetacardTransformer>> serviceReferences = new ArrayList<>();
    ServiceReference<MetacardTransformer> serviceReference = mock(ServiceReference.class);
    serviceReferences.add(serviceReference);

    List<ServiceReference<MetacardTransformer>> serviceReferencesFiltered =
        serviceReferences
            .stream()
            .filter(ref -> ref.getProperty(Constants.SERVICE_ID) != null)
            .collect(Collectors.toList());

    when(bundleContext.getServiceReferences(MetacardTransformer.class, FILTER))
        .thenReturn(serviceReferencesFiltered);

    transformersCommand.setMetacardOption(true);
    transformersCommand.setAllOption(true);
    transformersCommand.executeWithSubject();
    assertThat(
        consoleOutput.getOutput(),
        containsString(String.format("%s%n%n", "There are no active Metacard transformers")));
  }

  @Test
  public void testNullAndNotNullIdsForInputTransformers() throws Exception {

    int nullIds = 5;
    int notNullIds = 7;

    List<ServiceReference<InputTransformer>> serviceReferences = new ArrayList<>();

    // mock service refs with null and not null ids
    for (int i = 0; i < nullIds + notNullIds; i++) {
      ServiceReference<InputTransformer> serviceReference = mock(ServiceReference.class);
      serviceReferences.add(serviceReference);

      if (i < nullIds) {
        when(serviceReference.getProperty(SERVICE_ID)).thenReturn(null);
      } else {
        when(serviceReference.getProperty(SERVICE_ID)).thenReturn("Test");
      }
    }

    List<ServiceReference<InputTransformer>> serviceReferencesFiltered =
        serviceReferences
            .stream()
            .filter(ref -> ref.getProperty(Constants.SERVICE_ID) != null)
            .collect(Collectors.toList());

    when(bundleContext.getServiceReferences(InputTransformer.class, FILTER))
        .thenReturn(serviceReferencesFiltered);

    transformersCommand.setInputOption(true);
    transformersCommand.executeWithSubject();
    assertThat(
        consoleOutput.getOutput(),
        containsString(String.format(ACTIVE_INPUT_TRANSFORMERS, notNullIds)));
  }

  @Test
  public void testNullAndNotNullIdsForMetacardTransformers() throws Exception {

    int nullIds = 5;
    int notNullIds = 7;

    List<ServiceReference<MetacardTransformer>> serviceReferences = new ArrayList<>();

    // mock service refs with null and not null ids
    for (int i = 0; i < nullIds + notNullIds; i++) {
      ServiceReference<MetacardTransformer> serviceReference = mock(ServiceReference.class);
      serviceReferences.add(serviceReference);

      if (i < nullIds) {
        when(serviceReference.getProperty(SERVICE_ID)).thenReturn(null);
      } else {
        when(serviceReference.getProperty(SERVICE_ID)).thenReturn("Test");
      }
    }

    List<ServiceReference<MetacardTransformer>> serviceReferencesFiltered =
        serviceReferences
            .stream()
            .filter(ref -> ref.getProperty(Constants.SERVICE_ID) != null)
            .collect(Collectors.toList());

    when(bundleContext.getServiceReferences(MetacardTransformer.class, FILTER))
        .thenReturn(serviceReferencesFiltered);

    transformersCommand.setMetacardOption(true);
    transformersCommand.executeWithSubject();
    assertThat(
        consoleOutput.getOutput(),
        containsString(String.format(ACTIVE_METACARD_TRANSFORMERS, notNullIds)));
  }

  @Test
  public void testNullAndNotNullIdsWithAllOption() throws Exception {

    int nullIds = 5;
    int notNullIds = 7;

    List<ServiceReference<MetacardTransformer>> serviceReferences = new ArrayList<>();

    // mock service refs with null and not null ids
    for (int i = 0; i < nullIds + notNullIds; i++) {
      ServiceReference<MetacardTransformer> serviceReference = mock(ServiceReference.class);
      serviceReferences.add(serviceReference);

      // printAllTransformers() expects an array of property keys
      when(serviceReference.getPropertyKeys()).thenReturn(new String[] {"Test"});
      // when(serviceReference.getProperty("Test")).thenReturn("Test");

      if (i < nullIds) {
        when(serviceReference.getProperty(SERVICE_ID)).thenReturn(null);
      } else {
        when(serviceReference.getProperty(SERVICE_ID)).thenReturn("Test");
      }
    }

    List<ServiceReference<MetacardTransformer>> serviceReferencesFiltered =
        serviceReferences
            .stream()
            .filter(ref -> ref.getProperty(Constants.SERVICE_ID) != null)
            .collect(Collectors.toList());

    when(bundleContext.getServiceReferences(MetacardTransformer.class, FILTER))
        .thenReturn(serviceReferencesFiltered);

    transformersCommand.setMetacardOption(true);
    transformersCommand.setAllOption(true);
    transformersCommand.executeWithSubject();
    assertThat(
        consoleOutput.getOutput(),
        containsString(String.format(ACTIVE_METACARD_TRANSFORMERS, notNullIds)));
  }

  @Test
  public void testActiveInputTransformerCount() throws Exception {

    List<ServiceReference<InputTransformer>> serviceReferences;

    ServiceReference<InputTransformer> serviceReference = mock(ServiceReference.class);
    when(serviceReference.getProperty(SERVICE_ID)).thenReturn("Test");

    serviceReferences = ImmutableList.of(serviceReference, serviceReference, serviceReference);

    when(bundleContext.getServiceReferences(InputTransformer.class, FILTER))
        .thenReturn(serviceReferences);

    transformersCommand.setInputOption(true);
    transformersCommand.executeWithSubject();
    assertThat(
        consoleOutput.getOutput(), containsString(String.format(ACTIVE_INPUT_TRANSFORMERS, 3)));
  }

  @Test
  public void testActiveMetacardTransformerCount() throws Exception {

    List<ServiceReference<MetacardTransformer>> serviceReferences;

    ServiceReference<MetacardTransformer> serviceReference = mock(ServiceReference.class);
    when(serviceReference.getProperty(SERVICE_ID)).thenReturn("Test");

    serviceReferences = ImmutableList.of(serviceReference, serviceReference, serviceReference);

    when(bundleContext.getServiceReferences(MetacardTransformer.class, FILTER))
        .thenReturn(serviceReferences);

    transformersCommand.setMetacardOption(true);
    transformersCommand.executeWithSubject();
    assertThat(
        consoleOutput.getOutput(), containsString(String.format(ACTIVE_METACARD_TRANSFORMERS, 3)));
  }

  @Test
  public void testNullPropertiesWithValidId() throws Exception {

    List<ServiceReference<InputTransformer>> serviceReferences = new ArrayList<>();

    ServiceReference<InputTransformer> serviceReference = mock(ServiceReference.class);

    when(serviceReference.getProperty(SERVICE_ID)).thenReturn("Test");
    when(serviceReference.getProperty(SCHEMA)).thenReturn(null);
    when(serviceReference.getProperty(MIME_TYPE)).thenReturn(null);

    serviceReferences.add(serviceReference);

    when(bundleContext.getServiceReferences(InputTransformer.class, FILTER))
        .thenReturn(serviceReferences);

    transformersCommand.executeWithSubject();
    String output = consoleOutput.getOutput();
    assertThat(output, containsString("schema: N/A"));
    assertThat(output, containsString("mime-types: N/A"));
  }

  @Test
  public void testMultipleMimeTypes() throws Exception {

    List<ServiceReference<InputTransformer>> serviceReferences = new ArrayList<>();

    ServiceReference<InputTransformer> serviceReference = mock(ServiceReference.class);
    List<String> mimeTypes = new ArrayList<>();
    mimeTypes.add("mime1");
    mimeTypes.add("mime2");
    mimeTypes.add("mime3");

    when(serviceReference.getProperty(SERVICE_ID)).thenReturn("Test");
    when(serviceReference.getProperty(MIME_TYPE)).thenReturn(mimeTypes);

    serviceReferences.add(serviceReference);

    when(bundleContext.getServiceReferences(InputTransformer.class, FILTER))
        .thenReturn(serviceReferences);

    transformersCommand.executeWithSubject();
    String output = consoleOutput.getOutput();
    assertThat(output, containsString("mime-types: mime1"));
    assertThat(output, containsString("mime2"));
    assertThat(output, containsString("mime3"));
  }
}
