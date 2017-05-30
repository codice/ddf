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
package org.codice.ddf.admin.application.service.command;

import static org.fusesource.jansi.Ansi.ansi;

import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.codice.ddf.admin.application.service.ApplicationService;

/**
 * Abstract Profile Command retrieves the {@link ApplicationService}, {@link FeaturesService},
 * and {@link BundleService}. Provides console styling and common functions for profile commands
 */
public abstract class AbstractProfileCommand implements Action {

    protected static final String PROFILE_EXTENSION = ".json";

    @Reference
    protected ApplicationService applicationService;

    @Reference
    protected FeaturesService featuresService;

    @Reference
    protected BundleService bundleService;

    protected PrintStream console = System.out;

    protected Path profilePath = Paths.get(System.getProperty("ddf.home"), "etc", "profiles");

    void setProfilePath(Path profilePath) {
        this.profilePath = profilePath;
    }

    @Override
    public Object execute() throws Exception {
        doExecute(applicationService, featuresService, bundleService);
        return null;
    }

    /**
     * Execute profile command operations.
     * This should be extended to provide functionality for any of the profile commands
     * @param applicationService {@link ApplicationService} used by the command implementation
     * @param featuresService {@link FeaturesService} used by the command implementation
     * @param bundleService {@link BundleService} used by the command implementation
     */
    protected abstract void doExecute(ApplicationService applicationService,
                            FeaturesService featuresService,
                            BundleService bundleService)
            throws Exception;

    /**
     * Console output styling for a section heading
     * @param heading Section Name
     */
    public void printSectionHeading(String heading) {
        console.print(ansi()
                .fgBrightDefault().bold().a(heading)
                .newline()
                .reset());
    }

    /**
     * Console output styling for a successful operation for a given item
     * formatted as <code>Message: item</code>
     * Note: <code>:</code> will not be added to the message automatically
     * Message will be blue, item will be yellow
     * @param message message associated with the operation
     * @param item item name
     */
    public void printItemStatusPending(String message, String item) {
        console.print(ansi()
                .fgBlue().a(message)
                .fgYellow().a(item)
                .newline()
                .reset()
        );
    }

    /**
     * Console output styling for a successful operation for a given item
     * formatted as <code>Message: item</code>
     * Note: <code>:</code> will not be added to the message automatically
     * Message will be blue, item will be green
     * @param message message associated with the operation
     * @param item item name
     */
    public void printItemStatusSuccess(String message, String item) {
        console.print(ansi()
                .fgBlue().a(message)
                .fgGreen().a(item)
                .newline()
                .reset()
        );
    }

    /**
     * Console output styling for a successful operation for a given item
     * formatted as <code>Message: item</code>
     * Note: <code>:</code> will not be added to the message automatically
     * Message will be blue, item will be red
     * @param message message associated with the operation
     * @param item item name
     */
    public void printItemStatusFailure(String message, String item) {
        console.print(ansi()
                .fgBlue().a(message)
                .fgRed().a(item)
                .newline()
                .reset()
        );
    }

    /**
     * Console output styling for an error message
     * Message will be red
     * @param message error message
     */
    public void printError(String message) {
        console.print(ansi()
                .fgRed().a(message)
                .newline()
                .reset());
    }

    /**
     * Console output styling for a success message
     * Message will be green
     * @param message success message
     */
    public void printSuccess(String message) {
        console.print(ansi()
                .fgGreen().a(message)
                .newline()
                .reset());
    }
}
