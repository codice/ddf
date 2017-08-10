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
package org.codice.ddf.configuration.migration;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathUtils {
    // Forced to define it as non-static to simplify unit testing.
    private final Path ddfHome;

    /**
     * Creates a new path utility.
     *
     * @throws IOError if unable to determine ${ddf.home}
     */
    public PathUtils() {
        try {
            this.ddfHome = Paths.get(System.getProperty("ddf.home"))
                    .toRealPath();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    /**
     * Gets the path to ${ddf.home}.
     *
     * @return the path to ${ddf.home}
     */
    public Path getDDFHome() {
        return ddfHome;
    }

    /**
     * Checks if the specified path is located under ${ddf.home}.
     *
     * @param path the path to check if it is relative from ${ddf.home}
     * @return <code>true</code> if the path it located under ${ddf.home}; <code>false</code> otherwise
     */
    public boolean isRelativeToDDFHome(Path path) {
        return path.startsWith(ddfHome);
    }

    /**
     * Relativizes the specified path from ${ddf.home}.
     *
     * @param path the path to relativize from ${ddf.home}
     * @return the corresponding relativize path or <code>path</code> if it is not located under ${ddf.home}
     */
    public Path relativizeFromDDFHome(Path path) {
        if (isRelativeToDDFHome(path)) {
            return ddfHome.relativize(path);
        }
        return path;
    }

    /**
     * Resolves the specified path against ${ddf.home}.
     *
     * @param path the path to resolve against ${ddf.home}
     * @return the corresponding path resolved against ${ddf.home} if it is relative;
     * otherwise <code>path</code>
     */
    public Path resolveAgainstDDFHome(Path path) {
        return ddfHome.resolve(path);
    }

    /**
     * Resolves the specified path against ${ddf.home}.
     *
     * @param path the path to resolve against ${ddf.home}
     * @return the corresponding path resolved against ${ddf.home} if it is relative;
     * otherwise a path corresponding to <code>pathname</code>
     */
    public Path resolveAgainstDDFHome(String path) {
        return ddfHome.resolve(path);
    }
}
