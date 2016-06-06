package org.codice.ddf.catalog.ui.metacard.validation;

import java.util.Set;

import ddf.catalog.validation.violation.ValidationViolation;

public class AttributeValidationResponse {
    private Set<ValidationViolation> violations;

    private Set<String> suggestedValues;

    public AttributeValidationResponse(Set<ValidationViolation> violations,
            Set<String> suggestedValues) {
        this.violations = violations;
        this.suggestedValues = suggestedValues;
    }

    public Set<ValidationViolation> getViolations() {
        return violations;
    }

    public void setViolations(Set<ValidationViolation> violations) {
        this.violations = violations;
    }

    public Set<String> getSuggestedValues() {
        return suggestedValues;
    }

    public void setSuggestedValues(Set<String> suggestedValues) {
        this.suggestedValues = suggestedValues;
    }

}
