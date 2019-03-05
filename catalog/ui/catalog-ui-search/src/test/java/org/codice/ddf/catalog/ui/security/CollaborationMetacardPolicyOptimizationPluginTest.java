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

import static org.codice.ddf.catalog.ui.forms.data.AttributeGroupType.ATTRIBUTE_GROUP_TAG;
import static org.codice.ddf.catalog.ui.forms.data.QueryTemplateType.QUERY_TEMPLATE_TAG;
import static org.codice.ddf.catalog.ui.metacard.workspace.ListMetacardTypeImpl.LIST_TAG;
import static org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceConstants.WORKSPACE_TAG;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Security;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.StopProcessingException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;

public class CollaborationMetacardPolicyOptimizationPluginTest {
  private static final String ROLE_A = "A";

  private static final String ROLE_B = "B";

  private static final String EMAIL_BOB = "bob@example.net";

  private static final String TEXT_CRITERIA = "*criteria*";

  private static final String TEXT_ID = "123456578901";

  private static final String TEXT_WILDCARD = "*";

  private static final Date TEST_DATE = new Date();

  private static final FilterBuilder FILTER_BUILDER = new GeotoolsFilterBuilder();

  private static final DuplicatingFilterVisitor COPYING_VISITOR = new DuplicatingFilterVisitor();

  private CollaborationMetacardPolicyOptimizationPlugin plugin;

  @Before
  public void setUp() {
    plugin =
        new CollaborationMetacardPolicyOptimizationPluginUnderTest(
            new GeotoolsFilterBuilder(), // don't assert against things made by the same builder,
            ROLE_A,
            ROLE_B);
  }

  @Test
  public void testUserWithRoleThatCanSeeEverythingHasFilterLeftAlone()
      throws PluginExecutionException, StopProcessingException {
    plugin =
        new CollaborationMetacardPolicyOptimizationPluginUnderTest(
            new GeotoolsFilterBuilder(),
            ImmutableSet.of(WORKSPACE_TAG, LIST_TAG, QUERY_TEMPLATE_TAG, ATTRIBUTE_GROUP_TAG),
            ImmutableSet.of(ROLE_A),
            ImmutableList.of(ROLE_A, ROLE_B));
    Filter filter = FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(WORKSPACE_TAG);
    verifyPluginDoesNotAlterTheFilter(filter);
  }

  @Test
  public void testTypicalQueryForAllMetacardsOfSingleTypeUsingLikeIsOptimized()
      throws PluginExecutionException, StopProcessingException {
    Filter filter = FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(WORKSPACE_TAG);
    verifyPluginAddsSecurityBranchToFilter(filter);
  }

  @Test
  public void testTypicalQueryForAllMetacardsOfSingleTypeUsingEqualToIsOptimized()
      throws PluginExecutionException, StopProcessingException {
    Filter filter = FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(WORKSPACE_TAG);
    verifyPluginAddsSecurityBranchToFilter(filter);
  }

  @Test
  public void testTypicalQueryForSpecificCaseOfSingleTypeIsOptimized()
      throws PluginExecutionException, StopProcessingException {
    Filter typicalQueryForSpecificCaseOfSingleType =
        FILTER_BUILDER.allOf(
            FILTER_BUILDER.attribute(Core.TITLE).is().like().text(TEXT_CRITERIA),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(WORKSPACE_TAG));
    verifyPluginAddsSecurityBranchToFilter(typicalQueryForSpecificCaseOfSingleType);
  }

  @Test
  public void testTypicalQueryForSpecificCaseOfMultipleTypesIsOptimized()
      throws PluginExecutionException, StopProcessingException {
    Filter typicalQueryForSpecificCaseOfMultipleTypes =
        FILTER_BUILDER.allOf(
            FILTER_BUILDER.attribute(Core.TITLE).is().like().text(TEXT_CRITERIA),
            FILTER_BUILDER.anyOf(
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(WORKSPACE_TAG),
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(QUERY_TEMPLATE_TAG)));
    verifyPluginAddsSecurityBranchToFilter(typicalQueryForSpecificCaseOfMultipleTypes);
  }

  @Test
  public void testDisjointQueryForSpecificCaseOfManyTypesIsOptimized()
      throws PluginExecutionException, StopProcessingException {
    Filter disjointQuery =
        FILTER_BUILDER.anyOf(
            FILTER_BUILDER.allOf(
                FILTER_BUILDER.attribute(Core.CREATED).is().before().date(TEST_DATE),
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(WORKSPACE_TAG)),
            FILTER_BUILDER.allOf(
                FILTER_BUILDER.attribute(Core.TITLE).is().like().text(TEXT_CRITERIA),
                FILTER_BUILDER
                    .attribute(Core.METACARD_TAGS)
                    .is()
                    .equalTo()
                    .text(QUERY_TEMPLATE_TAG)),
            FILTER_BUILDER.allOf(
                FILTER_BUILDER.attribute(Core.ID).is().like().text(TEXT_ID),
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(LIST_TAG)));
    verifyPluginAddsSecurityBranchToFilter(disjointQuery);
  }

