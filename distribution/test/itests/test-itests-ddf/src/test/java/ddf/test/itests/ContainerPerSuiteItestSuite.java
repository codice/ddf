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
package ddf.test.itests;

import ddf.test.itests.catalog.TestCatalog;
import ddf.test.itests.catalog.TestCatalogValidation;
import ddf.test.itests.catalog.TestFanout;
import ddf.test.itests.catalog.TestFederation;
import ddf.test.itests.catalog.TestSecurityAuditPlugin;
import ddf.test.itests.catalog.TestSpatial;
import ddf.test.itests.platform.TestOidc;
import ddf.test.itests.platform.TestPlatform;
import ddf.test.itests.platform.TestSecurity;
import ddf.test.itests.platform.TestSolrCommands;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * This suite is for test classes that will all be run in the same container The test classes are
 * run in the same order as they appear in the array. This order is important for some test classes.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
  TestFederation.class,
  TestSpatial.class,
  TestCatalogValidation.class,
  TestCatalog.class,
  TestSolrCommands.class,
  TestSecurity.class,
  TestPlatform.class,
  TestFanout.class,
  TestOidc.class,
  TestSecurityAuditPlugin.class,
})
public class ContainerPerSuiteItestSuite {}
