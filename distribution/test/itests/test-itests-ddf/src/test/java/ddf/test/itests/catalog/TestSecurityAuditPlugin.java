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
package ddf.test.itests.catalog;

import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.delete;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.ingestXmlFromResourceAndWait;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.update;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.itests.common.WaitCondition;
import org.codice.ddf.test.common.annotations.BeforeExam;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.osgi.service.cm.Configuration;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class TestSecurityAuditPlugin extends AbstractIntegrationTest {

  private String auditMessageFormat =
      "Attribute %s on metacard %s with value(s) %s was updated to value(s) %s";

  private String configUpdateMessage =
      "Security Audit Plugin configuration changed to audit : description";

  private String startedMessage = "Security Audit Plugin started";

  private String stoppedMessage = "Security Audit Plugin stopped";

  @BeforeExam
  public void beforeExam() throws Exception {
    getSecurityPolicy().configureRestForGuest();
  }

  @After
  public void tearDown() {
    clearCatalog();
  }

  @Test
  public void testSecurityAuditPlugin() throws Exception {
    Configuration config =
        configAdmin.getConfiguration(
            "org.codice.ddf.catalog.plugin.security.audit.SecurityAuditPlugin", null);
    List attributes = new ArrayList<>();
    attributes.add("description");
    Dictionary properties = new Hashtable<>();
    properties.put("auditAttributes", attributes);
    config.update(properties);

    String logFilePath = System.getProperty("karaf.data") + "/log/security.log";

    File securityLog = new File(logFilePath);
    WaitCondition.expect("Securitylog exists")
        .within(2, TimeUnit.MINUTES)
        .checkEvery(2, TimeUnit.SECONDS)
        .until(securityLog::exists);

    WaitCondition.expect("Securitylog has log message: " + configUpdateMessage)
        .within(2, TimeUnit.MINUTES)
        .checkEvery(2, TimeUnit.SECONDS)
        .until(() -> getFileContent(securityLog).contains(configUpdateMessage));

    String id = ingestXmlFromResourceAndWait("metacard1.xml");

    update(id, getResourceAsString("metacard2.xml"), "text/xml");

    String expectedLogMessage =
        String.format(
            auditMessageFormat, "description", id, "My Description", "My Description (Updated)");
    WaitCondition.expect("Securitylog has log message: " + expectedLogMessage)
        .within(2, TimeUnit.MINUTES)
        .checkEvery(2, TimeUnit.SECONDS)
        .until(() -> getFileContent(securityLog).contains(expectedLogMessage));

    delete(id);
  }

  @Test
  public void testBundleStartAndStop() throws Exception {
    String logFilePath = System.getProperty("karaf.log") + "/security.log";
    File securityLog = new File(logFilePath);

    getServiceManager().stopBundle("catalog-plugin-security-audit");
    WaitCondition.expect("Securitylog has log message: " + stoppedMessage)
        .within(2, TimeUnit.MINUTES)
        .checkEvery(2, TimeUnit.SECONDS)
        .until(() -> getFileContent(securityLog).contains(stoppedMessage));

    getServiceManager().startBundle("catalog-plugin-security-audit");
    WaitCondition.expect("Securitylog has log message: " + startedMessage)
        .within(2, TimeUnit.MINUTES)
        .checkEvery(2, TimeUnit.SECONDS)
        .until(() -> getFileContent(securityLog).contains(startedMessage));
  }

  private String getResourceAsString(String resourcePath) throws IOException {
    InputStream inputStream = getFileContentAsStream(resourcePath);
    return IOUtils.toString(inputStream, Charset.forName("UTF-8"));
  }

  private String getFileContent(File file) throws IOException {
    InputStream inputStream = new FileInputStream(file);
    return IOUtils.toString(inputStream, Charset.forName("UTF-8"));
  }
}
