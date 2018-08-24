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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.types.SecurityAttributes;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Security;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.AccessPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.security.SubjectIdentity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import org.apache.shiro.subject.Subject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class AccessControlAccessPluginTest {

  private static final String MOCK_IDENTITY_ATTR = "user@email";

  private static final String MOCK_METACARD_ID = "100";

  private AccessPlugin accessPlugin;

  @Mock private Attribute mockAttribute;

  @Mock private Metacard mockMetacard;

  @Mock private Subject subject;

  private SubjectIdentity subjectIdentity = mock(SubjectIdentity.class);

  @Before
  public void setUp() {
    initMocks(this);
    when(mockMetacard.getId()).thenReturn(MOCK_METACARD_ID);
    when(subjectIdentity.getUniqueIdentifier(subject)).thenReturn(MOCK_IDENTITY_ATTR);
    when(mockMetacard.getAttribute(Security.ACCESS_ADMINISTRATORS)).thenReturn(mockAttribute);
    when(mockAttribute.getValues()).thenReturn(Collections.singletonList(MOCK_IDENTITY_ATTR));

    accessPlugin =
        new AccessControlAccessPlugin(subjectIdentity) {
          public boolean isAccessControlUpdated(Metacard prev, Metacard updated) {
            return true;
          }

          public Subject getSubject() {
            return subject;
          }
        };
  }

  private UpdateRequest mockUpdateRequest(Map<String, Metacard> updates) {
    UpdateRequest update = mock(UpdateRequest.class);
    doReturn(new ArrayList(updates.entrySet())).when(update).getUpdates();
    return update;
  }

  @Test
  public void testAccessibleMetacardsSucceed() throws Exception {
    Map<String, Metacard> updates = ImmutableMap.of(MOCK_METACARD_ID, mockMetacard);
    UpdateRequest update = mockUpdateRequest(updates);

    // User updating metacard perms is in the ACL, so we allow them to pass through the access
    // plugin
    assertThat(accessPlugin.processPreUpdate(update, updates), is(update));
  }

  @Test(expected = StopProcessingException.class)
  public void testUnauthorizedUserFailsUpdate() throws Exception {
    // Represents current list of those in access-administrators list
    when(mockAttribute.getValues()).thenReturn(Collections.emptyList());

    Map<String, Metacard> updates = ImmutableMap.of(MOCK_METACARD_ID, mockMetacard);
    UpdateRequest update = mockUpdateRequest(updates);

    // User not in ACL attempted to modify metacard perms, which will throw exception
    accessPlugin.processPreUpdate(update, updates);
  }

  @Test(expected = StopProcessingException.class)
  public void testBasicUpdateFailsWithMissingAdmin() throws Exception {
    Metacard testMetacard =
        AccessControlUtil.metacardFromAttributes(
            ImmutableMap.of(Core.ID, MOCK_METACARD_ID, Core.METACARD_OWNER, "guest"));

    Map<String, Metacard> updates = ImmutableMap.of(MOCK_METACARD_ID, testMetacard);
    UpdateRequest update = mockUpdateRequest(updates);

    // admin not in ACL, so update fails
    accessPlugin.processPreUpdate(update, updates);
  }

  @Test(expected = StopProcessingException.class)
  public void testStopProcessingWhenNotAdmin() throws Exception {

    Metacard before =
        AccessControlUtil.metacardFromAttributes(
            ImmutableMap.of(
                Core.ID,
                MOCK_METACARD_ID,
                Core.METACARD_OWNER,
                "before",
                SecurityAttributes.ACCESS_ADMINISTRATORS,
                ImmutableSet.of("admin")));

    Metacard after =
        AccessControlUtil.metacardFromAttributes(
            ImmutableMap.of(
                Core.ID,
                MOCK_METACARD_ID,
                Core.METACARD_OWNER,
                "before",
                SecurityAttributes.ACCESS_ADMINISTRATORS,
                ImmutableSet.of("admin", "guest")));

    UpdateRequest update = mockUpdateRequest(ImmutableMap.of(MOCK_METACARD_ID, after));
    accessPlugin.processPreUpdate(update, ImmutableMap.of(MOCK_METACARD_ID, before));
  }
}
