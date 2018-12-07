/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.metacard.validation;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.theInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Validation;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.Request;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.util.Describable;
import ddf.catalog.validation.MetacardValidator;
import ddf.catalog.validation.ValidationException;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

public class MetacardValidityMarkerPluginTest {
  private static final String ID = "ID";

  private static final String SAMPLE_WARNING = "sample warning";

  private static final String SAMPLE_ERROR = "sample error";

  private static final String FIRST = "first";

  private static final String SECOND = "second";

  private static final String VALID_TAG = "VALID";

  private static final String INVALID_TAG = "INVALID";

  private static final Map<String, Serializable> PROPERTIES =
      Collections.singletonMap("foo", "bar");

  private static final Set<String> DESTINATIONS = Sets.newHashSet("source 1", "source 2");

  private final Consumer<Attribute> expectNone =
      attribute -> assertThat(attribute, is(nullValue()));

  private final Consumer<Attribute> expectError =
      attribute -> {
        assertThat(attribute.getValues(), hasSize(1));
        assertThat(attribute.getValues(), contains(SAMPLE_ERROR));
      };

  private final Consumer<Attribute> expectWarning =
      attribute -> {
        assertThat(attribute.getValues(), hasSize(1));
        assertThat(attribute.getValues(), contains(SAMPLE_WARNING));
      };

  private MetacardValidityMarkerPlugin plugin;

  private List<MetacardValidator> metacardValidators;

  private List<String> enforcedMetacardValidators;

  @Before
  public void setUp() {
    metacardValidators = new ArrayList<>();
    enforcedMetacardValidators = new ArrayList<>();
    plugin = new MetacardValidityMarkerPlugin();
    plugin.setMetacardValidators(metacardValidators);
    plugin.setEnforcedMetacardValidators(enforcedMetacardValidators);
  }

  private List<Metacard> getUpdatedMetacards(UpdateRequest updateRequest) {
    return updateRequest
        .getUpdates()
        .stream()
        .map(Map.Entry::getValue)
        .collect(Collectors.toList());
  }

  private void verifyCreate(
      CreateRequest originalRequest,
      Consumer<Attribute> errorExpectation,
      Consumer<Attribute> warningExpectation,
      String expectedTag)
      throws PluginExecutionException, StopProcessingException {
    CreateRequest filteredRequest = plugin.process(originalRequest);
    List<Metacard> filteredMetacards = filteredRequest.getMetacards();

    verifyMetacardErrorsAndWarnings(
        filteredMetacards, errorExpectation, warningExpectation, expectedTag);
    verifyRequestPropertiesUnchanged(originalRequest, filteredRequest);
  }

  private void verifyUpdate(
      UpdateRequest originalRequest,
      Consumer<Attribute> errorExpectation,
      Consumer<Attribute> warningExpectation,
      String expectedTag)
      throws PluginExecutionException, StopProcessingException {
    UpdateRequest filteredRequest = plugin.process(originalRequest);
    List<Metacard> filteredMetacards = getUpdatedMetacards(filteredRequest);

    verifyMetacardErrorsAndWarnings(
        filteredMetacards, errorExpectation, warningExpectation, expectedTag);
    verifyRequestPropertiesUnchanged(originalRequest, filteredRequest);
  }

  private void verifyMetacardErrorsAndWarnings(
      List<Metacard> filteredMetacards,
      Consumer<Attribute> errorExpectation,
      Consumer<Attribute> warningExpectation,
      String expectedTag) {
    assertThat(filteredMetacards, hasSize(2));

    filteredMetacards.forEach(
        metacard -> {
          errorExpectation.accept(metacard.getAttribute(Validation.VALIDATION_ERRORS));
          warningExpectation.accept(metacard.getAttribute(Validation.VALIDATION_WARNINGS));
          assertThat(metacard.getTags(), hasItem(expectedTag));
        });
  }

  private void verifyRequestPropertiesUnchanged(Request original, Request processed) {
    assertThat(processed.getProperties(), is(original.getProperties()));
    assertThat(processed.getStoreIds(), is(original.getStoreIds()));
  }

  @Test
  public void testMultipleValidationTagsValid()
      throws StopProcessingException, PluginExecutionException {
    metacardValidators.add(getMockPassingValidator());
    CreateRequest request = getMockCreateRequest();
    Metacard m1 = request.getMetacards().get(0);

    Set<String> tags = m1.getTags();
    tags.add(INVALID_TAG);
    m1.setAttribute(new AttributeImpl(Metacard.TAGS, new ArrayList<String>(tags)));

    CreateRequest filteredRequest = plugin.process(request);
    assertThat(filteredRequest.getMetacards().get(0).getTags(), hasItem(VALID_TAG));
    assertThat(filteredRequest.getMetacards().get(0).getTags(), not(hasItem(INVALID_TAG)));
  }

