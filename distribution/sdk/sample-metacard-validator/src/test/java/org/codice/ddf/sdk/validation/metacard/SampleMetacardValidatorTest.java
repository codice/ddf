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

import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.validation.ValidationException;
import org.junit.Test;

public class SampleMetacardValidatorTest {

  @Test
  public void testValidateValidMetacard() throws ValidationException {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setTitle("sample");
    SampleMetacardValidator validator = new SampleMetacardValidator();
    validator.setValidWords(ImmutableSet.of("sample"));
    validator.setWarningWords(ImmutableSet.of("warning"));
    validator.validate(metacard);
  }

  @Test(expected = ValidationException.class)
  public void testValidateNoValidWordsMetacard() throws ValidationException {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setTitle("invalid");
    SampleMetacardValidator validator = new SampleMetacardValidator();
    validator.setValidWords(ImmutableSet.of("sample"));
    validator.validate(metacard);
  }

  @Test(expected = ValidationException.class)
  public void testValidateWarningMetacard() throws ValidationException {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setTitle("warning");
    SampleMetacardValidator validator = new SampleMetacardValidator();
    validator.setWarningWords(ImmutableSet.of("warning"));
    validator.validate(metacard);
  }

  @Test(expected = ValidationException.class)
  public void testValidateErrorMetacard() throws ValidationException {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setTitle("error");
    SampleMetacardValidator validator = new SampleMetacardValidator();
    validator.setErrorWords(ImmutableSet.of("error"));
    validator.validate(metacard);
  }
}
