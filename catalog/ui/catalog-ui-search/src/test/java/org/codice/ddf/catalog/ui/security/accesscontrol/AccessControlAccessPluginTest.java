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

import static org.codice.ddf.catalog.ui.forms.data.AttributeGroupType.ATTRIBUTE_GROUP_TAG;
import static org.codice.ddf.catalog.ui.forms.data.QueryTemplateType.QUERY_TEMPLATE_TAG;
import static org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceConstants.WORKSPACE_TAG;
import static org.codice.ddf.catalog.ui.security.accesscontrol.AclTestSupport.metacardFromAttributes;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Security;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.plugin.AccessPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.security.SubjectIdentity;
import java.util.Map;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.Before;
import org.junit.Test;

/**
 * Verify enforcement of the ACL list. Some tests may at first look incorrect, since a few of the
 * verifications do not reflect how the system actually behaves, but instead how this plugin should
 * decide to allow or deny the request within the context of everything else that happens.
 *
 * <p>Note the default subject is {@link #USER_EINSTEIN}, but that can be overridden in a test by
 * first calling {@link #setSubject(String)}.
 */
public class AccessControlAccessPluginTest {

  private static final String USER_NEWTON = "newton@localhost.local";

  private static final String USER_EINSTEIN = "einstein@localhost.local";

  private static final String USER_DIJKSTRA = "dijkstra@localhost.local";

  private static final String METACARD_ID = "100";

  private static final String RESOURCE_TAG = "resource";

  private AccessPlugin accessPlugin;

  @Before
  public void setUp() {
    setSubject(USER_EINSTEIN);
  }

  private void setSubject(String user) {
    ThreadContext.unbindSubject();
    Subject subject = mock(Subject.class);
    SubjectIdentity subjectIdentity = mock(SubjectIdentity.class);
    when(subjectIdentity.getUniqueIdentifier(subject)).thenReturn(user);
    ThreadContext.bind(subject);

    accessPlugin = new AccessControlAccessPlugin(subjectIdentity);
  }

  private UpdateRequest getUpdateRequest(String id, Metacard metacard) {
    return new UpdateRequestImpl(id, metacard);
  }

  @Test
  public void testOwnerCanModify() throws StopProcessingException {
    verifyUserWithAttributeCanModifyUnprotectedAttribute(Core.METACARD_OWNER);
  }

  @Test
  public void testAccessIndividualsReadCanModify() throws StopProcessingException {
    verifyUserWithAttributeCanModifyUnprotectedAttribute(Security.ACCESS_INDIVIDUALS_READ);
  }

  @Test
  public void testAccessIndividualsCanModify() throws StopProcessingException {
    verifyUserWithAttributeCanModifyUnprotectedAttribute(Security.ACCESS_INDIVIDUALS);
  }

  @Test
  public void testAccessAdminsCanModify() throws StopProcessingException {
    verifyUserWithAttributeCanModifyUnprotectedAttribute(Security.ACCESS_ADMINISTRATORS);
  }

  /**
   * If an unprotected attribute was modified, then the plugin does not care who modified it and
   * allows the request to continue. As of this writing, the {@code FilterPlugin} handles instances
   * of a user with {@link Security#ACCESS_INDIVIDUALS_READ} attempting to modify any attributes on
   * the metacard.
   *
   * <p>The choice of {@link Core#TITLE} for this set of tests was arbitrary. It is not an ACL
   * attribute and therefore is sufficient for the needs of the test.
   */
  private void verifyUserWithAttributeCanModifyUnprotectedAttribute(String securityAttribute)
      throws StopProcessingException {
    Metacard before =
        metacardFromAttributes(
            ImmutableMap.of(
                Core.ID,
                METACARD_ID,
                securityAttribute,
                ImmutableSet.of(USER_EINSTEIN),
                Core.TITLE,
                "title1",
                Core.METACARD_TAGS,
                WORKSPACE_TAG));
    Metacard after =
        metacardFromAttributes(
            ImmutableMap.of(
                Core.ID,
                METACARD_ID,
                securityAttribute,
                ImmutableSet.of(USER_EINSTEIN),
                Core.TITLE,
                "title2",
                Core.METACARD_TAGS,
                WORKSPACE_TAG));

    UpdateRequest update = getUpdateRequest(METACARD_ID, after);
    accessPlugin.processPreUpdate(update, ImmutableMap.of(METACARD_ID, before));
  }

  @Test
  public void testPluginIgnoresNoOpForOwner() throws StopProcessingException {
    verifyPluginIgnoresNoOpForAttribute(Core.METACARD_OWNER);
  }