  @Test
  public void testMultipleValidationTagsInvalid()
      throws StopProcessingException, PluginExecutionException, ValidationException {
    metacardValidators.add(getMockFailingValidatorWithErrorsAndWarnings());
    CreateRequest request = getMockCreateRequest();
    Metacard m1 = request.getMetacards().get(0);

    Set<String> tags = m1.getTags();
    tags.add(VALID_TAG);
    m1.setAttribute(new AttributeImpl(Metacard.TAGS, new ArrayList<String>(tags)));

    CreateRequest filteredRequest = plugin.process(request);
    assertThat(filteredRequest.getMetacards().get(0).getTags(), hasItem(INVALID_TAG));
    assertThat(filteredRequest.getMetacards().get(0).getTags(), not(hasItem(VALID_TAG)));
  }

  @Test
  public void testMarkMetacardValid() throws StopProcessingException, PluginExecutionException {
    metacardValidators.add(getMockPassingValidator());
    verifyCreate(getMockCreateRequest(), expectNone, expectNone, VALID_TAG);
    verifyUpdate(getMockUpdateRequest(), expectNone, expectNone, VALID_TAG);
  }

  @Test
  public void testMarkMetacardInvalidErrors()
      throws ValidationException, StopProcessingException, PluginExecutionException {
    metacardValidators.add(getMockFailingValidatorWithErrors());
    verifyCreate(getMockCreateRequest(), expectError, expectNone, INVALID_TAG);
    verifyUpdate(getMockUpdateRequest(), expectError, expectNone, INVALID_TAG);
  }

  @Test
  public void testMarkMetacardInvalidWarnings()
      throws ValidationException, StopProcessingException, PluginExecutionException {
    metacardValidators.add(getMockFailingValidatorWithWarnings());
    verifyCreate(getMockCreateRequest(), expectNone, expectWarning, INVALID_TAG);
    verifyUpdate(getMockUpdateRequest(), expectNone, expectWarning, INVALID_TAG);
  }

  @Test
  public void testMarkMetacardInvalidErrorsAndWarnings()
      throws ValidationException, StopProcessingException, PluginExecutionException {
    metacardValidators.add(getMockFailingValidatorWithErrorsAndWarnings());
    verifyCreate(getMockCreateRequest(), expectError, expectWarning, INVALID_TAG);
    verifyUpdate(getMockUpdateRequest(), expectError, expectWarning, INVALID_TAG);
  }

  @Test
  public void testPreExistingMetacardErrors()
      throws ValidationException, StopProcessingException, PluginExecutionException {
    metacardValidators.add(getMockPassingValidator());

    CreateRequestImpl request =
        new CreateRequestImpl(
            metacardsWithPreExistingAttributes(true, false), PROPERTIES, DESTINATIONS);
    verifyCreate(request, expectError, expectNone, INVALID_TAG);
  }

  @Test
  public void testPreExistingMetacardWarnings()
      throws ValidationException, StopProcessingException, PluginExecutionException {
    metacardValidators.add(getMockPassingValidator());

    CreateRequestImpl request =
        new CreateRequestImpl(
            metacardsWithPreExistingAttributes(false, true), PROPERTIES, DESTINATIONS);
    verifyCreate(request, expectNone, expectWarning, INVALID_TAG);
  }

  @Test
  public void testPreExistingMetacardErrorsAndWarnings()
      throws ValidationException, StopProcessingException, PluginExecutionException {
    metacardValidators.add(getMockPassingValidator());

    CreateRequestImpl request =
        new CreateRequestImpl(
            metacardsWithPreExistingAttributes(true, true), PROPERTIES, DESTINATIONS);
    verifyCreate(request, expectError, expectWarning, INVALID_TAG);
  }

  @Test
  public void testPreExistingMetacardErrorsAndWarningsNoDuplicates()
      throws ValidationException, StopProcessingException, PluginExecutionException {
    metacardValidators.add(getMockFailingValidatorWithErrorsAndWarnings());

    CreateRequestImpl request =
        new CreateRequestImpl(
            metacardsWithPreExistingAttributes(true, true), PROPERTIES, DESTINATIONS);
    verifyCreate(request, expectError, expectWarning, INVALID_TAG);
  }

