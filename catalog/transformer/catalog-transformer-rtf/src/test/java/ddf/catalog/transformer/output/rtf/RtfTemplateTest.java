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
package ddf.catalog.transformer.output.rtf;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

import com.tutego.jrtf.Rtf;
import ddf.catalog.data.Metacard;
import org.junit.Before;
import org.junit.Test;

public class RtfTemplateTest extends BaseTestConfiguration {
  private static final String METACARD_TITLE = "Test Metacard Title";

  private Metacard mockMetacard;

  @Before
  public void setup() {
    mockMetacard = createMockMetacard(METACARD_TITLE);
  }

  @Test
  public void testBuildingRtfFromTemplate() {
    RtfTemplate template =
        new RtfTemplate.Builder().withMetacard(mockMetacard).withCategories(MOCK_CATEGORY).build();

    assertThat("Template cannot be null", template, notNullValue());
    assertThat("There should be 5 categories", MOCK_CATEGORY.get(0).getAttributes(), hasSize(5));

    Rtf doc = Rtf.rtf();

    Rtf generatedTemplate = template.rtf(doc);

    assertThat("Rtf template instance cannot be null", generatedTemplate, notNullValue());

    String finishedDoc = generatedTemplate.out().toString();

    assertThat("RTF output cannot be null", finishedDoc, notNullValue());
    assertThat("RTF document must start with {\\rtf1", finishedDoc, startsWith("{\\rtf1"));
    assertThat(
        String.format("RTF document must contain section with title: %s", METACARD_TITLE),
        finishedDoc,
        containsString(METACARD_TITLE));
  }
}