  @Test
  public void testPluginIgnoresNoOpForIndividualsRead() throws StopProcessingException {
    verifyPluginIgnoresNoOpForAttribute(Security.ACCESS_INDIVIDUALS_READ);
  }

  @Test
  public void testPluginIgnoresNoOpForIndividuals() throws StopProcessingException {
    verifyPluginIgnoresNoOpForAttribute(Security.ACCESS_INDIVIDUALS);
  }

  @Test
  public void testPluginIgnoresNoOpForAdmins() throws StopProcessingException {
    verifyPluginIgnoresNoOpForAttribute(Security.ACCESS_ADMINISTRATORS);
  }

  /**
   * If the metacard didn't change, then it doesn't matter who invoked the operation. The plugin
   * will allow the request to continue.
   */
  private void verifyPluginIgnoresNoOpForAttribute(String securityAttribute)
      throws StopProcessingException {
    Metacard metacard =
        metacardFromAttributes(
            ImmutableMap.of(
                Core.ID,
                METACARD_ID,
                securityAttribute,
                USER_NEWTON,
                Core.METACARD_TAGS,
                WORKSPACE_TAG));

    Map<String, Metacard> updates = ImmutableMap.of(METACARD_ID, metacard);
    UpdateRequest update = getUpdateRequest(METACARD_ID, metacard);

    assertThat(accessPlugin.processPreUpdate(update, updates), is(update));
  }

  @Test(expected = StopProcessingException.class)
  public void testUserNotOnAclCannotUpdateIndividualsRead() throws StopProcessingException {
    verifyUserNotOnAclCannotUpdateAttribute(Security.ACCESS_INDIVIDUALS_READ);
  }

  @Test(expected = StopProcessingException.class)
  public void testUserNotOnAclCannotUpdateIndividuals() throws StopProcessingException {
    verifyUserNotOnAclCannotUpdateAttribute(Security.ACCESS_INDIVIDUALS);
  }

  @Test(expected = StopProcessingException.class)
  public void testUserNotOnAclCannotUpdateAdmins() throws StopProcessingException {
    verifyUserNotOnAclCannotUpdateAttribute(Security.ACCESS_ADMINISTRATORS);
  }

  /**
   * Parameterized test for verifying behavior when ACL attributes themselves are changed by a user
   * not found on any of the ACL attributes. These operations should <b>never</b> be allowed to
   * succeed.
   */
  private void verifyUserNotOnAclCannotUpdateAttribute(String securityAttribute)
      throws StopProcessingException {
    Metacard before =
        metacardFromAttributes(
            ImmutableMap.of(
                Core.ID,
                METACARD_ID,
                Core.METACARD_OWNER,
                USER_NEWTON,
                securityAttribute,
                ImmutableSet.of(USER_DIJKSTRA),
                Core.METACARD_TAGS,
                WORKSPACE_TAG));
    Metacard after =
        metacardFromAttributes(
            ImmutableMap.of(
                Core.ID,
                METACARD_ID,
                Core.METACARD_OWNER,
                USER_NEWTON,
                securityAttribute,
                ImmutableSet.of(),
                Core.METACARD_TAGS,
                WORKSPACE_TAG));

    UpdateRequest update = getUpdateRequest(METACARD_ID, after);
    accessPlugin.processPreUpdate(update, ImmutableMap.of(METACARD_ID, before));
  }

  @Test
  public void testIndividualCanBecomeAdminOnResources() throws StopProcessingException {
    verifyIndividualTryingToBecomeAdminForTag(RESOURCE_TAG);
  }

  @Test(expected = StopProcessingException.class)
  public void testIndividualCannotBecomeAdminOnWorkspaces() throws StopProcessingException {
    verifyIndividualTryingToBecomeAdminForTag(WORKSPACE_TAG);
  }

  @Test(expected = StopProcessingException.class)
  public void testIndividualCannotBecomeAdminOnSearchForms() throws StopProcessingException {
    verifyIndividualTryingToBecomeAdminForTag(QUERY_TEMPLATE_TAG);
  }

  @Test(expected = StopProcessingException.class)
  public void testIndividualCannotBecomeAdminOnResultForms() throws StopProcessingException {
    verifyIndividualTryingToBecomeAdminForTag(ATTRIBUTE_GROUP_TAG);
  }

