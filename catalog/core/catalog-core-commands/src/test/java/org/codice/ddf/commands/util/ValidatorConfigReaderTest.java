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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.junit.Test;

public class ValidatorConfigReaderTest {

    private static final String SAMPLE_RULE_SET_CONFIG_FILE =
            System.getProperty("file.separator") + "validation-ruleset.xml";

    @Test
    public void testOnlyAnyWeightSet() throws JAXBException {
        ValidatorConfigReader reader = new ValidatorConfigReader(getClass().getResourceAsStream(
                SAMPLE_RULE_SET_CONFIG_FILE));

        RuleSet ruleSet = reader.getRuleSet();

        List<ValidatorRuleSet> validatorRuleSets = ruleSet.getValidatorRuleSets();

        ValidatorRuleSet validatorRuleSet = validatorRuleSets.stream()
                .filter(p -> p.getValidatorName()
                        .equals("Schema"))
                .findFirst()
                .orElse(null);

        assertThat("Should have read in the 'Schema' rule set.",
                validatorRuleSet,
                is(notNullValue()));

        assertThat("Should have an 'anyWeight' of 15.0.",
                validatorRuleSet.getAnyWeight(),
                is(15.0));

        assertThat("'errorWeight' should be set to 0.", validatorRuleSet.getErrorWeight(), is(0.0));

        assertThat("'warningWeight' should be set to 0.",
                validatorRuleSet.getWarningWeight(),
                is(0.0));

        assertThat("'individualRules' should be empty.",
                validatorRuleSet.getIndividualRules().size(),
                is(0));
    }

    @Test
    public void testErrorAndWarningWeightSet() throws JAXBException {
        ValidatorConfigReader reader = new ValidatorConfigReader(getClass().getResourceAsStream(
                SAMPLE_RULE_SET_CONFIG_FILE));

        RuleSet ruleSet = reader.getRuleSet();

        List<ValidatorRuleSet> validatorRuleSets = ruleSet.getValidatorRuleSets();

        ValidatorRuleSet validatorRuleSet = validatorRuleSets.stream()
                .filter(p -> p.getValidatorName()
                        .equals("ValidatorWithErrorAndWarningWeight"))
                .findFirst()
                .orElse(null);

        assertThat("Should have read in the 'ValidatorWithErrorAndWarningWeight' rule set.", validatorRuleSet, is(notNullValue()));

        assertThat("Should have an 'errorWeight' of 15.0.",
                validatorRuleSet.getErrorWeight(),
                is(15.0));

        assertThat("Should have a 'warningWeight' of 5.0.",
                validatorRuleSet.getWarningWeight(),
                is(5.0));

        assertThat("'anyWeight' should be set to 0.", validatorRuleSet.getAnyWeight(), is(0.0));

        assertThat("'individualRules' should be empty.",
                validatorRuleSet.getIndividualRules().size(),
                is(0));
    }

    @Test
    public void testIndividualRules() throws JAXBException {
        ValidatorConfigReader reader = new ValidatorConfigReader(getClass().getResourceAsStream(
                SAMPLE_RULE_SET_CONFIG_FILE));

        RuleSet ruleSet = reader.getRuleSet();

        List<ValidatorRuleSet> validatorRuleSets = ruleSet.getValidatorRuleSets();

        ValidatorRuleSet validatorRuleSet = validatorRuleSets.stream()
                .filter(p -> p.getValidatorName()
                        .equals("ValidatorWithIndividualRuleWeight"))
                .findFirst()
                .orElse(null);

        assertThat("Should have read in the 'ValidatorWithIndividualRuleWeight' rule set.",
                validatorRuleSet,
                is(notNullValue()));

        assertThat("'anyWeight' should be set to 0.", validatorRuleSet.getAnyWeight(), is(0.0));

        assertThat("'errorWeight' should be set to 0.", validatorRuleSet.getErrorWeight(), is(0.0));

        assertThat("'warningWeight' should be set to 0.",
                validatorRuleSet.getWarningWeight(),
                is(0.0));

        assertThat("Should have a list of 'IndividualRule' objects in 'individualRules'.",
                validatorRuleSet.getIndividualRules(),
                instanceOf(ArrayList.class));

        assertThat("Should have read in a list of three individual rules.",
                validatorRuleSet.getIndividualRules()
                        .size(),
                is(3));

        validatorRuleSet.getIndividualRules()
                .stream()
                .forEach(p -> assertThat("Each individualValue should have a non-null weight.",
                        p.getWeight(),
                        is(notNullValue())));
    }
}
