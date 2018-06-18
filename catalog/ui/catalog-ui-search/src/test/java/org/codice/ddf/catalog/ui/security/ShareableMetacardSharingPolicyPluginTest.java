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
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.types.Core;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.PolicyResponse;
import java.util.Collections;
import java.util.Map;
import org.codice.ddf.catalog.ui.forms.data.AttributeGroupType;
import org.codice.ddf.catalog.ui.forms.data.QueryTemplateType;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceConstants;
import org.junit.Before;
import org.junit.Test;

public class ShareableMetacardSharingPolicyPluginTest {

  private Map properties;

  private PolicyPlugin plugin;

  private static final String EMAIL = "a@b.c";

  @Before
  public void setUp() {
    properties = mock(Map.class);
    plugin = new ShareableMetacardSharingPolicyPlugin();
  }

  @Test
  public void testOwnerOnCreate() throws Exception {
    ShareableMetacardImpl shareableMetacard = new ShareableMetacardImpl();
    shareableMetacard.setTags(Collections.singleton(WorkspaceConstants.WORKSPACE_TAG));
    shareableMetacard.setOwner(EMAIL);
    PolicyResponse response = plugin.processPreCreate(shareableMetacard, properties);
    assertThat(response.itemPolicy(), is(Collections.emptyMap()));
  }

  @Test
  public void testOwnerOnUpdate() throws Exception {
    ShareableMetacardImpl shareableMetacard = new ShareableMetacardImpl();
    shareableMetacard.setTags(Collections.singleton(WorkspaceConstants.WORKSPACE_TAG));
    shareableMetacard.setOwner(EMAIL);
    PolicyResponse response = plugin.processPreUpdate(shareableMetacard, properties);
    assertThat(
        response.itemPolicy(),
        is(
            ImmutableMap.of(
                Core.METACARD_OWNER,
                ImmutableSet.of(EMAIL),
                AttributeGroupType.ATTRIBUTE_GROUP_TAG,
                Collections.singleton(AttributeGroupType.ATTRIBUTE_GROUP_TAG),
                WorkspaceConstants.WORKSPACE_TAG,
                Collections.singleton(WorkspaceConstants.WORKSPACE_TAG),
                QueryTemplateType.QUERY_TEMPLATE_TAG,
                Collections.singleton(QueryTemplateType.QUERY_TEMPLATE_TAG))));
  }
}
