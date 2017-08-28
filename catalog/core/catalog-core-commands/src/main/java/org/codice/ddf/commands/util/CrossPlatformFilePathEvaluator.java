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

package org.codice.ddf.commands.util;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.SystemUtils;

import ddf.security.common.audit.SecurityLogger;

/*
 * Handles converting given Windows file paths (whose backslashes get escaped) into something the
 * commands can handle.
 */
public class CrossPlatformFilePathEvaluator {

    public static File handlePath(String path) {
        //return java can find the file using the path
        File file = new File(path);
        if (file.exists()) {
            return file;
        }

        //If on Windows, check Windows file path
        if (SystemUtils.IS_OS_WINDOWS) {
            //look for drive letter
            if (path.contains(":")) {
                String[] splitPath = path.split(":");
                Optional<File> result = handleWindowsPath(splitPath[1],
                        new File(splitPath[0] + ":" + File.separatorChar));
                return result.orElse(file);
            } else { //relative path
                Optional<File> result = handleWindowsPath(path,
                        new File(System.getProperty("ddf.home")));
                return result.orElse(file);
            }
        }
        return file;
    }

    private static Optional<File> handleWindowsPath(String path, File pwd) {
        if (!pwd.exists()) {
            return Optional.empty();
        }

        List<File> fileList = Arrays.asList(pwd.listFiles());

        for (File file : fileList) {
            if (path.equals(file.getName())) {
                //found the exact match
                SecurityLogger.audit("Found file with path : {}", file.getAbsolutePath());
                return Optional.of(file);
            } else if (path.startsWith(file.getName())) {
                Optional<File> result = handleWindowsPath(path.replace(file.getName(), ""), file);
                if (result.isPresent()) {
                    return result;
                }
            }
        }
        return Optional.empty();
    }
}
