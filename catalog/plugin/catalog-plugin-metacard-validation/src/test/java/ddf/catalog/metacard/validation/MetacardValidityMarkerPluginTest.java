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

package ddf.catalog.metacard.validation;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static ddf.catalog.metacard.validation.MetacardValidityMarkerPlugin.VALIDATION_ERRORS;
import static ddf.catalog.metacard.validation.MetacardValidityMarkerPlugin.VALIDATION_WARNINGS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.util.Describable;
import ddf.catalog.validation.MetacardValidator;
import ddf.catalog.validation.ValidationException;

public class MetacardValidityMarkerPluginTest {

    private static final String ID = "ID";

    private static final String SAMPLE = "sample";

    @Test
    public void testMarkMetacardValid()
            throws ValidationException, StopProcessingException, PluginExecutionException {
        MetacardValidityMarkerPlugin plugin = new MetacardValidityMarkerPlugin();
        List<MetacardValidator> metacardValidators = new ArrayList<>();
        metacardValidators.add(getMockPassingValidator());
        plugin.setMetacardValidators(metacardValidators);
        CreateRequest filteredRequest = plugin.process(getMockCreateRequest());
        assertThat(filteredRequest.getMetacards().get(0).getAttribute(VALIDATION_WARNINGS),
                is(nullValue(null)));
        assertThat(filteredRequest.getMetacards().get(0).getAttribute(VALIDATION_WARNINGS),
                is(nullValue(null)));
    }

    @Test
    public void testMarkMetacardInvalidErrors()
            throws ValidationException, StopProcessingException, PluginExecutionException {
        MetacardValidityMarkerPlugin plugin = new MetacardValidityMarkerPlugin();
        List<MetacardValidator> metacardValidators = new ArrayList<>();
        metacardValidators.add(getMockFailingValidatorWithErrors(ID));
        plugin.setMetacardValidators(metacardValidators);
        CreateRequest filteredRequest = plugin.process(getMockCreateRequest());
        assertThat(filteredRequest.getMetacards().get(0).getAttribute(VALIDATION_ERRORS).getValues()
                .contains(ID), is(true));
        assertThat(filteredRequest.getMetacards().get(0).getAttribute(VALIDATION_ERRORS).getValues()
                .size(), is(1));
        assertThat(filteredRequest.getMetacards().get(0).getAttribute(VALIDATION_WARNINGS),
                is(nullValue(null)));
    }

    @Test
    public void testMarkMetacardInvalidWarnings()
            throws ValidationException, StopProcessingException, PluginExecutionException {
        MetacardValidityMarkerPlugin plugin = new MetacardValidityMarkerPlugin();
        List<MetacardValidator> metacardValidators = new ArrayList<>();
        metacardValidators.add(getMockFailingValidatorWithWarnings(ID));
        plugin.setMetacardValidators(metacardValidators);
        CreateRequest filteredRequest = plugin.process(getMockCreateRequest());
        assertThat(filteredRequest.getMetacards().get(0).getAttribute(VALIDATION_ERRORS),
                is(nullValue(null)));
        assertThat(
                filteredRequest.getMetacards().get(0).getAttribute(VALIDATION_WARNINGS).getValues()
                        .contains(ID), is(true));
        assertThat(
                filteredRequest.getMetacards().get(0).getAttribute(VALIDATION_WARNINGS).getValues()
                        .size(), is(1));
    }

    @Test
    public void testMarkMetacardInvalidErrorsAndWarnings()
            throws ValidationException, StopProcessingException, PluginExecutionException {
        MetacardValidityMarkerPlugin plugin = new MetacardValidityMarkerPlugin();
        List<MetacardValidator> metacardValidators = new ArrayList<>();
        metacardValidators.add(getMockFailingValidatorWithErrorsAndWarnings(ID));
        plugin.setMetacardValidators(metacardValidators);
        CreateRequest filteredRequest = plugin.process(getMockCreateRequest());
        assertThat(filteredRequest.getMetacards().get(0).getAttribute(VALIDATION_ERRORS).getValues()
                .contains(ID), is(true));
        assertThat(filteredRequest.getMetacards().get(0).getAttribute(VALIDATION_ERRORS).getValues()
                .size(), is(1));
        assertThat(
                filteredRequest.getMetacards().get(0).getAttribute(VALIDATION_WARNINGS).getValues()
                        .contains(ID), is(true));
        assertThat(
                filteredRequest.getMetacards().get(0).getAttribute(VALIDATION_WARNINGS).getValues()
                        .size(), is(1));
    }