  @Test
  public void testProcessDelete() throws StopProcessingException, PluginExecutionException {
    DeleteRequestImpl deleteRequest = mock(DeleteRequestImpl.class);
    DeleteRequest returnedDeleteRequest = plugin.process(deleteRequest);
    assertThat(returnedDeleteRequest, is(theInstance(deleteRequest)));
  }

  private void verifyEnforcedCreate(
      CreateRequest originalRequest, List<Metacard> expectedAllowedMetacards)
      throws PluginExecutionException, StopProcessingException {
    CreateRequest filteredRequest = plugin.process(originalRequest);

    List<Metacard> filteredMetacards = filteredRequest.getMetacards();
    verifyAllowedMetacards(filteredMetacards, expectedAllowedMetacards);

    verifyRequestPropertiesUnchanged(originalRequest, filteredRequest);
  }

  private void verifyEnforcedUpdate(
      UpdateRequest originalRequest, List<Metacard> expectedAllowedMetacards)
      throws PluginExecutionException, StopProcessingException {
    UpdateRequest filteredRequest = plugin.process(originalRequest);

    List<Metacard> filteredMetacards = getUpdatedMetacards(filteredRequest);
    verifyAllowedMetacards(filteredMetacards, expectedAllowedMetacards);

    verifyRequestPropertiesUnchanged(originalRequest, filteredRequest);
  }

  private void verifyAllowedMetacards(
      List<Metacard> filteredMetacards, List<Metacard> expectedAllowedMetacards) {
    Matcher[] metacardMatchers =
        expectedAllowedMetacards.stream().map(Matchers::theInstance).toArray(Matcher[]::new);
    assertThat(filteredMetacards, contains(metacardMatchers));
  }

  @Test
  public void testMetacardPassesEnforcedValidators()
      throws StopProcessingException, PluginExecutionException {
    metacardValidators.add(getMockEnforcedPassingValidatorWithId(ID));
    enforcedMetacardValidators.add(ID);

    CreateRequest createRequest = getMockCreateRequest();
    List<Metacard> createdMetacards = createRequest.getMetacards();
    verifyEnforcedCreate(createRequest, createdMetacards);

    UpdateRequest updateRequest = getMockUpdateRequest();
    List<Metacard> updatedMetacards = getUpdatedMetacards(updateRequest);
    verifyEnforcedUpdate(updateRequest, updatedMetacards);
  }

  @Test
  public void testMetacardFailsEnforcedValidator()
      throws ValidationException, StopProcessingException, PluginExecutionException {
    metacardValidators.add(getMockEnforcedFailingValidatorWithId(ID));
    enforcedMetacardValidators.add(ID);

    CreateRequest createRequest = getMockCreateRequest();
    List<Metacard> createdMetacards = createRequest.getMetacards();
    verifyEnforcedCreate(createRequest, createdMetacards.subList(1, createdMetacards.size()));

    UpdateRequest updateRequest = getMockUpdateRequest();
    List<Metacard> updatedMetacards = getUpdatedMetacards(updateRequest);
    verifyEnforcedUpdate(updateRequest, updatedMetacards.subList(1, updatedMetacards.size()));
  }

  @Test
  public void testMetacardPassesEnforcedValidatorsNoDescribable()
      throws StopProcessingException, PluginExecutionException {
    MetacardValidator mockValidator = getMockPassingValidatorNoDescribable();
    metacardValidators.add(mockValidator);
    enforcedMetacardValidators.add(mockValidator.getClass().getCanonicalName());

    CreateRequest createRequest = getMockCreateRequest();
    List<Metacard> createdMetacards = createRequest.getMetacards();
    verifyEnforcedCreate(createRequest, createdMetacards);

    UpdateRequest updateRequest = getMockUpdateRequest();
    List<Metacard> updatedMetacards = getUpdatedMetacards(updateRequest);
    verifyEnforcedUpdate(updateRequest, updatedMetacards);
  }

  @Test
  public void testMetacardFailsEnforcedValidatorNoDescribable()
      throws ValidationException, StopProcessingException, PluginExecutionException {
    MetacardValidator mockValidator = getMockFailingValidatorNoDescribable();
    metacardValidators.add(mockValidator);
    enforcedMetacardValidators.add(mockValidator.getClass().getCanonicalName());

    CreateRequest createRequest = getMockCreateRequest();
    List<Metacard> createdMetacards = createRequest.getMetacards();
    verifyEnforcedCreate(createRequest, createdMetacards.subList(1, createdMetacards.size()));

    UpdateRequest updateRequest = getMockUpdateRequest();
    List<Metacard> updatedMetacards = getUpdatedMetacards(updateRequest);
    verifyEnforcedUpdate(updateRequest, updatedMetacards.subList(1, updatedMetacards.size()));
  }

