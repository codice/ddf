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
package org.codice.ddf.catalog.ui.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.types.SecurityAttributes;
import java.util.Collections;
import java.util.Set;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceAttributes;
import org.junit.Before;
import org.junit.Test;

public class ShareableMetacardImplTest {

  private ShareableMetacardImpl shareableMetacard;

  private static final String ADMIN = "admin";

  private static final String ADMIN_1 = "admin1";

  private static final String ADMIN_2 = "admin2";

  private static final String ADMIN_3 = "admin3";

  private static final String GUEST = "guest";

  private static final String TEST_EMAIL_1 = "admin@connexta.com";

  private static final String TEST_EMAIL_2 = "admin2@connexta.com";

  private static final String TEST_EMAIL_3 = "owner@localhost";

  private static final String TEST_ID = "0";

  @Before
  public void setUp() {
    shareableMetacard = new ShareableMetacardImpl();
    shareableMetacard.setTags(Collections.singleton(WorkspaceAttributes.WORKSPACE_TAG));
  }

  @Test
  public void testcanShare() {
    assertThat(ShareableMetacardImpl.canShare(null), is(false));
    assertThat(ShareableMetacardImpl.canShare(new MetacardImpl()), is(false));
    assertThat(ShareableMetacardImpl.canShare(shareableMetacard), is(true));
  }

  @Test
  public void testShareableMetacardFrom() {
    ShareableMetacardImpl wrapped =
        ShareableMetacardImpl.createOrThrow(new MetacardImpl(shareableMetacard));
    wrapped.setTags(Collections.singleton(WorkspaceAttributes.WORKSPACE_TAG));
    wrapped.setId(TEST_ID);
    assertThat(wrapped.getId(), is(TEST_ID));
    assertThat(shareableMetacard.getId(), is(TEST_ID));
  }

  @Test
  public void testSharingAccessGroups() {
    Set<String> accessGroups = ImmutableSet.of(ADMIN, GUEST);
    assertThat(shareableMetacard.setAccessGroups(accessGroups).getAccessGroups(), is(accessGroups));
  }

  @Test
  public void testSharingAccessIndividuals() {
    Set<String> accessIndividuals = ImmutableSet.of(TEST_EMAIL_1);
    assertThat(
        shareableMetacard.setAccessIndividuals(accessIndividuals).getAccessIndividuals(),
        is(accessIndividuals));
  }

  @Test
  public void testOwner() {
    String owner = TEST_EMAIL_3;
    assertThat(shareableMetacard.setOwner(owner).getOwner(), is(owner));
  }

  @Test
  public void testDiffSharingAccessGroupsNoChanges() {
    shareableMetacard.setAccessGroups(ImmutableSet.of(ADMIN));
    assertThat(
        shareableMetacard.diffSharingAccessGroups(shareableMetacard), is(Collections.emptySet()));
  }

  @Test
  public void testDiffSharingAccessIndividualsNoChanges() {
    shareableMetacard.setAccessIndividuals(ImmutableSet.of(TEST_EMAIL_1));
    assertThat(
        shareableMetacard.diffSharingAccessIndividuals(shareableMetacard),
        is(Collections.emptySet()));
  }

  @Test
  public void testDiffSharingAccessGroupsWithChanges() {
    ShareableMetacardImpl m =
        ShareableMetacardImpl.create(
            ImmutableMap.of(SecurityAttributes.ACCESS_GROUPS, ImmutableList.of(ADMIN, GUEST)));
    m.setTags(Collections.singleton(WorkspaceAttributes.WORKSPACE_TAG));

    shareableMetacard.setAccessGroups(ImmutableSet.of(ADMIN));
    Set<String> diffGroups = ImmutableSet.of(GUEST);

    assertThat(shareableMetacard.diffSharingAccessGroups(m), is(diffGroups));
    assertThat(m.diffSharingAccessGroups(shareableMetacard), is(diffGroups));
  }

  @Test
  public void testDiffSharingAccessIndividualsWithChanges() {
    ShareableMetacardImpl m =
        ShareableMetacardImpl.create(
            ImmutableMap.of(
                SecurityAttributes.ACCESS_INDIVIDUALS,
                ImmutableList.of(TEST_EMAIL_1, TEST_EMAIL_2)));
    m.setTags(Collections.singleton(WorkspaceAttributes.WORKSPACE_TAG));

    shareableMetacard.setAccessIndividuals(ImmutableSet.of(TEST_EMAIL_1));
    Set<String> diffIndividuals = ImmutableSet.of(TEST_EMAIL_2);

    assertThat(shareableMetacard.diffSharingAccessIndividuals(m), is(diffIndividuals));
    assertThat(m.diffSharingAccessIndividuals(shareableMetacard), is(diffIndividuals));
  }

  @Test
  public void testDiffSharingGroupsWithElementOverlap() {
    ShareableMetacardImpl m =
        ShareableMetacardImpl.create(
            ImmutableMap.of(SecurityAttributes.ACCESS_GROUPS, ImmutableList.of(ADMIN_2, ADMIN_3)));
    m.setTags(Collections.singleton(WorkspaceAttributes.WORKSPACE_TAG));

    shareableMetacard.setAccessGroups(ImmutableSet.of(ADMIN_1, ADMIN_2));
    Set<String> diffGroups = ImmutableSet.of(ADMIN_1, ADMIN_3);

    assertThat(shareableMetacard.diffSharingAccessGroups(m), is(diffGroups));
    assertThat(m.diffSharingAccessGroups(shareableMetacard), is(diffGroups));
  }

  @Test
  public void testDiffSharingIndividualsWithElementOverlap() {
    ShareableMetacardImpl m =
        ShareableMetacardImpl.create(
            ImmutableMap.of(
                SecurityAttributes.ACCESS_INDIVIDUALS,
                ImmutableList.of(TEST_EMAIL_2, TEST_EMAIL_3)));
    m.setTags(Collections.singleton(WorkspaceAttributes.WORKSPACE_TAG));

    shareableMetacard.setAccessIndividuals(ImmutableSet.of(TEST_EMAIL_1, TEST_EMAIL_2));
    Set<String> diffIndividuals = ImmutableSet.of(TEST_EMAIL_1, TEST_EMAIL_3);

    assertThat(shareableMetacard.diffSharingAccessIndividuals(m), is(diffIndividuals));
    assertThat(m.diffSharingAccessIndividuals(shareableMetacard), is(diffIndividuals));
  }
}
