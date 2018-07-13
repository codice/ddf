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
package org.codice.solr.factory.impl;

import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;

/** Extends functionality of CoreContainer to allow direct registration of a SolrCore */
public class SolrCoreContainer extends CoreContainer {
  public SolrCoreContainer(SolrResourceLoader loader) {
    super(loader);
    this.load();
  }

  public void register(String coreName, SolrCore core, boolean publish, boolean skipRecovery) {
    this.registerCore(getCoreDescriptor(coreName), core, publish, skipRecovery);
  }
}
