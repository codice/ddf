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
package org.codice.ddf.sdk.validation.metacard;

import com.google.common.collect.Sets;
import ddf.catalog.data.Metacard;
import ddf.catalog.util.Describable;
import ddf.catalog.validation.MetacardValidator;
import ddf.catalog.validation.ValidationException;
import ddf.catalog.validation.impl.ValidationExceptionImpl;
import java.util.Collections;
import java.util.Set;

public class SampleMetacardValidator implements MetacardValidator, Describable {
  private Set<String> validWords = Sets.newHashSet("clean", "test", "default", "sample");

  private Set<String> warningWords = Sets.newHashSet("warning");

  private Set<String> errorWords = Sets.newHashSet("error");

  private String id = "sample-validator";

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getTitle() {
    return "Sample Metacard Validator";
  }

  public void setId(String newId) {
    id = newId;
  }

  @Override
  public String getDescription() {
    return "A sample metacard validator used for development and testing.";
  }

  @Override
  public String getOrganization() {
    return "Codice";
  }

  @Override
  public void validate(Metacard metacard) throws ValidationException {
    if (checkMetacardForWarningWords(metacard.getTitle())) {
      ValidationExceptionImpl validationException =
          new ValidationExceptionImpl(
              "Metacard title contains one of the warning words: " + warningWords);
      validationException.setWarnings(Collections.singletonList("sampleWarnings"));
      throw validationException;
    }
    if (checkMetacardForErrorWords(metacard.getTitle())) {
      ValidationExceptionImpl validationException =
          new ValidationExceptionImpl(
              "Metacard title contains one of the error words: " + errorWords);
      validationException.setErrors(Collections.singletonList("sampleError"));
      throw validationException;
    }
    if (!checkMetacardForValidWords(metacard.getTitle())) {
      ValidationExceptionImpl validationException =
          new ValidationExceptionImpl("Metacard title does not contain any of: " + validWords);
      validationException.setErrors(Collections.singletonList("sampleError"));
      validationException.setWarnings(Collections.singletonList("sampleWarnings"));
      throw validationException;
    }
  }

  private boolean checkMetacardForValidWords(String title) {
    return validWords.stream().anyMatch(title::contains);
  }

  private boolean checkMetacardForWarningWords(String title) {
    return warningWords.stream().anyMatch(title::contains);
  }

  private boolean checkMetacardForErrorWords(String title) {
    return errorWords.stream().anyMatch(title::contains);
  }

  public void setValidWords(Set<String> validWords) {
    if (validWords != null) {
      this.validWords = validWords;
    }
  }

  public void setWarningWords(Set<String> warningWords) {
    if (warningWords != null) {
      this.warningWords = warningWords;
    }
  }

  public void setErrorWords(Set<String> errorWords) {
    if (errorWords != null) {
      this.errorWords = errorWords;
    }
  }

  public Set<String> getValidWords() {
    return validWords;
  }

  public Set<String> getWarningWords() {
    return warningWords;
  }

  public Set<String> getErrorWords() {
    return errorWords;
  }
}
