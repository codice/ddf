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
package ddf.migration.commands;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import ddf.migration.api.DataMigratable;
import ddf.migration.api.ServiceNotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

@RunWith(MockitoJUnitRunner.class)
public class MigrateCommandTest {

  private MigrateCommand migrateCommand;

  private static ByteArrayOutputStream buffer;

  private static PrintStream realSystemOut;

  @Mock private BundleContext bundleContext;

  @BeforeClass
  public static void setupOutput() {
    interceptSystemOut();
  }

  @Before
  public void setup() {
    migrateCommand = new MigrateCommand();
    migrateCommand.setBundleContext(bundleContext);
  }

  @After
  public void resetBuffer() {
    buffer.reset();
  }

  @AfterClass
  public static void closeOutput() throws IOException {
    System.setOut(realSystemOut);
    buffer.close();
  }

  @Test
  public void testListMigrationTasks() throws Exception {
    migrateCommand.setAllMigrationTasks(false);
    migrateCommand.setListMigrationTasks(true);

    ServiceReference<DataMigratable> serviceRef = getMockServiceReference(null, "name", "desc");
    Collection<ServiceReference<DataMigratable>> mockServices = ImmutableList.of(serviceRef);

    when(bundleContext.getServiceReferences(eq(DataMigratable.class), anyString()))
        .thenReturn(mockServices);

    migrateCommand.executeWithSubject();

    String output = getOutput();
    assertThat(output, containsString("Data Migration"));
    assertThat(output, containsString("name"));
    assertThat(output, containsString("desc"));
  }

  @Test
  public void testExecuteMigrationTask() throws Exception {
    migrateCommand.setAllMigrationTasks(false);
    migrateCommand.setListMigrationTasks(false);
    migrateCommand.setServiceId("serviceId");

    ServiceReference<DataMigratable> serviceRef =
        getMockServiceReference("serviceId", "service", null);
    Collection<ServiceReference<DataMigratable>> services = ImmutableList.of(serviceRef);

    when(bundleContext.getServiceReferences(eq(DataMigratable.class), anyString()))
        .thenReturn(services);

    DataMigratable dataMigratable = mock(DataMigratable.class);

    when(bundleContext.getService(serviceRef)).thenReturn(dataMigratable);

    migrateCommand.executeWithSubject();

    verify(dataMigratable, times(1)).migrate();
  }

  @Test
  public void testExecuteAllDataMigrationTasks() throws Exception {
    migrateCommand.setAllMigrationTasks(true);

    ServiceReference<DataMigratable> serviceRef1 =
        getMockServiceReference("serviceId1", "name1", "description1");
    ServiceReference<DataMigratable> serviceRef2 =
        getMockServiceReference("serviceId2", "name2", "description2");

    DataMigratable dataMigratable1 = mock(DataMigratable.class);
    DataMigratable dataMigratable2 = mock(DataMigratable.class);

    when(bundleContext.getService(serviceRef1)).thenReturn(dataMigratable1);
    when(bundleContext.getService(serviceRef2)).thenReturn(dataMigratable2);

    when(bundleContext.getServiceReferences(eq(DataMigratable.class), anyString()))
        .thenReturn(ImmutableList.of(serviceRef1, serviceRef2));

    migrateCommand.executeWithSubject();

    verify(dataMigratable1, times(1)).migrate();
    verify(dataMigratable2, times(1)).migrate();
  }

  @Test(expected = ServiceNotFoundException.class)
  public void testMigrationTaskNotFound() throws Exception {
    migrateCommand.setAllMigrationTasks(false);
    migrateCommand.setListMigrationTasks(false);

    when(bundleContext.getServiceReferences(eq(DataMigratable.class), anyString()))
        .thenReturn(Collections.emptyList());

    migrateCommand.executeWithSubject();
  }

  private ServiceReference<DataMigratable> getMockServiceReference(
      String id, String name, String desc) {
    ServiceReference<DataMigratable> serviceRef = mock(ServiceReference.class);

    when(serviceRef.getProperty("id")).thenReturn(id);
    when(serviceRef.getProperty("name")).thenReturn(name);
    when(serviceRef.getProperty("description")).thenReturn(desc);

    return serviceRef;
  }

  private static void interceptSystemOut() {
    realSystemOut = System.out;
    buffer = new ByteArrayOutputStream();
    System.setOut(new PrintStream(buffer));
  }

  public String getOutput() {
    return buffer.toString();
  }
}
