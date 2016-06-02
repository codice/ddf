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
 **/
package org.codice.ddf.platform.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InputValidation {

    private static final Logger LOGGER = LoggerFactory.getLogger(InputValidation.class);

    private static final String DEFAULT_EXTENSION = ".bin";

    private static final String DEFAULT_FILE = "file" + DEFAULT_EXTENSION;

    private static final List<String> BAD_FILES = Arrays.asList(System.getProperty("bad.files")
            .split(","));

    private static final List<String> BAD_FILE_EXTENSIONS = Arrays.asList(
            System.getProperty("bad.file.extensions")
                    .split(","));

    private static final List<String> BAD_MIME_TYPES = Arrays.asList(
            System.getProperty("bad.mime.types")
                    .split(","));

    private static final Pattern BAD_CHAR_PATTERN = Pattern.compile("[^a-z0-9.-]");

    private static final Pattern BAD_PATH_PATTERN = Pattern.compile("\\.\\.");

    private InputValidation() {

    }

    /**
     * Removes disallowed characters and file extensions from the filename.
     *
     * @param filename - filename to sanitize
     * @return sanitized filename
     */
    public static String sanitizeFilename(String filename) {
        Path path = Paths.get(filename.toLowerCase());
        filename = path.getFileName()
                .toString();
        if (BAD_FILES.contains(filename)) {
            filename = DEFAULT_FILE;
        }
        filename = BAD_CHAR_PATTERN.matcher(filename)
                .replaceAll("_");
        filename = BAD_PATH_PATTERN.matcher(filename)
                .replaceAll("_");
        for (String extension : BAD_FILE_EXTENSIONS) {
            if (filename.contains(extension)) {
                filename = filename.replace(extension, DEFAULT_EXTENSION);
            }
        }
        if (filename.charAt(0) == '.') {
            if (filename.length() == 1) {
                filename = DEFAULT_FILE;
            } else {
                filename = filename.substring(1);
            }
        }
        return filename;
    }

    /**
     * Checks for mime types that have been disallowed by the system.
     *
     * @param mimetype
     * @return true if the mime type is acceptable
     */
    public static boolean checkForClientSideVulnerableMimeType(String mimetype) {
        mimetype = mimetype.toLowerCase();
        for (String type : BAD_MIME_TYPES) {
            if (mimetype.contains(type)) {
                LOGGER.debug("Mime type {} is flagged as client side vulnerable.", mimetype);
                return false;
            }
        }
        return true;
    }
}
