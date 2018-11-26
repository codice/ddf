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

import java.util.Dictionary;
import java.util.Hashtable;
import org.apache.felix.cm.PersistenceManager;
import org.apache.felix.cm.file.FilePersistenceManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {
  private volatile ServiceRegistration filepmRegistration;

  private WrappedPersistenceManager persistenceManager;

  @Override
  public void start(BundleContext bundleContext) throws Exception {
    persistenceManager =
        new DelegatingPersistenceManager(
            new EncryptingPersistenceManager(
                new FilePersistenceManager(
                    bundleContext, bundleContext.getProperty("felix.cm.dir"))));

    final Dictionary<String, Object> props = new Hashtable<>();
    props.put(Constants.SERVICE_DESCRIPTION, "DDF :: Platform :: OSGi :: Configuration Admin");
    props.put(Constants.SERVICE_VENDOR, "Codice Foundation");
    props.put("name", "CodicePM");
    filepmRegistration =
        bundleContext.registerService(
            PersistenceManager.class.getName(), persistenceManager, props);
  }

  @Override
  public void stop(BundleContext bundleContext) throws Exception {
    persistenceManager.close();

    final ServiceRegistration filePmReg = filepmRegistration;
    filepmRegistration = null;
    if (filePmReg != null) {
      filePmReg.unregister();
    }
  }
}