  @Test
  public void testGetters() {
    assertThat(plugin.getMetacardValidators(), is(empty()));
    assertThat(plugin.getEnforcedMetacardValidators(), is(empty()));
  }

  @Test
  public void testEnforceWarningsOnly() throws Exception {
    markerPluginResponseHelper(getMockFailingValidatorWithWarnings(), false, true, 0);
    markerPluginResponseHelper(getMockFailingValidatorWithErrors(), false, true, 2);
  }

  @Test
  public void testEnforceErrorsOnly() throws Exception {
    markerPluginResponseHelper(getMockFailingValidatorWithErrors(), true, false, 0);
    markerPluginResponseHelper(getMockFailingValidatorWithWarnings(), true, false, 2);
  }

  @Test
  public void testEnforceErrorsAndWarnings() throws Exception {
    markerPluginResponseHelper(getMockFailingValidatorWithErrorsAndWarnings(), true, true, 0);
  }

  @Test
  public void testNoEnforcement() throws Exception {
    markerPluginResponseHelper(getMockFailingValidatorWithErrorsAndWarnings(), false, false, 2);
    markerPluginResponseHelper(getMockFailingValidatorWithWarnings(), false, false, 2);
    markerPluginResponseHelper(getMockFailingValidatorWithErrors(), false, false, 2);
  }

  @Test
  public void testTrackingErrors() throws Exception {
    testTrackingHelper(getMockFailingValidatorWithErrors(), true, false);
  }

  @Test
  public void testTrackingWarnings() throws Exception {
    testTrackingHelper(getMockFailingValidatorWithWarnings(), false, true);
  }

  @Test
  public void testTrackingErrorsAndWarnings() throws Exception {
    testTrackingHelper(getMockFailingValidatorWithErrorsAndWarnings(), true, true);
  }

  @Test
  public void testTrackingClean() throws Exception {
    testTrackingHelper(getMockPassingValidator(), false, false);
  }

  private void testTrackingHelper(
      MetacardValidator validator, boolean expectErrors, boolean expectWarnings) throws Exception {
    List<Metacard> metacards = markerPluginResponseHelper(validator, false, false, 2);
    for (Metacard m : metacards) {
      if (expectErrors) {
        assertThat(
            m.getAttribute(Validation.FAILED_VALIDATORS_ERRORS).getValues().isEmpty(), is(false));
      }
      if (expectWarnings) {
        assertThat(
            m.getAttribute(Validation.FAILED_VALIDATORS_WARNINGS).getValues().isEmpty(), is(false));
      }
    }
  }

  private Metacard metacardWithTitle(String title) {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setTitle(title);
    return metacard;
  }

  private List<Metacard> metacardsWithPreExistingAttributes(boolean error, boolean warning) {
    Metacard metacard1 = metacardWithTitle(FIRST);
    Metacard metacard2 = metacardWithTitle(SECOND);
    AttributeImpl attribute;

    if (error) {
      attribute = new AttributeImpl(Validation.VALIDATION_ERRORS, SAMPLE_ERROR);
      metacard1.setAttribute(attribute);
      metacard2.setAttribute(attribute);
    }

    if (warning) {
      attribute = new AttributeImpl(Validation.VALIDATION_WARNINGS, SAMPLE_WARNING);
      metacard1.setAttribute(attribute);
      metacard2.setAttribute(attribute);
    }

    return Lists.newArrayList(metacard1, metacard2);
  }

  private CreateRequest getMockCreateRequest() {
    List<Metacard> listMetacards =
        Lists.newArrayList(metacardWithTitle(FIRST), metacardWithTitle(SECOND));
    return new CreateRequestImpl(listMetacards, PROPERTIES, DESTINATIONS);
  }

  private UpdateRequest getMockUpdateRequest() {
    List<Map.Entry<Serializable, Metacard>> updates = new ArrayList<>();
    updates.add(new AbstractMap.SimpleEntry<>(FIRST, metacardWithTitle(FIRST)));
    updates.add(new AbstractMap.SimpleEntry<>(SECOND, metacardWithTitle(SECOND)));
    return new UpdateRequestImpl(updates, Metacard.TITLE, PROPERTIES, DESTINATIONS);
  }

  private MetacardValidator getMockPassingValidator() {
    MetacardValidator mockValidator =
        mock(MetacardValidator.class, withSettings().extraInterfaces(Describable.class));
    return mockValidator;
  }

