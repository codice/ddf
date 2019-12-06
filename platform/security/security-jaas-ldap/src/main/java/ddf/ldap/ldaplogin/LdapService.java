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
package ddf.ldap.ldaplogin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.jaas.config.impl.Config;
import org.apache.karaf.jaas.config.impl.Module;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LdapService {

  private static final Logger LOGGER = LoggerFactory.getLogger(LdapService.class);

  private static final String CONFIG_NAME = "ldap";

  private Config config;

  private List<Module> modules;

  public LdapService(final BundleContext context) {
    config = new Config();
    modules = new ArrayList<>();
    config.setBundleContext(context);
    config.setName(CONFIG_NAME);
    config.setRank(2);
    config.setModules(new Module[] {});

    LOGGER.debug("Registering new service as a JaasRealm.");
    context.registerService(JaasRealm.class, config, null);
  }

  /**
   * Updates an existing ldap module with a new one or adds a new module to the list of existing
   * modules.
   *
   * @param newModule that will replace a module or be added to the list of modules.
   */
  public synchronized void update(Module newModule) {
    modules.removeIf(m -> m.getName().equals(newModule.getName()));
    modules.add(newModule);
    config.setModules(modules.toArray(new Module[modules.size()]));
  }

  /**
   * Delete an ldap module given its id.
   *
   * @param id of the module.
   * @return true, if the delete was successful, false otherwise.
   */
  public synchronized boolean delete(String id) {
    int initSize = modules.size();
    modules.removeIf(m -> m.getName().equals(id));
    config.setModules(modules.toArray(new Module[modules.size()]));
    return initSize > modules.size();
  }

  /**
   * Return the list of created ldap modules.
   *
   * @return list of modules.
   */
  synchronized List<Module> getModules() {
    return Collections.unmodifiableList(this.modules);
  }
}
