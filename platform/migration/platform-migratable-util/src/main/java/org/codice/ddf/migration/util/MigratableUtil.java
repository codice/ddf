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
package org.codice.ddf.migration.util;

import static org.apache.commons.lang.Validate.notEmpty;
import static org.apache.commons.lang.Validate.notNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Properties;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.migration.ExportMigrationException;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationWarning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for services that implement {@link org.codice.ddf.migration.Migratable}.
 * <p/>
 * <p>
 * <b>This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library.</b>
 * </p>
 */
public class MigratableUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(MigratableUtil.class);

    private static final String DDF_HOME_SYSTEM_PROP = "ddf.home";

    private final Path ddfHome;

    /**
     * Constructor.
     *
     * @throws MigrationException if the utility class fails to initialize
     */
    public MigratableUtil() throws MigrationException {
        this.ddfHome = Paths.get(getSystemProperty(DDF_HOME_SYSTEM_PROP));
    }

    /**
     * Copies a file to a destination directory. The file to copy must be a relative path under
     * {@code ddf.home}, and its path must not contain any symbolic link, otherwise the file will
     * not be copied and a {@link MigrationWarning} will be returned.
     *
     * @param sourceFile      relative path to the file to copy
     * @param exportDirectory root directory where the file will be copied. If the file'
     *                        relative path included any directories, those will be re-created
     *                        under this directory.
     * @param warnings        any warnings generated during this operation (e.g., source file
     *                        outside of {@code ddf.home}) will be added to this collection
     * @throws MigrationException thrown if a file system error prevents the file to be copied
     */
    public void copyFile(@NotNull Path sourceFile, @NotNull Path exportDirectory,
            @NotNull Collection<MigrationWarning> warnings) throws MigrationException {
        copy(sourceFile,
                exportDirectory,
                warnings,
                () -> isSourceMigratable(sourceFile,
                        (reason) -> new PathMigrationWarning(sourceFile, reason),
                        warnings));
    }

    public void copyFiles(@NotNull Path sourceDirectory, @NotNull PathMatcher filter,
            @NotNull Path exportDirectory, @NotNull Collection<MigrationWarning> warnings)
            throws MigrationException {
        if (isSourceMigratable(sourceDirectory,
                (reason) -> new PathMigrationWarning(sourceDirectory, reason),
                warnings)) {
            try (Stream<Path> stream = Files.list(sourceDirectory)) {
                stream.filter(filter::matches)
                        .forEach((sourceFile) -> copy(sourceFile,
                                exportDirectory,
                                warnings,
                                () -> isSourceMigratable(sourceFile,
                                        (reason) -> new PathMigrationWarning(sourceFile, reason),
                                        warnings)));
            } catch (IOException e) {
                throw new ExportMigrationException(String.format(
                        "Unable to list files in %s that match supplied filter.",
                        sourceDirectory), e);
            }
        }
    }

    /**
     * Recursively copies a directory to a destination directory. The directory to
     * copy must be a relative path under {@code ddf.home}, and its path must not contain any
     * symbolic link, otherwise the directory will not be copied and a {@link MigrationWarning}
     * will be returned.
     *
     * @param source          relative path to the directory to copy
     * @param exportDirectory root directory where the file will be copied. If the file'
     *                        relative path included any directories, those will be re-created
     *                        under this directory.
     * @param warnings        any warnings generated during this operation (e.g., source file outside
     *                        of {@code ddf.home}) will be added to this collection
     * @throws MigrationException thrown if file system error prevents the directory to be copied
     */
    public void copyDirectory(@NotNull Path source, @NotNull Path exportDirectory,
            @NotNull Collection<MigrationWarning> warnings) throws MigrationException {
        notNull(source, "Source cannot be null");
        notNull(exportDirectory, "Destination cannot be null");
        notNull(warnings, "Warning collection cannot be null");

        try {
            if (isSourceMigratable(source,
                    (reason) -> new PathMigrationWarning(source, reason),
                    warnings)) {
                FileUtils.copyDirectory(source.toFile(),
                        exportDirectory.resolve(source)
                                .toFile());
            }
        } catch (IOException e) {
            String message = String.format("Unable to copy [%s] to [%s].",
                    source.toString(),
                    exportDirectory.toString());
            LOGGER.info(message, e);
            throw new ExportMigrationException(message, e);
        }

    }

    /**
     * Copies a file, whose path is taken from a {@link System} property value, to a destination
     * directory. The file to copy must be a relative path under {@code ddf.home}, and its path
     * must not contain any symbolic link, otherwise the file will not be copied and a
     * {@link MigrationWarning} will be returned.
     *
     * @param systemProperty  name of the {@link System} property that contains the path to the
     *                        source file
     * @param exportDirectory root directory where the file will be copied. If the file'
     *                        relative path included any directories, those will be re-created
     *                        under this directory.
     * @param warnings        any warnings generated during this operation (e.g., source file outside
     *                        of {@code ddf.home}) will be added to this collection
     */
    public void copyFileFromSystemPropertyValue(@NotNull String systemProperty,
            @NotNull Path exportDirectory, @NotNull Collection<MigrationWarning> warnings)
            throws MigrationException {
        String source = System.getProperty(systemProperty);
        notEmpty(source,
                String.format("Source path property [%s] is invalid: [%s]",
                        systemProperty,
                        source));

        Path sourcePath = Paths.get(source);

        copy(sourcePath,
                exportDirectory,
                warnings,
                () -> isSourceMigratable(sourcePath,
                        (reason) -> new PathMigrationWarning(systemProperty, sourcePath, reason),
                        warnings));
    }

    /**
     * Copies a file, whose path is taken from the value a Java properties file, to a destination.
     * directory. The file to copy must be a relative path under {@code ddf.home}, and its path
     * must not contain any symbolic link, otherwise the file will not be copied and a
     * {@link MigrationWarning} will be returned.
     *
     * @param propertyFilePath path to the Java properties file that contains the path to the
     *                         source file to copy
     * @param javaProperty     name of the property inside the Java properties file that contains
     *                         the path to the source file
     * @param exportDirectory  path to the destination
     * @param warnings         any warnings generated during this operation (e.g., source file
     *                         outside of {@code ddf.home}) will be added to this collection
     */
    public void copyFileFromJavaPropertyValue(@NotNull Path propertyFilePath,
            @NotNull String javaProperty, @NotNull Path exportDirectory,
            @NotNull Collection<MigrationWarning> warnings) throws MigrationException {
        notNull(propertyFilePath, "Java properties file cannot be null");
        Properties properties = readPropertiesFile(ddfHome.resolve(propertyFilePath));
        String source = (String) properties.get(javaProperty);
        notEmpty(source,
                String.format("Source path property [%s] is invalid: [%s]", javaProperty, source));

        Path sourcePath = Paths.get(source);

        copy(sourcePath,
                exportDirectory,
                warnings,
                () -> isSourceMigratable(sourcePath,
                        (reason) -> new PathMigrationWarning(propertyFilePath,
                                javaProperty,
                                sourcePath,
                                reason),
                        warnings));
    }

    /**
     * Reads a java properties file and returns the value of the specified property.
     * 
     * @param propertyFilePath path to the Java properties file that contains the path to the
     *                         source file to copy
     * @param javaProperty     name of the property inside the Java properties file that contains
     *                         the path to the source file
     * @return                 the value of the property if found, null otherwise
     * @throws MigrationException thrown when the supplied properties file cannot be read
     */
    public String getJavaPropertyValue(@NotNull Path propertyFilePath, @NotNull String javaProperty) throws MigrationException {
        notNull(propertyFilePath, "Java properties file cannot be null");
        notNull(javaProperty, "Property cannot be null");
        Properties properties = readPropertiesFile(ddfHome.resolve(propertyFilePath));
        String value = (String) properties.get(javaProperty);
        return value;
    }

    private String getSystemProperty(String property) throws MigrationException {
        String prop = System.getProperty(property);

        if (StringUtils.isBlank(prop)) {
            String message = String.format("System property [%s] is not set", property);
            LOGGER.info(message);
            throw new ExportMigrationException(message);
        }

        return prop;
    }

    private void copy(Path sourceFile, Path exportDirectory, Collection<MigrationWarning> warnings,
            BooleanSupplier isSourceMigratable) throws MigrationException {
        notNull(sourceFile, "Source file cannot be null");
        notNull(exportDirectory, "Destination cannot be null");
        notNull(warnings, "Warning collection cannot be null");

        try {
            if (isSourceMigratable.getAsBoolean()) {
                FileUtils.copyFile(sourceFile.toFile(),
                        exportDirectory.resolve(sourceFile)
                                .toFile());
            }
        } catch (IOException e) {
            String message = String.format("Unable to copy [%s] to [%s]",
                    sourceFile.toString(),
                    exportDirectory.toString());
            LOGGER.info(message, e);
            throw new ExportMigrationException(message, e);
        }
    }

    private Properties readPropertiesFile(Path propertiesFile) throws MigrationException {
        Properties properties = new Properties();

        try (InputStream inputStream = new FileInputStream(propertiesFile.toString())) {
            properties.load(inputStream);
            return properties;
        } catch (IOException e) {
            String message = String.format("Unable to read properties file [%s]",
                    propertiesFile.toString());
            LOGGER.info(message, e);
            throw new ExportMigrationException(message, e);
        }
    }

    private boolean isSourceMigratable(Path source,
            Function<String, PathMigrationWarning> pathWarningBuilder,
            Collection<MigrationWarning> warnings) {

        if (source.isAbsolute()) {
            warnings.add(pathWarningBuilder.apply("is absolute"));
            return false;
        }

        if (Files.isSymbolicLink(source)) {
            warnings.add(pathWarningBuilder.apply("contains a symbolic link"));
            return false;
        }

        try {
            if (!ddfHome.resolve(source)
                    .toRealPath()
                    .startsWith(ddfHome.toRealPath())) {
                warnings.add(pathWarningBuilder.apply(String.format("is outside [%s]", ddfHome)));
                return false;
            }
        } catch (IOException e) {
            warnings.add(pathWarningBuilder.apply("does not exist or cannot be read"));
            return false;
        }

        return true;
    }
}
