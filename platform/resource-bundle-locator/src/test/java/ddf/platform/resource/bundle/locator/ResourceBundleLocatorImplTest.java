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
package ddf.platform.resource.bundle.locator;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ResourceBundleLocatorImplTest {

  private ResourceBundleLocatorImpl resourceBundleLocator;

  private static final String TEST_BASE_NAME = "TestResourceBundle";

  @Before
  public void setup() {
    System.setProperty("ddf.home", "./");
    this.resourceBundleLocator = new ResourceBundleLocatorImpl();
    resourceBundleLocator.setResourceBundleBaseDir("src/test/resources/");
  }

  @After
  public void after() {
    System.clearProperty("ddf.home");
  }

  @Test
  public void testGetResourceBundle() throws IOException {
    ResourceBundle resourceBundle = resourceBundleLocator.getBundle(TEST_BASE_NAME);

    assertThat(resourceBundle.keySet().size(), is(3));
    assertThat(resourceBundle.getObject("Hello World"), is("Foo Bar"));
    assertThat(resourceBundle.getObject("Foo"), is("Bar"));
    assertThat(resourceBundle.getObject("foo"), is("bar"));
  }

  @Test
  public void testGetResourceBundleWithLocale() throws IOException {
    ResourceBundle resourceBundle =
        resourceBundleLocator.getBundle("TestFrenchResourceBundle", Locale.FRENCH);

    assertThat(resourceBundle.keySet().size(), is(2));
    assertThat(resourceBundle.getObject("hello"), is("world"));
    assertThat(resourceBundle.getObject("foo"), is("bar"));
  }

  @Test
  public void testResourceBundleFallback() throws IOException {
    ResourceBundle resourceBundle =
        resourceBundleLocator.getBundle(TEST_BASE_NAME, Locale.JAPANESE);

    assertThat(resourceBundle.keySet().size(), is(3));
    assertThat(resourceBundle.getObject("Hello World"), is("Foo Bar"));
    assertThat(resourceBundle.getObject("Foo"), is("Bar"));
    assertThat(resourceBundle.getObject("foo"), is("bar"));
  }

  @Test(expected = MissingResourceException.class)
  public void testGetResourceBundleNonExistentBaseName() throws IOException {
    resourceBundleLocator.getBundle("FooBarBundle");
  }
}
