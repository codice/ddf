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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Security;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.security.Subject;
import ddf.security.SubjectIdentity;
import ddf.security.SubjectUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.opengis.filter.Filter;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin that looks for queries that target collaboration metacards (workspaces, forms, etc) and
 * updates the query filter so part of the collaboration metacard security policy is baked in,
 * removing a significant burden from post-query filtering.
 *
 * <p><b>This plugin does not add any new policy. It only optimizes existing policy by adding the
 * rules to the {@link Filter} so the database does some heavy lifting for us.</b>
 *
 * <p>When identifying a {@link Filter} that only selects collaboration metacards, the current
 * implementation takes a best effort approach on the entire filter as a whole. It does not perform
 * modification on individual subtrees, only the entire filter, or not at all. This leads to some
 * behavioral notes:
 *
 * <ul>
 *   <li>The presence of any metacard tag that does not imply identity as a collaboration metacard
 *       will exclude the entire filter from being modified with policy information because it is
 *       not a safe assumption.
 *   <li>The lack of any metacard tag predicate on the filter will exclude the entire filter from
 *       being modified with policy information because it is not a safe assumption.
 * </ul>
 *
 * Put another way, this plugin will skip processing queries when the user has at least one role
 * found in the {@link #rolesThatCanSeeEverything} set, when no predicates on {@link
 * Core#METACARD_TAGS} were found in the filter, or there was at least one tags predicate on the
 * filter that was not in the {@link #collaborationTags} set.
 */
public class CollaborationMetacardPolicyOptimizationPlugin implements PreQueryPlugin {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(CollaborationMetacardPolicyOptimizationPlugin.class);

  private final FilterBuilder filterBuilder;

  private final SubjectIdentity identity;

  // Define which metacard tags denote a metacard as a "collaboration" or "Intrigue" metacard
  private Set<String> collaborationTags;

  // Define which roles give the user permission to bypass this pre-query filtering
  private Set<String> rolesThatCanSeeEverything;

  public CollaborationMetacardPolicyOptimizationPlugin(
      FilterBuilder filterBuilder, SubjectIdentity identity) {
    this(
        filterBuilder,
        identity,
        ImmutableSet.of(WORKSPACE_TAG, LIST_TAG, QUERY_TEMPLATE_TAG, ATTRIBUTE_GROUP_TAG),
        ImmutableSet.of());
  }

  @VisibleForTesting
  CollaborationMetacardPolicyOptimizationPlugin(
      FilterBuilder filterBuilder,
      SubjectIdentity identity,
      Set<String> collaborationTags,
      Set<String> rolesThatCanSeeEverything) {
    this.filterBuilder = filterBuilder;
    this.identity = identity;
    this.collaborationTags = collaborationTags;
    this.rolesThatCanSeeEverything = rolesThatCanSeeEverything;
  }

  public void setCollaborationTags(Set<String> collaborationTags) {
    this.collaborationTags = collaborationTags;
  }

  public void setRolesThatCanSeeEverything(Set<String> rolesThatCanSeeEverything) {
    this.rolesThatCanSeeEverything = rolesThatCanSeeEverything;
  }

  @Override
  public QueryRequest process(QueryRequest input)
      throws PluginExecutionException, StopProcessingException {
    final String subjectIdentifier = getSubjectIdentifier();
    final Set<String> roles = new HashSet<>(getSubjectRoles());

    if (CollectionUtils.isNotEmpty(Sets.intersection(rolesThatCanSeeEverything, roles))) {
      LOGGER.debug(
          "Will not modify filter; subject ({}) had at least one role [{}] that was exempt [{}]",
          subjectIdentifier,
          roles,
          rolesThatCanSeeEverything);
      return input;
    }

    final TagAggregationVisitor visitor = new TagAggregationVisitor();
    final Query query = input.getQuery();
    query.accept(visitor, null);
    final Set<String> discoveredTags = visitor.getTags();

    if (CollectionUtils.isEmpty(discoveredTags)) {
      LOGGER.debug(
          "Will not modify filter; subject's ({}) query [{}] specified no tags",
          subjectIdentifier,
          query);
      return input;
    }

    Set<String> tagsNotInCollaborationSet =
        Sets.filter(discoveredTags, tag -> !collaborationTags.contains(tag));
    if (CollectionUtils.isNotEmpty(tagsNotInCollaborationSet)) {
      LOGGER.debug(
          "Will not modify filter; subject's ({}) query [{}] referenced tags not in collab set [{}]",
          subjectIdentifier,
          query,
          collaborationTags);
      return input;
    }

    Filter policyBranch = createBakedInSecurityPolicySubset(subjectIdentifier, roles);
    Filter combined = filterBuilder.allOf(query, policyBranch);
    return new QueryRequestImpl(
        new QueryImpl(combined), input.isEnterprise(), input.getSourceIds(), input.getProperties());
  }

  @VisibleForTesting
  String getSubjectIdentifier() {
    final Subject subject = (Subject) SecurityUtils.getSubject();
    return identity.getUniqueIdentifier(subject);
  }

  @VisibleForTesting
  List<String> getSubjectRoles() {
    final Subject subject = (Subject) SecurityUtils.getSubject();
    return SubjectUtils.getAttribute(subject, SubjectUtils.ROLE_CLAIM_URI);
  }

  private Filter createBakedInSecurityPolicySubset(String identifier, Set<String> groups) {
    ImmutableList.Builder<Filter> policyBranch = ImmutableList.builder();
    policyBranch.add(isEqualToText(Core.METACARD_OWNER, identifier));

    policyBranch.add(isEqualToText(Security.ACCESS_INDIVIDUALS, identifier));
    policyBranch.add(isEqualToText(Security.ACCESS_INDIVIDUALS_READ, identifier));
    policyBranch.add(isEqualToText(Security.ACCESS_ADMINISTRATORS, identifier));

    groups.forEach(group -> policyBranch.add(isEqualToText(Security.ACCESS_GROUPS, group)));
    groups.forEach(group -> policyBranch.add(isEqualToText(Security.ACCESS_GROUPS_READ, group)));

    return filterBuilder.anyOf(policyBranch.build());
  }

  private Filter isEqualToText(String attribute, String text) {
    return filterBuilder.attribute(attribute).is().equalTo().text(text);
  }

  private static class TagAggregationVisitor extends DefaultFilterVisitor {
    private final Set<String> tags = new HashSet<>();

    @Override
    public Object visit(PropertyIsLike filter, Object data) {
      predicateAsKeyValuePair(filter.getExpression(), filter.getLiteral())
          .ifPresent(pair -> saveTag(pair.getKey(), pair.getValue()));
      return super.visit(filter, data);
    }

    @Override
    public Object visit(PropertyIsEqualTo filter, Object data) {
      predicateAsKeyValuePair(filter.getExpression1(), filter.getExpression2())
          .ifPresent(pair -> saveTag(pair.getKey(), pair.getValue()));
      return super.visit(filter, data);
    }

    public Set<String> getTags() {
      return tags;
    }

    private void saveTag(String property, String value) {
      if (Core.METACARD_TAGS.equals(property) && StringUtils.isNotBlank(value)) {
        tags.add(value);
      }
    }

    private static Optional<Map.Entry<String, String>> predicateAsKeyValuePair(
        Expression expression1, String literal) {
      final String property = expressionPropertyAsString(expression1);
      if (property == null || literal == null) {
        return Optional.empty();
      }
      return Optional.of(new HashMap.SimpleEntry<>(property, literal));
    }

    private static Optional<Map.Entry<String, String>> predicateAsKeyValuePair(
        Expression expression1, Expression expression2) {
      final String property = expressionPropertyAsString(expression1);
      final String value = expressionLiteralAsString(expression2);
      if (property == null || value == null) {
        return Optional.empty();
      }
      return Optional.of(new HashMap.SimpleEntry<>(property, value));
    }

    @Nullable
    private static String expressionPropertyAsString(Expression expression) {
      if (!(expression instanceof PropertyName)) {
        return null;
      }

      final PropertyName name = (PropertyName) expression;
      return name.getPropertyName();
    }

    @Nullable
    private static String expressionLiteralAsString(Expression expression) {
      if (!(expression instanceof Literal)) {
        return null;
      }

      final Literal literal = (Literal) expression;
      final Object value = literal.getValue();

      if (!(value instanceof String)) {
        return null;
      }

      return (String) value;
    }
  }
}