  @Test
  public void testTypicalQueryForAllResourcesByUsingWildcardIsLeftAlone()
      throws PluginExecutionException, StopProcessingException {
    Filter typicalQueryForAllResourcesWildcard =
        FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TEXT_WILDCARD);
    verifyPluginDoesNotAlterTheFilter(typicalQueryForAllResourcesWildcard);
  }

  @Test
  public void testTypicalQueryForAllResourcesByOmittingTagsAttributeIsLeftAlone()
      throws PluginExecutionException, StopProcessingException {
    Filter typicalQueryForAllResourcesNoTags =
        FILTER_BUILDER.attribute(Metacard.ANY_TEXT).is().like().text(TEXT_WILDCARD);
    verifyPluginDoesNotAlterTheFilter(typicalQueryForAllResourcesNoTags);
  }

  @Test
  public void testTypicalQueryForSpecificCaseOfResourcesIsLeftAlone()
      throws PluginExecutionException, StopProcessingException {
    Filter typicalQueryForSpecificResource =
        FILTER_BUILDER.anyOf(
            FILTER_BUILDER.allOf(
                FILTER_BUILDER.attribute(Core.TITLE).is().like().text(TEXT_CRITERIA),
                FILTER_BUILDER.attribute(Core.CREATED).is().before().date(TEST_DATE)),
            FILTER_BUILDER.allOf(
                FILTER_BUILDER.attribute(Core.DESCRIPTION).is().like().text(TEXT_CRITERIA),
                FILTER_BUILDER.attribute(Core.MODIFIED).is().before().date(TEST_DATE)));
    verifyPluginDoesNotAlterTheFilter(typicalQueryForSpecificResource);
  }

  @Test
  public void testOddQueryAcrossMultipleTypesIsLeftAlone()
      throws PluginExecutionException, StopProcessingException {
    Filter filter =
        FILTER_BUILDER.anyOf(
            FILTER_BUILDER.allOf(
                FILTER_BUILDER.attribute(Core.CREATED).is().before().date(TEST_DATE),
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(WORKSPACE_TAG)),
            FILTER_BUILDER.allOf(
                FILTER_BUILDER.attribute(Core.TITLE).is().like().text(TEXT_CRITERIA),
                FILTER_BUILDER
                    .attribute(Core.METACARD_TAGS)
                    .is()
                    .equalTo()
                    .text(QUERY_TEMPLATE_TAG)),
            FILTER_BUILDER.allOf(
                FILTER_BUILDER.attribute(Core.ID).is().like().text(TEXT_ID),
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text("resource")));
    verifyPluginDoesNotAlterTheFilter(filter);
  }

  private void verifyPluginAddsSecurityBranchToFilter(Filter inputFilter)
      throws PluginExecutionException, StopProcessingException {
    QueryRequest request = plugin.process(new QueryRequestImpl(new QueryImpl(inputFilter)));
    assertThat(
        asGeotoolsComparableFilter(request.getQuery()),
        is(
            equalTo(
                FILTER_BUILDER.allOf(
                    inputFilter,
                    FILTER_BUILDER.anyOf(
                        FILTER_BUILDER
                            .attribute(Core.METACARD_OWNER)
                            .is()
                            .equalTo()
                            .text(EMAIL_BOB),
                        FILTER_BUILDER
                            .attribute(Security.ACCESS_INDIVIDUALS)
                            .is()
                            .equalTo()
                            .text(EMAIL_BOB),
                        FILTER_BUILDER
                            .attribute(Security.ACCESS_INDIVIDUALS_READ)
                            .is()
                            .equalTo()
                            .text(EMAIL_BOB),
                        FILTER_BUILDER
                            .attribute(Security.ACCESS_ADMINISTRATORS)
                            .is()
                            .equalTo()
                            .text(EMAIL_BOB),
                        FILTER_BUILDER
                            .attribute(Security.ACCESS_GROUPS)
                            .is()
                            .equalTo()
                            .text(ROLE_A),
                        FILTER_BUILDER
                            .attribute(Security.ACCESS_GROUPS_READ)
                            .is()
                            .equalTo()
                            .text(ROLE_A),
                        FILTER_BUILDER
                            .attribute(Security.ACCESS_GROUPS)
                            .is()
                            .equalTo()
                            .text(ROLE_B),
                        FILTER_BUILDER
                            .attribute(Security.ACCESS_GROUPS_READ)
                            .is()
                            .equalTo()
                            .text(ROLE_B))))));
  }

  private void verifyPluginDoesNotAlterTheFilter(Filter inputFilter)
      throws PluginExecutionException, StopProcessingException {
    QueryRequest request = plugin.process(new QueryRequestImpl(new QueryImpl(inputFilter)));
    assertThat(asGeotoolsComparableFilter(request.getQuery()), is(equalTo(inputFilter)));
  }

  private static Filter asGeotoolsComparableFilter(Query query) {
    return (Filter) query.accept(COPYING_VISITOR, null);
  }

  private static class CollaborationMetacardPolicyOptimizationPluginUnderTest
      extends CollaborationMetacardPolicyOptimizationPlugin {
    private final List<String> subjectRoles;

    private CollaborationMetacardPolicyOptimizationPluginUnderTest(
        FilterBuilder filterBuilder, String... subjectRoles) {
      super(filterBuilder, null /* Only method interacting with this ref gets overridden */);
      this.subjectRoles = Arrays.asList(subjectRoles);
    }

    private CollaborationMetacardPolicyOptimizationPluginUnderTest(
        FilterBuilder filterBuilder,
        Set<String> collaborationTags,
        Set<String> rolesThatCanSeeEverything,
        List<String> subjectRoles) {
      super(filterBuilder, null, collaborationTags, rolesThatCanSeeEverything);
      this.subjectRoles = subjectRoles;
    }

    @Override
    String getSubjectIdentifier() {
      return EMAIL_BOB;
    }

    @Override
    List<String> getSubjectRoles() {
      return subjectRoles;
    }
  }
}