  private MetacardValidator getMockFailingValidatorWithErrors() throws ValidationException {
    ValidationException validationException = mock(ValidationException.class);
    when(validationException.getErrors()).thenReturn(Collections.singletonList(SAMPLE_ERROR));
    MetacardValidator metacardValidator =
        mock(MetacardValidator.class, withSettings().extraInterfaces(Describable.class));
    doThrow(validationException).when(metacardValidator).validate(any(Metacard.class));
    when(((Describable) metacardValidator).getId()).thenReturn(ID);
    return metacardValidator;
  }

  private MetacardValidator getMockFailingValidatorWithWarnings() throws ValidationException {
    ValidationException validationException = mock(ValidationException.class);
    when(validationException.getWarnings()).thenReturn(Collections.singletonList(SAMPLE_WARNING));
    MetacardValidator metacardValidator =
        mock(MetacardValidator.class, withSettings().extraInterfaces(Describable.class));
    doThrow(validationException).when(metacardValidator).validate(any(Metacard.class));
    when(((Describable) metacardValidator).getId()).thenReturn(ID);
    return metacardValidator;
  }

  private MetacardValidator getMockFailingValidatorWithErrorsAndWarnings()
      throws ValidationException {
    ValidationException validationException = mock(ValidationException.class);
    when(validationException.getErrors()).thenReturn(Collections.singletonList(SAMPLE_ERROR));
    when(validationException.getWarnings()).thenReturn(Collections.singletonList(SAMPLE_WARNING));
    MetacardValidator metacardValidator =
        mock(MetacardValidator.class, withSettings().extraInterfaces(Describable.class));
    doThrow(validationException).when(metacardValidator).validate(any(Metacard.class));
    when(((Describable) metacardValidator).getId()).thenReturn(ID);
    return metacardValidator;
  }

  private MetacardValidator getMockEnforcedPassingValidatorWithId(String id) {
    MetacardValidator metacardValidator =
        mock(MetacardValidator.class, withSettings().extraInterfaces(Describable.class));
    when(((Describable) metacardValidator).getId()).thenReturn(id);
    return metacardValidator;
  }

  private MetacardValidator getMockEnforcedFailingValidatorWithId(String id)
      throws ValidationException {
    ValidationException validationException = mock(ValidationException.class);
    when(validationException.getErrors()).thenReturn(Collections.singletonList(SAMPLE_ERROR));
    MetacardValidator metacardValidator =
        mock(MetacardValidator.class, withSettings().extraInterfaces(Describable.class));
    doThrow(validationException)
        .when(metacardValidator)
        .validate(argThat(isMetacardWithTitle(FIRST)));
    when(((Describable) metacardValidator).getId()).thenReturn(id);
    return metacardValidator;
  }

  private IsMetacardWithTitle isMetacardWithTitle(String title) {
    return new IsMetacardWithTitle(title);
  }

  private MetacardValidator getMockPassingValidatorNoDescribable() {
    return mock(MetacardValidator.class);
  }

  private MetacardValidator getMockFailingValidatorNoDescribable() throws ValidationException {
    MetacardValidator metacardValidator = mock(MetacardValidator.class);
    ValidationException validationException = mock(ValidationException.class);
    when(validationException.getErrors()).thenReturn(Collections.singletonList(SAMPLE_ERROR));
    doThrow(validationException)
        .when(metacardValidator)
        .validate(argThat(isMetacardWithTitle(FIRST)));
    return metacardValidator;
  }

  private class IsMetacardWithTitle implements ArgumentMatcher<Metacard> {
    private final String title;

    private IsMetacardWithTitle(String title) {
      this.title = title;
    }

    @Override
    public boolean matches(Metacard o) {
      return o.getTitle().equals(title);
    }
  }

  private List<Metacard> markerPluginResponseHelper(
      MetacardValidator validator,
      boolean enforceErrors,
      boolean enforceWarnings,
      int numNotFiltered)
      throws Exception {
    String validatorName = plugin.getValidatorName(validator);

    metacardValidators.add(validator);
    enforcedMetacardValidators.add(validatorName);

    plugin.setMetacardValidators(metacardValidators);
    plugin.setEnforcedMetacardValidators(enforcedMetacardValidators);

    plugin.setEnforceErrors(enforceErrors);
    plugin.setEnforceWarnings(enforceWarnings);

    CreateRequest createRequest = plugin.process(getMockCreateRequest());
    List<Metacard> createdMetacards = createRequest.getMetacards();
    assertThat(createdMetacards.size(), is(numNotFiltered));

    // reset
    metacardValidators.remove(validator);
    enforcedMetacardValidators.remove(validatorName);
    plugin.setEnforceErrors(true);
    plugin.setEnforceWarnings(false);

    return createdMetacards;
  }
}
