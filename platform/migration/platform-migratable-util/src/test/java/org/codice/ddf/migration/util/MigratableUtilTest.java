/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.migration.util;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationWarning;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

@PrepareForTest({MigratableUtil.class, FileUtils.class})
public class MigratableUtilTest {

    private static final Path DDF_BASE_DIR = Paths.get("ddf");

    public static final String CONFIG_FILE_NAME = "file.config";

    private static final Path VALID_SOURCE_PATH = Paths.get("etc");

    private static final Path VALID_SOURCE_FILE = Paths.get(VALID_SOURCE_PATH.toString(),
            CONFIG_FILE_NAME);

    private static final Path ABSOLUTE_SOURCE_PATH = Paths.get("/root", "etc");

    private static final Path ABSOLUTE_SOURCE_FILE = Paths.get(ABSOLUTE_SOURCE_PATH.toString(),
            CONFIG_FILE_NAME);

    private static final Path OUTSIDE_DDF_HOME_SOURCE_PATH = Paths.get("..", "outside");

    private static final Path OUTSIDE_DDF_HOME_SOURCE_FILE =
            Paths.get(OUTSIDE_DDF_HOME_SOURCE_PATH.toString(), CONFIG_FILE_NAME);

    private static final Path EXPORT_DIR = Paths.get("etc", "exported");

    private static final Path VALID_DESTINATION_PATH = EXPORT_DIR;

    private static final String DDF_HOME_PROPERTY_NAME = "ddf.home";

    private static final String SOURCE_PATH_PROPERTY_NAME = "source";

    private static final Path JAVA_PROPERTIES_FILE = Paths.get("etc", "java.properties");

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private Path ddfHome;

    private Collection<MigrationWarning> warnings;

    @Before
    public void setup() throws Exception {
        mockStatic(FileUtils.class);
        mockStatic(Files.class);

        tempDir.newFolder(DDF_BASE_DIR.toString());

        ddfHome = Paths.get(tempDir.getRoot()
                .getAbsolutePath(), DDF_BASE_DIR.toString())
                .toRealPath();
        warnings = new ArrayList<>();

        System.setProperty(DDF_HOME_PROPERTY_NAME, ddfHome.toString());
        System.setProperty(SOURCE_PATH_PROPERTY_NAME, VALID_SOURCE_FILE.toString());

        createTempPropertiesFiles();
    }

    @Test(expected = MigrationException.class)
    public void constructorWithDdfHomeNotSet() {
        System.clearProperty(DDF_HOME_PROPERTY_NAME);
        new MigratableUtil();
    }

    @Test(expected = MigrationException.class)
    public void constructorWithDdfHomeEmpty() {
        System.setProperty(DDF_HOME_PROPERTY_NAME, "");
        new MigratableUtil();
    }

    @Test
    public void copyFile() throws IOException {
        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFile(VALID_SOURCE_FILE, VALID_DESTINATION_PATH, warnings);

        assertThat(warnings, is(empty()));
        verifyStatic();
        FileUtils.copyFile(VALID_SOURCE_FILE.toFile(),
                VALID_DESTINATION_PATH.resolve(VALID_SOURCE_FILE)
                        .toFile());
    }

    @Test(expected = IllegalArgumentException.class)
    public void copyFileNullSource() {
        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFile(null, VALID_DESTINATION_PATH, warnings);
    }

    @Test(expected = IllegalArgumentException.class)
    public void copyFileNullExportDirectory() {
        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFile(VALID_SOURCE_FILE, null, warnings);
    }

    @Test(expected = IllegalArgumentException.class)
    public void copyFileNullWarningsCollection() {
        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFile(VALID_SOURCE_FILE, VALID_DESTINATION_PATH, null);
    }

    @Test
    public void copyFileSourceIsAbsolutePath() throws IOException {
        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFile(ABSOLUTE_SOURCE_FILE, VALID_DESTINATION_PATH, warnings);

        assertWarnings("is absolute");
    }

    @Test
    public void copyFileSourceHasSymbolicLink() throws IOException {
        when(Files.isSymbolicLink(VALID_SOURCE_FILE)).thenReturn(true);

        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFile(VALID_SOURCE_FILE, VALID_DESTINATION_PATH, warnings);

        assertWarnings("contains a symbolic link");
    }

    @Test
    public void copyFileSourceNotUnderDdfHome() throws IOException {
        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFile(OUTSIDE_DDF_HOME_SOURCE_FILE, VALID_DESTINATION_PATH, warnings);

        assertWarnings(String.format("is outside [%s]", ddfHome));
    }

