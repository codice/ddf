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

package ddf.services.schematron;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.validation.ValidationException;

public class SchematronValidationServiceTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private static File fileWithSpaces;

    @Before
    public void setup() throws IOException {
        URL src = SchematronValidationServiceTest.class.getClassLoader().getResource("dog_legs.sch");
        fileWithSpaces = Paths.get(testFolder.getRoot().getAbsolutePath()).resolve("folder with spaces").resolve("dog_legs.sch").toFile();
        FileUtils.copyURLToFile(src, fileWithSpaces);
    }

    @Test
    public void testSingleSchematron() throws ValidationException, IOException, SchematronInitializationException {
        getService("dog_legs.sch").validate(getMetacard("dog_4leg_3paw.xml"));
    }

    @Test(expected = ValidationException.class)
    public void testMultipleSchematron() throws ValidationException, IOException, SchematronInitializationException {
        getService("dog_legs.sch", "dog_paws.sch").validate(getMetacard("dog_4leg_3paw.xml"));
    }

    @Test(expected = ValidationException.class)
    public void testWithWarnings() throws ValidationException, IOException, SchematronInitializationException {
        getService("dog_legs.sch", "dog_paws.sch").validate(getMetacard("dog_4leg_3paw.xml"));
    }

    @Test(expected = ValidationException.class)
    public void testWithErrors() throws ValidationException, IOException, SchematronInitializationException {
        getService("dog_legs.sch", "dog_paws.sch").validate(getMetacard("dog_3leg_3paw.xml"));
    }

    @Test
    public void testWithWarningsAndSupressWarnings() throws ValidationException, IOException, SchematronInitializationException {
        SchematronValidationService service = getService(true, null, true, "dog_legs.sch", "dog_paws.sch");
        service.validate(getMetacard("dog_4leg_3paw.xml"));
    }

    @Test(expected = ValidationException.class)
    public void testWithErrorsAndSuppressWarnings() throws ValidationException, IOException, SchematronInitializationException {
        SchematronValidationService service = getService(true, null, true, "dog_legs.sch", "dog_paws.sch");
        service.validate(getMetacard("dog_3leg_3paw.xml"));
    }

    @Test
    public void testWithCorrectNamespace() throws ValidationException, IOException, SchematronInitializationException {
        SchematronValidationService service = getService(false, "doggy-namespace", true, "dog_legs.sch", "dog_paws.sch");
        service.validate(getMetacard("dog_4leg_4paw_namespace.xml"));
        assertThat(service.getSchematronReport(), is(notNullValue()));
    }

    @Test
    public void testWithIncorrectNamespace() throws ValidationException, IOException, SchematronInitializationException {
        SchematronValidationService service = getService(false, "this-is-the-wrong-namespace", true, "dog_legs.sch", "dog_paws.sch");
        service.validate(getMetacard("dog_4leg_4paw_namespace.xml"));
        assertThat(service.getSchematronReport(), is(nullValue()));
    }

    @Test(expected = SchematronInitializationException.class)
    public void testSchematronFileNotFound() throws ValidationException, IOException, SchematronInitializationException {
        SchematronValidationService service = getService(false, null, false, "definitely_does_not_exist.sch");
        service.validate(getMetacard("dog_4leg_4paw.xml"));
    }

    @Test(expected = SchematronValidationException.class)
    public void testDocumentFunctionWithIncorrectRelativePathAndValidName() throws ValidationException, IOException, SchematronInitializationException {
        getService("dog_name.sch").validate(getMetacard("dog_valid_name.xml"));
    }

    @Test(expected = SchematronValidationException.class)
    public void testDocumentFunctionWithIncorrectRelativePathAndInvalidName() throws ValidationException, IOException, SchematronInitializationException {
        getService("dog_name.sch").validate(getMetacard("dog_invalid_name.xml"));
    }

    @Test
    public void testDocumentFunctionWithCorrectRelativePathAndValidName() throws ValidationException, IOException, SchematronInitializationException {
        getService("dog_name_relative.sch").validate(getMetacard("dog_valid_name.xml"));
    }

    @Test(expected = SchematronValidationException.class)
    public void testDocumentFunctionWithCorrectRelativePathAndInvalidName() throws ValidationException, IOException, SchematronInitializationException {
        getService("dog_name_relative.sch").validate(getMetacard("dog_invalid_name.xml"));
    }

    @Test(expected = SchematronValidationException.class)
    public void testWithSpacesInPathToSchematronFile()  throws ValidationException, IOException, SchematronInitializationException {
        getService(false, null, false, fileWithSpaces.toString()).validate(getMetacard("dog_3leg_3paw.xml"));
    }

    private MetacardImpl getMetacard(String filename) throws IOException {
        String metadata = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(filename));
        MetacardImpl metacard = new MetacardImpl();
        metacard.setMetadata(metadata);
        return metacard;
    }

    private SchematronValidationService getService(String... schematronFiles) throws SchematronInitializationException {
        return getService(false, null, true, schematronFiles);
    }

    private SchematronValidationService getService(boolean suppressWarnings, String namespace, boolean useClassLoader, String... schematronFiles)
            throws SchematronInitializationException {
        SchematronValidationService service = new SchematronValidationService();
        service.setSuppressWarnings(suppressWarnings);
        service.setNamespace(namespace);

        ArrayList<String> schemaFiles = new ArrayList<>();
        for (String schematronFile : schematronFiles) {
            String path = schematronFile;
            if (useClassLoader) {
                path = SchematronValidationServiceTest.class.getClassLoader().getResource(schematronFile).getPath();
            }
            schemaFiles.add(path);
        }
        service.setSchematronFileNames(schemaFiles);

        service.init();
        return service;
    }

}