    @Test
    public void testUpdateProcess() throws StopProcessingException, PluginExecutionException {
        MetacardValidityMarkerPlugin plugin = new MetacardValidityMarkerPlugin();
        UpdateRequestImpl updateRequest = mock(UpdateRequestImpl.class);
        UpdateRequest returnedUpdateRequest = plugin.process(updateRequest);
        assertThat(returnedUpdateRequest, is(equalTo(updateRequest)));

    }

    @Test
    public void testDeleteProcess() throws StopProcessingException, PluginExecutionException {
        MetacardValidityMarkerPlugin plugin = new MetacardValidityMarkerPlugin();
        DeleteRequestImpl deleteRequest = mock(DeleteRequestImpl.class);
        DeleteRequest returnedDeleteRequest = plugin.process(deleteRequest);
        assertThat(returnedDeleteRequest, is(equalTo(deleteRequest)));
    }

    @Test
    public void testAllowMetacardEnforcedValidation()
            throws ValidationException, StopProcessingException, PluginExecutionException {
        String id = ID + "1";
        MetacardValidityMarkerPlugin plugin = new MetacardValidityMarkerPlugin();
        List<MetacardValidator> metacardValidators = new ArrayList<>();
        metacardValidators.add(getMockPassingValidatorWithId(id));
        plugin.setMetacardValidators(metacardValidators);
        List<String> enforcedMetacardValidators = new ArrayList<>();
        enforcedMetacardValidators.add(id);
        plugin.setEnforcedMetacardValidators(enforcedMetacardValidators);
        CreateRequest filteredRequest = plugin.process(getMockCreateRequest());
        assertThat(filteredRequest.getMetacards().isEmpty(), is(false));
    }

    @Test
    public void testRemoveMetacardEnforcedValidation()
            throws ValidationException, StopProcessingException, PluginExecutionException {
        String id = ID + "1";
        MetacardValidityMarkerPlugin plugin = new MetacardValidityMarkerPlugin();
        List<MetacardValidator> metacardValidators = new ArrayList<>();
        metacardValidators.add(getMockFailingValidatorWithId(id));
        plugin.setMetacardValidators(metacardValidators);
        List<String> enforcedMetacardValidators = new ArrayList<>();
        enforcedMetacardValidators.add(id);
        plugin.setEnforcedMetacardValidators(enforcedMetacardValidators);
        CreateRequest filteredRequest = plugin.process(getMockCreateRequest());
        assertThat(filteredRequest.getMetacards().isEmpty(), is(true));
    }

    @Test
    public void testAllowMetacardNoDescribable()
            throws ValidationException, StopProcessingException, PluginExecutionException {
        MetacardValidityMarkerPlugin plugin = new MetacardValidityMarkerPlugin();
        List<MetacardValidator> metacardValidators = new ArrayList<>();
        metacardValidators.add(getMockPassingValidatorNoDescribable());
        plugin.setMetacardValidators(metacardValidators);
        CreateRequest filteredRequest = plugin.process(getMockCreateRequest());
        assertThat(filteredRequest.getMetacards().isEmpty(), is(false));
    }

    @Test
    public void testRemoveMetacardNoDescribable()
            throws ValidationException, StopProcessingException, PluginExecutionException {
        MetacardValidityMarkerPlugin plugin = new MetacardValidityMarkerPlugin();
        List<MetacardValidator> metacardValidators = new ArrayList<>();
        metacardValidators.add(getMockFailingValidatorNoDescribable());
        plugin.setMetacardValidators(metacardValidators);
        CreateRequest filteredRequest = plugin.process(getMockCreateRequest());
        assertThat(filteredRequest.getMetacards().isEmpty(), is(false));

    }

