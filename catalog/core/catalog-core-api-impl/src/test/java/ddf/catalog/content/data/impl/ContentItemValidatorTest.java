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
package ddf.catalog.content.data.impl;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import ddf.catalog.content.data.ContentItem;
import org.junit.Test;

public class ContentItemValidatorTest {

  @Test
  public void testInvalidQualifier() throws Exception {
    String id = "634e8505-bd4b-436e-97e8-2045d1b0d265".replace("-", "");
    String qualifier = "!wow!#$These%^characters&*aren't(_`allowed!!";
    ContentItem item = new ContentItemImpl(id, qualifier, null, "", null);
    assertThat(ContentItemValidator.validate(item), is(false));
  }

  @Test
  public void testInvalidId() throws Exception {
    // "123456789 is not a guid
    ContentItem item = new ContentItemImpl("123456789", "good-qualifier", null, "", null);
    assertThat(ContentItemValidator.validate(item), is(false));
  }

  @Test
  public void testValidItem() throws Exception {
    String id = "de7a758e-ed76-45c0-af82-aa731950693d".replace("-", "");
    ContentItem item = new ContentItemImpl(id, null, null, "", null);
    assertThat(ContentItemValidator.validate(item), is(true));
  }

  @Test
  public void testValidItemWithQualifier() throws Exception {
    String id = "634e8505-bd4b-436e-97e8-2045d1b0d265".replace("-", "");
    String qualifier = "zoom-and-enhanced-overview";
    ContentItem item = new ContentItemImpl(id, qualifier, null, "", null);
    assertThat(ContentItemValidator.validate(item), is(true));
  }

  @Test
  public void testValidItemWithEmptyQualifier() throws Exception {
    String id = "634e8505-bd4b-436e-97e8-2045d1b0d265".replace("-", "");
    String qualifier = "";
    ContentItem item = new ContentItemImpl(id, qualifier, null, "", null);
    assertThat(item.getQualifier(), nullValue());
    assertThat(ContentItemValidator.validate(item), is(true));
  }

  @Test
  public void testValidItemWithBlankNotEmptyQualifier() throws Exception {
    String id = "634e8505-bd4b-436e-97e8-2045d1b0d265".replace("-", "");
    String qualifier = "              ";
    ContentItem item = new ContentItemImpl(id, qualifier, null, "", null);
    assertThat(item.getQualifier(), nullValue());
    assertThat(ContentItemValidator.validate(item), is(true));
  }

  @Test
  public void testInvalidIdWithQualifier() throws Exception {
    String id = "634e8505-bd4b-436e-97e8-2045d1b0d265-abcdef".replace("-", "");
    String qualifier = "zoom-and-enhanced-overview";
    ContentItem item = new ContentItemImpl(id, qualifier, null, "", null);
    assertThat(ContentItemValidator.validate(item), is(false));
  }

  @Test
  public void testValidIdWithDashes() throws Exception {
    String id = "634e8505-bd4b-436e-97e8-2045d1b0d265"; // still valid with dashes left in
    String qualifier = "zoom-and-enhanced-overview";
    ContentItem item = new ContentItemImpl(id, qualifier, null, "", null);
    assertThat(ContentItemValidator.validate(item), is(true));
  }

  @Test
  public void testInvalidIdColon() throws Exception {
    /* content colon should not be added by the caller */
    String id = "content:634e8505-bd4b-436e-97e8-2045d1b0d265".replace("-", "");
    String qualifier = "zoom-and-enhanced-overview";
    ContentItem item = new ContentItemImpl(id, qualifier, null, "", null);
    assertThat(ContentItemValidator.validate(item), is(false));
  }

  @Test
  public void testInvalidIdWithQualifierInId() throws Exception {
    /* fragment should not be added by the caller */
    String id = "634e8505-bd4b-436e-97e8-2045d1b0d265#zoom-and-enhance-overview".replace("-", "");
    ContentItem item = new ContentItemImpl(id, null, null, "", null);
    assertThat(ContentItemValidator.validate(item), is(false));
  }
}
