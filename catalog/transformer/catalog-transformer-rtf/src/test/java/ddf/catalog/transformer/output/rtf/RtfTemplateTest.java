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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import com.tutego.jrtf.Rtf;
import ddf.catalog.data.Metacard;
import ddf.catalog.transformer.output.rtf.model.RtfCategory;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class RtfTemplateTest extends BaseTestConfiguration {
  private static final String METACARD_TITLE = "Test Metacard Title";

  private Metacard mockMetacard;
  private List<RtfCategory> mockCategories;

  @Before
  public void setup() {
    mockMetacard = createMockMetacard(METACARD_TITLE);
    mockCategories = getCategories();
  }

  @Test
  public void testRemplateCreation() {
    RtfTemplate template =
        new RtfTemplate.Builder()
            .withMetacard(mockMetacard)
            .usingCategories(mockCategories)
            .build();

    assertThat("Template cannot be null", template, notNullValue());
  }

  @Test
  public void testBuildingRtfFromTemplate() {
    RtfTemplate template =
        new RtfTemplate.Builder()
            .withMetacard(mockMetacard)
            .usingCategories(mockCategories)
            .build();

    assertThat("Template cannot be null", template, notNullValue());
    assertThat(
        "There should be 4 categories", mockCategories.get(0).getAttributes().size(), equalTo(4));

    Rtf doc = Rtf.rtf();

    Rtf generatedTemplate = template.rtf(doc);

    assertThat("Rtf template instance cannot be null", generatedTemplate, notNullValue());

    String finishedDoc = generatedTemplate.out().toString();

    assertThat("RTF output cannot be null", finishedDoc, notNullValue());
    assertThat("RTF document must start with {\\rtf1", finishedDoc.startsWith("{\\rtf1"), is(true));
    assertThat(
        String.format("RTF document must contain section with title: %s", METACARD_TITLE),
        finishedDoc.contains(METACARD_TITLE),
        is(true));
  }
}
