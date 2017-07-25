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
 */
package ddf.test.itests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import ddf.test.itests.catalog.TestCatalog;
import ddf.test.itests.catalog.TestCatalogSearchUi;
import ddf.test.itests.catalog.TestCatalogValidation;
import ddf.test.itests.catalog.TestFanout;
import ddf.test.itests.catalog.TestFederation;
import ddf.test.itests.catalog.TestFtp;
import ddf.test.itests.catalog.TestMessageBroker;
import ddf.test.itests.catalog.TestRegistry;
import ddf.test.itests.catalog.TestSecurityAuditPlugin;
import ddf.test.itests.catalog.TestSpatial;
import ddf.test.itests.platform.TestApplicationService;
import ddf.test.itests.platform.TestConfiguration;
import ddf.test.itests.platform.TestPlatform;
import ddf.test.itests.platform.TestSecurity;
import ddf.test.itests.platform.TestSingleSignOn;
import ddf.test.itests.platform.TestSolrCommands;

/**
 * This suite is for test classes that will all be run in the same container
 * The test classes are run in the same order as they appear in the array.
 * This order is important for some test classes, particularly for TestConfiguration which needs to
 * be last since it alters the configuration in an unrecoverable way
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({TestCatalogSearchUi.class, TestFederation.class, TestRegistry.class,
        TestFtp.class, TestSpatial.class, TestCatalogValidation.class, TestCatalog.class,
        TestSingleSignOn.class, TestSolrCommands.class, TestSecurity.class,
        TestApplicationService.class, TestPlatform.class, TestFanout.class, TestMessageBroker.class,
        TestSecurityAuditPlugin.class, /* THIS CLASS MUST BE LAST */ TestConfiguration.class /* THIS CLASS MUST BE LAST */})
public class ContainerPerSuiteItestSuite {

}