  private void verifyIndividualTryingToBecomeAdminForTag(String metacardTag)
      throws StopProcessingException {
    Metacard before =
        metacardFromAttributes(
            ImmutableMap.of(
                Core.ID,
                METACARD_ID,
                Core.METACARD_OWNER,
                USER_NEWTON,
                Security.ACCESS_INDIVIDUALS,
                ImmutableSet.of(USER_EINSTEIN),
                Core.METACARD_TAGS,
                metacardTag));
    Metacard after =
        metacardFromAttributes(
            ImmutableMap.of(
                Core.ID,
                METACARD_ID,
                Core.METACARD_OWNER,
                USER_NEWTON,
                Security.ACCESS_ADMINISTRATORS,
                ImmutableSet.of(USER_EINSTEIN),
                Core.METACARD_TAGS,
                metacardTag));

    UpdateRequest update = getUpdateRequest(METACARD_ID, after);
    accessPlugin.processPreUpdate(update, ImmutableMap.of(METACARD_ID, before));
  }

  @Test
  public void testIndividualCanBecomeOwnerOnResources() throws StopProcessingException {
    verifyIndividualTryingToBecomeOwnerForTag(RESOURCE_TAG);
  }

  @Test(expected = StopProcessingException.class)
  public void testIndividualCannotBecomeOwnerOnWorkspaces() throws StopProcessingException {
    verifyIndividualTryingToBecomeOwnerForTag(WORKSPACE_TAG);
  }

  @Test(expected = StopProcessingException.class)
  public void testIndividualCannotBecomeOwnerOnSearchForms() throws StopProcessingException {
    verifyIndividualTryingToBecomeOwnerForTag(QUERY_TEMPLATE_TAG);
  }

  @Test(expected = StopProcessingException.class)
  public void testIndividualCannotBecomeOwnerOnResultForms() throws StopProcessingException {
    verifyIndividualTryingToBecomeOwnerForTag(ATTRIBUTE_GROUP_TAG);
  }

  private void verifyIndividualTryingToBecomeOwnerForTag(String metacardTag)
      throws StopProcessingException {
    Metacard before =
        metacardFromAttributes(
            ImmutableMap.of(
                Core.ID,
                METACARD_ID,
                Core.METACARD_OWNER,
                USER_NEWTON,
                Security.ACCESS_INDIVIDUALS,
                ImmutableSet.of(USER_EINSTEIN),
                Core.METACARD_TAGS,
                metacardTag));
    Metacard after =
        metacardFromAttributes(
            ImmutableMap.of(
                Core.ID,
                METACARD_ID,
                Core.METACARD_OWNER,
                USER_EINSTEIN,
                Core.METACARD_TAGS,
                metacardTag));

    UpdateRequest update = getUpdateRequest(METACARD_ID, after);
    accessPlugin.processPreUpdate(update, ImmutableMap.of(METACARD_ID, before));
  }

  @Test
  public void testAdminCanBecomeOwnerOnResources() throws StopProcessingException {
    verifyAdminTryingToBecomeOwnerForTag(RESOURCE_TAG);
  }

  @Test(expected = StopProcessingException.class)
  public void testAdminCannotBecomeOwnerOnWorkspaces() throws StopProcessingException {
    verifyAdminTryingToBecomeOwnerForTag(WORKSPACE_TAG);
  }

  @Test(expected = StopProcessingException.class)
  public void testAdminCannotBecomeOwnerOnSearchForms() throws StopProcessingException {
    verifyAdminTryingToBecomeOwnerForTag(QUERY_TEMPLATE_TAG);
  }

  @Test(expected = StopProcessingException.class)
  public void testAdminCannotBecomeOwnerOnResultForms() throws StopProcessingException {
    verifyAdminTryingToBecomeOwnerForTag(ATTRIBUTE_GROUP_TAG);
  }

  private void verifyAdminTryingToBecomeOwnerForTag(String metacardTag)
      throws StopProcessingException {
    Metacard before =
        metacardFromAttributes(
            ImmutableMap.of(
                Core.ID,
                METACARD_ID,
                Core.METACARD_OWNER,
                USER_NEWTON,
                Security.ACCESS_ADMINISTRATORS,
                ImmutableSet.of(USER_EINSTEIN),
                Core.METACARD_TAGS,
                metacardTag));
    Metacard after =
        metacardFromAttributes(
            ImmutableMap.of(
                Core.ID,
                METACARD_ID,
                Core.METACARD_OWNER,
                USER_EINSTEIN,
                Security.ACCESS_ADMINISTRATORS,
                ImmutableSet.of(USER_EINSTEIN),
                Core.METACARD_TAGS,
                metacardTag));

    UpdateRequest update = getUpdateRequest(METACARD_ID, after);
    accessPlugin.processPreUpdate(update, ImmutableMap.of(METACARD_ID, before));
  }
}
