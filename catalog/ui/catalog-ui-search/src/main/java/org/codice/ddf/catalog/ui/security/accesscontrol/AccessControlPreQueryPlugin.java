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

import static org.codice.ddf.catalog.ui.security.Constants.SYSTEM_TEMPLATE;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;
import org.apache.shiro.SecurityUtils;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin that looks for queries that target access controlled metacards (workspaces, forms, etc)
 * and updates the query filter so part of the access control security policy is baked in, removing
 * a significant burden from post-query filtering.
 *
 * <p><b>This plugin does not add any new policy. It only optimizes existing policy by adding the
 * rules to the {@link Filter} so the database does some heavy lifting for us.</b>
 *
 * <p>When identifying a {@link Filter} that only selects access controlled metacards, the current
 * implementation takes a best effort approach on the entire filter as a whole. It does not perform
 * modification on individual subtrees, only the entire filter, or not at all.
 *
 * <p>This plugin will skip processing queries when any of the below are true:
 *
 * <ul>
 *   <li>The user has the {@link AccessControlSecurityConfiguration} group.
 *   <li>When no predicates operating on {@link Core#METACARD_TAGS} were found in the filter.
 *   <li>There was at least one tags predicate on the filter that was not in the {@link
 *       AccessControlTags}.
 * </ul>
 *
 * This plugin will ignore {@link Core#METACARD_TAGS} present on filter predicates if any of the
 * below are true:
 *
 * <ul>
 *   <li>The predicate is a negative check, such as {@link org.opengis.filter.PropertyIsNotEqualTo}.
 *   <li>The predicate falls under the logical composite {@link org.opengis.filter.Not} operator.
 * </ul>
 */
public class AccessControlPreQueryPlugin implements PreQueryPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(AccessControlPreQueryPlugin.class);

  private final FilterBuilder filterBuilder;

  private final SubjectIdentity identity;

  private final AccessControlTags tagSet;

  private final AccessControlSecurityConfiguration configuration;

  public AccessControlPreQueryPlugin(
      FilterBuilder filterBuilder,
      SubjectIdentity identity,
      AccessControlTags tagSet,
      AccessControlSecurityConfiguration configuration) {
    this.filterBuilder = filterBuilder;
    this.identity = identity;
    this.tagSet = tagSet;
    this.configuration = configuration;
  }

  @Override
  public QueryRequest process(QueryRequest input)
      throws PluginExecutionException, StopProcessingException {
    final String subjectIdentifier = getSubjectIdentifier();
    final Set<String> groups = new HashSet<>(getSubjectGroups());
    final String groupThatCanSeeEverything = configuration.getSystemUserAttributeValue();

    if (groups.contains(groupThatCanSeeEverything)) {
      LOGGER.debug(
          "Will not modify filter; subject ({}) had at least one group [{}] that was exempt [{}]",
          subjectIdentifier,
          groups,
          groupThatCanSeeEverything);
      return input;
    }

    final Query query = input.getQuery();
    final SimplifyingFilterVisitor simpleVisitor = new SimplifyingFilterVisitor();
    final Filter simplified = (Filter) query.accept(simpleVisitor, null);

    LOGGER.trace("Simplified filter [{}]", simplified);

    final TagAggregationVisitor tagVisitor = new TagAggregationVisitor();
    simplified.accept(tagVisitor, null);

    final Set<String> discoveredTags = tagVisitor.getTags();
    if (CollectionUtils.isEmpty(discoveredTags)) {
      LOGGER.debug(
          "Will not modify filter; subject's ({}) query [{}] did not completely bind results to tags",
          subjectIdentifier,
          query);
      return input;
    }

    final Set<String> tagsThatAreAccessControlled = tagSet.getAccessControlledTags();
    final Set<String> tagsNotAccessControlled =
        Sets.difference(discoveredTags, tagsThatAreAccessControlled);
    if (CollectionUtils.isNotEmpty(tagsNotAccessControlled)) {
      LOGGER.debug(
          "Will not modify filter; subject's ({}) query [{}] referenced tags that are not in access controlled set [{}]",
          subjectIdentifier,
          query,
          tagsThatAreAccessControlled);
      return input;
    }

    final Filter policyBranch = createSecurityPolicySubset(subjectIdentifier, groups);
    LOGGER.debug("Adding access control policy to the query filter [{}]", policyBranch);

    final Filter combined = filterBuilder.allOf(query, policyBranch);
    return new QueryRequestImpl(
        new QueryImpl(combined), input.isEnterprise(), input.getSourceIds(), input.getProperties());
  }

  @VisibleForTesting
  String getSubjectIdentifier() {
    final Subject subject = (Subject) SecurityUtils.getSubject();
    final String uniqueIdentifier = identity.getUniqueIdentifier(subject);
    LOGGER.debug("Obtained user's unique identifier: {}", uniqueIdentifier);
    return uniqueIdentifier;
  }

  @VisibleForTesting
  List<String> getSubjectGroups() {
    final Subject subject = (Subject) SecurityUtils.getSubject();
    final List<String> groups =
        SubjectUtils.getAttribute(subject, configuration.getSystemUserAttribute());
    LOGGER.debug("Obtained user's groups: {}", groups);
    return groups;
  }

  private Filter createSecurityPolicySubset(String identifier, Set<String> groups) {
    final ImmutableList.Builder<Filter> policyBranch = ImmutableList.builder();
    policyBranch.add(isEqualToText(Core.METACARD_OWNER, identifier));
    policyBranch.add(isEqualToText(Core.METACARD_TAGS, SYSTEM_TEMPLATE));

    policyBranch.add(isEqualToText(Security.ACCESS_INDIVIDUALS, identifier));
    policyBranch.add(isEqualToText(Security.ACCESS_INDIVIDUALS_READ, identifier));
    policyBranch.add(isEqualToText(Security.ACCESS_ADMINISTRATORS, identifier));

    groups.forEach(group -> policyBranch.add(isEqualToText(Security.ACCESS_GROUPS, group)));
    groups.forEach(group -> policyBranch.add(isEqualToText(Security.ACCESS_GROUPS_READ, group)));

    return filterBuilder.anyOf(policyBranch.build());
  }

  private Filter isEqualToText(String attribute, String text) {
    LOGGER.trace("Adding \"{}\" = '{}' to filter", attribute, text);
    return filterBuilder.attribute(attribute).is().equalTo().text(text);
  }
}
