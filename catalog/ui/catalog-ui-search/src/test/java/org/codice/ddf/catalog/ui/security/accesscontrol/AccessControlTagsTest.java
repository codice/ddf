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
package org.codice.ddf.catalog.ui.security.accesscontrol;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

public class AccessControlTagsTest {

  private static final String ACCESS_CONTROLLED_TAG = "access-controlled-tag";

  private static final String EXPECTED_TAG_A = "EXPECTED-TAG-A";

  private static final String EXPECTED_TAG_B = "EXPECTED-TAG-B";

  private ServiceReference<?> mockTypeRef;

  private AccessControlTagsUnderTest tagSet;

  @Before
  public void setup() {
    mockTypeRef = mock(ServiceReference.class);
    tagSet = new AccessControlTagsUnderTest();
    // All tests written to assume we start empty, so explicitly assert it here for clarity
    assertThat(tagSet.getAccessControlledTags(), is(empty()));
  }

  @Test
  public void testConstructorInitsItems() {
    AccessControlTags localSet = new AccessControlTags(Collections.singleton(EXPECTED_TAG_A));
    assertThat(localSet.getAccessControlledTags(), hasSize(1));
    assertThat(localSet.getAccessControlledTags(), hasItems(EXPECTED_TAG_A));
  }

  @Test
  public void testBindNullIsIgnored() {
    tagSet.bindTag(null);
    assertThat(tagSet.getAccessControlledTags(), is(empty()));
  }

  @Test
  public void testBindWithoutServicePropertyIsIgnored() {
    tagSet.bindTag(mockTypeRef);
    assertThat(tagSet.getAccessControlledTags(), is(empty()));
  }

  @Test
  public void testBindWithServicePropertyOfIncorrectTypeIsIgnored() {
    when(mockTypeRef.getProperty(ACCESS_CONTROLLED_TAG)).thenReturn(new Object());
    tagSet.bindTag(mockTypeRef);
    assertThat(tagSet.getAccessControlledTags(), is(empty()));
  }

  @Test
  public void testBindWithServicePropertyOfInvalidSetIsIgnored() {
    when(mockTypeRef.getProperty(ACCESS_CONTROLLED_TAG))
        .thenReturn(ImmutableSet.of("string", new Object()));
    tagSet.bindTag(mockTypeRef);
    assertThat(tagSet.getAccessControlledTags(), is(empty()));
  }

  @Test
  public void testBindWithExpectedServicePropertySavesTheTag() {
    when(mockTypeRef.getProperty(ACCESS_CONTROLLED_TAG)).thenReturn(EXPECTED_TAG_A);
    tagSet.bindTag(mockTypeRef);
    assertThat(tagSet.getAccessControlledTags(), hasSize(1));
    assertThat(tagSet.getAccessControlledTags(), hasItems(EXPECTED_TAG_A));
  }

  @Test
  public void testBindWithExpectedServicePropertySetSavesTheTags() {
    when(mockTypeRef.getProperty(ACCESS_CONTROLLED_TAG))
        .thenReturn(ImmutableSet.of(EXPECTED_TAG_A, EXPECTED_TAG_B));
    tagSet.bindTag(mockTypeRef);
    assertThat(tagSet.getAccessControlledTags(), hasSize(2));
    assertThat(tagSet.getAccessControlledTags(), hasItems(EXPECTED_TAG_A, EXPECTED_TAG_B));
  }

  @Test
  public void testUnbindNullIsIgnored() {
    bindTagForTest(EXPECTED_TAG_A);
    assertThat(tagSet.getAccessControlledTags(), hasSize(1));
    assertThat(tagSet.getAccessControlledTags(), hasItems(EXPECTED_TAG_A));

    tagSet.unbindTag(null);
    assertThat(tagSet.getAccessControlledTags(), hasSize(1));
    assertThat(tagSet.getAccessControlledTags(), hasItems(EXPECTED_TAG_A));
  }

  @Test
  public void testUnbindWithoutServicePropertyIsIgnored() {
    bindTagForTest(EXPECTED_TAG_A);
    assertThat(tagSet.getAccessControlledTags(), hasSize(1));
    assertThat(tagSet.getAccessControlledTags(), hasItems(EXPECTED_TAG_A));

    tagSet.unbindTag(mockTypeRef);
    assertThat(tagSet.getAccessControlledTags(), hasSize(1));
    assertThat(tagSet.getAccessControlledTags(), hasItems(EXPECTED_TAG_A));
  }

