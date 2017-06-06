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
package ddf.test.itests.platform;

import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.ingest;
import static org.codice.ddf.itests.common.opensearch.OpenSearchTestCommons.getOpenSearch;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.itests.common.annotations.AfterExam;
import org.codice.ddf.itests.common.annotations.BeforeExam;
import org.codice.ddf.itests.common.utils.LoggingUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.osgi.framework.BundleException;

import com.jayway.restassured.path.xml.XmlPath;
import com.jayway.restassured.path.xml.element.NodeChildren;
import com.jayway.restassured.response.ValidatableResponse;

/**
 * TODO DDF-3075 Rewrite/refactor these configuration tests to better evaluate expectations now that DDF relies on the Felix configadmin to read .config files
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class TestConfiguration extends AbstractIntegrationTest {

    private static final String EXPORT_COMMAND = "migration:export";

    private static final String CATALOG_REMOVE_ALL_COMMAND = "catalog:removeall --force";

    private static final String CATALOG_INGEST_COMMAND = "catalog:ingest -t ser";

    private static final String CRL_PEM = "crl.pem";

    private static final String CRL_ENABLED_SERVER_ENCRYPTION_PROPERTIES_FILE =
            "/serverencryption.properties";

    private static final String CRL_ENABLED_SERVER_SIGNATURE_PROPERTIES_FILE =
            "/serversignature.properties";

    private static final String CRL_ENABLED_ISSUER_ENCRYPTION_PROPERTIES_FILE =
            "/issuerencryption.properties";

    private static final String CRL_ENABLED_ISSUER_SIGNATURE_PROPERTIES_FILE =
            "/issuersignature.properties";

    private static final String SERVER_SIGNATURE_DIR = "serversignature";

    private static final Path SERVER_SIGNATURE_DIR_PATH = Paths.get("etc", SERVER_SIGNATURE_DIR);

    private static final String ISSUER_SIGNATURE_DIR = "issuersignature";

    private static final Path ISSUER_SIGNATURE_DIR_PATH = Paths.get("etc", ISSUER_SIGNATURE_DIR);

    private static final String ISSUER_ENCRYPTION_DIR = "issuerencryption";

    private static final Path ISSUER_ENCRYPTION_DIR_PATH = Paths.get("etc", ISSUER_ENCRYPTION_DIR);

    private static final Path DEMO_CA_CRL_PATH = Paths.get("certs", "demoCA", "crl");

    private static final String TEST_FILE = "../cat.txt";

    private static final Path SYSTEM_PROPERTIES = Paths.get("etc", "system.properties");

    private static final Path SYSTEM_PROPERTIES_COPY = Paths.get("etc", "system.properties.copy");

    private static final Path USERS_PROPERTIES = Paths.get("etc", "users.properties");

    private static final Path USERS_PROPERTIES_COPY = Paths.get("etc", "users.properties.copy");

    private static final Path WS_SECURITY = Paths.get("etc", "ws-security");

    private static final Path WS_SECURITY_COPY = Paths.get("etc", "ws-security-copy");

    private static final Path PDP = Paths.get("etc", "pdp");

    private static final Path PDP_COPY = Paths.get("etc", "pdp-copy");

    private static final Path SERVER_ENCRYPTION_PROPERTIES = Paths.get("etc",
            "ws-security",
            "server",
            "encryption.properties");

    private static final Path SERVER_ENCRYPTION_PROPERTIES_COPY = Paths.get(
            "server.encryption.properties.copy");

    private static final Path SERVER_SIGNATURE_PROPERTIES = Paths.get("etc",
            "ws-security",
            "server",
            "signature.properties");

    private static final Path SERVER_SIGNATURE_PROPERTIES_COPY = Paths.get(
            "server.signature.properties.copy");

    private static final Path ISSUER_ENCRYPTION_PROPERTIES = Paths.get("etc",
            "ws-security",
            "issuer",
            "encryption.properties");

    private static final Path ISSUER_ENCRYPTION_PROPERTIES_COPY = Paths.get(
            "issuer.encryption.properties.copy");

    private static final Path ISSUER_SIGNATURE_PROPERTIES = Paths.get("etc",
            "ws-security",
            "issuer",
            "signature.properties");

    private static final Path ISSUER_SIGNATURE_PROPERTIES_COPY = Paths.get(
            "issuer.signature.properties.copy");

    private static final String KEYSTORE_PROPERTY = "javax.net.ssl.keyStore";

    private static final String FILE_SEPARATOR = System.getProperty("file.separator");

    private static final String FELIX_FILE_INSTALLER = "org.apache.felix.fileinstall";

    private static Path symbolicLink;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @BeforeExam
    public void beforeExam() throws Exception {
        try {
            waitForSystemReady();
            symbolicLink = Paths.get(ddfHome)
                    .resolve("link");
        } catch (Exception e) {
            LoggingUtils.failWithThrowableStacktrace(e, "Failed in @BeforeExam: ");
        }
    }

    @AfterExam
    public void afterExam() throws Exception {
        getServiceManager().startBundle(FELIX_FILE_INSTALLER);
    }

    public void resetInitialState() throws Exception {

        FileUtils.deleteQuietly(getDefaultExportDirectory().toFile());
        FileUtils.deleteQuietly(new File(TEST_FILE));
        FileUtils.deleteQuietly(symbolicLink.toFile());

        restoreBackup(SYSTEM_PROPERTIES_COPY, SYSTEM_PROPERTIES);
        restoreBackup(USERS_PROPERTIES_COPY, USERS_PROPERTIES);
        restoreBackup(WS_SECURITY_COPY, WS_SECURITY);
        restoreBackup(PDP_COPY, PDP);

        System.setProperty(KEYSTORE_PROPERTY,
                "etc" + File.separator + "keystores" + File.separator + "serverKeystore.jks");

        disableCrls();
        console.runCommand(CATALOG_REMOVE_ALL_COMMAND, new RolePrincipal("admin"));
    }

    @Test
    public void testExport() throws Exception {
        closeFileHandlesInEtc();
        resetInitialState();

        console.runCommand(EXPORT_COMMAND);

        assertExportContents(getDefaultExportDirectory());
    }

    @Test
    public void testExportToDirectory() throws Exception {
        closeFileHandlesInEtc();
        resetInitialState();

        String response = console.runCommand(
                EXPORT_COMMAND + " \"" + temporaryFolder.getRoot() + "\"");

        assertThat(String.format("Exporting current configurations to %s.",
                temporaryFolder.toString()),
                response,
                containsString("Successfully exported all configurations."));
        assertExportContents(temporaryFolder.getRoot()
                .toPath());
    }

    @Test
    public void testExportOnTopOfFile() throws Exception {
        closeFileHandlesInEtc();
        resetInitialState();

        File file = getDefaultExportDirectory().toFile();
        file.createNewFile();

        String response = console.runCommand(EXPORT_COMMAND);

        assertThat(String.format("Should not have been able to export to %s.",
                getDefaultExportDirectory()),
                response,
                containsString("Unable to create export directories."));
    }

    @Test
    public void testExportOnTopOfNestedFile() throws Exception {
        closeFileHandlesInEtc();
        resetInitialState();

        File file = getDefaultExportDirectory().toFile();
        file.mkdir();
        File fileEtc = getDefaultExportDirectory().resolve("etc")
                .toFile();
        fileEtc.createNewFile();

        String response = console.runCommand(EXPORT_COMMAND);

        assertThat(String.format("Should not have been able to export to %s.",
                getDefaultExportDirectory()),
                response,
                containsString("Unable to create export directories."));
    }

    /**
     * Tests that absolute path pointing outside ddfHome causes a warning
     *
     * @throws Exception
     */
    @Test
    @Ignore("TODO DDF-3075 Re-evaluate the need for the ConfigurationFilesPoller and decouple the poller from the export feature")
    public void testExportWarningForAbsolutePathOutsideDdfHome() throws Exception {
        closeFileHandlesInEtc();
        resetInitialState();

        FileUtils.copyFile(SYSTEM_PROPERTIES.toFile(), new File(TEST_FILE));
        System.setProperty(KEYSTORE_PROPERTY, ddfHome + File.separator + TEST_FILE);

        String response = console.runCommand(EXPORT_COMMAND);
        assertThat(String.format("Should not have been able to export to %s.",
                getDefaultExportDirectory()),
                response,
                containsString(String.format("Failed to export all configurations to %s",
                        getDefaultExportDirectory())));
    }

    /**
     * Tests that absolute path pointing inside ddfHome causes a warning
     *
     * @throws Exception
     */
    @Test
    public void testExportWarningForAbsolutePathInsideDdfHome() throws Exception {
        closeFileHandlesInEtc();
        resetInitialState();

        System.setProperty(KEYSTORE_PROPERTY,
                ddfHome + File.separator + "etc" + File.separator + "keystores" + File.separator
                        + "serverKeystore.jks");

        String response = console.runCommand(EXPORT_COMMAND);
        assertThat(String.format("Should not have been able to export to %s.",
                getDefaultExportDirectory()),
                response,
                containsString(String.format("Failed to export all configurations to %s",
                        getDefaultExportDirectory())));
    }

    /**
     * Tests that paths containing symbolic links cause a warning
     *
     * @throws Exception
     */
    @Test
    @Ignore("TODO DDF-3075 Re-evaluate the need for the ConfigurationFilesPoller and decouple the poller from the export feature")
    public void testExportWarningForSymbolicLinkPath() throws Exception {
        if (System.getProperty("os.name")
                .startsWith("Win")) {
            // can't create symlinks in windows (borrowed from Apache commonsio)
            return;
        }

        resetInitialState();

        FileUtils.copyFile(SYSTEM_PROPERTIES.toFile(), new File(TEST_FILE));

        Files.createSymbolicLink(symbolicLink, Paths.get(TEST_FILE));

        System.setProperty(KEYSTORE_PROPERTY, symbolicLink.toString());

        String response = console.runCommand(EXPORT_COMMAND);

        assertThat(String.format("Should not have been able to export to %s.",
                getDefaultExportDirectory()),
                response,
                containsString(String.format("Failed to export all configurations to %s",
                        getDefaultExportDirectory())));

    }

    /**
     * Tests that relative paths that point outside ddfHome causes a warning
     *
     * @throws Exception
     */
    @Test
    @Ignore("TODO DDF-3075 Re-evaluate the need for the ConfigurationFilesPoller and decouple the poller from the export feature")
    public void testExportWarningForRelativePathOutsideDdfHome() throws Exception {
        closeFileHandlesInEtc();
        resetInitialState();

        File systemProperties = new File(
                ddfHome + File.separator + "etc" + File.separator + "system.properties");
        File testFile = new File(TEST_FILE);
        FileUtils.copyFile(systemProperties, testFile);
        System.setProperty(KEYSTORE_PROPERTY, TEST_FILE);

        String response = console.runCommand(EXPORT_COMMAND);
        assertThat(String.format("Should not have been able to export to %s.",
                getDefaultExportDirectory()),
                response,
                containsString(String.format("Failed to export all configurations to %s",
                        getDefaultExportDirectory())));
    }

    /**
     * Tests that when system properties file is missing, export fails
     *
     * @throws Exception
     */
    @Test
    public void testExportFailureWithoutSystemPropertiesFile() throws Exception {
        closeFileHandlesInEtc();
        resetInitialState();

        FileUtils.moveFile(SYSTEM_PROPERTIES.toFile(), SYSTEM_PROPERTIES_COPY.toFile());

        String response = console.runCommand(EXPORT_COMMAND);

        assertThat(String.format("Warning should have been returned when exporting to %s.",
                getDefaultExportDirectory()),
                response,
                containsString("Path [etc" + FILE_SEPARATOR
                        + "system.properties] does not exist or cannot be read; therefore, it will not be included in the export."));
    }

    /**
     * Tests that when system properties file is missing, export fails
     *
     * @throws Exception
     */
    @Test
    public void testExportFailureWithoutUsersPropertiesFile() throws Exception {
        closeFileHandlesInEtc();
        resetInitialState();

        FileUtils.moveFile(USERS_PROPERTIES.toFile(), USERS_PROPERTIES_COPY.toFile());

        String response = console.runCommand(EXPORT_COMMAND);

        assertThat(String.format("Warning should have been returned when exporting to %s.",
                getDefaultExportDirectory()),
                response,
                containsString("Path [etc" + FILE_SEPARATOR
                        + "users.properties] does not exist or cannot be read; therefore, it will not be included in the export."));
    }

    /**
     * Tests that when ws-security directory is missing, export fails
     */
    @Test
    public void testExportFailureWithoutWSSecurityDirectory() throws Exception {
        closeFileHandlesInEtc();
        resetInitialState();

        FileUtils.moveDirectory(WS_SECURITY.toFile(), WS_SECURITY_COPY.toFile());

        String response = console.runCommand(EXPORT_COMMAND);

        assertThat(String.format("Should not have been able to export to %s.",
                getDefaultExportDirectory()),
                response,
                containsString("An error was encountered while executing this command."));
    }

    /**
     * Tests that when CRLs are enabled, they get exported
     */
    @Test
    public void testExportCrlsEnabled() throws Exception {
        closeFileHandlesInEtc();
        resetInitialState();
        enableCrls();

        console.runCommand(EXPORT_COMMAND);

        assertExportContentsWithCrlsEnabled(getDefaultExportDirectory());
    }

    /**
     * Tests that when system properties file is missing, export fails
     *
     * @throws Exception
     */
    @Test
    public void testExportFailureWithoutPDPDirectory() throws Exception {
        closeFileHandlesInEtc();
        resetInitialState();

        FileUtils.moveDirectory(PDP.toFile(), PDP_COPY.toFile());

        String response = console.runCommand(EXPORT_COMMAND);
        assertThat(String.format("Warning should have been returned when exporting to %s.",
                getDefaultExportDirectory()),
                response,
                containsString("Path [etc" + FILE_SEPARATOR
                        + "pdp] does not exist or cannot be read; therefore, it will not be included in the export."));
    }

    /**
     * Test that exporting twice overrides the previous files
     *
     * @throws Exception
     */
    @Test
    public void testExportOverridesPreviousExport() throws Exception {
        closeFileHandlesInEtc();
        resetInitialState();

        String firstExportMessage = console.runCommand(EXPORT_COMMAND);

        File firstExport = getExportSubDirectory(getDefaultExportDirectory(),
                "system.properties").toFile();
        long firstLength = firstExport.length();
        FileUtils.copyFile(SYSTEM_PROPERTIES.toFile(), SYSTEM_PROPERTIES_COPY.toFile());
        FileUtils.writeStringToFile(SYSTEM_PROPERTIES.toFile(), "testtesttest", true);

        String secondExportMessage = console.runCommand(EXPORT_COMMAND);

        File secondExport = getExportSubDirectory(getDefaultExportDirectory(),
                "system.properties").toFile();
        long secondLength = secondExport.length();

        assertThat("The first export failed to export",
                firstExportMessage,
                not(containsString("Failed to export all configurations")));
        assertThat("The second export failed to export",
                secondExportMessage,
                not(containsString("Failed to export all configurations")));
        assertThat("The second failed to modify the first export's files.",
                firstLength,
                is(not(equalTo(secondLength))));
    }

    @Test
    public void testExportMetacards() throws Exception {
        closeFileHandlesInEtc();
        resetInitialState();

        List<String> metacardIds = ingestMetacardsForExport();

        console.runCommand(EXPORT_COMMAND);

        assertExportCatalog(getDefaultExportDirectory().resolve("ddf.metacards"));

        console.runCommand(CATALOG_REMOVE_ALL_COMMAND, new RolePrincipal("admin"));

        console.runCommand(String.format("%s \"%s\"",
                CATALOG_INGEST_COMMAND,
                getDefaultExportDirectory().resolve("ddf.metacards")), new RolePrincipal("admin"));

        assertMetacardsIngested(metacardIds.size());
    }

    /**
     * The felix file installer keeps open file handles on files and directories in the etc directory on Windows.
     * This prevents files and directories from being deleted in some of the TestConfiguration itests when running
     * the itests on Windows. This method stops the felix file installer which releases the file handles.
     */
    private void closeFileHandlesInEtc() throws BundleException {
        getServiceManager().stopBundle(FELIX_FILE_INSTALLER);
    }

    private void assertMetacardsIngested(int expectedumberOfMetacards) throws Exception {
        ValidatableResponse response = getOpenSearch("xml", null, null, "q=*", "src=local");
        String bodyXml = response.extract()
                .body()
                .asString();
        NodeChildren metacards = new XmlPath(bodyXml).get("metacards.metacard");

        assertThat(metacards.size(), is(expectedumberOfMetacards));

    }

    private List<String> ingestMetacardsForExport() {
        List<String> metacardIds = new ArrayList<>(2);
        String metacardId1 = ingest(getFileContent(
                JSON_RECORD_RESOURCE_PATH + "/SimpleGeoJsonRecord"), "application/json");
        metacardIds.add(metacardId1);
        String metacardId2 = ingest(getFileContent(XML_RECORD_RESOURCE_PATH + "/SimpleXmlMetacard"),
                "text/xml");
        metacardIds.add(metacardId2);

        return metacardIds;
    }

    private void enableCrls() throws IOException {
        backupCrl(SERVER_ENCRYPTION_PROPERTIES, SERVER_ENCRYPTION_PROPERTIES_COPY);
        copyCrlEnabledPropertiesFile(CRL_ENABLED_SERVER_ENCRYPTION_PROPERTIES_FILE,
                SERVER_ENCRYPTION_PROPERTIES);

        backupCrl(SERVER_SIGNATURE_PROPERTIES, SERVER_SIGNATURE_PROPERTIES_COPY);
        copyCrlEnabledPropertiesFile(CRL_ENABLED_SERVER_SIGNATURE_PROPERTIES_FILE,
                SERVER_SIGNATURE_PROPERTIES);
        copyCrl(SERVER_SIGNATURE_DIR_PATH);

        backupCrl(ISSUER_ENCRYPTION_PROPERTIES, ISSUER_ENCRYPTION_PROPERTIES_COPY);
        copyCrlEnabledPropertiesFile(CRL_ENABLED_ISSUER_ENCRYPTION_PROPERTIES_FILE,
                ISSUER_ENCRYPTION_PROPERTIES);
        copyCrl(ISSUER_ENCRYPTION_DIR_PATH);

        backupCrl(ISSUER_SIGNATURE_PROPERTIES, ISSUER_SIGNATURE_PROPERTIES_COPY);
        copyCrlEnabledPropertiesFile(CRL_ENABLED_ISSUER_SIGNATURE_PROPERTIES_FILE,
                ISSUER_SIGNATURE_PROPERTIES);
        copyCrl(ISSUER_SIGNATURE_DIR_PATH);
    }

    private void backupCrl(Path source, Path destination) throws IOException {
        FileUtils.moveFile(source.toFile(), destination.toFile());
    }

    private void copyCrlEnabledPropertiesFile(String source, Path destination) throws IOException {
        FileUtils.copyInputStreamToFile(getFileContentAsStream(source),
                Paths.get(ddfHome)
                        .resolve(destination)
                        .toFile());
    }

    private void copyCrl(Path destinationDir) throws IOException {
        FileUtils.forceMkdir(Paths.get(ddfHome)
                .resolve(destinationDir)
                .toFile());
        FileUtils.copyInputStreamToFile(getFileContentAsStream(CRL_PEM),
                Paths.get(ddfHome)
                        .resolve(destinationDir)
                        .resolve(CRL_PEM)
                        .toFile());
    }

    private void disableCrls() throws IOException {
        restoreBackup(SERVER_ENCRYPTION_PROPERTIES_COPY, SERVER_ENCRYPTION_PROPERTIES);
        restoreBackup(SERVER_SIGNATURE_PROPERTIES_COPY, SERVER_SIGNATURE_PROPERTIES);
        restoreBackup(ISSUER_ENCRYPTION_PROPERTIES_COPY, ISSUER_ENCRYPTION_PROPERTIES);
        restoreBackup(ISSUER_SIGNATURE_PROPERTIES_COPY, ISSUER_SIGNATURE_PROPERTIES);
        FileUtils.deleteQuietly(Paths.get(ddfHome)
                .resolve(SERVER_SIGNATURE_DIR_PATH)
                .toFile());
        FileUtils.deleteQuietly(Paths.get(ddfHome)
                .resolve(ISSUER_ENCRYPTION_DIR_PATH)
                .toFile());
        FileUtils.deleteQuietly(Paths.get(ddfHome)
                .resolve(ISSUER_SIGNATURE_DIR_PATH)
                .toFile());
    }

    private Path getDefaultExportDirectory() {
        return Paths.get(ddfHome, "etc", "exported");

    }

    private Path getExportSubDirectory(Path exportDirectory, String... paths) {
        Path directory = exportDirectory.resolve("etc");

        for (String path : paths) {
            directory = directory.resolve(path);
        }

        return directory;
    }

    private void assertExportContents(Path exportDirectory) {
        assertThat(getExportSubDirectory(exportDirectory, "system.properties").toFile()
                .exists(), is(true));
        assertThat(getExportSubDirectory(exportDirectory, "users.properties").toFile()
                .exists(), is(true));
        assertThat(getExportSubDirectory(exportDirectory, "users.attributes").toFile()
                .exists(), is(true));
        assertThat(getExportSubDirectory(exportDirectory,
                "org.codice.ddf.admin.applicationlist.properties").toFile()
                .exists(), is(true));
        assertThatDirectoryContains(getExportSubDirectory(exportDirectory, "keystores"),
                "serverKeystore.jks",
                "serverTruststore.jks");
        assertThatDirectoryContains(getExportSubDirectory(exportDirectory, "pdp", "policies"),
                "access-policy.xml");
        assertThatDirectoryContains(getExportSubDirectory(exportDirectory, "ws-security"),
                "attributeMap.properties",
                "issuer",
                "server");
        assertThatDirectoryContains(getExportSubDirectory(exportDirectory, "ws-security", "issuer"),
                "encryption.properties",
                "signature.properties");
        assertThatDirectoryContains(getExportSubDirectory(exportDirectory, "ws-security", "server"),
                "encryption.properties",
                "signature.properties");
    }

    private void assertExportCatalog(Path exportPath) {
        String[] metacards = exportPath.toFile()
                .list();
        assertThat("Exported files should not be null.", metacards, is(notNullValue()));
        assertThat(metacards.length, is(2));
    }

    private void assertExportContentsWithCrlsEnabled(Path exportDirectory) {
        assertExportContents(exportDirectory);
        assertThatDirectoryContains(getExportSubDirectory(exportDirectory,
                DEMO_CA_CRL_PATH.toString()), CRL_PEM);
        assertThatDirectoryContains(getExportSubDirectory(exportDirectory, SERVER_SIGNATURE_DIR),
                CRL_PEM);
        assertThatDirectoryContains(getExportSubDirectory(exportDirectory, ISSUER_ENCRYPTION_DIR),
                CRL_PEM);
        assertThatDirectoryContains(getExportSubDirectory(exportDirectory, ISSUER_SIGNATURE_DIR),
                CRL_PEM);
    }

    private void assertThatDirectoryContains(Path path, String... fileNames) {
        String[] keystoreFiles = path.toFile()
                .list();
        assertThat("Exported files should not be null.", keystoreFiles, is(notNullValue()));
        assertThat(String.format("Files missing in %s directory", path.toString()),
                keystoreFiles,
                arrayContainingInAnyOrder(fileNames));
    }

    private void restoreBackup(Path copy, Path original) throws IOException {
        if (Files.exists(copy) && Files.isDirectory(copy)) {
            FileUtils.deleteQuietly(original.toFile());
            FileUtils.moveDirectory(copy.toFile(), original.toFile());
        } else if (Files.exists(copy) && !Files.isDirectory(copy)) {
            FileUtils.deleteQuietly(original.toFile());
            FileUtils.moveFile(copy.toFile(), original.toFile());
        }
    }

}
