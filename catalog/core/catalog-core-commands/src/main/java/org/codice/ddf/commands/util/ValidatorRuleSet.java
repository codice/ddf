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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class ValidatorRuleSet {

    @XmlAttribute
    private String validatorName;

    @XmlElement(defaultValue = "0")
    private double errorWeight;

    @XmlElement(defaultValue = "0")
    private double warningWeight;

    @XmlElement(defaultValue = "0")
    private double anyWeight;

    @XmlElement(name = "individualRule")
    private List<IndividualRule> individualRules = new ArrayList<>();

    public List<IndividualRule> getIndividualRules() {
        return individualRules;
    }

    public double getRuleWeight(String message) {
        IndividualRule rule = individualRules.stream()
                .filter(p -> p.getMessage().replaceAll("[^A-Za-z0-9]", "")
                        .equalsIgnoreCase(message.replaceAll("[^A-Za-z0-9]", "")))
                .findFirst()
                .orElse(null);
        return (rule == null) ? 0 : rule.getWeight();
    }

    public double getErrorWeight() {
        return errorWeight;
    }

    public double getWarningWeight() {
        return warningWeight;
    }

    public double getAnyWeight() {
        return anyWeight;
    }

    public String getValidatorName() {
        return validatorName;
    }

}