    @Test
    public void testGetters() {
        assertThat(new MetacardValidityMarkerPlugin().getMetacardValidators(), is(nullValue(null)));
        assertThat(new MetacardValidityMarkerPlugin().getEnforcedMetacardValidators(),
                is(nullValue(null)));
    }

    private CreateRequest getMockCreateRequest() {
        List<Metacard> listMetacards = new ArrayList<>();
        listMetacards.add(new MetacardImpl());
        return new CreateRequestImpl(listMetacards);
    }

    private MetacardValidator getMockPassingValidator() throws ValidationException {
        return mock(MetacardValidator.class, withSettings().extraInterfaces(Describable.class));
    }

    private MetacardValidator getMockFailingValidatorWithErrors(String id)
            throws ValidationException {
        ValidationException validationException = mock(ValidationException.class);
        when(validationException.getErrors()).thenReturn(new ArrayList<>(Arrays.asList(SAMPLE)));
        MetacardValidator metacardValidator = mock(MetacardValidator.class,
                withSettings().extraInterfaces(Describable.class));
        when(((Describable) metacardValidator).getId()).thenReturn(id);
        doThrow(validationException).when(metacardValidator).validate(any(Metacard.class));
        return metacardValidator;
    }

    private MetacardValidator getMockFailingValidatorWithWarnings(String id)
            throws ValidationException {
        ValidationException validationException = mock(ValidationException.class);
        when(validationException.getWarnings()).thenReturn(new ArrayList<>(Arrays.asList(SAMPLE)));
        MetacardValidator metacardValidator = mock(MetacardValidator.class,
                withSettings().extraInterfaces(Describable.class));
        when(((Describable) metacardValidator).getId()).thenReturn(id);
        doThrow(validationException).when(metacardValidator).validate(any(Metacard.class));
        return metacardValidator;
    }

    private MetacardValidator getMockFailingValidatorWithErrorsAndWarnings(String id)
            throws ValidationException {
        ValidationException validationException = mock(ValidationException.class);
        when(validationException.getErrors()).thenReturn(new ArrayList<>(Arrays.asList(SAMPLE)));
        when(validationException.getWarnings()).thenReturn(new ArrayList<>(Arrays.asList(SAMPLE)));
        MetacardValidator metacardValidator = mock(MetacardValidator.class,
                withSettings().extraInterfaces(Describable.class));
        when(((Describable) metacardValidator).getId()).thenReturn(id);
        doThrow(validationException).when(metacardValidator).validate(any(Metacard.class));
        return metacardValidator;
    }

    private MetacardValidator getMockPassingValidatorWithId(String id) throws ValidationException {
        MetacardValidator metacardValidator = mock(MetacardValidator.class,
                withSettings().extraInterfaces(Describable.class));
        when(((Describable) metacardValidator).getId()).thenReturn(id);
        return metacardValidator;

    }

    private MetacardValidator getMockFailingValidatorWithId(String id) throws ValidationException {
        MetacardValidator metacardValidator = mock(MetacardValidator.class,
                withSettings().extraInterfaces(Describable.class));
        doThrow(mock(ValidationException.class)).when(metacardValidator)
                .validate(any(Metacard.class));
        when(((Describable) metacardValidator).getId()).thenReturn(id);
        return metacardValidator;
    }

    private MetacardValidator getMockPassingValidatorNoDescribable() throws ValidationException {
        MetacardValidator metacardValidator = mock(MetacardValidator.class);
        return metacardValidator;
    }

    private MetacardValidator getMockFailingValidatorNoDescribable() throws ValidationException {
        MetacardValidator metacardValidator = mock(MetacardValidator.class);
        doThrow(mock(ValidationException.class)).when(metacardValidator)
                .validate(any(Metacard.class));
        return metacardValidator;
    }
}
