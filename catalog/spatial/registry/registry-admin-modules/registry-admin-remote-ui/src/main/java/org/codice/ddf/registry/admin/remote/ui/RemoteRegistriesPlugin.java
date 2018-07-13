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
package org.codice.ddf.registry.admin.remote.ui;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.codice.ddf.admin.application.plugin.AbstractApplicationPlugin;

public class RemoteRegistriesPlugin extends AbstractApplicationPlugin {

  public RemoteRegistriesPlugin() {
    this.displayName = "Remote Registries";
    this.iframeLocation = URI.create("./registry/remote/index.html");
    List<String> apps = new ArrayList<>();
    apps.add("catalog-app");
    apps.add("registry-app");
    this.setAssociations(apps);
  }
}
