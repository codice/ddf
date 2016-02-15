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

import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.validation.ValidationException;

public class SchematronValidationServiceTest {

    @Test
    public void testSingleSchematron()
            throws ValidationException, IOException, SchematronInitializationException {
        SchematronValidationService service = getService("dog_legs.sch");
        MetacardImpl metacard = getMetacard("dog_4leg_3paw.xml");
        service.validate(metacard);
    }

    @Test(expected = ValidationException.class)
    public void testMultipleSchematron()
            throws ValidationException, IOException, SchematronInitializationException {
        SchematronValidationService service = getService("dog_legs.sch", "dog_paws.sch");
        MetacardImpl metacard = getMetacard("dog_4leg_3paw.xml");
        service.validate(metacard);
    }

    @Test(expected = ValidationException.class)
    public void testWithWarnings()
            throws ValidationException, IOException, SchematronInitializationException {
        SchematronValidationService service = getService("dog_legs.sch", "dog_paws.sch");
        MetacardImpl metacard = getMetacard("dog_4leg_3paw.xml");
        service.validate(metacard);
    }

    @Test(expected = ValidationException.class)
    public void testWithErrors()
            throws ValidationException, IOException, SchematronInitializationException {
        SchematronValidationService service = getService("dog_legs.sch", "dog_paws.sch");
        MetacardImpl metacard = getMetacard("dog_3leg_3paw.xml");
        service.validate(metacard);
    }

    @Test
    public void testWithWarningsAndSupressWarnings()
            throws ValidationException, IOException, SchematronInitializationException {
        SchematronValidationService service = getService(true,
                null,
                true,
                "dog_legs.sch",
                "dog_paws.sch");
        MetacardImpl metacard = getMetacard("dog_4leg_3paw.xml");
        service.validate(metacard);
    }

    @Test(expected = ValidationException.class)
    public void testWithErrorsAndSuppressWarnings()
            throws ValidationException, IOException, SchematronInitializationException {
        SchematronValidationService service = getService(true,
                null,
                true,
                "dog_legs.sch",
                "dog_paws.sch");
        MetacardImpl metacard = getMetacard("dog_3leg_3paw.xml");
        service.validate(metacard);
    }

    @Test
    public void testWithCorrectNamespace()
            throws ValidationException, IOException, SchematronInitializationException {
        SchematronValidationService service = getService(false,
                "doggy-namespace",
                true,
                "dog_legs.sch",
                "dog_paws.sch");
        MetacardImpl metacard = getMetacard("dog_4leg_4paw_namespace.xml");
        service.validate(metacard);
        assertThat(service.getSchematronReport(), is(notNullValue()));
    }

    @Test
    public void testWithWrongNamespace()
            throws ValidationException, IOException, SchematronInitializationException {
        SchematronValidationService service = getService(false,
                "this-is-the-wrong-namespace",
                true,
                "dog_legs.sch",
                "dog_paws.sch");
        MetacardImpl metacard = getMetacard("dog_4leg_4paw_namespace.xml");
        service.validate(metacard);
        assertThat(service.getSchematronReport(), is(nullValue()));
    }

    @Test(expected = SchematronInitializationException.class)
    public void testSchematronFileNotFound()
            throws ValidationException, IOException, SchematronInitializationException {
        SchematronValidationService service = getService(false,
                null,
                false,
                "definitely_does_not_exist.sch");
        MetacardImpl metacard = getMetacard("dog_4leg_4paw.xml");
        service.validate(metacard);
    }

    @Test
    public void testWithValidNameReferencingXmlList()
            throws ValidationException, IOException, SchematronInitializationException {
        SchematronValidationService service = getService("dog_name.sch");
        MetacardImpl metacard = getMetacard("dog_valid_name.xml");
        service.validate(metacard);
    }

    @Test(expected = SchematronValidationException.class)
    public void testWithInvalidNameReferencingXmlList()
            throws ValidationException, IOException, SchematronInitializationException {
        SchematronValidationService service = getService("dog_name.sch");
        MetacardImpl metacard = getMetacard("dog_invalid_name.xml");
        service.validate(metacard);
    }

    private MetacardImpl getMetacard(String filename) throws IOException {
        String metadata = IOUtils.toString(getClass().getClassLoader()
                .getResourceAsStream(filename));
        MetacardImpl metacard = new MetacardImpl();
        metacard.setMetadata(metadata);
        return metacard;
    }

    private SchematronValidationService getService(String... schematronFiles)
            throws SchematronInitializationException {
        return getService(false, null, true, schematronFiles);
    }

    private SchematronValidationService getService(boolean suppressWarnings, String namespace,
            boolean useClassLoader, String... schematronFiles)
            throws SchematronInitializationException {
        SchematronValidationService service = new SchematronValidationService();
        service.setSuppressWarnings(suppressWarnings);
        service.setNamespace(namespace);

        ArrayList<String> schemaFiles = new ArrayList<>();
        for (String schematronFile : schematronFiles) {
            if (useClassLoader) {
                schemaFiles.add(SchematronValidationServiceTest.class.getClassLoader()
                        .getResource(schematronFile)
                        .getPath());
            } else {
                schemaFiles.add(schematronFile);
            }
        }
        service.setSchematronFileNames(schemaFiles);

        service.init();
        return service;
    }

}
