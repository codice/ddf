package org.codice.ddf.catalog.ui.metacard.validation;

import java.util.ArrayList;
import java.util.List;

public class ViolationResult {
    private String attribute;

    private List<String> errors = new ArrayList<>();

    private List<String> warnings = new ArrayList<>();

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

}