    @Test(expected = MigrationException.class)
    public void copyFileFails() throws IOException {
        doThrow(new IOException()).when(FileUtils.class);
        FileUtils.copyFile(any(File.class), any(File.class));

        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFile(VALID_SOURCE_FILE, VALID_DESTINATION_PATH, warnings);
    }

    @Test
    public void copyFileSourceOutsideDdfHomeDoesNotExist() throws IOException {
        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFile(OUTSIDE_DDF_HOME_SOURCE_PATH.resolve("invalid.properties"),
                VALID_DESTINATION_PATH,
                warnings);

        assertWarnings(String.format("does not exist or cannot be read", ddfHome));
    }

    @Test
    public void copyDirectory() throws IOException {
        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyDirectory(VALID_SOURCE_PATH, VALID_DESTINATION_PATH, warnings);

        assertThat(warnings, is(empty()));
        verifyStatic();
        FileUtils.copyDirectory(VALID_SOURCE_PATH.toFile(),
                VALID_DESTINATION_PATH.resolve(VALID_SOURCE_PATH)
                        .toFile());
    }

    @Test(expected = IllegalArgumentException.class)
    public void copyDirectoryNullSource() {
        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyDirectory(null, VALID_DESTINATION_PATH, warnings);
    }

    @Test(expected = IllegalArgumentException.class)
    public void copyDirectoryNullExportDirectory() {
        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyDirectory(VALID_SOURCE_PATH, null, warnings);
    }

    @Test(expected = IllegalArgumentException.class)
    public void copyDirectoryNullWarningsCollection() {
        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyDirectory(VALID_SOURCE_PATH, VALID_DESTINATION_PATH, null);
    }

    @Test
    public void copyDirectorySourceIsAbsolutePath() throws IOException {
        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyDirectory(ABSOLUTE_SOURCE_PATH, VALID_DESTINATION_PATH, warnings);

        assertWarnings("is absolute");
    }

    @Test
    public void copyDirectorySourceHasSymbolicLink() throws IOException {
        when(Files.isSymbolicLink(VALID_SOURCE_PATH)).thenReturn(true);

        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyDirectory(VALID_SOURCE_PATH, VALID_DESTINATION_PATH, warnings);

        assertWarnings("contains a symbolic link");
    }

    @Test
    public void copyDirectorySourceNotUnderDdfHome() throws IOException {
        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyDirectory(OUTSIDE_DDF_HOME_SOURCE_PATH,
                VALID_DESTINATION_PATH,
                warnings);

        assertWarnings(String.format("is outside [%s]", ddfHome));
    }

    @Test(expected = MigrationException.class)
    public void copyDirectoryFails() throws IOException {
        doThrow(new IOException()).when(FileUtils.class);
        FileUtils.copyDirectory(any(File.class), any(File.class));

        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyDirectory(VALID_SOURCE_PATH, VALID_DESTINATION_PATH, warnings);
    }

    @Test
    public void copyDirectorySourceOutsideDdfHomeDoesNotExist() throws IOException {
        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyDirectory(OUTSIDE_DDF_HOME_SOURCE_PATH.resolve("invalid"),
                VALID_DESTINATION_PATH,
                warnings);

        assertWarnings("does not exist or cannot be read");
    }

    @Test
    public void copyFileFromSystemPropertyValue() throws IOException {
        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFileFromSystemPropertyValue(SOURCE_PATH_PROPERTY_NAME,
                VALID_DESTINATION_PATH,
                warnings);

        assertThat(warnings, is(empty()));
        verifyStatic();
        FileUtils.copyFile(VALID_SOURCE_FILE.toFile(),
                VALID_DESTINATION_PATH.resolve(VALID_SOURCE_FILE)
                        .toFile());
    }

    @Test(expected = IllegalArgumentException.class)
    public void copyFileFromSystemPropertyWithNullValue() {
        System.clearProperty(SOURCE_PATH_PROPERTY_NAME);
        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFileFromSystemPropertyValue(SOURCE_PATH_PROPERTY_NAME,
                VALID_DESTINATION_PATH,
                warnings);
    }

    @Test(expected = IllegalArgumentException.class)
    public void copyFileFromSystemPropertyWithEmptyValue() {
        System.setProperty(SOURCE_PATH_PROPERTY_NAME, "");
        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFileFromSystemPropertyValue(SOURCE_PATH_PROPERTY_NAME,
                VALID_DESTINATION_PATH,
                warnings);
    }

    @Test(expected = IllegalArgumentException.class)
    public void copyFileFromSystemPropertyValueNullExportDirectory() {
        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFileFromSystemPropertyValue(SOURCE_PATH_PROPERTY_NAME, null, warnings);
    }

