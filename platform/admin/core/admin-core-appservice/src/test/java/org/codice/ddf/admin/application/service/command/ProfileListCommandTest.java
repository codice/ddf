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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.bundle.core.internal.BundleServiceImpl;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.internal.service.FeaturesServiceImpl;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.impl.ApplicationServiceImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ProfileListCommandTest {
    private ApplicationService applicationService;
    private FeaturesService featuresService;
    private BundleService bundleService;

    private PrintStream console;
    private ByteArrayOutputStream out;
    private ProfileListCommand profileListCommand;

    @Rule
    public TemporaryFolder ddfHome = new TemporaryFolder();

    private Path profilePath;

    @Before
    public void setup() throws IOException {
        ddfHome.newFolder("etc", "profiles");
        profilePath = Paths.get(ddfHome.getRoot().toString(), "etc", "profiles");
        Files.copy(this.getClass().getResourceAsStream("/profiles/devProfile.json"), Paths.get(profilePath.toAbsolutePath().toString(), "devProfile.json"));
        Files.copy(this.getClass().getResourceAsStream("/profiles/profileWithDuplicates.json"), Paths.get(profilePath.toAbsolutePath().toString(), "profileWithDuplicates.json"));

        this.applicationService = mock(ApplicationServiceImpl.class);
        this.featuresService = mock(FeaturesServiceImpl.class);
        this.bundleService = mock(BundleServiceImpl.class);

        out = new ByteArrayOutputStream();
        console = new PrintStream(out);

        profileListCommand = getProfileListCommand(profilePath, console);

        Feature feature = createMockFeature("standard");
        List<Dependency> deps = Arrays.asList(createMockDependency("app1"),
                createMockDependency("app2"), createMockDependency("app3"));
        when(feature.getDependencies()).thenReturn(deps);
        when(applicationService.getInstallationProfiles()).thenReturn(Collections.singletonList(feature));
    }

    @Test
    public void testProfileFilesExist() {
        profileListCommand.doExecute(applicationService, featuresService, bundleService);
        assertThat(out.toString().contains("standard"), is(true));
        assertThat(out.toString().contains("devProfile"), is(true));
        assertThat(out.toString().contains("profileWithDuplicates"), is(true));
    }

    @Test
    public void testProfileFilesNotExist() {
        profileListCommand = getProfileListCommand(Paths.get("foo"), console);
        profileListCommand.doExecute(applicationService, featuresService, bundleService);
        assertThat(out.toString().contains("standard"), is(true));
        assertThat(out.toString().contains("devProfile"), is(false));
        assertThat(out.toString().contains("profileWithDuplicates"), is(false));
    }

    @Test
    public void testRegularProfileNotExistAndProfileFilesExist() {
        when(applicationService.getInstallationProfiles()).thenReturn(Collections.emptyList());
        profileListCommand.doExecute(applicationService, featuresService, bundleService);
        assertThat(out.toString().contains("standard"), is(false));
        assertThat(out.toString().contains("devProfile"), is(true));
        assertThat(out.toString().contains("profileWithDuplicates"), is(true));
    }

    @Test
    public void testNoProfilesExist() {
        when(applicationService.getInstallationProfiles()).thenReturn(Collections.emptyList());
        profileListCommand = getProfileListCommand(Paths.get("foo"), console);
        profileListCommand.console = console;
        profileListCommand.doExecute(applicationService, featuresService, bundleService);
        assertThat(out.toString().contains("standard"), is(false));
        assertThat(out.toString().contains("devProfile"), is(false));
        assertThat(out.toString().contains("profileWithDuplicates"), is(false));
    }

    private Feature createMockFeature(String name) {
        Feature feature = mock(Feature.class);
        when(feature.getName()).thenReturn(name);
        when(feature.getVersion()).thenReturn("0.0.0");
        return feature;
    }

    private Dependency createMockDependency(String name) {
        Dependency dependency = mock(Dependency.class);
        when(dependency.getName()).thenReturn(name);
        when(dependency.getVersion()).thenReturn("0.0.0");
        return dependency;
    }

    private ProfileListCommand getProfileListCommand(Path profilePath, PrintStream console) {
        ProfileListCommand command = new ProfileListCommand();
        command.console = console;
        command.setProfilePath(profilePath);
        return command;
    }
}