  @Test
  public void testUnbindWithServicePropertyOfIncorrectTypeIsIgnored() {
    bindTagForTest(EXPECTED_TAG_A);
    assertThat(tagSet.getAccessControlledTags(), hasSize(1));
    assertThat(tagSet.getAccessControlledTags(), hasItems(EXPECTED_TAG_A));

    when(mockTypeRef.getProperty(ACCESS_CONTROLLED_TAG)).thenReturn(new Object());
    tagSet.unbindTag(mockTypeRef);
    assertThat(tagSet.getAccessControlledTags(), hasSize(1));
    assertThat(tagSet.getAccessControlledTags(), hasItems(EXPECTED_TAG_A));
  }

  @Test
  public void testUnbindWithServicePropertyOfInvalidSetIsIgnored() {
    bindTagForTest(EXPECTED_TAG_A);
    assertThat(tagSet.getAccessControlledTags(), hasSize(1));
    assertThat(tagSet.getAccessControlledTags(), hasItems(EXPECTED_TAG_A));

    when(mockTypeRef.getProperty(ACCESS_CONTROLLED_TAG))
        .thenReturn(ImmutableSet.of("string", new Object()));
    tagSet.unbindTag(mockTypeRef);
    assertThat(tagSet.getAccessControlledTags(), hasSize(1));
    assertThat(tagSet.getAccessControlledTags(), hasItems(EXPECTED_TAG_A));
  }

  @Test
  public void testUnbindWithExpectedServicePropertyRemovesTheTag() {
    bindTagForTest(EXPECTED_TAG_A);
    assertThat(tagSet.getAccessControlledTags(), hasSize(1));
    assertThat(tagSet.getAccessControlledTags(), hasItems(EXPECTED_TAG_A));

    when(mockTypeRef.getProperty(ACCESS_CONTROLLED_TAG)).thenReturn(EXPECTED_TAG_A);
    tagSet.unbindTag(mockTypeRef);
    assertThat(tagSet.getAccessControlledTags(), is(empty()));
  }

  @Test
  public void testUnbindWithExpectedServicePropertySetRemovesTheTags() {
    bindTagForTest(EXPECTED_TAG_A);
    bindTagForTest(EXPECTED_TAG_B);
    assertThat(tagSet.getAccessControlledTags(), hasSize(2));
    assertThat(tagSet.getAccessControlledTags(), hasItems(EXPECTED_TAG_A, EXPECTED_TAG_B));

    when(mockTypeRef.getProperty(ACCESS_CONTROLLED_TAG))
        .thenReturn(ImmutableSet.of(EXPECTED_TAG_A, EXPECTED_TAG_B));
    tagSet.unbindTag(mockTypeRef);
    assertThat(tagSet.getAccessControlledTags(), is(empty()));
  }

  @Test
  public void testBindAndUnbindMultipleTagsBehavesLikeCollection() {
    bindTagForTest(EXPECTED_TAG_A);
    assertThat(tagSet.getAccessControlledTags(), hasSize(1));
    assertThat(tagSet.getAccessControlledTags(), hasItems(EXPECTED_TAG_A));

    bindTagForTest(EXPECTED_TAG_B);
    assertThat(tagSet.getAccessControlledTags(), hasSize(2));
    assertThat(tagSet.getAccessControlledTags(), hasItems(EXPECTED_TAG_A, EXPECTED_TAG_B));

    unbindTagForTest(EXPECTED_TAG_A);
    assertThat(tagSet.getAccessControlledTags(), hasSize(1));
    assertThat(tagSet.getAccessControlledTags(), hasItems(EXPECTED_TAG_B));

    unbindTagForTest(EXPECTED_TAG_B);
    assertThat(tagSet.getAccessControlledTags(), is(empty()));
  }

  @Test
  public void testUnbindTagThatDoesNotExistIsIgnored() {
    bindTagForTest(EXPECTED_TAG_A);
    assertThat(tagSet.getAccessControlledTags(), hasSize(1));
    assertThat(tagSet.getAccessControlledTags(), hasItems(EXPECTED_TAG_A));

    unbindTagForTest(EXPECTED_TAG_B);
    assertThat(tagSet.getAccessControlledTags(), hasSize(1));
    assertThat(tagSet.getAccessControlledTags(), hasItems(EXPECTED_TAG_A));
  }

  private void bindTagForTest(String tag) {
    ServiceReference ref = mock(ServiceReference.class);
    when(ref.getProperty(ACCESS_CONTROLLED_TAG)).thenReturn(tag);
    tagSet.bindTag(ref);
  }

  private void unbindTagForTest(String tag) {
    ServiceReference ref = mock(ServiceReference.class);
    when(ref.getProperty(ACCESS_CONTROLLED_TAG)).thenReturn(tag);
    tagSet.unbindTag(ref);
  }

  private static class AccessControlTagsUnderTest extends AccessControlTags {
    @Override
    String typeName(ServiceReference metacardTypeRef) {
      // For logging purposes only, so no point dealing with OSGi within a unit test
      return "UNIT_TESTING_TYPE";
    }
  }
}
