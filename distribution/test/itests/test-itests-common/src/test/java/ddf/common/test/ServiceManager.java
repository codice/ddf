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
 **/
package ddf.common.test;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.karaf.features.FeatureState;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public interface ServiceManager {

    BundleContext getBundleContext();

    void createManagedService(String factoryPid, Map<String, Object> properties) throws IOException;

    void startManagedService(String servicePid, Map<String, Object> properties) throws IOException;

    void stopManagedService(String servicePid) throws IOException;

    void startFeature(boolean wait, String... featureNames) throws Exception;

    void stopFeature(boolean wait, String... featureNames) throws Exception;

    void restartBundles(String... bundleSymbolicNames) throws BundleException;

    void stopBundle(String bundleSymbolicName) throws BundleException;

    void startBundle(String bundleSymbolicName) throws BundleException;

    void waitForRequiredApps(String... appNames) throws InterruptedException;

    void waitForAllBundles() throws InterruptedException;

    void waitForRequiredBundles(String symbolicNamePrefix) throws InterruptedException;

    void waitForFeature(String featureName, Predicate<FeatureState> predicate) throws Exception;

    void waitForHttpEndpoint(String path) throws InterruptedException;

    void waitForSourcesToBeAvailable(String restPath, String... sources)
            throws InterruptedException;

    void waitForAllConfigurations() throws InterruptedException;

    Map<String, Object> getMetatypeDefaults(String symbolicName, String factoryPid);

    void printInactiveBundles();

    <S> ServiceReference<S> getServiceReference(Class<S> aClass);

    <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> aClass, String s)
            throws InvalidSyntaxException;

    <S> S getService(ServiceReference<S> serviceReference);

    <S> S getService(Class<S> aClass);
}
