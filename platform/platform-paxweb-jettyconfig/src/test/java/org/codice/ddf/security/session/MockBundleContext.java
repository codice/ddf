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
package org.codice.ddf.security.session;

import static org.mockito.Mockito.mock;

import ddf.security.audit.SecurityLogger;
import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Dictionary;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class MockBundleContext implements BundleContext {

  private ServiceReference serviceReference = mock(ServiceReference.class);
  private Object service = mock(SecurityLogger.class);

  @Override
  public String getProperty(String s) {
    return null;
  }

  @Override
  public Bundle getBundle() {
    return null;
  }

  @Override
  public Bundle installBundle(String s, InputStream inputStream) throws BundleException {
    return null;
  }

  @Override
  public Bundle installBundle(String s) throws BundleException {
    return null;
  }

  @Override
  public Bundle getBundle(long l) {
    return null;
  }

  @Override
  public Bundle[] getBundles() {
    return new Bundle[0];
  }

  @Override
  public void addServiceListener(ServiceListener serviceListener, String s)
      throws InvalidSyntaxException {}

  @Override
  public void addServiceListener(ServiceListener serviceListener) {}

  @Override
  public void removeServiceListener(ServiceListener serviceListener) {}

  @Override
  public void addBundleListener(BundleListener bundleListener) {}

  @Override
  public void removeBundleListener(BundleListener bundleListener) {}

  @Override
  public void addFrameworkListener(FrameworkListener frameworkListener) {}

  @Override
  public void removeFrameworkListener(FrameworkListener frameworkListener) {}

  @Override
  public ServiceRegistration<?> registerService(
      String[] strings, Object o, Dictionary<String, ?> dictionary) {
    return null;
  }

  @Override
  public ServiceRegistration<?> registerService(
      String s, Object o, Dictionary<String, ?> dictionary) {
    return null;
  }

  @Override
  public <S> ServiceRegistration<S> registerService(
      Class<S> aClass, S s, Dictionary<String, ?> dictionary) {
    return null;
  }

  @Override
  public ServiceReference<?>[] getServiceReferences(String s, String s1)
      throws InvalidSyntaxException {
    return new ServiceReference[] {serviceReference};
  }

  @Override
  public ServiceReference<?>[] getAllServiceReferences(String s, String s1)
      throws InvalidSyntaxException {
    return new ServiceReference[] {serviceReference};
  }

  @Override
  public ServiceReference<?> getServiceReference(String s) {
    return serviceReference;
  }

  @Override
  public <S> ServiceReference<S> getServiceReference(Class<S> aClass) {
    return null;
  }

  @Override
  public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> aClass, String s)
      throws InvalidSyntaxException {
    return null;
  }

  @Override
  public <S> S getService(ServiceReference<S> serviceReference) {
    return (S) service;
  }

  @Override
  public boolean ungetService(ServiceReference<?> serviceReference) {
    return false;
  }

  @Override
  public File getDataFile(String s) {
    return null;
  }

  @Override
  public Filter createFilter(String s) throws InvalidSyntaxException {
    return null;
  }

  @Override
  public Bundle getBundle(String s) {
    return null;
  }
}
