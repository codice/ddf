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
package org.codice.felix.cm.file;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import java.util.Hashtable;
import org.codice.felix.cm.internal.ConfigurationContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.cm.Configuration;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationContextFactoryImplTest {

  @Mock private Configuration mockConfig;

  private ConfigurationContextFactoryImpl factory = new ConfigurationContextFactoryImpl();

  @Test(expected = IllegalArgumentException.class)
  public void testCreateContextNullConfiguration() {
    factory.createContext(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateContextNullProperties() {
    when(mockConfig.getPid()).thenReturn("myPid");
    when(mockConfig.getProperties()).thenReturn(null);
    factory.createContext(mockConfig);
  }

  @Test
  public void testCreateContext() {
    when(mockConfig.getPid()).thenReturn("myPid");
    when(mockConfig.getProperties()).thenReturn(new Hashtable<>());
    ConfigurationContext context = factory.createContext(mockConfig);
    assertThat(context.getServicePid(), is("myPid"));
  }
}
