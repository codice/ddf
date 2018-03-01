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
package ddf.catalog.transform.xml;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.transformer.xml.XmlValidationEventHandler;
import javax.xml.bind.ValidationEvent;
import org.junit.Before;
import org.junit.Test;

public class XmlValidationEventHandlerTest {

  ValidationEvent validationEvent = mock(ValidationEvent.class);

  XmlValidationEventHandler xmlValidationEventHandler = new XmlValidationEventHandler();

  @Before
  public void setup() {}

  @Test
  public void testHandleWarningEvent() throws Exception {
    when(validationEvent.getSeverity()).thenReturn(ValidationEvent.WARNING);
    assertThat(xmlValidationEventHandler.handleEvent(validationEvent), is(true));
  }

  @Test
  public void testHandleErrorEvent() throws Exception {
    when(validationEvent.getSeverity()).thenReturn(ValidationEvent.ERROR);
    assertThat(xmlValidationEventHandler.handleEvent(validationEvent), is(false));
  }

  @Test
  public void testHandleFatalEvent() throws Exception {
    when(validationEvent.getSeverity()).thenReturn(ValidationEvent.FATAL_ERROR);
    assertThat(xmlValidationEventHandler.handleEvent(validationEvent), is(false));
  }

  @Test(expected = AssertionError.class)
  public void testHandleDefaultEvent() throws Exception {
    when(validationEvent.getSeverity()).thenReturn(5);
    xmlValidationEventHandler.handleEvent(validationEvent);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullEvent() {
    xmlValidationEventHandler.handleEvent(null);
  }
}