    @Test(expected = IllegalArgumentException.class)
    public void copyFileFromSystemPropertyValueNullWarningsCollection() {
        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFileFromSystemPropertyValue(SOURCE_PATH_PROPERTY_NAME,
                VALID_DESTINATION_PATH,
                null);
    }

    @Test
    public void copyFileFromSystemPropertyValueSourceIsAbsolutePath() throws IOException {
        System.setProperty(SOURCE_PATH_PROPERTY_NAME, ABSOLUTE_SOURCE_FILE.toString());

        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFileFromSystemPropertyValue(SOURCE_PATH_PROPERTY_NAME,
                VALID_DESTINATION_PATH,
                warnings);

        assertWarnings("is absolute");
    }

    @Test
    public void copyFileFromSystemPropertyValueSourceHasSymbolicLink() throws IOException {
        when(Files.isSymbolicLink(VALID_SOURCE_FILE)).thenReturn(true);

        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFileFromSystemPropertyValue(SOURCE_PATH_PROPERTY_NAME,
                VALID_DESTINATION_PATH,
                warnings);

        assertWarnings("contains a symbolic link");
    }

    @Test
    public void copyFileFromSystemPropertyValueSourceNotUnderDdfHome() throws IOException {
        System.setProperty(SOURCE_PATH_PROPERTY_NAME, OUTSIDE_DDF_HOME_SOURCE_FILE.toString());

        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFileFromSystemPropertyValue(SOURCE_PATH_PROPERTY_NAME,
                VALID_DESTINATION_PATH,
                warnings);

        assertWarnings(String.format("is outside [%s]", ddfHome));
    }

    @Test(expected = MigrationException.class)
    public void copyFileFromSystemPropertyValueFails() throws IOException {
        doThrow(new IOException()).when(FileUtils.class);
        FileUtils.copyFile(any(File.class), any(File.class));

        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFileFromSystemPropertyValue(SOURCE_PATH_PROPERTY_NAME,
                VALID_DESTINATION_PATH,
                warnings);
    }

    @Test
    public void copyFileFromSystemPropertyValueSourceOutsideDdfHomeDoesNotExist() {
        System.setProperty(SOURCE_PATH_PROPERTY_NAME,
                OUTSIDE_DDF_HOME_SOURCE_PATH.resolve("invalid.properties")
                        .toString());

        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFileFromSystemPropertyValue(SOURCE_PATH_PROPERTY_NAME,
                VALID_DESTINATION_PATH,
                warnings);
    }

    @Test
    public void copyFileFromJavaProperty() throws IOException {
        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFileFromJavaPropertyValue(JAVA_PROPERTIES_FILE,
                SOURCE_PATH_PROPERTY_NAME,
                VALID_DESTINATION_PATH,
                warnings);

        assertThat(warnings, is(empty()));
        verifyStatic();
        FileUtils.copyFile(VALID_SOURCE_FILE.toFile(),
                VALID_DESTINATION_PATH.resolve(VALID_SOURCE_FILE)
                        .toFile());
    }

    @Test(expected = MigrationException.class)
    public void copyFileFromJavaPropertyThatCannotBeRead() throws IOException {
        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFileFromJavaPropertyValue(Paths.get("nowhere",
                JAVA_PROPERTIES_FILE.toString()),
                SOURCE_PATH_PROPERTY_NAME,
                VALID_DESTINATION_PATH,
                warnings);
    }

    @Test
    public void copyFileFromJavaPropertyWithAnAbsolutePath() {
        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFileFromJavaPropertyValue(ddfHome.resolve(JAVA_PROPERTIES_FILE),
                SOURCE_PATH_PROPERTY_NAME,
                VALID_DESTINATION_PATH,
                warnings);
    }

    @Test(expected = IllegalArgumentException.class)
    public void copyFileFromJavaPropertyWithNullJavaPropertiesFile() throws IOException {
        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFileFromJavaPropertyValue(null,
                SOURCE_PATH_PROPERTY_NAME,
                VALID_DESTINATION_PATH,
                warnings);
    }

    @Test(expected = IllegalArgumentException.class)
    public void copyFileFromJavaPropertyWithNullValue() throws Exception {
        Properties properties = new Properties();
        properties.store(new FileOutputStream(ddfHome.resolve(JAVA_PROPERTIES_FILE)
                .toFile()), "Test");

        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFileFromJavaPropertyValue(JAVA_PROPERTIES_FILE,
                SOURCE_PATH_PROPERTY_NAME,
                VALID_DESTINATION_PATH,
                warnings);
    }

