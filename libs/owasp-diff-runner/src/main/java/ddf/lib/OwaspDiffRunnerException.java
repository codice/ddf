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
package ddf.lib;

public class OwaspDiffRunnerException extends Exception {

    public static final String UNABLE_TO_RUN_MAVEN =
            "Unable to run maven on the following modules: ";

    public static final String UNABLE_TO_RETRIEVE_GIT_INFO =
            "Unable to retrieve branch name or committed changed files: ";

    public static final String UNABLE_TO_RETRIEVE_MAVEN_HOME = "Unable to retrieve maven home: ";

    public static final String UNABLE_TO_RETRIEVE_LOCAL_MAVEN_REPO =
            "Unable to retrieve local maven repo: ";

    public static final String FOUND_VULNERABILITIES =
            "Owasp-diff-runner found newly added vulnerabilities on the current committed branch."
                    + System.getProperty("line.separator")
                    + "For more information see the maven log or the dependency-check-report in the target folder of the owasp-ddf-runner module.";

    public OwaspDiffRunnerException(String message) {
        super(message);
    }

    public OwaspDiffRunnerException(String message, Throwable throwable) {
        super(message, throwable);
    }
}