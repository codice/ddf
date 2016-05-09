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
package org.codice.ddf.commands.catalog;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.ArrayList;
import java.util.List;

import org.codice.ddf.commands.util.RuleSet;
import org.codice.ddf.commands.util.ValidatorRuleSet;
import org.junit.Test;

import ddf.catalog.data.Metacard;
import ddf.catalog.util.Describable;
import ddf.catalog.validation.MetacardValidator;
import ddf.catalog.validation.ValidationException;

public class ScoreCommandTest {

    private static final String VALIDATOR_NAME = "validator";
    private static final String ERROR_MESSAGE = "error";

    @Test
    public void testWithInvalidRuleset() throws Exception {
        ConsoleOutput consoleOutput = new ConsoleOutput();
        consoleOutput.interceptSystemOut();

        ScoreCommand scoreCommand = new ScoreCommand();
        scoreCommand.setRuleSetFilename("does-not-exist.xml");
        scoreCommand.execute();

        consoleOutput.resetSystemOut();
        assertThat(consoleOutput.getOutput(), containsString("not configured"));
    }

    @Test
    public void testWithPerfectScore() throws Exception {
        MetacardValidator validator = mock(MetacardValidator.class);
        MockRuleSet mockRuleSet = new MockRuleSet(validator.getClass().getName());
        assertEquals(score(mockRuleSet, validator), 100, 0.1);
    }

    @Test (expected = Exception.class)
    public void testValidatorNotFound() throws Exception {
        MockRuleSet mockRuleSet = new MockRuleSet("Does Not Exist");
        score(mockRuleSet, mock(MetacardValidator.class));
    }

    @Test
    public void testWithAnyWeightEqualTo10() throws Exception {
        testWithWeight(WeightType.ANY, 10);
    }

    @Test
    public void testWithErrorWeightEqualTo10() throws Exception {
        testWithWeight(WeightType.ERROR, 10);
    }

    @Test
    public void testWithWarningWeightEqualTo10() throws Exception {
        testWithWeight(WeightType.WARNING, 10);
    }

    @Test
    public void testWithIndividualWeightEqualTo10() throws Exception {
        testWithWeight(WeightType.INDIVIDUAL, 10);
    }

    @Test
    public void testWithIndividualWeightEqualTo0() throws Exception {
        testWithWeight(WeightType.INDIVIDUAL, 0);
    }

    private enum WeightType {
        ANY,
        ERROR,
        WARNING,
        INDIVIDUAL
    }

    private void testWithWeight(WeightType weightType, int weightValue) throws Exception {
        MockRuleSet mockRuleSet = new MockRuleSet(VALIDATOR_NAME);
        switch (weightType) {
        case ANY:
            mockRuleSet.anyWeight = weightValue;
            break;
        case ERROR:
            mockRuleSet.errorWeight = weightValue;
            break;
        case WARNING:
            mockRuleSet.warningWeight = weightValue;
            break;
        case INDIVIDUAL:
            mockRuleSet.individualWeight = weightValue;
            break;
        }

        MetacardValidator validator = getMockValidator(VALIDATOR_NAME, ERROR_MESSAGE);
        assertEquals(score(mockRuleSet, validator), 100 - weightValue, 0.1);
    }

    private MetacardValidator getMockValidator(String id, String errorMessage)
            throws ValidationException {
        List<String> errors = new ArrayList<>();
        errors.add(errorMessage);
        ValidationException validationException = mock(ValidationException.class);
        when(validationException.getErrors()).thenReturn(errors);
        MetacardValidator metacardValidator = mock(MetacardValidator.class, withSettings().extraInterfaces(Describable.class));
        when(((Describable) metacardValidator).getId()).thenReturn(id);
        doThrow(validationException).when(metacardValidator).validate(any(Metacard.class));
        return metacardValidator;
    }

    private double score(MockRuleSet mockRuleSet, MetacardValidator validator) throws Exception {
        ConsoleOutput consoleOutput = new ConsoleOutput();
        consoleOutput.interceptSystemOut();

        List<MetacardValidator> validators = new ArrayList<>();
        validators.add(validator);

        ScoreCommand scoreCommand = new ScoreCommand();
        scoreCommand.ruleSet = mockRuleSet.getRuleSet();
        scoreCommand.validators = validators;
        double score = scoreCommand.score(mock(Metacard.class));

        consoleOutput.resetSystemOut();
        return score;
    }

    private class MockRuleSet {
        String validatorName;
        double anyWeight;
        double errorWeight;
        double warningWeight;
        double individualWeight;

        MockRuleSet(String validatorName) {
            this.validatorName = validatorName;
        }

        RuleSet getRuleSet() {
            ValidatorRuleSet validatorRuleSet = mock(ValidatorRuleSet.class);
            when(validatorRuleSet.getAnyWeight()).thenReturn(anyWeight);
            when(validatorRuleSet.getErrorWeight()).thenReturn(errorWeight);
            when(validatorRuleSet.getWarningWeight()).thenReturn(warningWeight);
            when(validatorRuleSet.getRuleWeight(anyString())).thenReturn(individualWeight);
            when(validatorRuleSet.getValidatorName()).thenReturn(validatorName);

            List<ValidatorRuleSet> validatorRuleSets = new ArrayList<>();
            validatorRuleSets.add(validatorRuleSet);

            RuleSet ruleSet = mock(RuleSet.class);
            when(ruleSet.getValidatorRuleSets()).thenReturn(validatorRuleSets);

            return ruleSet;
        }
    }
}