    @Test(expected = IllegalArgumentException.class)
    public void copyFileFromJavaPropertyWithEmptyValue() throws Exception {
        createJavaPropertiesFile("");

        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFileFromJavaPropertyValue(JAVA_PROPERTIES_FILE,
                SOURCE_PATH_PROPERTY_NAME,
                VALID_DESTINATION_PATH,
                warnings);
    }

    @Test(expected = IllegalArgumentException.class)
    public void copyFileFromJavaPropertyValueNullExportDirectory() {
        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFileFromJavaPropertyValue(JAVA_PROPERTIES_FILE,
                SOURCE_PATH_PROPERTY_NAME,
                null,
                warnings);
    }

    @Test(expected = IllegalArgumentException.class)
    public void copyFileFromJavaPropertyValueNullWarningsCollection() {
        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFileFromJavaPropertyValue(JAVA_PROPERTIES_FILE,
                SOURCE_PATH_PROPERTY_NAME,
                VALID_DESTINATION_PATH,
                null);
    }

    @Test
    public void copyFileFromJavaPropertyValueSourceIsAbsolutePath() throws IOException {
        createJavaPropertiesFile(ABSOLUTE_SOURCE_FILE.toString());

        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFileFromJavaPropertyValue(JAVA_PROPERTIES_FILE,
                SOURCE_PATH_PROPERTY_NAME,
                VALID_DESTINATION_PATH,
                warnings);

        assertWarnings("is absolute");
    }

    @Test
    public void copyFileFromJavaPropertyValueSourceHasSymbolicLink() throws IOException {
        when(Files.isSymbolicLink(VALID_SOURCE_FILE)).thenReturn(true);

        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFileFromJavaPropertyValue(JAVA_PROPERTIES_FILE,
                SOURCE_PATH_PROPERTY_NAME,
                VALID_DESTINATION_PATH,
                warnings);

        assertWarnings("contains a symbolic link");
    }

    @Test
    public void copyFileFromJavaPropertyValueSourceNotUnderDdfHome() throws IOException {
        createJavaPropertiesFile(OUTSIDE_DDF_HOME_SOURCE_FILE.toString());

        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFileFromJavaPropertyValue(JAVA_PROPERTIES_FILE,
                SOURCE_PATH_PROPERTY_NAME,
                VALID_DESTINATION_PATH,
                warnings);

        assertWarnings(String.format("is outside [%s]", ddfHome));
    }

    @Test(expected = MigrationException.class)
    public void copyFileFromJavaPropertyValueFails() throws IOException {
        doThrow(new IOException()).when(FileUtils.class);
        FileUtils.copyFile(any(File.class), any(File.class));

        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFileFromJavaPropertyValue(JAVA_PROPERTIES_FILE,
                SOURCE_PATH_PROPERTY_NAME,
                VALID_DESTINATION_PATH,
                warnings);
    }

    @Test
    public void copyFileFromJavaPropertyValueSourceOutsideDdfHomeDoesNotExist() throws IOException {
        createJavaPropertiesFile(OUTSIDE_DDF_HOME_SOURCE_PATH.resolve("invalid.properties")
                .toString());

        MigratableUtil migratableUtil = new MigratableUtil();
        migratableUtil.copyFileFromJavaPropertyValue(JAVA_PROPERTIES_FILE,
                SOURCE_PATH_PROPERTY_NAME,
                VALID_DESTINATION_PATH,
                warnings);

        assertWarnings("does not exist or cannot be read");
    }

    private void createTempPropertiesFiles() throws IOException {
        File etcPath = tempDir.newFolder(DDF_BASE_DIR.toString(), "etc");
        File propertiesFile = new File(etcPath + File.separator + CONFIG_FILE_NAME);
        propertiesFile.createNewFile();

        File outsidePath = tempDir.newFolder("outside");
        File outsidePropertiesFile = new File(outsidePath + File.separator + CONFIG_FILE_NAME);
        outsidePropertiesFile.createNewFile();

        createJavaPropertiesFile(VALID_SOURCE_FILE.toString());
    }

    private void createJavaPropertiesFile(String sourceDirectoryValue) throws IOException {
        Properties properties = new Properties();
        properties.setProperty(SOURCE_PATH_PROPERTY_NAME, sourceDirectoryValue);
        properties.store(new FileOutputStream(ddfHome.resolve(JAVA_PROPERTIES_FILE)
                .toFile()), "Test");
    }

    private void assertWarnings(String expectedReason) throws IOException {
        assertThat(warnings, hasSize(1));
        assertThat(warnings.iterator()
                .next()
                .getMessage(), containsString(expectedReason));
        verifyStatic(never());
        FileUtils.copyFile(any(File.class), any(File.class));
    }
}
